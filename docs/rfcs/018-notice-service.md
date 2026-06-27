# RFC-018: Notification Service

| Field       | Value                              |
|-------------|------------------------------------|
| **RFC**     | 018                                |
| **Title**   | Notification Service               |
| **Status**  | Draft                              |
| **Created** | 2026-04-11                         |

## Summary

This RFC defines the **mate-notice** module (`mate-biz/mate-notice`), a DDD-structured notification service supporting multi-channel delivery via the adapter pattern: SMS, Email, WeChat, and Push. Adapters are auto-registered via a custom `@NoticeAdapter` annotation and resolved at runtime through a factory.

Key features:
- DDD 4-layer architecture (domain / infrastructure / application / trigger)
- Adapter pattern with `@NoticeAdapter` annotation for auto-registration
- Factory-based adapter resolution by `NoticeChannel`
- Batch retry via XxlJob with CompletableFuture parallel processing (200 per batch, max 10000 total)
- Dubbo RPC interface for cross-service invocation
- Port: 9050

Reference: kaleido-ai/kaleido-notice

---

## 1. Module Structure

```
mate-biz/
  mate-notice/
    pom.xml
    src/main/java/vip/mate/biz/notice/
      MateNoticeApplication.java
      domain/
        model/
          aggregate/
            NoticeAggregate.java
            NoticeTemplateAggregate.java
          valobj/
            RetryStatus.java
            TargetAddress.java
        adapter/
          port/
            INoticeAdapter.java
          repository/
            INoticeRepository.java
            INoticeTemplateRepository.java
        factory/
          INoticeAdapterFactory.java
        service/
          INoticeDomainService.java
      infrastructure/
        adapter/
          AbstractNoticeAdapter.java
          SmsNoticeAdapter.java
          EmailNoticeAdapter.java
          WeChatNoticeAdapter.java
          PushNoticeAdapter.java
          repository/
            NoticeRepositoryImpl.java
            NoticeTemplateRepositoryImpl.java
        factory/
          NoticeAdapterFactoryImpl.java
        dao/
          NoticeDao.java
          NoticeTemplateDao.java
          po/
            NoticePO.java
            NoticeTemplatePO.java
      application/
        NoticeCommandService.java
        NoticeQueryService.java
      trigger/
        http/
          NoticeController.java
        rpc/
          RpcNoticeServiceImpl.java
        job/
          NoticeRetryJob.java
          NoticeRetryConstants.java
      types/
        annotation/
          NoticeAdapter.java
        enums/
          NoticeChannel.java
          NoticeStatus.java
          BusinessType.java
        exception/
          NoticeErrorCode.java
          NoticeException.java
    src/main/resources/
      application.yml
      mapper/
        NoticeMapper.xml
        NoticeTemplateMapper.xml
```

---

## 2. pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>mate-biz</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>mate-notice</artifactId>
    <packaging>jar</packaging>
    <description>MateCloud Notice Service - Multi-channel notification with adapter pattern</description>

    <dependencies>
        <!-- mate-base -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-base</artifactId>
        </dependency>

        <!-- mate-api-notice (Dubbo interfaces, enums) -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-api-notice</artifactId>
        </dependency>

        <!-- mate-distribute-starter (Snowflake ID) -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-distribute-starter</artifactId>
        </dependency>

        <!-- mate-cache-starter (Redis) -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-cache-starter</artifactId>
        </dependency>

        <!-- mate-job-starter (XXL-Job) -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-job-starter</artifactId>
        </dependency>

        <!-- mate-dynamic-tp-starter (Dynamic thread pool) -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-dynamic-tp-starter</artifactId>
        </dependency>

        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Dubbo -->
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-spring-boot-starter</artifactId>
        </dependency>

        <!-- MyBatis Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        </dependency>

        <!-- MySQL -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Mail (for EmailNoticeAdapter) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>

        <!-- Hutool -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 3. Types Layer

### 3.1 NoticeChannel Enum

```java
package vip.mate.biz.notice.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Notification channel types.
 */
@Getter
@AllArgsConstructor
public enum NoticeChannel {

    SMS("SMS", "Short Message Service"),
    EMAIL("EMAIL", "Email"),
    WECHAT("WECHAT", "WeChat Template Message"),
    PUSH("PUSH", "App Push Notification");

    private final String code;
    private final String description;
}
```

### 3.2 NoticeStatus Enum

```java
package vip.mate.biz.notice.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Notification delivery status.
 */
@Getter
@AllArgsConstructor
public enum NoticeStatus {

    PENDING(0, "Pending"),
    SUCCESS(1, "Sent successfully"),
    FAILED(2, "Send failed");

    private final int code;
    private final String description;
}
```

### 3.3 BusinessType Enum

```java
package vip.mate.biz.notice.types.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Business scenario types for notifications.
 */
@Getter
@AllArgsConstructor
public enum BusinessType {

    VERIFY_CODE("VERIFY_CODE", "Verification code"),
    ORDER_NOTIFY("ORDER_NOTIFY", "Order notification"),
    SYSTEM_ALERT("SYSTEM_ALERT", "System alert"),
    MARKETING("MARKETING", "Marketing message");

    private final String code;
    private final String description;
}
```

### 3.4 @NoticeAdapter Annotation

