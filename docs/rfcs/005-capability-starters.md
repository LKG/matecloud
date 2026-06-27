# RFC-005: Capability Starters

| Field       | Value                        |
|-------------|------------------------------|
| **RFC**     | 005                          |
| **Title**   | Capability Starters          |
| **Status**  | Draft                        |
| **Created** | 2026-04-11                   |

## Summary

This RFC defines four capability-level Spring Boot starters for the matecloud DDD microservice scaffold:

1. **mate-mq-starter** -- RabbitMQ event publishing + Redisson delay queue + @DomainEventHandler
2. **mate-job-starter** -- XXL-Job distributed task scheduling
3. **mate-seata-starter** -- Seata distributed transaction (minimal wrapper)
4. **mate-file-starter** -- MinIO file storage operations

All starters follow the convention:
- GroupId: `vip.mate`
- Package base: `vip.mate.starter.<module>`
- Each provides `@AutoConfiguration` plus `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

---

## 1. mate-mq-starter

### 1.1 pom.xml

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
    <description>MateCloud MQ Starter - RabbitMQ event publishing abstraction</description>

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

        <!-- Jackson for message serialization -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
    </dependencies>
</project>
```

### 1.2 Java Sources

#### EventMessage.java

```java
package vip.mate.starter.mq;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * Generic event message wrapper.
 * <p>
 * Every event published through the MQ layer is wrapped in an {@code EventMessage}
 * which provides traceability via a unique id and timestamp.
 *
 * @param <T> the payload type
 */
public class EventMessage<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique message identifier.
     */
    private String id;

    /**
     * Message creation timestamp.
     */
    private Date timestamp;

    /**
     * Business payload.
     */
    private T data;

    public EventMessage() {
    }

    public EventMessage(T data) {
        this.id = UUID.randomUUID().toString().replace("-", "");
        this.timestamp = new Date();
        this.data = data;
    }

    public EventMessage(String id, Date timestamp, T data) {
        this.id = id;
        this.timestamp = timestamp;
        this.data = data;
    }

    /**
     * Factory method for creating an event message with auto-generated id and timestamp.
     */
    public static <T> EventMessage<T> of(T data) {
        return new EventMessage<>(data);
    }

    // ======================== Getters & Setters ========================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "EventMessage{id='" + id + "', timestamp=" + timestamp + ", data=" + data + '}';
    }
}
```

#### BaseEvent.java

```java
package vip.mate.starter.mq;

/**
 * Abstract base class for domain events.
 * <p>
 * Concrete event classes should extend this and provide:
 * <ul>
 *   <li>{@link #topic()} -- the RabbitMQ routing key / exchange topic</li>
 *   <li>{@link #buildEventMessage()} -- constructs the {@link EventMessage} to publish</li>
 * </ul>
 *
 * <pre>{@code
 * public class OrderCreatedEvent extends BaseEvent<OrderDTO> {
 *     private final OrderDTO order;
 *
 *     public OrderCreatedEvent(OrderDTO order) {
 *         this.order = order;
 *     }
 *
 *     @Override
 *     public String topic() {
 *         return "order.created";
 *     }
 *
 *     @Override
 *     public EventMessage<OrderDTO> buildEventMessage() {
 *         return EventMessage.of(order);
 *     }
 * }
 * }</pre>
 *
 * @param <T> the payload type
 */
public abstract class BaseEvent<T> {

    /**
     * The topic / routing key for this event.
     *
     * @return topic string (e.g. "order.created", "user.registered")
     */
    public abstract String topic();

    /**
     * Build the event message to be published.
     *
     * @return fully constructed {@link EventMessage}
     */
    public abstract EventMessage<T> buildEventMessage();
}
```

#### EventPublisher.java

