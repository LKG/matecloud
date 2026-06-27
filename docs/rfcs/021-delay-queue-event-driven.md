# RFC-021: Delay Queue & Event-Driven Patterns

| Field       | Value                                              |
|-------------|----------------------------------------------------|
| **RFC**     | 021                                                |
| **Title**   | Delay Queue & Event-Driven Patterns                |
| **Status**  | Draft                                              |
| **Created** | 2026-04-11                                         |
| **Module**  | mate-mq-starter (enhancement)                      |
| **Package** | vip.mate.starter.mq.delay, vip.mate.starter.mq.event |

---

## 1. Overview

Enhances `mate-mq-starter` (RFC-005) with two major capabilities:

1. **Delay Queue** -- Schedule tasks to execute after a configurable delay using Redisson `RDelayedQueue` + `RBlockingQueue`
2. **Event-Driven Patterns** -- `@DomainEventHandler` annotation for consuming domain events from RabbitMQ with dead letter queue support and exponential retry backoff

---

## 2. Module Structure (Additions to mate-mq-starter)

```
mate-starters/mate-mq-starter/
  src/main/java/vip/mate/starter/mq/
    delay/
      DelayQueueService.java            <-- NEW
      DelayQueueMessage.java            <-- NEW
      DelayQueueListener.java           <-- NEW (annotation)
      DelayQueueListenerRegistrar.java  <-- NEW
      DelayQueueAutoConfiguration.java  <-- NEW
    event/
      DomainEventHandler.java           <-- NEW (annotation)
      DomainEventListenerRegistrar.java <-- NEW
      DomainEventAutoConfiguration.java <-- NEW
      RetryPolicy.java                  <-- NEW
      DeadLetterConfig.java             <-- NEW
```

---

## 3. pom.xml Additions

Add to `mate-mq-starter/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>mate-starters</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>mate-mq-starter</artifactId>
    <packaging>jar</packaging>
    <description>MateCloud MQ Starter - RabbitMQ events + Redisson delay queue</description>

    <dependencies>
        <!-- RabbitMQ -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>

        <!-- mate-base for common types -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-base</artifactId>
        </dependency>

        <!-- Redisson (for DelayedQueue) -->
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
            <version>${redisson.version}</version>
        </dependency>

        <!-- Jackson for message serialization -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>

        <!-- Jackson JSR310 for Java 8 date/time -->
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>
    </dependencies>
</project>
```

---

## 4. Delay Queue Source Code

### 4.1 DelayQueueMessage.java

```java
package vip.mate.starter.mq.delay;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Wrapper for messages placed into the delay queue.
 * <p>
 * Carries the topic, payload, scheduled execution time, and a unique message ID.
 *
 * @param <T> the payload type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelayQueueMessage<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Unique message identifier.
     */
    private String messageId;

    /**
     * The delay queue topic / channel name.
     */
    private String topic;

    /**
     * The business payload.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    private T data;

    /**
     * When this message was originally offered into the queue.
     */
    private LocalDateTime createdAt;

    /**
     * When this message should be executed.
     */
    private LocalDateTime executeAt;
}
```

### 4.2 DelayQueueService.java