```java
package vip.mate.biz.notice.types.annotation;

import org.springframework.stereotype.Component;
import vip.mate.biz.notice.types.enums.NoticeChannel;

import java.lang.annotation.*;

/**
 * Marks a class as a notification adapter for a specific channel.
 * <p>
 * Classes annotated with {@code @NoticeAdapter} are automatically discovered
 * and registered in the {@link vip.mate.biz.notice.infrastructure.factory.NoticeAdapterFactoryImpl}.
 * </p>
 *
 * <p>Usage:</p>
 * <pre>
 * {@literal @}NoticeAdapter(NoticeChannel.SMS)
 * public class SmsNoticeAdapter extends AbstractNoticeAdapter { ... }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface NoticeAdapter {

    /**
     * The notification channel this adapter supports.
     */
    NoticeChannel value();
}
```

### 3.5 NoticeErrorCode

```java
package vip.mate.biz.notice.types.exception;

import lombok.Getter;

/**
 * Error codes for the notification service.
 */
@Getter
public enum NoticeErrorCode {

    // Send errors
    NOTICE_SEND_FAILED("NOTICE_SEND_FAILED", "Notification send failed"),
    NOTICE_RECORD_NOT_FOUND("NOTICE_RECORD_NOT_FOUND", "Notification record not found"),
    NOTICE_TEMPLATE_NOT_FOUND("NOTICE_TEMPLATE_NOT_FOUND", "Notification template not found"),

    // Verification code errors
    VERIFY_CODE_GENERATE_FAILED("VERIFY_CODE_GENERATE_FAILED", "Verification code generation failed"),
    VERIFY_CODE_TEMPLATE_EMPTY("VERIFY_CODE_TEMPLATE_EMPTY", "Verification code template is empty"),
    VERIFY_CODE_SEND_FAILED("VERIFY_CODE_SEND_FAILED", "Verification code send failed"),
    VERIFY_CODE_INVALID("VERIFY_CODE_INVALID", "Verification code is invalid"),
    VERIFY_CODE_EXPIRED("VERIFY_CODE_EXPIRED", "Verification code has expired"),

    // Validation errors
    TARGET_EMPTY("TARGET_EMPTY", "Notification target cannot be empty"),
    BUSINESS_TYPE_EMPTY("BUSINESS_TYPE_EMPTY", "Business type cannot be empty"),
    NOTICE_CONTENT_EMPTY("NOTICE_CONTENT_EMPTY", "Notification content cannot be empty"),
    NOTICE_TYPE_EMPTY("NOTICE_TYPE_EMPTY", "Notification type cannot be empty"),

    // Template errors
    TEMPLATE_CODE_EXISTS("TEMPLATE_CODE_EXISTS", "Template code already exists"),
    TEMPLATE_RENDER_FAILED("TEMPLATE_RENDER_FAILED", "Template rendering failed"),

    // Retry errors
    RETRY_FAILED("RETRY_FAILED", "Retry send failed"),
    MAX_RETRY_EXCEEDED("MAX_RETRY_EXCEEDED", "Maximum retry count exceeded"),

    // Adapter errors
    UNSUPPORTED_NOTICE_TYPE("UNSUPPORTED_NOTICE_TYPE", "Unsupported notification channel"),
    ADAPTER_NOT_FOUND("ADAPTER_NOT_FOUND", "Notification adapter not found");

    private final String code;
    private final String message;

    NoticeErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

### 3.6 NoticeException

```java
package vip.mate.biz.notice.types.exception;

import lombok.Getter;

/**
 * Business exception for the notification service.
 */
@Getter
public class NoticeException extends RuntimeException {

    private final String code;

    public NoticeException(String code, String message) {
        super(message);
        this.code = code;
    }

    public static NoticeException of(NoticeErrorCode errorCode) {
        return new NoticeException(errorCode.getCode(), errorCode.getMessage());
    }
}
```

---

## 4. Domain Layer

### 4.1 NoticeAggregate

```java
package vip.mate.biz.notice.domain.model.aggregate;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import vip.mate.base.model.entity.BaseEntity;
import vip.mate.biz.notice.domain.model.valobj.RetryStatus;
import vip.mate.biz.notice.domain.model.valobj.TargetAddress;
import vip.mate.biz.notice.types.enums.BusinessType;
import vip.mate.biz.notice.types.enums.NoticeChannel;
import vip.mate.biz.notice.types.enums.NoticeStatus;
import vip.mate.starter.distribute.util.SnowflakeUtil;

import java.util.Date;