```java
package vip.mate.starter.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;

/**
 * Event publisher that wraps {@link RabbitTemplate} to provide a simple
 * event-driven publishing API.
 *
 * <pre>{@code
 * // Publish via BaseEvent
 * eventPublisher.publish(new OrderCreatedEvent(orderDTO));
 *
 * // Publish directly with topic + message
 * eventPublisher.publish("order.created", EventMessage.of(orderDTO));
 * }</pre>
 */
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public EventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish an event message to the specified topic.
     *
     * @param topic   the routing key / topic
     * @param message the event message wrapper
     */
    public void publish(String topic, EventMessage<?> message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setContentEncoding(StandardCharsets.UTF_8.name());
            props.setMessageId(message.getId());

            Message amqpMessage = new Message(json.getBytes(StandardCharsets.UTF_8), props);
            rabbitTemplate.send(topic, "", amqpMessage);

            log.info("Published event to topic [{}], messageId={}", topic, message.getId());
        } catch (Exception e) {
            log.error("Failed to publish event to topic [{}], messageId={}",
                    topic, message.getId(), e);
            throw new RuntimeException("Failed to publish event message", e);
        }
    }

    /**
     * Publish a typed {@link BaseEvent}.
     *
     * @param event the domain event
     */
    public void publish(BaseEvent<?> event) {
        publish(event.topic(), event.buildEventMessage());
    }

    /**
     * Publish a payload directly (auto-wrapped in {@link EventMessage}).
     *
     * @param topic the routing key / topic
     * @param data  the payload object
     * @param <T>   payload type
     */
    public <T> void publish(String topic, T data) {
        publish(topic, EventMessage.of(data));
    }

    /**
     * Convert and send using RabbitTemplate's built-in Jackson converter.
     * Simpler but less control over message properties.
     *
     * @param exchange   the exchange name
     * @param routingKey the routing key
     * @param message    the event message
     */
    public void convertAndSend(String exchange, String routingKey, EventMessage<?> message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        log.info("Sent event to exchange [{}] routingKey [{}], messageId={}",
                exchange, routingKey, message.getId());
    }
}
```

#### RabbitMqAutoConfiguration.java

```java
package vip.mate.starter.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for RabbitMQ event publishing.
 * <p>
 * Provides:
 * <ul>
 *   <li>Jackson-based message converter for RabbitMQ</li>
 *   <li>{@link EventPublisher} for simplified event publishing</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass(RabbitTemplate.class)
public class RabbitMqAutoConfiguration {

    /**
     * Jackson message converter so RabbitMQ payloads are serialized as JSON.
     */
    @Bean
    public MessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * Event publisher bean.
     */
    @Bean
    public EventPublisher eventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        return new EventPublisher(rabbitTemplate, objectMapper);
    }
}
```

### 1.3 Auto-Configuration Imports

**File:** `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
vip.mate.starter.mq.RabbitMqAutoConfiguration
```

---

## 2. mate-job-starter

### 2.1 pom.xml

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

    <artifactId>mate-job-starter</artifactId>
    <packaging>jar</packaging>
    <description>MateCloud Job Starter - XXL-Job distributed task scheduling</description>

    <dependencies>
        <!-- XXL-Job -->
        <dependency>
            <groupId>com.xuxueli</groupId>
            <artifactId>xxl-job-core</artifactId>
            <version>3.3.2</version>
        </dependency>

        <!-- Spring Boot auto-configuration support -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
    </dependencies>
</project>
```

### 2.2 Java Sources

#### XxlJobProperties.java

```java
package vip.mate.starter.job;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for XXL-Job.
 *
 * <pre>
 * xxl:
 *   job:
 *     admin-addresses: http://127.0.0.1:8080/xxl-job-admin
 *     access-token: default_token
 *     app-name: ${spring.application.name}
 *     port: 9999
 *     log-path: /data/logs/xxl-job
 *     log-retention-days: 30
 * </pre>
 */
@ConfigurationProperties(prefix = "xxl.job")
public class XxlJobProperties {