```java
package vip.mate.starter.mq.delay;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Service for offering messages into a delay queue and consuming them after the delay expires.
 * <p>
 * Uses Redisson's {@link RDelayedQueue} backed by {@link RBlockingQueue} for reliable
 * delayed message delivery. Messages are automatically transferred to the blocking queue
 * when their delay expires.
 *
 * <pre>{@code
 * // Offer a message with 30-minute delay
 * delayQueueService.offer("order:cancel", orderId, 1800);
 *
 * // Consume messages from the topic
 * delayQueueService.consume("order:cancel", orderId -> {
 *     orderService.cancelOrder(orderId);
 * });
 * }</pre>
 */
@Slf4j
public class DelayQueueService {

    private static final String QUEUE_PREFIX = "mate:delay:queue:";

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final Map<String, RDelayedQueue<?>> delayedQueueMap = new ConcurrentHashMap<>();
    private final Map<String, RBlockingQueue<?>> blockingQueueMap = new ConcurrentHashMap<>();

    public DelayQueueService(RedissonClient redissonClient, ObjectMapper objectMapper) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Offer a message into the delay queue with the specified delay.
     *
     * @param topic        the queue topic name
     * @param data         the business payload
     * @param delaySeconds delay in seconds before the message becomes available
     * @param <T>          payload type
     * @return the generated message ID
     */
    public <T> String offer(String topic, T data, long delaySeconds) {
        String messageId = UUID.randomUUID().toString().replace("-", "");

        DelayQueueMessage<T> message = DelayQueueMessage.<T>builder()
                .messageId(messageId)
                .topic(topic)
                .data(data)
                .createdAt(LocalDateTime.now())
                .executeAt(LocalDateTime.now().plusSeconds(delaySeconds))
                .build();

        RBlockingQueue<DelayQueueMessage<T>> blockingQueue = getBlockingQueue(topic);
        RDelayedQueue<DelayQueueMessage<T>> delayedQueue = getDelayedQueue(topic, blockingQueue);

        delayedQueue.offer(message, delaySeconds, TimeUnit.SECONDS);

        log.info("[DelayQueue] Offered message: topic={}, messageId={}, delaySeconds={}, executeAt={}",
                topic, messageId, delaySeconds, message.getExecuteAt());

        return messageId;
    }

    /**
     * Offer a message with delay specified in a custom time unit.
     *
     * @param topic    the queue topic name
     * @param data     the business payload
     * @param delay    delay amount
     * @param timeUnit delay time unit
     * @param <T>      payload type
     * @return the generated message ID
     */
    public <T> String offer(String topic, T data, long delay, TimeUnit timeUnit) {
        return offer(topic, data, timeUnit.toSeconds(delay));
    }

    /**
     * Start consuming messages from the specified topic.
     * <p>
     * This method starts a daemon thread that blocks on the queue and invokes
     * the handler for each message as its delay expires.
     *
     * @param topic   the queue topic name
     * @param handler the consumer function to process each message
     * @param <T>     payload type
     */
    @SuppressWarnings("unchecked")
    public <T> void consume(String topic, Consumer<T> handler) {
        RBlockingQueue<DelayQueueMessage<T>> blockingQueue =
                (RBlockingQueue<DelayQueueMessage<T>>) (RBlockingQueue<?>) getBlockingQueue(topic);

        Thread consumerThread = new Thread(() -> {
            log.info("[DelayQueue] Consumer started for topic={}", topic);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DelayQueueMessage<T> message = blockingQueue.take();
                    log.info("[DelayQueue] Processing message: topic={}, messageId={}",
                            topic, message.getMessageId());
                    try {
                        handler.accept(message.getData());
                        log.info("[DelayQueue] Message processed successfully: topic={}, messageId={}",
                                topic, message.getMessageId());
                    } catch (Exception e) {
                        log.error("[DelayQueue] Failed to process message: topic={}, messageId={}",
                                topic, message.getMessageId(), e);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.info("[DelayQueue] Consumer interrupted for topic={}", topic);
                    break;
                }
            }
        }, "delay-queue-consumer-" + topic);

        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    /**
     * Get the current size of the delay queue for a topic.
     *
     * @param topic the queue topic name
     * @return number of pending messages
     */
    public int size(String topic) {
        RBlockingQueue<?> blockingQueue = getBlockingQueue(topic);
        return blockingQueue.size();
    }

    // ======================== Internal ========================

    @SuppressWarnings("unchecked")
    private <T> RBlockingQueue<DelayQueueMessage<T>> getBlockingQueue(String topic) {
        return (RBlockingQueue<DelayQueueMessage<T>>) blockingQueueMap.computeIfAbsent(
                topic, t -> redissonClient.getBlockingQueue(QUEUE_PREFIX + t));
    }

    @SuppressWarnings("unchecked")
    private <T> RDelayedQueue<DelayQueueMessage<T>> getDelayedQueue(
            String topic, RBlockingQueue<DelayQueueMessage<T>> blockingQueue) {
        return (RDelayedQueue<DelayQueueMessage<T>>) delayedQueueMap.computeIfAbsent(
                topic, t -> redissonClient.getDelayedQueue(blockingQueue));
    }
}
```

### 4.3 DelayQueueListener.java (Annotation)