/**
 * Notice aggregate root.
 * <p>
 * Encapsulates the complete lifecycle of a notification: creation, sending,
 * success/failure tracking, and retry management.
 * </p>
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class NoticeAggregate extends BaseEntity {

    public static final int MAX_RETRY_COUNT = 3;

    /** Notification channel (SMS, EMAIL, WECHAT, PUSH) */
    private NoticeChannel channel;

    /** Target address value object */
    private TargetAddress target;

    /** Current delivery status */
    private NoticeStatus status;

    /** Business scenario type */
    private BusinessType businessType;

    /** Notification content */
    private String content;

    /** Send result message */
    private String resultMessage;

    /** Actual send time */
    private Date sentAt;

    /** Retry tracking value object */
    private RetryStatus retryStatus;

    /**
     * Factory method: create with auto-generated Snowflake ID.
     */
    public static NoticeAggregate create(NoticeChannel channel, TargetAddress target,
                                         BusinessType businessType, String content) {
        String id = SnowflakeUtil.newSnowflakeId();
        return create(id, channel, target, businessType, content);
    }

    /**
     * Factory method: create with externally provided ID.
     */
    public static NoticeAggregate create(String noticeId, NoticeChannel channel,
                                         TargetAddress target, BusinessType businessType,
                                         String content) {
        return NoticeAggregate.builder()
                .id(noticeId)
                .channel(channel)
                .target(target)
                .status(NoticeStatus.PENDING)
                .businessType(businessType)
                .content(content)
                .retryStatus(RetryStatus.init())
                .build();
    }

    /**
     * Mark as successfully sent.
     */
    public void markAsSuccess(String resultMessage) {
        this.status = NoticeStatus.SUCCESS;
        this.resultMessage = resultMessage;
        this.sentAt = new Date();
    }

    /**
     * Mark as failed. Updates retry status and determines if further retry is needed.
     */
    public void markAsFailed(String resultMessage) {
        this.resultMessage = resultMessage;
        this.sentAt = new Date();
        retryStatus.update(MAX_RETRY_COUNT);

        if (isNeedRetry()) {
            this.status = NoticeStatus.PENDING;
        } else {
            this.status = NoticeStatus.FAILED;
        }
    }

    public boolean isNeedRetry() {
        return retryStatus.getRetryNum() < MAX_RETRY_COUNT;
    }

    public String getTargetAddress() {
        return target != null ? target.getFormattedAddress() : null;
    }

    public int getCurrentRetryCount() {
        return retryStatus != null ? retryStatus.getRetryNum() : 0;
    }

    public Date getNextRetryAt() {
        return retryStatus != null ? retryStatus.getNextRetryAt() : null;
    }
}
```

### 4.2 NoticeTemplateAggregate

```java
package vip.mate.biz.notice.domain.model.aggregate;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import vip.mate.base.model.entity.BaseEntity;
import vip.mate.biz.notice.types.enums.NoticeChannel;
import vip.mate.starter.distribute.util.SnowflakeUtil;

/**
 * Notice template aggregate root.
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class NoticeTemplateAggregate extends BaseEntity {

    /** Template name */
    private String name;

    /** Template code (unique business identifier) */
    private String code;

    /** Template content with placeholders, e.g., "Your code is ${code}" */
    private String content;

    /** Channel this template applies to */
    private NoticeChannel channel;

    /** Whether this template is active */
    private Boolean enabled;

    public static NoticeTemplateAggregate create(String name, String code,
                                                  String content, NoticeChannel channel) {
        return NoticeTemplateAggregate.builder()
                .id(SnowflakeUtil.newSnowflakeId())
                .name(name)
                .code(code)
                .content(content)
                .channel(channel)
                .enabled(true)
                .build();
    }
}
```

### 4.3 RetryStatus Value Object

```java
package vip.mate.biz.notice.domain.model.valobj;

import lombok.Builder;
import lombok.Getter;

import java.util.Date;

/**
 * Retry status value object.
 * <p>
 * Tracks retry count and next retry time with exponential-like backoff
 * (fixed 10-second interval for simplicity).
 * </p>
 */
@Getter
@Builder
public class RetryStatus {

    /** Retry interval in milliseconds (10 seconds) */
    private static final long RETRY_INTERVAL_MS = 10_000L;

    /** Next retry time */
    private Date nextRetryAt;

    /** Current retry count */
    private Integer retryNum;

    public static RetryStatus init() {
        return RetryStatus.builder()
                .nextRetryAt(null)
                .retryNum(0)
                .build();
    }

    public static RetryStatus create(Date nextRetryAt, Integer retryNum) {
        return RetryStatus.builder()
                .nextRetryAt(nextRetryAt)
                .retryNum(retryNum)
                .build();
    }

    /**
     * Update retry status after a failed attempt.
     * Increments retry count and sets next retry time if under max.
     */
    public void update(int maxRetryCount) {
        if (retryNum >= maxRetryCount - 1) {
            nextRetryAt = null; // no more retries
        } else {
            nextRetryAt = new Date(System.currentTimeMillis() + RETRY_INTERVAL_MS);
        }
        retryNum++;
    }
}
```

### 4.4 TargetAddress Value Object

```java
package vip.mate.biz.notice.domain.model.valobj;

import lombok.Getter;
import vip.mate.biz.notice.types.enums.NoticeChannel;
import vip.mate.biz.notice.types.exception.NoticeErrorCode;
import vip.mate.biz.notice.types.exception.NoticeException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Target address value object.
 * <p>
 * Encapsulates and validates the notification target address based on channel type:
 * SMS (phone number), Email (email address), WeChat (OpenID), Push (device token).
 * </p>
 */
@Getter
public class TargetAddress {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final String address;
    private final NoticeChannel channel;

    private TargetAddress(String address, NoticeChannel channel) {
        this.address = address;
        this.channel = channel;
        validate();
    }

    public static TargetAddress of(String address, NoticeChannel channel) {
        return new TargetAddress(address, channel);
    }

    private void validate() {
        if (address == null || address.trim().isEmpty()) {
            throw NoticeException.of(NoticeErrorCode.TARGET_EMPTY);
        }
        if (channel == null) {
            throw NoticeException.of(NoticeErrorCode.NOTICE_TYPE_EMPTY);
        }

        String trimmed = address.trim();
        boolean valid = switch (channel) {
            case SMS -> PHONE_PATTERN.matcher(trimmed).matches();
            case EMAIL -> EMAIL_PATTERN.matcher(trimmed).matches();
            case WECHAT -> trimmed.length() >= 20 && trimmed.length() <= 50;
            case PUSH -> trimmed.length() >= 10 && trimmed.length() <= 256;
        };

        if (!valid) {
            throw NoticeException.of(NoticeErrorCode.TARGET_EMPTY);
        }
    }

    public String getFormattedAddress() {
        return address.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TargetAddress that = (TargetAddress) o;
        return Objects.equals(address, that.address) && channel == that.channel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, channel);
    }
}
```

### 4.5 INoticeAdapter (Domain Port)

```java
package vip.mate.biz.notice.domain.adapter.port;