    /**
     * XXL-Job admin server addresses (comma-separated for HA).
     */
    private String adminAddresses = "http://127.0.0.1:8080/xxl-job-admin";

    /**
     * Access token for admin communication.
     */
    private String accessToken;

    /**
     * Executor application name (registered in admin).
     */
    private String appName;

    /**
     * Executor RPC port.
     */
    private int port = 9999;

    /**
     * Job execution log path.
     */
    private String logPath = "/data/logs/xxl-job";

    /**
     * Log retention in days. -1 means never expire.
     */
    private int logRetentionDays = 30;

    // ======================== Getters & Setters ========================

    public String getAdminAddresses() {
        return adminAddresses;
    }

    public void setAdminAddresses(String adminAddresses) {
        this.adminAddresses = adminAddresses;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public int getLogRetentionDays() {
        return logRetentionDays;
    }

    public void setLogRetentionDays(int logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }
}
```

#### XxlJobAutoConfiguration.java

```java
package vip.mate.starter.job;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for XXL-Job executor.
 * <p>
 * Reads properties from {@link XxlJobProperties} and creates a
 * {@link XxlJobSpringExecutor} bean that registers with the XXL-Job admin.
 */
@AutoConfiguration
@ConditionalOnClass(XxlJobSpringExecutor.class)
@EnableConfigurationProperties(XxlJobProperties.class)
public class XxlJobAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(XxlJobAutoConfiguration.class);

    @Bean(initMethod = "start", destroyMethod = "destroy")
    public XxlJobSpringExecutor xxlJobSpringExecutor(XxlJobProperties properties) {
        log.info("Initializing XXL-Job executor, appName={}, adminAddresses={}",
                properties.getAppName(), properties.getAdminAddresses());

        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(properties.getAdminAddresses());
        executor.setAccessToken(properties.getAccessToken());
        executor.setAppname(properties.getAppName());
        executor.setPort(properties.getPort());
        executor.setLogPath(properties.getLogPath());
        executor.setLogRetentionDays(properties.getLogRetentionDays());

        return executor;
    }
}
```

### 2.3 Auto-Configuration Imports

**File:** `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
vip.mate.starter.job.XxlJobAutoConfiguration
```

---

## 3. mate-seata-starter

### 3.1 pom.xml

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

    <artifactId>mate-seata-starter</artifactId>
    <packaging>jar</packaging>
    <description>MateCloud Seata Starter - Distributed transaction support</description>

    <dependencies>
        <!-- Seata via Spring Cloud Alibaba -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
            <exclusions>
                <!-- Exclude built-in seata-all to manage version independently if needed -->
                <exclusion>
                    <groupId>io.seata</groupId>
                    <artifactId>seata-all</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <!-- Seata Spring Boot starter (provides its own auto-configuration) -->
        <dependency>
            <groupId>io.seata</groupId>
            <artifactId>seata-spring-boot-starter</artifactId>
        </dependency>
    </dependencies>
</project>
```

### 3.2 Java Sources

#### SeataAutoConfiguration.java

```java
package vip.mate.starter.seata;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Minimal auto-configuration for Seata distributed transactions.
 * <p>
 * Seata's own {@code seata-spring-boot-starter} handles the heavy lifting
 * (global transaction scanning, TM/RM initialization, etc.).
 * <p>
 * This starter exists to:
 * <ul>
 *   <li>Provide a single dependency for microservices to pull in</li>
 *   <li>Manage version alignment centrally in mate-starters BOM</li>
 *   <li>Allow conditional enablement via property</li>
 * </ul>
 *
 * <pre>
 * seata:
 *   enabled: true
 *   tx-service-group: my_tx_group
 *   service:
 *     vgroup-mapping:
 *       my_tx_group: default
 *     grouplist:
 *       default: 127.0.0.1:8091
 * </pre>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "seata", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SeataAutoConfiguration {

    // Intentionally empty.
    // Seata's own auto-configuration handles all bean registration.
    // This class serves as the entry point in AutoConfiguration.imports
    // and a future extension point for MateCloud-specific Seata customization.
}
```