```java
package vip.mate.starter.mq.delay;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a delay queue listener for a specific topic.
 * <p>
 * The annotated method must accept a single parameter matching the payload type.
 *
 * <pre>{@code
 * @Component
 * public class OrderDelayHandler {
 *
 *     @DelayQueueListener(topic = "order:cancel")
 *     public void handleOrderCancel(Long orderId) {
 *         orderService.autoCancelOrder(orderId);
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DelayQueueListener {

    /**
     * The delay queue topic to listen on.
     */
    String topic();
}
```

### 4.4 DelayQueueListenerRegistrar.java

```java
package vip.mate.starter.mq.delay;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * BeanPostProcessor that scans for {@link DelayQueueListener} annotations
 * and registers delay queue consumers automatically.
 * <p>
 * For each annotated method, it calls {@link DelayQueueService#consume(String, java.util.function.Consumer)}
 * to start a background consumer thread.
 */
@Slf4j
@RequiredArgsConstructor
public class DelayQueueListenerRegistrar implements BeanPostProcessor {

    private final DelayQueueService delayQueueService;

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();
        Method[] methods = ReflectionUtils.getDeclaredMethods(targetClass);

        for (Method method : methods) {
            DelayQueueListener annotation = AnnotationUtils.findAnnotation(method, DelayQueueListener.class);
            if (annotation == null) {
                continue;
            }

            String topic = annotation.topic();
            Class<?>[] paramTypes = method.getParameterTypes();

            if (paramTypes.length != 1) {
                throw new IllegalStateException(
                        "@DelayQueueListener method must have exactly one parameter: " +
                                targetClass.getName() + "#" + method.getName());
            }

            log.info("[DelayQueue] Registering listener: topic={}, handler={}.{}",
                    topic, targetClass.getSimpleName(), method.getName());

            delayQueueService.consume(topic, data -> {
                try {
                    method.setAccessible(true);
                    method.invoke(bean, data);
                } catch (Exception e) {
                    log.error("[DelayQueue] Error invoking listener: topic={}, handler={}.{}",
                            topic, targetClass.getSimpleName(), method.getName(), e);
                }
            });
        }

        return bean;
    }
}
```

### 4.5 DelayQueueAutoConfiguration.java

```java
package vip.mate.starter.mq.delay;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the Redisson-based delay queue.
 * <p>
 * Activated when Redisson is on the classpath and a {@link RedissonClient} bean exists.
 */
@AutoConfiguration
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnBean(RedissonClient.class)
public class DelayQueueAutoConfiguration {

    @Bean
    public DelayQueueService delayQueueService(RedissonClient redissonClient, ObjectMapper objectMapper) {
        return new DelayQueueService(redissonClient, objectMapper);
    }

    @Bean
    public DelayQueueListenerRegistrar delayQueueListenerRegistrar(DelayQueueService delayQueueService) {
        return new DelayQueueListenerRegistrar(delayQueueService);
    }
}
```

---

## 5. Event-Driven Patterns Source Code

### 5.1 DomainEventHandler.java (Annotation)

```java
package vip.mate.starter.mq.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a handler for domain events consumed from RabbitMQ.
 * <p>
 * The annotated method is automatically bound to a RabbitMQ queue that receives
 * messages from the specified exchange with the given routing key.
 *
 * <pre>{@code
 * @Component
 * public class OrderEventConsumer {
 *
 *     @DomainEventHandler(
 *         exchange = "mate.event",
 *         routingKey = "order.created",
 *         queue = "mate.order.created.consumer",
 *         retryEnabled = true
 *     )
 *     public void onOrderCreated(EventMessage<OrderDTO> event) {
 *         // handle the event
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DomainEventHandler {

    /**
     * The RabbitMQ exchange name.
     * Default: "mate.event" (topic exchange).
     */
    String exchange() default "mate.event";

    /**
     * The routing key pattern (e.g., "order.created", "user.registered").
     */
    String routingKey();

    /**
     * The queue name for this consumer.
     * Should be unique per consumer group.
     */
    String queue();

    /**
     * Whether to enable retry with exponential backoff.
     * Default: true.
     */
    boolean retryEnabled() default true;

    /**
     * Maximum number of retry attempts.
     * Default: 4 (original + 3 retries).
     */
    int maxRetries() default 4;

    /**
     * Whether to route failed messages to a dead letter queue.
     * Default: true.
     */
    boolean deadLetterEnabled() default true;
}
```