import vip.mate.base.result.Result;

/**
 * Notification adapter port (domain layer interface).
 * <p>
 * Implemented by infrastructure adapters for each notification channel.
 * </p>
 */
public interface INoticeAdapter {

    /**
     * Send a notification.
     *
     * @param target  the target address (phone, email, openid, device token)
     * @param content the notification content
     * @return result indicating success or failure
     */
    Result<Void> send(String target, String content);
}
```

### 4.6 INoticeAdapterFactory (Domain Factory)

```java
package vip.mate.biz.notice.domain.factory;

import vip.mate.biz.notice.domain.adapter.port.INoticeAdapter;
import vip.mate.biz.notice.types.enums.NoticeChannel;

/**
 * Factory interface for resolving notification adapters by channel.
 */
public interface INoticeAdapterFactory {

    /**
     * Get the adapter for the given notification channel.
     *
     * @param channel notification channel
     * @return the adapter implementation
     * @throws vip.mate.biz.notice.types.exception.NoticeException if channel is unsupported
     */
    INoticeAdapter getAdapter(NoticeChannel channel);
}
```

### 4.7 INoticeRepository

```java
package vip.mate.biz.notice.domain.adapter.repository;

import vip.mate.biz.notice.domain.model.aggregate.NoticeAggregate;

import java.util.List;

/**
 * Notice repository port.
 */
public interface INoticeRepository {

    void save(NoticeAggregate notice);

    void update(NoticeAggregate notice);

    NoticeAggregate findById(String id);

    List<NoticeAggregate> findRetryNotices(int batchSize);

    void cacheVerifyCode(String targetType, String mobile, String code);

    boolean checkVerifyCode(String targetType, String mobile, String code);
}
```

### 4.8 INoticeTemplateRepository

```java
package vip.mate.biz.notice.domain.adapter.repository;

import vip.mate.biz.notice.domain.model.aggregate.NoticeTemplateAggregate;

/**
 * Notice template repository port.
 */
public interface INoticeTemplateRepository {

    void save(NoticeTemplateAggregate template);

    NoticeTemplateAggregate findByCode(String code);

    NoticeTemplateAggregate findById(String id);
}
```

---

## 5. Infrastructure Layer

### 5.1 AbstractNoticeAdapter

```java
package vip.mate.biz.notice.infrastructure.adapter;

import lombok.extern.slf4j.Slf4j;
import vip.mate.base.result.Result;
import vip.mate.biz.notice.domain.adapter.port.INoticeAdapter;
import vip.mate.biz.notice.types.exception.NoticeErrorCode;

/**
 * Abstract base class for notification adapters.
 * <p>
 * Provides common error handling and logging. Subclasses implement
 * {@link #doSend(String, String)} for channel-specific logic.
 * </p>
 */
@Slf4j
public abstract class AbstractNoticeAdapter implements INoticeAdapter {

    @Override
    public Result<Void> send(String target, String content) {
        try {
            log.info("[notice] Sending to target={}, contentLength={}", target, content.length());
            return doSend(target, content);
        } catch (Exception e) {
            log.error("[notice] Send failed: target={}, error={}", target, e.getMessage(), e);
            return Result.error(NoticeErrorCode.NOTICE_SEND_FAILED.getCode(), e.getMessage());
        }
    }

    /**
     * Channel-specific send implementation.
     */
    protected abstract Result<Void> doSend(String target, String content);
}
```

### 5.2 SmsNoticeAdapter

```java
package vip.mate.biz.notice.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vip.mate.base.result.Result;
import vip.mate.biz.notice.types.annotation.NoticeAdapter;
import vip.mate.biz.notice.types.enums.NoticeChannel;

/**
 * SMS notification adapter.
 * <p>
 * Integrates with the configured SMS provider (e.g., Alibaba Cloud SMS, Tencent Cloud SMS).
 * The actual SMS SDK should be injected here.
 * </p>
 */
@Slf4j
@NoticeAdapter(NoticeChannel.SMS)
@RequiredArgsConstructor
public class SmsNoticeAdapter extends AbstractNoticeAdapter {

    // Inject SMS SDK service bean here
    // private final SmsService smsService;

    @Override
    protected Result<Void> doSend(String target, String content) {
        log.info("[notice-sms] Sending SMS to {}", target);
        // return smsService.sendSmsMsg(target, content);
        // TODO: integrate with actual SMS provider
        return Result.success();
    }
}
```

### 5.3 EmailNoticeAdapter

```java
package vip.mate.biz.notice.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import vip.mate.base.result.Result;
import vip.mate.biz.notice.types.annotation.NoticeAdapter;
import vip.mate.biz.notice.types.enums.NoticeChannel;

/**
 * Email notification adapter.
 * Uses Spring Boot Mail for email delivery.
 */
@Slf4j
@NoticeAdapter(NoticeChannel.EMAIL)
@RequiredArgsConstructor
public class EmailNoticeAdapter extends AbstractNoticeAdapter {

    private final JavaMailSender mailSender;

    @Override
    protected Result<Void> doSend(String target, String content) {
        log.info("[notice-email] Sending email to {}", target);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@mate.vip");
        message.setTo(target);
        message.setSubject("MateCloud Notification");
        message.setText(content);
        mailSender.send(message);
        return Result.success();
    }
}
```

### 5.4 WeChatNoticeAdapter

```java
package vip.mate.biz.notice.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vip.mate.base.result.Result;
import vip.mate.biz.notice.types.annotation.NoticeAdapter;
import vip.mate.biz.notice.types.enums.NoticeChannel;