### 3.3 Auto-Configuration Imports

**File:** `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
vip.mate.starter.seata.SeataAutoConfiguration
```

---

## 4. mate-file-starter

### 4.1 pom.xml

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

    <artifactId>mate-file-starter</artifactId>
    <packaging>jar</packaging>
    <description>MateCloud File Starter - MinIO object storage operations</description>

    <dependencies>
        <!-- MinIO SDK -->
        <dependency>
            <groupId>io.minio</groupId>
            <artifactId>minio</artifactId>
            <version>8.6.0</version>
        </dependency>

        <!-- Spring Web (for MultipartFile support) -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-web</artifactId>
        </dependency>

        <!-- Spring Boot auto-configuration support -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
    </dependencies>
</project>
```

### 4.2 Java Sources

#### MinioProperties.java

```java
package vip.mate.starter.file;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for MinIO.
 *
 * <pre>
 * minio:
 *   endpoint: http://127.0.0.1:9000
 *   access-key: minioadmin
 *   secret-key: minioadmin
 *   bucket-name: matecloud
 * </pre>
 */
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /**
     * MinIO server endpoint URL.
     */
    private String endpoint = "http://127.0.0.1:9000";

    /**
     * Access key (username).
     */
    private String accessKey;

    /**
     * Secret key (password).
     */
    private String secretKey;

    /**
     * Default bucket name.
     */
    private String bucketName = "matecloud";

    // ======================== Getters & Setters ========================

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }
}
```

#### MinioTemplate.java

```java
package vip.mate.starter.file;

import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for MinIO file operations.
 * <p>
 * Provides upload, download, delete, and URL generation for objects
 * stored in MinIO.
 *
 * <pre>{@code
 * // Upload a file
 * String objectName = minioTemplate.upload(multipartFile);
 *
 * // Get a presigned download URL
 * String url = minioTemplate.getPresignedUrl(objectName);
 *
 * // Delete a file
 * minioTemplate.delete(objectName);
 * }</pre>
 */
public class MinioTemplate {