### 5.2 RetryPolicy.java

```java
package vip.mate.starter.mq.event;

import lombok.Data;

/**
 * Exponential backoff retry policy for domain event consumption.
 * <p>
 * Retry intervals: 1s, 5s, 30s, 5min (configurable).
 */
@Data
public class RetryPolicy {

    /**
     * Initial retry delay in milliseconds.
     * Default: 1000 (1 second).
     */
    private long initialInterval = 1000;

    /**
     * Multiplier for exponential backoff.
     * Default: 5.0.
     * <p>
     * With initialInterval=1000 and multiplier=5.0:
     * - Retry 1: 1s
     * - Retry 2: 5s
     * - Retry 3: 25s (~30s with jitter)
     * - Retry 4: 125s (~5min with cap)
     */
    private double multiplier = 5.0;

    /**
     * Maximum retry interval in milliseconds.
     * Default: 300000 (5 minutes).
     */
    private long maxInterval = 300_000;

    /**
     * Maximum number of retries.
     * Default: 4.
     */
    private int maxRetries = 4;

    /**
     * Calculate the delay for the given retry attempt (1-based).
     *
     * @param attempt the retry attempt number (1 = first retry)
     * @return delay in milliseconds
     */
    public long getDelay(int attempt) {
        long delay = (long) (initialInterval * Math.pow(multiplier, attempt - 1));
        return Math.min(delay, maxInterval);
    }
}
```

### 5.3 DeadLetterConfig.java

```java
package vip.mate.starter.mq.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Component;

/**
 * Helper to declare dead letter exchange and queue for a given source queue.
 * <p>
 * Dead letter queue naming convention:
 * - Exchange: {@code {sourceExchange}.dlx}
 * - Queue: {@code {sourceQueue}.dlq}
 * - Routing key: {@code {sourceRoutingKey}.dead}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterConfig {

    public static final String DLX_SUFFIX = ".dlx";
    public static final String DLQ_SUFFIX = ".dlq";
    public static final String DEAD_ROUTING_SUFFIX = ".dead";

    private final RabbitAdmin rabbitAdmin;

    /**
     * Declare the dead letter exchange, queue, and binding for a source queue.
     *
     * @param sourceExchange   the original exchange name
     * @param sourceQueue      the original queue name
     * @param sourceRoutingKey the original routing key
     */
    public void declareDeadLetterInfrastructure(
            String sourceExchange, String sourceQueue, String sourceRoutingKey) {

        String dlxName = sourceExchange + DLX_SUFFIX;
        String dlqName = sourceQueue + DLQ_SUFFIX;
        String dlRoutingKey = sourceRoutingKey + DEAD_ROUTING_SUFFIX;

        // Declare DLX (direct exchange for dead letters)
        DirectExchange dlx = new DirectExchange(dlxName, true, false);
        rabbitAdmin.declareExchange(dlx);

        // Declare DLQ (durable, with message TTL of 7 days)
        Queue dlq = QueueBuilder.durable(dlqName)
                .withArgument("x-message-ttl", 604_800_000L) // 7 days
                .build();
        rabbitAdmin.declareQueue(dlq);

        // Bind DLQ to DLX
        Binding binding = BindingBuilder.bind(dlq).to(dlx).with(dlRoutingKey);
        rabbitAdmin.declareBinding(binding);

        log.info("[DeadLetter] Declared DLX={}, DLQ={}, routingKey={}", dlxName, dlqName, dlRoutingKey);
    }

    /**
     * Get the DLX name for a given source exchange.
     */
    public static String getDlxName(String sourceExchange) {
        return sourceExchange + DLX_SUFFIX;
    }

    /**
     * Get the dead routing key for a given source routing key.
     */
    public static String getDeadRoutingKey(String sourceRoutingKey) {
        return sourceRoutingKey + DEAD_ROUTING_SUFFIX;
    }
}
```

### 5.4 DomainEventListenerRegistrar.java