/**
 * WeChat template message notification adapter.
 */
@Slf4j
@NoticeAdapter(NoticeChannel.WECHAT)
@RequiredArgsConstructor
public class WeChatNoticeAdapter extends AbstractNoticeAdapter {

    @Override
    protected Result<Void> doSend(String target, String content) {
        log.info("[notice-wechat] Sending WeChat message to openId={}", target);
        // TODO: integrate with WeChat Official Account Template Message API
        return Result.success();
    }
}
```

### 5.5 PushNoticeAdapter

```java
package vip.mate.biz.notice.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vip.mate.base.result.Result;
import vip.mate.biz.notice.types.annotation.NoticeAdapter;
import vip.mate.biz.notice.types.enums.NoticeChannel;

/**
 * App push notification adapter.
 */
@Slf4j
@NoticeAdapter(NoticeChannel.PUSH)
@RequiredArgsConstructor
public class PushNoticeAdapter extends AbstractNoticeAdapter {

    @Override
    protected Result<Void> doSend(String target, String content) {
        log.info("[notice-push] Sending push to deviceToken={}", target);
        // TODO: integrate with push provider (JPush, Firebase, etc.)
        return Result.success();
    }
}
```

### 5.6 NoticeAdapterFactoryImpl

```java
package vip.mate.biz.notice.infrastructure.factory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.mate.biz.notice.domain.adapter.port.INoticeAdapter;
import vip.mate.biz.notice.domain.factory.INoticeAdapterFactory;
import vip.mate.biz.notice.types.annotation.NoticeAdapter;
import vip.mate.biz.notice.types.enums.NoticeChannel;
import vip.mate.biz.notice.types.exception.NoticeErrorCode;
import vip.mate.biz.notice.types.exception.NoticeException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory implementation that auto-discovers adapters via {@link NoticeAdapter} annotation.
 * <p>
 * On construction, iterates all {@link INoticeAdapter} beans, reads their
 * {@code @NoticeAdapter} annotation, and registers them in a map keyed by
 * {@link NoticeChannel}. Duplicate channel registrations are logged as warnings.
 * </p>
 */
@Slf4j
@Component
public class NoticeAdapterFactoryImpl implements INoticeAdapterFactory {

    private final Map<NoticeChannel, INoticeAdapter> adapterMap;

    public NoticeAdapterFactoryImpl(List<INoticeAdapter> adapters) {
        this.adapterMap = new HashMap<>();

        for (INoticeAdapter adapter : adapters) {
            NoticeAdapter annotation = adapter.getClass().getAnnotation(NoticeAdapter.class);
            if (annotation != null) {
                NoticeChannel channel = annotation.value();
                if (adapterMap.containsKey(channel)) {
                    log.warn("[notice-factory] Duplicate adapter for channel {}, ignoring {}",
                            channel, adapter.getClass().getName());
                } else {
                    adapterMap.put(channel, adapter);
                    log.debug("[notice-factory] Registered adapter: {} -> {}",
                            channel, adapter.getClass().getSimpleName());
                }
            }
        }

        log.info("[notice-factory] Initialized with {} adapters: {}", adapterMap.size(), adapterMap.keySet());
    }

    @Override
    public INoticeAdapter getAdapter(NoticeChannel channel) {
        if (channel == null) {
            throw NoticeException.of(NoticeErrorCode.NOTICE_TYPE_EMPTY);
        }

        INoticeAdapter adapter = adapterMap.get(channel);
        if (adapter == null) {
            log.error("[notice-factory] No adapter found for channel: {}", channel);
            throw NoticeException.of(NoticeErrorCode.UNSUPPORTED_NOTICE_TYPE);
        }

        return adapter;
    }
}
```

### 5.7 NoticePO (Persistence Object)

```java
package vip.mate.biz.notice.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Notice persistence object.
 */
@Data
@TableName("mate_notice")
public class NoticePO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** Channel: SMS, EMAIL, WECHAT, PUSH */
    private String channel;

    /** Target address */
    private String target;

    /** Delivery status: 0=PENDING, 1=SUCCESS, 2=FAILED */
    private Integer status;

    /** Business type */
    private String businessType;

    /** Notification content */
    private String content;

    /** Send result message */
    private String resultMessage;

    /** Actual send time */
    private LocalDateTime sentAt;

    /** Retry count */
    private Integer retryNum;

    /** Next retry time */
    private LocalDateTime nextRetryAt;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

### 5.8 NoticeTemplatePO

```java
package vip.mate.biz.notice.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Notice template persistence object.
 */
@Data
@TableName("mate_notice_template")
public class NoticeTemplatePO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** Template name */
    private String name;

    /** Template code (unique) */
    private String code;

    /** Template content with placeholders */
    private String content;

    /** Channel: SMS, EMAIL, WECHAT, PUSH */
    private String channel;

    /** Is enabled */
    private Boolean enabled;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

### 5.9 NoticeDao and NoticeTemplateDao

```java
package vip.mate.biz.notice.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import vip.mate.biz.notice.infrastructure.dao.po.NoticePO;

@Mapper
public interface NoticeDao extends BaseMapper<NoticePO> {
}
```

```java
package vip.mate.biz.notice.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import vip.mate.biz.notice.infrastructure.dao.po.NoticeTemplatePO;