    private static final Logger log = LoggerFactory.getLogger(MinioTemplate.class);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public MinioTemplate(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    /**
     * Get the underlying MinIO client for advanced operations.
     */
    public MinioClient getClient() {
        return minioClient;
    }

    /**
     * Ensure the default bucket exists; create it if not.
     */
    public void ensureBucketExists() {
        ensureBucketExists(properties.getBucketName());
    }

    /**
     * Ensure a bucket exists; create it if not.
     */
    public void ensureBucketExists(String bucketName) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created MinIO bucket: {}", bucketName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure bucket exists: " + bucketName, e);
        }
    }

    /**
     * Upload a {@link MultipartFile} to the default bucket.
     * The object name is auto-generated with a date-based path and UUID.
     *
     * @param file the multipart file
     * @return the object name (key) in MinIO
     */
    public String upload(MultipartFile file) {
        return upload(properties.getBucketName(), file);
    }

    /**
     * Upload a {@link MultipartFile} to the specified bucket.
     *
     * @param bucketName target bucket
     * @param file       the multipart file
     * @return the object name (key) in MinIO
     */
    public String upload(String bucketName, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String objectName = LocalDate.now().format(DATE_FORMAT) + "/"
                + UUID.randomUUID().toString().replace("-", "") + extension;

        try {
            ensureBucketExists(bucketName);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.info("Uploaded file to MinIO: bucket={}, object={}", bucketName, objectName);
            return objectName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }

    /**
     * Upload an input stream to the default bucket.
     *
     * @param objectName  target object name
     * @param inputStream the data stream
     * @param size        the stream size (-1 if unknown)
     * @param contentType MIME type
     * @return the object name
     */
    public String upload(String objectName, InputStream inputStream, long size, String contentType) {
        return upload(properties.getBucketName(), objectName, inputStream, size, contentType);
    }

    /**
     * Upload an input stream to the specified bucket.
     */
    public String upload(String bucketName, String objectName, InputStream inputStream,
                         long size, String contentType) {
        try {
            ensureBucketExists(bucketName);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());
            log.info("Uploaded stream to MinIO: bucket={}, object={}", bucketName, objectName);
            return objectName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload stream to MinIO", e);
        }
    }

    /**
     * Download a file as an {@link InputStream} from the default bucket.
     *
     * @param objectName the object name
     * @return input stream of the file content
     */
    public InputStream download(String objectName) {
        return download(properties.getBucketName(), objectName);
    }

    /**
     * Download a file as an {@link InputStream} from the specified bucket.
     */
    public InputStream download(String bucketName, String objectName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file from MinIO: " + objectName, e);
        }
    }

    /**
     * Delete an object from the default bucket.
     *
     * @param objectName the object name
     */
    public void delete(String objectName) {
        delete(properties.getBucketName(), objectName);
    }

    /**
     * Delete an object from the specified bucket.
     */
    public void delete(String bucketName, String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            log.info("Deleted file from MinIO: bucket={}, object={}", bucketName, objectName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file from MinIO: " + objectName, e);
        }
    }

    /**
     * Get the public URL for an object (direct access, requires bucket policy to allow).
     *
     * @param objectName the object name
     * @return the full URL
     */
    public String getUrl(String objectName) {
        return getUrl(properties.getBucketName(), objectName);
    }

    /**
     * Get the public URL for an object in the specified bucket.
     */
    public String getUrl(String bucketName, String objectName) {
        return properties.getEndpoint() + "/" + bucketName + "/" + objectName;
    }

    /**
     * Get a presigned URL for temporary access (default 7 days).
     *
     * @param objectName the object name
     * @return presigned URL string
     */
    public String getPresignedUrl(String objectName) {
        return getPresignedUrl(properties.getBucketName(), objectName, 7, TimeUnit.DAYS);
    }

    /**
     * Get a presigned URL with custom expiry.
     */
    public String getPresignedUrl(String bucketName, String objectName,
                                  int duration, TimeUnit unit) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(duration, unit)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL for: " + objectName, e);
        }
    }
}
```

#### MinioAutoConfiguration.java

```java
package vip.mate.starter.file;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for MinIO file storage.
 * <p>
 * Provides a configured {@link MinioClient} and a {@link MinioTemplate}
 * for common file operations.
 */
@AutoConfiguration
@ConditionalOnClass(MinioClient.class)
@ConditionalOnProperty(prefix = "minio", name = "endpoint")
@EnableConfigurationProperties(MinioProperties.class)
public class MinioAutoConfiguration {

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    @Bean
    public MinioTemplate minioTemplate(MinioClient minioClient, MinioProperties properties) {
        return new MinioTemplate(minioClient, properties);
    }
}
```

### 4.3 Auto-Configuration Imports

**File:** `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
vip.mate.starter.file.MinioAutoConfiguration
```

---

## Appendix: Recommended application.yml Snippets

### MQ (mate-mq-starter)

```yaml
spring:
  rabbitmq:
    host: 127.0.0.1
    port: 5672
    username: guest
    password: guest
    virtual-host: /
```

### Job (mate-job-starter)

```yaml
xxl:
  job:
    admin-addresses: http://127.0.0.1:8080/xxl-job-admin
    access-token: default_token
    app-name: ${spring.application.name}
    port: 9999
    log-path: /data/logs/xxl-job
    log-retention-days: 30
```

### Seata (mate-seata-starter)

```yaml
seata:
  enabled: true
  tx-service-group: my_tx_group
  service:
    vgroup-mapping:
      my_tx_group: default
    grouplist:
      default: 127.0.0.1:8091
```

### File (mate-file-starter)

```yaml
minio:
  endpoint: http://127.0.0.1:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: matecloud
```