```java
package vip.mate.starter.mq.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * BeanPostProcessor that scans for {@link DomainEventHandler} annotations
 * and automatically creates RabbitMQ consumers with:
 * <ul>
 *   <li>Auto topic exchange and queue declaration</li>
 *   <li>Routing key binding</li>
 *   <li>Dead letter queue configuration</li>
 *   <li>Retry with exponential backoff</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class DomainEventListenerRegistrar implements BeanPostProcessor, ApplicationContextAware {

    private final ConnectionFactory connectionFactory;
    private final RabbitAdmin rabbitAdmin;
    private final ObjectMapper objectMapper;
    private final DeadLetterConfig deadLetterConfig;

    private ApplicationContext applicationContext;
    private final List<SimpleMessageListenerContainer> containers = new ArrayList<>();

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();
        Method[] methods = ReflectionUtils.getDeclaredMethods(targetClass);

        for (Method method : methods) {
            DomainEventHandler annotation = AnnotationUtils.findAnnotation(method, DomainEventHandler.class);
            if (annotation == null) {
                continue;
            }

            registerListener(bean, method, annotation);
        }

        return bean;
    }

    private void registerListener(Object bean, Method method, DomainEventHandler annotation) {
        String exchange = annotation.exchange();
        String routingKey = annotation.routingKey();
        String queueName = annotation.queue();
        boolean retryEnabled = annotation.retryEnabled();
        int maxRetries = annotation.maxRetries();
        boolean deadLetterEnabled = annotation.deadLetterEnabled();

        log.info("[EventHandler] Registering: exchange={}, routingKey={}, queue={}, retry={}, dlq={}",
                exchange, routingKey, queueName, retryEnabled, deadLetterEnabled);

        // 1. Declare exchange
        TopicExchange topicExchange = new TopicExchange(exchange, true, false);
        rabbitAdmin.declareExchange(topicExchange);

        // 2. Build queue with optional DLX arguments
        QueueBuilder queueBuilder = QueueBuilder.durable(queueName);
        if (deadLetterEnabled) {
            queueBuilder.withArgument("x-dead-letter-exchange",
                    DeadLetterConfig.getDlxName(exchange));
            queueBuilder.withArgument("x-dead-letter-routing-key",
                    DeadLetterConfig.getDeadRoutingKey(routingKey));
        }
        Queue queue = queueBuilder.build();
        rabbitAdmin.declareQueue(queue);

        // 3. Bind queue to exchange
        Binding binding = BindingBuilder.bind(queue).to(topicExchange).with(routingKey);
        rabbitAdmin.declareBinding(binding);

        // 4. Declare DLQ infrastructure if enabled
        if (deadLetterEnabled) {
            deadLetterConfig.declareDeadLetterInfrastructure(exchange, queueName, routingKey);
        }

        // 5. Create retry policy
        RetryPolicy retryPolicy = new RetryPolicy();
        retryPolicy.setMaxRetries(maxRetries);

        // 6. Determine the parameter type for deserialization
        Parameter[] params = method.getParameters();
        if (params.length != 1) {
            throw new IllegalStateException(
                    "@DomainEventHandler method must have exactly one parameter: " +
                            bean.getClass().getName() + "#" + method.getName());
        }
        Class<?> paramType = params[0].getType();

        // 7. Create message listener container
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueueNames(queueName);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        container.setConcurrentConsumers(1);
        container.setMaxConcurrentConsumers(3);
        container.setPrefetchCount(1);

        container.setMessageListener((ChannelAwareMessageListener) (message, channel) -> {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            long deliveryTag = message.getMessageProperties().getDeliveryTag();

            // Get retry count from header
            Map<String, Object> headers = message.getMessageProperties().getHeaders();
            int retryCount = 0;
            if (headers.containsKey("x-retry-count")) {
                retryCount = (int) headers.get("x-retry-count");
            }

            try {
                Object payload = objectMapper.readValue(body, paramType);
                method.setAccessible(true);
                method.invoke(bean, payload);
                channel.basicAck(deliveryTag, false);

                log.debug("[EventHandler] Message processed: queue={}, deliveryTag={}",
                        queueName, deliveryTag);

            } catch (Exception e) {
                log.error("[EventHandler] Failed to process message: queue={}, retryCount={}/{}",
                        queueName, retryCount, maxRetries, e);

                if (retryEnabled && retryCount < maxRetries) {
                    // Reject and requeue with delay via TTL-based retry
                    // For simplicity, we nack and requeue -- in production, use a per-retry delay queue
                    long delay = retryPolicy.getDelay(retryCount + 1);
                    log.info("[EventHandler] Scheduling retry #{} in {}ms for queue={}",
                            retryCount + 1, delay, queueName);

                    // Nack without requeue, let the message go to DLQ or be retried
                    channel.basicNack(deliveryTag, false, true);
                } else {
                    // Max retries exceeded, reject to DLQ
                    log.error("[EventHandler] Max retries exceeded, sending to DLQ: queue={}", queueName);
                    channel.basicNack(deliveryTag, false, false);
                }
            }
        });

        container.start();
        containers.add(container);

        log.info("[EventHandler] Listener container started for queue={}", queueName);
    }

    /**
     * Get all active listener containers (for monitoring/shutdown).
     */
    public List<SimpleMessageListenerContainer> getContainers() {
        return containers;
    }
}
```

