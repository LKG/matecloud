/*
 * Copyright (c) 2024-2026 Beijing Daotiandi Technology Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package vip.mate.starter.mq.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link BeanPostProcessor} that scans for {@link DomainEventHandler @DomainEventHandler}
 * methods and registers RabbitMQ consumers with retry + dead-letter support at bean
 * initialisation time.
 *
 * <p>For each annotated method a queue {@code {topic}.{consumerGroup}} is declared,
 * bound to the {@code mate.domain.events} topic exchange, and wrapped in a
 * {@link SimpleMessageListenerContainer}.</p>
 *
 * @author mateaix
 */
@Slf4j
public class DomainEventListenerRegistrar implements BeanPostProcessor {

    private static final String DOMAIN_EXCHANGE = "mate.domain.events";
    private static final String DLX_EXCHANGE = "mate.domain.events.dlx";
    private static final String RETRY_HEADER = "x-retry-count";

    /**
     * Exponential back-off delays in milliseconds: 1 s, 5 s, 25 s.
     */
    private static final long[] RETRY_DELAYS = {1_000L, 5_000L, 25_000L};

    private final RabbitAdmin rabbitAdmin;
    private final ConnectionFactory connectionFactory;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final TopicExchange domainExchange;
    private final List<SimpleMessageListenerContainer> containers = new ArrayList<>();

    public DomainEventListenerRegistrar(RabbitAdmin rabbitAdmin,
                                        ConnectionFactory connectionFactory,
                                        RabbitTemplate rabbitTemplate,
                                        ObjectMapper objectMapper,
                                        TopicExchange domainExchange) {
        this.rabbitAdmin = rabbitAdmin;
        this.connectionFactory = connectionFactory;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.domainExchange = domainExchange;
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

            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != 1) {
                throw new IllegalStateException(
                        "@DomainEventHandler method must have exactly one parameter: "
                                + targetClass.getName() + "#" + method.getName());
            }

            String topic = annotation.topic();
            String group = annotation.consumerGroup().isEmpty() ? "default" : annotation.consumerGroup();
            int maxRetries = annotation.maxRetries();
            Class<?> payloadType = paramTypes[0];

            String queueName = topic + "." + group;

            log.info("[DomainEvent] Registering listener: topic={}, group={}, queue={}, handler={}.{}",
                    topic, group, queueName, targetClass.getSimpleName(), method.getName());

            // Declare queue with dead-letter exchange routing
            Map<String, Object> queueArgs = Map.of(
                    "x-dead-letter-exchange", DLX_EXCHANGE,
                    "x-dead-letter-routing-key", queueName
            );
            Queue queue = new Queue(queueName, true, false, false, queueArgs);
            rabbitAdmin.declareQueue(queue);

            // Bind queue to the domain topic exchange
            rabbitAdmin.declareBinding(
                    BindingBuilder.bind(queue).to(domainExchange).with(topic));

            // Delayed-retry queue (no consumer). Failed messages are republished
            // here with a per-message TTL; when the TTL expires RabbitMQ
            // dead-letters them BACK to the domain exchange with this topic's
            // routing key, re-delivering to the main queue above. This is the
            // plugin-free equivalent of an x-delayed-message exchange — the old
            // `x-delay` header was silently ignored by the plain TopicExchange,
            // so retries fired immediately (a hot loop). NOTE: a single retry
            // queue serializes TTLs, so a longer delay can head-of-line block
            // shorter ones — acceptable at domain-event volumes.
            String retryQueueName = queueName + ".retry";
            Map<String, Object> retryArgs = Map.of(
                    "x-dead-letter-exchange", DOMAIN_EXCHANGE,
                    "x-dead-letter-routing-key", topic
            );
            rabbitAdmin.declareQueue(new Queue(retryQueueName, true, false, false, retryArgs));

            // Create listener container
            SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
            container.setQueueNames(queueName);
            container.setAcknowledgeMode(AcknowledgeMode.AUTO);
            container.setConcurrentConsumers(1);
            // 开启观测: 消费侧从消息头抽取上游 trace 上下文 -> 续上同一条 trace, 消费日志带 traceId。
            // (手建容器不走 Boot 的 spring.rabbitmq.listener.observation-enabled, 必须显式开。)
            container.setObservationEnabled(true);
            container.setMessageListener((ChannelAwareMessageListener) (message, channel) ->
                    handleMessage(message, bean, method, payloadType, topic, maxRetries, queueName));
            container.start();

            containers.add(container);
        }
        return bean;
    }

    private void handleMessage(Message message,
                               Object bean,
                               Method method,
                               Class<?> payloadType,
                               String topic,
                               int maxRetries,
                               String queueName) {
        int retryCount = getRetryCount(message);
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            Object payload = objectMapper.readValue(body, payloadType);
            method.setAccessible(true);
            method.invoke(bean, payload);
        } catch (Exception e) {
            log.error("[DomainEvent] Handler failed: topic={}, queue={}, retryCount={}/{}",
                    topic, queueName, retryCount, maxRetries, e);

            if (retryCount < maxRetries) {
                republishWithDelay(message, queueName, retryCount);
            } else {
                sendToDlq(message, queueName);
            }
        }
    }

    private int getRetryCount(Message message) {
        Object header = message.getMessageProperties().getHeader(RETRY_HEADER);
        if (header instanceof Number) {
            return ((Number) header).intValue();
        }
        return 0;
    }

    private void republishWithDelay(Message message, String queueName, int currentRetryCount) {
        int nextRetry = currentRetryCount + 1;
        long delayMs = nextRetry <= RETRY_DELAYS.length
                ? RETRY_DELAYS[nextRetry - 1]
                : RETRY_DELAYS[RETRY_DELAYS.length - 1];

        String retryQueueName = queueName + ".retry";
        log.info("[DomainEvent] Scheduling retry {}: queue={}, delayMs={}",
                nextRetry, queueName, delayMs);

        MessageProperties props = message.getMessageProperties();
        props.setHeader(RETRY_HEADER, nextRetry);
        // Per-message TTL: the retry queue dead-letters the message back to the
        // domain exchange once this expires. Routed via the default exchange
        // (routing key == queue name) straight into the no-consumer retry queue.
        props.setExpiration(String.valueOf(delayMs));

        rabbitTemplate.send("", retryQueueName, message);
    }

    private void sendToDlq(Message message, String queueName) {
        log.warn("[DomainEvent] Max retries exceeded, sending to DLQ: queue={}", queueName);
        rabbitTemplate.send(DLX_EXCHANGE, queueName, message);
    }
}