@Mapper
public interface NoticeTemplateDao extends BaseMapper<NoticeTemplatePO> {
}
```

---

## 6. Application Layer

### 6.1 NoticeCommandService

```java
package vip.mate.biz.notice.application;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.base.result.Result;
import vip.mate.biz.notice.domain.adapter.port.INoticeAdapter;
import vip.mate.biz.notice.domain.adapter.repository.INoticeRepository;
import vip.mate.biz.notice.domain.factory.INoticeAdapterFactory;
import vip.mate.biz.notice.domain.model.aggregate.NoticeAggregate;
import vip.mate.biz.notice.domain.model.valobj.TargetAddress;
import vip.mate.biz.notice.types.enums.BusinessType;
import vip.mate.biz.notice.types.enums.NoticeChannel;

import java.util.List;

/**
 * Notification command service (application layer).
 * <p>
 * Orchestrates the send/retry workflow: creates the aggregate, resolves
 * the adapter via factory, sends, and persists the result.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeCommandService {

    private final INoticeAdapterFactory adapterFactory;
    private final INoticeRepository noticeRepository;

    /**
     * Send a single notification.
     */
    @Transactional(rollbackFor = Exception.class)
    public String send(NoticeChannel channel, String target, BusinessType businessType, String content) {
        // 1. Create aggregate
        TargetAddress targetAddress = TargetAddress.of(target, channel);
        NoticeAggregate notice = NoticeAggregate.create(channel, targetAddress, businessType, content);

        // 2. Resolve adapter and send
        INoticeAdapter adapter = adapterFactory.getAdapter(channel);
        Result<Void> result = adapter.send(notice.getTargetAddress(), content);

        // 3. Update status based on result
        if (result.getSuccess()) {
            notice.markAsSuccess(JSONUtil.toJsonStr(result));
            log.info("[notice] Send success: channel={}, target={}", channel, target);
        } else {
            notice.markAsFailed(JSONUtil.toJsonStr(result));
            log.error("[notice] Send failed: channel={}, target={}, msg={}", channel, target, result.getMsg());
        }

        // 4. Persist
        noticeRepository.save(notice);
        return notice.getId();
    }

    /**
     * Batch send notifications (same content to multiple targets).
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchSend(NoticeChannel channel, List<String> targets,
                          BusinessType businessType, String content) {
        for (String target : targets) {
            try {
                send(channel, target, businessType, content);
            } catch (Exception e) {
                log.error("[notice] Batch send error for target={}: {}", target, e.getMessage(), e);
            }
        }
    }

    /**
     * Retry a single failed notification.
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean retry(NoticeAggregate notice) {
        INoticeAdapter adapter = adapterFactory.getAdapter(notice.getChannel());
        Result<Void> result = adapter.send(notice.getTargetAddress(), notice.getContent());

        if (result.getSuccess()) {
            notice.markAsSuccess(JSONUtil.toJsonStr(result));
            log.info("[notice] Retry success: id={}, target={}", notice.getId(), notice.getTargetAddress());
        } else {
            notice.markAsFailed(JSONUtil.toJsonStr(result));
            log.error("[notice] Retry failed: id={}, target={}", notice.getId(), notice.getTargetAddress());
        }

        noticeRepository.update(notice);
        return result.getSuccess();
    }
}
```

### 6.2 NoticeQueryService

```java
package vip.mate.biz.notice.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.biz.notice.domain.adapter.repository.INoticeRepository;
import vip.mate.biz.notice.domain.adapter.repository.INoticeTemplateRepository;
import vip.mate.biz.notice.domain.model.aggregate.NoticeAggregate;
import vip.mate.biz.notice.domain.model.aggregate.NoticeTemplateAggregate;

/**
 * Notification query service (application layer).
 */
@Service
@RequiredArgsConstructor
public class NoticeQueryService {

    private final INoticeRepository noticeRepository;
    private final INoticeTemplateRepository templateRepository;

    public NoticeAggregate findById(String id) {
        return noticeRepository.findById(id);
    }

    public NoticeTemplateAggregate findTemplateByCode(String code) {
        return templateRepository.findByCode(code);
    }
}
```

---

## 7. Trigger Layer

### 7.1 NoticeController

```java
package vip.mate.biz.notice.trigger.http;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vip.mate.base.result.Result;
import vip.mate.biz.notice.application.NoticeCommandService;
import vip.mate.biz.notice.application.NoticeQueryService;
import vip.mate.biz.notice.domain.model.aggregate.NoticeAggregate;
import vip.mate.biz.notice.types.enums.BusinessType;
import vip.mate.biz.notice.types.enums.NoticeChannel;

import java.util.List;

/**
 * Notification HTTP controller.
 */