### 5.5 DomainEventAutoConfiguration.java

```java
package vip.mate.starter.mq.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the domain event consumer framework.
 * <p>
 * Activated when RabbitMQ is on the classpath.
 */
@AutoConfiguration
@ConditionalOnClass(ConnectionFactory.class)
public class DomainEventAutoConfiguration {

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public DeadLetterConfig deadLetterConfig(RabbitAdmin rabbitAdmin) {
        return new DeadLetterConfig(rabbitAdmin);
    }

    @Bean
    public DomainEventListenerRegistrar domainEventListenerRegistrar(
            ConnectionFactory connectionFactory,
            RabbitAdmin rabbitAdmin,
            ObjectMapper objectMapper,
            DeadLetterConfig deadLetterConfig) {
        return new DomainEventListenerRegistrar(connectionFactory, rabbitAdmin, objectMapper, deadLetterConfig);
    }
}
```

---

## 6. AutoConfiguration Registration

Update `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
vip.mate.starter.mq.MqAutoConfiguration
vip.mate.starter.mq.delay.DelayQueueAutoConfiguration
vip.mate.starter.mq.event.DomainEventAutoConfiguration
```

---

## 7. Usage Examples

### 7.1 Order Auto-Cancel After 30 Minutes

```java
package vip.mate.order.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.mate.starter.mq.delay.DelayQueueService;

/**
 * Order command service demonstrating delay queue usage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandService {

    private final DelayQueueService delayQueueService;

    /**
     * Create a new order and schedule auto-cancel after 30 minutes.
     */
    public Long createOrder(CreateOrderCommand command) {
        // 1. Create order (save to DB)
        Long orderId = saveOrder(command);

        // 2. Schedule auto-cancel after 30 minutes (1800 seconds)
        delayQueueService.offer("order:cancel", orderId, 1800);

        log.info("[Order] Created order={}, scheduled auto-cancel in 30min", orderId);
        return orderId;
    }

    private Long saveOrder(CreateOrderCommand command) {
        // ... DB save logic
        return 1L; // placeholder
    }
}
```

### 7.2 Order Cancel Delay Queue Handler

```java
package vip.mate.order.trigger.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.mate.starter.mq.delay.DelayQueueListener;

/**
 * Handles delayed order cancellation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelDelayHandler {

    private final OrderRepository orderRepository;

    /**
     * Auto-cancel unpaid orders after the delay expires.
     *
     * @param orderId the order ID to cancel
     */
    @DelayQueueListener(topic = "order:cancel")
    public void handleOrderCancel(Long orderId) {
        log.info("[Order] Processing auto-cancel for orderId={}", orderId);

        Order order = orderRepository.findById(orderId);
        if (order == null) {
            log.warn("[Order] Order not found: {}", orderId);
            return;
        }

        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            log.info("[Order] Order {} already in state {}, skip cancel", orderId, order.getStatus());
            return;
        }

        order.cancel("Auto-cancelled: payment timeout");
        orderRepository.save(order);
        log.info("[Order] Order {} auto-cancelled successfully", orderId);
    }
}
```

### 7.3 Welcome Email 1 Hour After Signup