@RestController
@RequestMapping("/api/v1/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeCommandService commandService;
    private final NoticeQueryService queryService;

    @PostMapping("/send")
    public Result<String> send(@RequestParam NoticeChannel channel,
                               @RequestParam String target,
                               @RequestParam BusinessType businessType,
                               @RequestParam String content) {
        String noticeId = commandService.send(channel, target, businessType, content);
        return Result.success(noticeId);
    }

    @PostMapping("/batch-send")
    public Result<Void> batchSend(@RequestParam NoticeChannel channel,
                                  @RequestBody List<String> targets,
                                  @RequestParam BusinessType businessType,
                                  @RequestParam String content) {
        commandService.batchSend(channel, targets, businessType, content);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<NoticeAggregate> getById(@PathVariable String id) {
        return Result.success(queryService.findById(id));
    }
}
```

### 7.2 RpcNoticeServiceImpl

```java
package vip.mate.biz.notice.trigger.rpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import vip.mate.api.notice.IRpcNoticeService;
import vip.mate.api.notice.command.SendNoticeCommand;
import vip.mate.base.result.Result;
import vip.mate.biz.notice.application.NoticeCommandService;
import vip.mate.biz.notice.types.enums.BusinessType;
import vip.mate.biz.notice.types.enums.NoticeChannel;

/**
 * Dubbo RPC notification service implementation.
 * <p>
 * Exposes notification capabilities to other microservices via Dubbo RPC.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@DubboService(version = "1.0.0")
public class RpcNoticeServiceImpl implements IRpcNoticeService {

    private final NoticeCommandService commandService;

    @Override
    public Result<String> sendNotice(SendNoticeCommand command) {
        String noticeId = commandService.send(
                NoticeChannel.valueOf(command.getChannel()),
                command.getTarget(),
                BusinessType.valueOf(command.getBusinessType()),
                command.getContent()
        );
        return Result.success(noticeId);
    }
}
```

### 7.3 NoticeRetryConstants

```java
package vip.mate.biz.notice.trigger.job;

/**
 * Constants for the notification retry job.
 */
public final class NoticeRetryConstants {

    private NoticeRetryConstants() {
    }

    /** Number of notices fetched per batch */
    public static final int RETRY_BATCH_SIZE = 200;

    /** Maximum loop iterations per job execution */
    public static final int MAX_LOOP_COUNT = 100;

    /** Maximum total notices processed per job execution */
    public static final int MAX_PROCESS_COUNT = 10_000;

    /** Timeout for parallel processing per batch (seconds) */
    public static final int PARALLEL_TIMEOUT_SECONDS = 300;

    /**
     * Result status for individual retry attempts.
     */
    public enum RetryStatus {
        SUCCESS,
        FAILED,
        TIMEOUT,
        EXCEPTION
    }
}
```

### 7.4 NoticeRetryJob

```java
package vip.mate.biz.notice.trigger.job;

import cn.hutool.json.JSONUtil;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vip.mate.base.result.Result;
import vip.mate.biz.notice.domain.adapter.port.INoticeAdapter;
import vip.mate.biz.notice.domain.adapter.repository.INoticeRepository;
import vip.mate.biz.notice.domain.factory.INoticeAdapterFactory;
import vip.mate.biz.notice.domain.model.aggregate.NoticeAggregate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Notification retry job.
 * <p>
 * Scheduled via XXL-Job. Processes failed notifications in batches of 200,
 * up to 10,000 total per execution. Uses CompletableFuture for parallel
 * processing within each batch.
 * </p>
 *
 * <p>Algorithm:</p>
 * <ol>
 *   <li>Query up to 200 PENDING notices with retry_num &lt; max and next_retry_at &lt;= now</li>
 *   <li>Submit each to thread pool via CompletableFuture</li>
 *   <li>Wait for batch completion (timeout: 300s)</li>
 *   <li>Collect statistics and repeat until no more or limits reached</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeRetryJob {

    private final INoticeRepository noticeRepository;
    private final INoticeAdapterFactory adapterFactory;

    @Resource
    private ThreadPoolExecutor noticeRetryExecutor;

    @XxlJob("noticeRetryHandler")
    public void noticeRetryHandler() {
        log.info("[notice-retry] Starting notification retry job");
        XxlJobHelper.log("Starting notification retry job (parallel processing)");

        int totalProcessed = 0;
        int totalSuccess = 0;
        int totalFailed = 0;
        int loopCount = 0;

        try {
            while (loopCount < NoticeRetryConstants.MAX_LOOP_COUNT
                    && totalProcessed < NoticeRetryConstants.MAX_PROCESS_COUNT) {

                loopCount++;

                // Query batch
                List<NoticeAggregate> retryNotices = noticeRepository.findRetryNotices(
                        NoticeRetryConstants.RETRY_BATCH_SIZE);
                if (retryNotices.isEmpty()) {
                    log.info("[notice-retry] Loop {}: no more notices to retry", loopCount);
                    break;
                }

                int batchSize = retryNotices.size();
                totalProcessed += batchSize;
                log.info("[notice-retry] Loop {}: processing {} notices", loopCount, batchSize);

                // Parallel processing via CompletableFuture
                List<CompletableFuture<NoticeRetryConstants.RetryStatus>> futures = retryNotices.stream()
                        .map(notice -> CompletableFuture.supplyAsync(
                                () -> retrySingleNotice(notice), noticeRetryExecutor))
                        .toList();

                // Wait for all to complete
                try {
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .get(NoticeRetryConstants.PARALLEL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.warn("[notice-retry] Parallel processing timeout or error", e);
                }

                // Collect results
                int batchSuccess = 0;
                int batchFailed = 0;
                for (CompletableFuture<NoticeRetryConstants.RetryStatus> future : futures) {
                    try {
                        if (future.isDone() && !future.isCompletedExceptionally()) {
                            NoticeRetryConstants.RetryStatus status =
                                    future.getNow(NoticeRetryConstants.RetryStatus.FAILED);
                            if (status == NoticeRetryConstants.RetryStatus.SUCCESS) {
                                batchSuccess++;
                            } else {
                                batchFailed++;
                            }
                        } else {
                            batchFailed++;
                        }
                    } catch (Exception e) {
                        batchFailed++;
                    }
                }

                totalSuccess += batchSuccess;
                totalFailed += batchFailed;

                String batchResult = String.format(
                        "Loop %d: processed=%d, success=%d, failed=%d",
                        loopCount, batchSize, batchSuccess, batchFailed);
                XxlJobHelper.log(batchResult);
                log.info("[notice-retry] {}", batchResult);

                // If batch was not full, no more data
                if (batchSize < NoticeRetryConstants.RETRY_BATCH_SIZE) {
                    break;
                }
            }

            String finalResult = String.format(
                    "Retry job completed: loops=%d, total=%d, success=%d, failed=%d",
                    loopCount, totalProcessed, totalSuccess, totalFailed);
            XxlJobHelper.log(finalResult);
            log.info("[notice-retry] {}", finalResult);

        } catch (Exception e) {
            String errorMsg = String.format(
                    "Retry job exception: processed=%d, success=%d, failed=%d, error=%s",
                    totalProcessed, totalSuccess, totalFailed, e.getMessage());
            XxlJobHelper.log(errorMsg);
            log.error("[notice-retry] {}", errorMsg, e);
        }
    }

    private NoticeRetryConstants.RetryStatus retrySingleNotice(NoticeAggregate notice) {
        try {
            INoticeAdapter adapter = adapterFactory.getAdapter(notice.getChannel());
            Result<Void> result = adapter.send(notice.getTargetAddress(), notice.getContent());

            if (result.getSuccess()) {
                notice.markAsSuccess(JSONUtil.toJsonStr(result));
                log.info("[notice-retry] Success: id={}", notice.getId());
            } else {
                notice.markAsFailed(JSONUtil.toJsonStr(result));
                log.warn("[notice-retry] Failed: id={}, msg={}", notice.getId(), result.getMsg());
            }

            noticeRepository.update(notice);
            return result.getSuccess()
                    ? NoticeRetryConstants.RetryStatus.SUCCESS
                    : NoticeRetryConstants.RetryStatus.FAILED;

        } catch (Exception e) {
            log.error("[notice-retry] Exception for id={}: {}", notice.getId(), e.getMessage(), e);
            return NoticeRetryConstants.RetryStatus.EXCEPTION;
        }
    }
}
```

---

## 8. SQL Schema

### 8.1 mate_notice

```sql
CREATE TABLE `mate_notice` (
    `id` VARCHAR(32) NOT NULL COMMENT 'Notice ID (Snowflake)',
    `channel` VARCHAR(20) NOT NULL COMMENT 'Channel: SMS, EMAIL, WECHAT, PUSH',
    `target` VARCHAR(255) NOT NULL COMMENT 'Target address (phone, email, openid, device token)',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT 'Status: 0=PENDING, 1=SUCCESS, 2=FAILED',
    `business_type` VARCHAR(50) NOT NULL COMMENT 'Business type: VERIFY_CODE, ORDER_NOTIFY, etc.',
    `content` TEXT NOT NULL COMMENT 'Notification content',
    `result_message` TEXT NULL COMMENT 'Send result (JSON)',
    `sent_at` DATETIME NULL COMMENT 'Actual send time',
    `retry_num` INT NOT NULL DEFAULT 0 COMMENT 'Current retry count',
    `next_retry_at` DATETIME NULL COMMENT 'Next retry time',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (`id`),
    INDEX `idx_status_retry` (`status`, `next_retry_at`),
    INDEX `idx_target` (`target`),
    INDEX `idx_channel_status` (`channel`, `status`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Notification records';
```

### 8.2 mate_notice_template

```sql
CREATE TABLE `mate_notice_template` (
    `id` VARCHAR(32) NOT NULL COMMENT 'Template ID (Snowflake)',
    `name` VARCHAR(100) NOT NULL COMMENT 'Template name',
    `code` VARCHAR(100) NOT NULL COMMENT 'Template code (unique)',
    `content` TEXT NOT NULL COMMENT 'Template content with placeholders',
    `channel` VARCHAR(20) NOT NULL COMMENT 'Channel: SMS, EMAIL, WECHAT, PUSH',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Is enabled',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_code` (`code`),
    INDEX `idx_channel` (`channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Notification templates';
```

---

## 9. application.yml

```yaml
server:
  port: 9050

spring:
  application:
    name: mate-notice
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/mate_notice?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD:root}
  mail:
    host: smtp.example.com
    port: 465
    username: noreply@mate.vip
    password: ${MAIL_PASSWORD:}
    properties:
      mail.smtp.ssl.enable: true

mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true

# Dubbo
dubbo:
  application:
    name: mate-notice
  registry:
    address: nacos://${NACOS_ADDR:localhost:8848}
  protocol:
    name: dubbo
    port: 20893

# XXL-Job
xxl:
  job:
    admin:
      addresses: http://${XXL_JOB_ADDR:localhost:8080}/xxl-job-admin
    executor:
      appname: mate-notice
      port: 9999
```

---

## 10. Design Notes

1. **Adapter pattern with annotation-driven registration**: The `@NoticeAdapter` annotation (meta-annotated with `@Component`) ensures Spring auto-detects adapter beans. The factory collects them by inspecting the annotation value. Adding a new channel requires only implementing `AbstractNoticeAdapter` with `@NoticeAdapter(NoticeChannel.NEW_CHANNEL)`.

2. **Retry mechanism**: The retry job uses a batch-loop pattern (200 per batch) with `CompletableFuture` parallel processing. Safety limits prevent runaway processing: max 100 loops and 10,000 total notices per execution.

3. **DDD structure**: Domain layer defines ports (INoticeAdapter, INoticeRepository) and aggregates. Infrastructure implements them. Application layer orchestrates. Trigger layer handles HTTP, RPC, and scheduled job entry points.

4. **Thread pool**: The `noticeRetryExecutor` bean should be provided via mate-dynamic-tp-starter for dynamic management via Nacos. See RFC-019 for details.

5. **Port 9050**: Consistent with the matecloud service port allocation scheme.