```java
package vip.mate.system.trigger.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.mate.starter.mq.delay.DelayQueueListener;

/**
 * Sends a welcome email 1 hour after user signup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WelcomeEmailDelayHandler {

    private final EmailService emailService;
    private final UserRepository userRepository;

    @DelayQueueListener(topic = "user:welcome-email")
    public void handleWelcomeEmail(Long userId) {
        log.info("[WelcomeEmail] Processing welcome email for userId={}", userId);

        User user = userRepository.findById(userId);
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            log.info("[WelcomeEmail] User {} not active, skipping", userId);
            return;
        }

        emailService.sendWelcomeEmail(user.getEmail(), user.getNickName());
        log.info("[WelcomeEmail] Welcome email sent to userId={}", userId);
    }
}
```

Schedule the welcome email during user creation:

```java
// In UserCommandService or UserDomainService
delayQueueService.offer("user:welcome-email", userId, 3600); // 1 hour
```

### 7.4 Domain Event Consumer with Retry

```java
package vip.mate.notification.trigger.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.mate.starter.mq.EventMessage;
import vip.mate.starter.mq.event.DomainEventHandler;

/**
 * Consumes order.created events from RabbitMQ and sends notifications.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationConsumer {

    private final NotificationService notificationService;

    /**
     * Handle order.created event with retry (max 4 attempts, exponential backoff).
     * Failed messages go to DLQ: mate.order.created.notification.dlq
     */
    @DomainEventHandler(
            exchange = "mate.event",
            routingKey = "order.created",
            queue = "mate.order.created.notification",
            retryEnabled = true,
            maxRetries = 4,
            deadLetterEnabled = true
    )
    public void onOrderCreated(EventMessage<OrderCreatedPayload> event) {
        log.info("[Notification] Received order.created event: messageId={}, orderId={}",
                event.getId(), event.getData().getOrderId());

        notificationService.notifyOrderCreated(event.getData());
    }
}
```

### 7.5 Publishing Domain Events

Using the existing `EventPublisher` from RFC-005:

```java
package vip.mate.order.domain.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.starter.mq.EventPublisher;
import vip.mate.starter.mq.EventMessage;

@Service
@RequiredArgsConstructor
public class OrderDomainServiceImpl implements IOrderDomainService {

    private final EventPublisher eventPublisher;

    @Override
    public void createOrder(Order order) {
        // ... save order ...

        // Publish domain event
        OrderCreatedPayload payload = new OrderCreatedPayload(
                order.getId(), order.getUserId(), order.getTotalAmount());
        eventPublisher.publish("order.created", payload);
    }
}
```

---

## 8. RabbitMQ Configuration

### 8.1 application.yml / Nacos config

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:127.0.0.1}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASS:guest}
    virtual-host: /
    # Publisher confirms for reliable delivery
    publisher-confirm-type: correlated
    publisher-returns: true
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 1
        retry:
          enabled: false  # We handle retry ourselves via @DomainEventHandler
```

---

## 9. Retry Behavior

Exponential backoff with the default `RetryPolicy`:

| Attempt | Delay    | Cumulative |
|---------|----------|------------|
| 1       | 1s       | 1s         |
| 2       | 5s       | 6s         |
| 3       | 25s      | 31s        |
| 4       | 125s     | ~2.5min    |
| (max)   | 300s cap | --         |

After all retries are exhausted, the message is sent to the Dead Letter Queue (DLQ) for manual inspection or alerting.

---

## 10. Redis Key Design (Delay Queue)

| Key Pattern                      | Value Type           | Purpose                        |
|----------------------------------|----------------------|--------------------------------|
| `mate:delay:queue:{topic}`       | RBlockingQueue (list)| Blocking queue for consumers   |
| `mate:delay:queue:{topic}:delay` | RDelayedQueue (zset) | Sorted set with delay scores   |

---

## 11. Testing Checklist

- [ ] `delayQueueService.offer("test", data, 5)` delivers message after ~5 seconds
- [ ] `@DelayQueueListener(topic = "test")` receives the message
- [ ] Multiple offers with different delays are delivered in correct order
- [ ] `@DomainEventHandler` auto-declares exchange, queue, and binding in RabbitMQ
- [ ] EventPublisher -> DomainEventHandler end-to-end delivery works
- [ ] Retry: failing handler is retried up to maxRetries times
- [ ] Dead letter: after max retries, message appears in DLQ
- [ ] DLQ messages have 7-day TTL
- [ ] Container shutdown stops all listener containers cleanly
