# RFC-014: Developer Experience & Engineering

- **Status**: Draft
- **Created**: 2026-04-12
- **Author**: MateCloud Team
- **Wave**: 5
- **Dependencies**: RFC-003~005 (starters), RFC-010 (CLI)

## Overview

Developer experience determines adoption. This RFC provides three new starters (`mate-idempotent-starter`, `mate-gray-starter`, `mate-devtools-starter`) and adds `AsyncTaskExecutor` + `VirtualThreadAsyncTaskExecutor` to `mate-base`.

---

## 1. Async Task Framework (mate-base)

### 1.1 Task Progress Model

**File**: `mate-common/mate-base/src/main/java/vip/mate/base/async/TaskState.java`

```java
package vip.mate.base.async;

/**
 * Lifecycle states for an async task.
 */
public enum TaskState {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED
}
```

**File**: `mate-common/mate-base/src/main/java/vip/mate/base/async/TaskProgress.java`

```java
package vip.mate.base.async;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * Progress tracker for an async task. Shared between producer (task thread) and
 * consumer (polling endpoint).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskProgress implements Serializable {

    private String taskId;
    private String taskName;
    private TaskState state;
    private int total;
    private int current;
    private String message;
    private Date startTime;
    private Date endTime;

    public TaskProgress(String taskId, String taskName, TaskState state) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.state = state;
    }

    /**
     * Calculate percentage (0-100). Returns 0 if total is 0.
     */
    public int getPercent() {
        return total == 0 ? 0 : (int) ((current * 100L) / total);
    }
}
```

### 1.2 AsyncTaskExecutor Interface

**File**: `mate-common/mate-base/src/main/java/vip/mate/base/async/AsyncTaskExecutor.java`

```java
package vip.mate.base.async;

/**
 * Unified abstraction for submitting and tracking long-running async tasks.
 */
public interface AsyncTaskExecutor {

    /**
     * Submit a named task for async execution.
     *
     * @param taskName human-readable name (used in logs and progress)
     * @param task     the work to execute
     * @return the assigned task ID for progress polling
     */
    String submit(String taskName, Runnable task);

    /**
     * Get the current progress of a task.
     *
     * @param taskId the task ID returned by submit()
     * @return progress or null if not found
     */
    TaskProgress getProgress(String taskId);

    /**
     * Request cancellation of a task. The task must cooperate by checking
     * Thread.interrupted().
     *
     * @param taskId the task ID
     */
    void cancel(String taskId);
}
```

### 1.3 Virtual Thread Implementation

**File**: `mate-common/mate-base/src/main/java/vip/mate/base/async/VirtualThreadAsyncTaskExecutor.java`

```java
package vip.mate.base.async;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AsyncTaskExecutor backed by Java 21 virtual threads.
 * Tracks progress in a ConcurrentHashMap (in-memory, per-JVM).
 * For distributed progress tracking, replace the map with Redis.
 */
public class VirtualThreadAsyncTaskExecutor implements AsyncTaskExecutor {

    private final Map<String, TaskProgress> tasks = new ConcurrentHashMap<>();
    private final Map<String, Thread> threads = new ConcurrentHashMap<>();

    /** Max number of completed tasks to retain in memory */
    private static final int MAX_HISTORY = 500;

    @Override
    public String submit(String taskName, Runnable task) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        TaskProgress progress = new TaskProgress(taskId, taskName, TaskState.PENDING);
        tasks.put(taskId, progress);

        Thread vThread = Thread.ofVirtual()
                .name("async-" + taskName + "-" + taskId.substring(0, 8))
                .start(() -> {
                    progress.setState(TaskState.RUNNING);
                    progress.setStartTime(new Date());
                    try {
                        task.run();
                        progress.setState(TaskState.SUCCESS);
                    } catch (Exception e) {
                        progress.setState(TaskState.FAILED);
                        progress.setMessage(e.getMessage());
                    } finally {
                        progress.setEndTime(new Date());
                        threads.remove(taskId);
                        evictOldEntries();
                    }
                });
        threads.put(taskId, vThread);

        return taskId;
    }

    @Override
    public TaskProgress getProgress(String taskId) {
        return tasks.get(taskId);
    }

    @Override
    public void cancel(String taskId) {
        TaskProgress progress = tasks.get(taskId);
        if (progress != null && progress.getState() == TaskState.RUNNING) {
            Thread thread = threads.get(taskId);
            if (thread != null) {
                thread.interrupt();
            }
            progress.setState(TaskState.CANCELLED);
            progress.setEndTime(new Date());
        }
    }

    /**
     * Evict completed tasks beyond MAX_HISTORY to prevent memory leak.
     */
    private void evictOldEntries() {
        if (tasks.size() > MAX_HISTORY) {
            tasks.entrySet().stream()
                    .filter(e -> e.getValue().getEndTime() != null)
                    .sorted((a, b) -> a.getValue().getEndTime().compareTo(b.getValue().getEndTime()))
                    .limit(tasks.size() - MAX_HISTORY)
                    .forEach(e -> tasks.remove(e.getKey()));
        }
    }
}
```

---

## 2. Idempotency Framework (mate-idempotent-starter)

### 2.1 pom.xml

**File**: `mate-starters/mate-idempotent-starter/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>mate-starters</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>mate-idempotent-starter</artifactId>
    <packaging>jar</packaging>
    <name>mate-idempotent-starter</name>
    <description>Idempotency framework starter - PARAM/TOKEN/HEADER modes via Redis</description>

    <dependencies>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-base</artifactId>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-cache-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure-processor</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

### 2.2 Annotation & Enum

**File**: `mate-starters/mate-idempotent-starter/src/main/java/vip/mate/starter/idempotent/annotation/IdempotentType.java`

```java
package vip.mate.starter.idempotent.annotation;

/**
 * Strategy for computing the idempotency key.
 */
public enum IdempotentType {
    /** Hash of method parameters via SpEL key expression */
    PARAM,
    /** Client obtains a one-time token, sends it with the request */
    TOKEN,
    /** Key from a specific HTTP request header (e.g. X-Idempotent-Key) */
    HEADER
}
```

**File**: `mate-starters/mate-idempotent-starter/src/main/java/vip/mate/starter/idempotent/annotation/Idempotent.java`

```java
package vip.mate.starter.idempotent.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as idempotent. Duplicate invocations within the expiry window
 * will be rejected with a business exception.
 *
 * <p>Usage:</p>
 * <pre>
 * &#64;Idempotent(key = "#cmd.orderNo", expireSeconds = 3600)
 * &#64;PostMapping("/orders")
 * public Result&lt;String&gt; createOrder(@RequestBody CreateOrderCommand cmd) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * SpEL expression for computing the idempotency key.
     * - PARAM mode: "#cmd.orderNo" or "#p0.mobile"
     * - TOKEN mode: not used (key comes from the token itself)
     * - HEADER mode: header name, e.g. "X-Idempotent-Key"
     */
    String key() default "";

    /** How long (seconds) to keep the idempotency lock. Default: 24 hours. */
    long expireSeconds() default 86400;

    /** Error message shown to the caller on duplicate request. */
    String message() default "Duplicate request rejected";

    /** Key computation strategy. */
    IdempotentType type() default IdempotentType.PARAM;
}
```

### 2.3 Configuration Properties

**File**: `mate-starters/mate-idempotent-starter/src/main/java/vip/mate/starter/idempotent/config/IdempotentProperties.java`

```java
package vip.mate.starter.idempotent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the idempotent starter.
 */
@Data
@ConfigurationProperties(prefix = "mate.idempotent")
public class IdempotentProperties {

    /** Whether idempotent checking is enabled globally. */
    private boolean enabled = true;

    /** Redis key prefix for idempotent locks. */
    private String keyPrefix = "idem:";

    /** Default token validity in seconds (TOKEN mode). */
    private long tokenExpireSeconds = 600;

    /** Header name to read the idempotent key from (HEADER mode). */
    private String headerName = "X-Idempotent-Key";
}
```

### 2.4 AOP Aspect

**File**: `mate-starters/mate-idempotent-starter/src/main/java/vip/mate/starter/idempotent/aspect/IdempotentAspect.java`

```java
package vip.mate.starter.idempotent.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vip.mate.base.exception.BizException;
import vip.mate.starter.idempotent.annotation.Idempotent;
import vip.mate.starter.idempotent.annotation.IdempotentType;
import vip.mate.starter.idempotent.config.IdempotentProperties;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Objects;

/**
 * AOP aspect that enforces idempotency using Redis setNx.
 * On exception, the key is released to allow retry.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final RedissonClient redissonClient;
    private final IdempotentProperties properties;

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer DISCOVERER = new DefaultParameterNameDiscoverer();

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        if (!properties.isEnabled()) {
            return pjp.proceed();
        }

        String key = resolveKey(pjp, idempotent);
        String redisKey = properties.getKeyPrefix() + key;
        Duration ttl = Duration.ofSeconds(idempotent.expireSeconds());

        RBucket<String> bucket = redissonClient.getBucket(redisKey);
        boolean acquired = bucket.setIfAbsent("1", ttl);

        if (!acquired) {
            log.warn("Idempotent rejection: key={}", redisKey);
            throw new BizException("IDEMPOTENT_REJECT", idempotent.message());
        }

        try {
            return pjp.proceed();
        } catch (Throwable e) {
            // Release key on exception to allow retry
            bucket.delete();
            log.debug("Idempotent key released on exception: key={}", redisKey);
            throw e;
        }
    }

    private String resolveKey(ProceedingJoinPoint pjp, Idempotent idempotent) {
        return switch (idempotent.type()) {
            case PARAM -> resolveParamKey(pjp, idempotent.key());
            case TOKEN -> resolveTokenKey();
            case HEADER -> resolveHeaderKey(idempotent.key());
        };
    }

    /**
     * PARAM mode: evaluate SpEL expression against method parameters.
     */
    private String resolveParamKey(ProceedingJoinPoint pjp, String spelKey) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        Object[] args = pjp.getArgs();

        MethodBasedEvaluationContext ctx =
                new MethodBasedEvaluationContext(null, method, args, DISCOVERER);

        Object value = PARSER.parseExpression(spelKey).getValue(ctx);
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        return className + ":" + methodName + ":" + Objects.toString(value, "null");
    }

    /**
     * TOKEN mode: read the idempotent token from X-Idempotent-Token header
     * and verify it exists in Redis (one-time use).
     */
    private String resolveTokenKey() {
        HttpServletRequest request = getCurrentRequest();
        String token = request.getHeader("X-Idempotent-Token");
        if (token == null || token.isBlank()) {
            throw new BizException("IDEMPOTENT_TOKEN_MISSING", "Missing X-Idempotent-Token header");
        }

        // Verify and consume the token
        String tokenRedisKey = properties.getKeyPrefix() + "token:" + token;
        RBucket<String> tokenBucket = redissonClient.getBucket(tokenRedisKey);
        String val = tokenBucket.getAndDelete();
        if (val == null) {
            throw new BizException("IDEMPOTENT_TOKEN_INVALID", "Invalid or expired idempotent token");
        }

        return "token:" + token;
    }

    /**
     * HEADER mode: read key from a custom header.
     */
    private String resolveHeaderKey(String headerNameOrKey) {
        HttpServletRequest request = getCurrentRequest();
        String headerName = headerNameOrKey.isBlank() ? properties.getHeaderName() : headerNameOrKey;
        String headerValue = request.getHeader(headerName);
        if (headerValue == null || headerValue.isBlank()) {
            throw new BizException("IDEMPOTENT_HEADER_MISSING", "Missing header: " + headerName);
        }
        return "header:" + headerName + ":" + headerValue;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new BizException("IDEMPOTENT_ERROR", "No HTTP request context available");
        }
        return attrs.getRequest();
    }
}
```

### 2.5 Token Controller

**File**: `mate-starters/mate-idempotent-starter/src/main/java/vip/mate/starter/idempotent/controller/IdempotentTokenController.java`

```java
package vip.mate.starter.idempotent.controller;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.base.result.Result;
import vip.mate.starter.idempotent.config.IdempotentProperties;

import java.time.Duration;
import java.util.UUID;

/**
 * Endpoint for TOKEN-mode idempotency.
 * Client calls GET /api/v1/idempotent/token to obtain a one-time token,
 * then includes it as X-Idempotent-Token header in the actual request.
 */
@RestController
@RequestMapping("/api/v1/idempotent")
@RequiredArgsConstructor
public class IdempotentTokenController {

    private final RedissonClient redissonClient;
    private final IdempotentProperties properties;

    @GetMapping("/token")
    public Result<String> getToken() {
        String token = UUID.randomUUID().toString().replace("-", "");
        String redisKey = properties.getKeyPrefix() + "token:" + token;

        RBucket<String> bucket = redissonClient.getBucket(redisKey);
        bucket.set("1", Duration.ofSeconds(properties.getTokenExpireSeconds()));

        return Result.ok(token);
    }
}
```

### 2.6 Auto-Configuration

**File**: `mate-starters/mate-idempotent-starter/src/main/java/vip/mate/starter/idempotent/config/IdempotentAutoConfiguration.java`

```java
package vip.mate.starter.idempotent.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import vip.mate.starter.idempotent.aspect.IdempotentAspect;
import vip.mate.starter.idempotent.controller.IdempotentTokenController;

/**
 * Auto-configuration for the idempotency framework.
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.redisson.api.RedissonClient")
@EnableConfigurationProperties(IdempotentProperties.class)
@ComponentScan("vip.mate.starter.idempotent")
public class IdempotentAutoConfiguration {
}
```

### 2.7 Spring Boot Auto-Configuration Registration

**File**: `mate-starters/mate-idempotent-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
vip.mate.starter.idempotent.config.IdempotentAutoConfiguration
```

### 2.8 Usage Example

```java
// PARAM mode: idempotent by order number
@Idempotent(key = "#cmd.orderNo", expireSeconds = 3600)
@PostMapping("/orders")
public Result<String> createOrder(@RequestBody CreateOrderCommand cmd) {
    return Result.ok(orderService.create(cmd));
}

// TOKEN mode: client must first GET /api/v1/idempotent/token
@Idempotent(type = IdempotentType.TOKEN)
@PostMapping("/payment")
public Result<String> pay(@RequestBody PayCommand cmd) {
    return Result.ok(paymentService.pay(cmd));
}

// HEADER mode: key from X-Idempotent-Key header
@Idempotent(type = IdempotentType.HEADER, key = "X-Idempotent-Key")
@PostMapping("/callback")
public Result<Void> handleCallback(@RequestBody CallbackDTO dto) {
    callbackService.process(dto);
    return Result.ok();
}
```

---

## 3. Gray Release / Canary Deployment (mate-gray-starter)

### 3.1 pom.xml

**File**: `mate-starters/mate-gray-starter/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>mate-starters</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>mate-gray-starter</artifactId>
    <packaging>jar</packaging>
    <name>mate-gray-starter</name>
    <description>Gray release / canary deployment starter - Gateway + Dubbo traffic splitting</description>

    <dependencies>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-base</artifactId>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-cache-starter</artifactId>
        </dependency>
        <!-- WebFlux for Gateway filter (optional, only active when WebFlux is on classpath) -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-webflux</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-gateway-server</artifactId>
            <optional>true</optional>
        </dependency>
        <!-- Dubbo for consumer filter (optional) -->
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure-processor</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

### 3.2 Gray Rule Model

**File**: `mate-starters/mate-gray-starter/src/main/java/vip/mate/starter/gray/model/GrayRule.java`

```java
package vip.mate.starter.gray.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Gray release rule configuration. Typically loaded from Nacos or Redis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrayRule implements Serializable {

    /** Master switch for gray routing */
    private boolean enabled;

    /** Percentage of traffic to route to gray (0-100) */
    private int percentage;

    /** Users who always go to gray (whitelist) */
    @Builder.Default
    private List<String> whiteUserIds = new ArrayList<>();

    /** Users who never go to gray (blacklist) */
    @Builder.Default
    private List<String> blackUserIds = new ArrayList<>();

    /** Optional custom header key for additional matching */
    private String headerKey;

    /** Optional custom header value for additional matching */
    private String headerValue;
}
```

### 3.3 Gray Rule Service

**File**: `mate-starters/mate-gray-starter/src/main/java/vip/mate/starter/gray/service/GrayRuleService.java`

```java
package vip.mate.starter.gray.service;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import vip.mate.starter.gray.model.GrayRule;

/**
 * Service for loading and evaluating gray release rules.
 * Rules are stored in Redis under key "gray:rule" as JSON.
 * In production, rules can also be pushed from Nacos config listener.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrayRuleService {

    private static final String RULE_KEY = "gray:rule";

    private final RedissonClient redissonClient;

    /**
     * Load the current gray rule from Redis.
     */
    public GrayRule loadRule() {
        RBucket<String> bucket = redissonClient.getBucket(RULE_KEY);
        String json = bucket.get();
        if (json == null || json.isBlank()) {
            return GrayRule.builder().enabled(false).build();
        }
        return JSON.parseObject(json, GrayRule.class);
    }

    /**
     * Save/update the gray rule to Redis.
     */
    public void saveRule(GrayRule rule) {
        RBucket<String> bucket = redissonClient.getBucket(RULE_KEY);
        bucket.set(JSON.toJSONString(rule));
        log.info("Gray rule updated: enabled={}, percentage={}", rule.isEnabled(), rule.getPercentage());
    }

    /**
     * Determine whether a given userId should be routed to the gray environment.
     *
     * @param userId the user ID (may be null for anonymous traffic)
     * @return true if the request should go to gray
     */
    public boolean matchGray(String userId) {
        GrayRule rule = loadRule();
        if (!rule.isEnabled()) {
            return false;
        }

        // Blacklist takes priority
        if (userId != null && rule.getBlackUserIds().contains(userId)) {
            return false;
        }

        // Whitelist
        if (userId != null && rule.getWhiteUserIds().contains(userId)) {
            return true;
        }

        // Percentage-based: hash the userId (or random for anonymous)
        if (rule.getPercentage() <= 0) {
            return false;
        }
        if (rule.getPercentage() >= 100) {
            return true;
        }

        int hash = userId != null ? Math.abs(userId.hashCode()) : (int) (Math.random() * 100);
        return (hash % 100) < rule.getPercentage();
    }
}
```

### 3.4 Gateway Gray Filter (WebFlux)

**File**: `mate-starters/mate-gray-starter/src/main/java/vip/mate/starter/gray/filter/GrayGatewayFilter.java`

```java
package vip.mate.starter.gray.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import vip.mate.starter.gray.service.GrayRuleService;

/**
 * Spring Cloud Gateway global filter for gray traffic routing.
 * Evaluates gray rules and adds X-Gray-Tag header to matching requests.
 * Downstream services and Dubbo consumers read this header to route accordingly.
 *
 * Only active when Spring Cloud Gateway is on the classpath.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnClass(name = "org.springframework.cloud.gateway.filter.GlobalFilter")
public class GrayGatewayFilter implements GlobalFilter, Ordered {

    private static final String GRAY_TAG_HEADER = "X-Gray-Tag";
    private static final String USER_ID_HEADER = "X-User-Id";

    private final GrayRuleService grayRuleService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String userId = exchange.getRequest().getHeaders().getFirst(USER_ID_HEADER);
        boolean isGray = grayRuleService.matchGray(userId);

        if (isGray) {
            log.debug("Gray route matched: userId={}", userId);
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header(GRAY_TAG_HEADER, "gray")
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // Run early so downstream filters see the gray tag
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
```

### 3.5 Dubbo Consumer Gray Filter

**File**: `mate-starters/mate-gray-starter/src/main/java/vip/mate/starter/gray/dubbo/GrayDubboConsumerFilter.java`

```java
package vip.mate.starter.gray.dubbo;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;

/**
 * Dubbo consumer-side filter that propagates the gray tag.
 * When X-Gray-Tag is set in RpcContext (by upstream HTTP filter or gateway),
 * this filter sets the "dubbo.tag" attachment so that the Dubbo router
 * directs traffic to gray-tagged provider instances.
 *
 * Provider-side gray instances are configured via:
 *   dubbo.provider.tag=gray
 */
@Slf4j
@Activate(group = CONSUMER, order = -1000)
public class GrayDubboConsumerFilter implements Filter {

    private static final String GRAY_TAG_KEY = "X-Gray-Tag";
    private static final String DUBBO_TAG_KEY = "dubbo.tag";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String grayTag = RpcContext.getContext().getAttachment(GRAY_TAG_KEY);
        if (grayTag != null && !grayTag.isBlank()) {
            invocation.setAttachment(DUBBO_TAG_KEY, grayTag);
            log.debug("Dubbo gray tag set: {} -> {}", DUBBO_TAG_KEY, grayTag);
        }
        return invoker.invoke(invocation);
    }
}
```

### 3.6 Dubbo SPI Registration

**File**: `mate-starters/mate-gray-starter/src/main/resources/META-INF/dubbo/org.apache.dubbo.rpc.Filter`

```
grayConsumer=vip.mate.starter.gray.dubbo.GrayDubboConsumerFilter
```

### 3.7 Auto-Configuration

**File**: `mate-starters/mate-gray-starter/src/main/java/vip/mate/starter/gray/config/GrayAutoConfiguration.java`

```java
package vip.mate.starter.gray.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration for gray release / canary deployment support.
 * Registers:
 * - GrayRuleService: load/evaluate gray rules from Redis
 * - GrayGatewayFilter: Gateway global filter (only when Gateway is on classpath)
 * - GrayDubboConsumerFilter: registered via Dubbo SPI, not Spring
 */
@AutoConfiguration
@ComponentScan("vip.mate.starter.gray")
public class GrayAutoConfiguration {
}
```

### 3.8 Spring Boot Auto-Configuration Registration

**File**: `mate-starters/mate-gray-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
vip.mate.starter.gray.config.GrayAutoConfiguration
```

### 3.9 Usage Example

```java
// 1. Set up gray rule (e.g., via admin API or Nacos)
GrayRule rule = GrayRule.builder()
    .enabled(true)
    .percentage(10)   // 10% of traffic
    .whiteUserIds(List.of("test_user_01", "test_user_02"))
    .blackUserIds(List.of("vip_user_99"))
    .build();
grayRuleService.saveRule(rule);

// 2. Gateway automatically routes matching requests with X-Gray-Tag: gray
// 3. Dubbo consumers auto-propagate dubbo.tag=gray to gray provider instances

// Provider-side application.yml for gray instance:
// dubbo:
//   provider:
//     tag: gray
```

---

## 4. Developer Tools (mate-devtools-starter)

### 4.1 pom.xml

**File**: `mate-starters/mate-devtools-starter/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>mate-starters</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>mate-devtools-starter</artifactId>
    <packaging>jar</packaging>
    <name>mate-devtools-starter</name>
    <description>Development tools starter - SQL log, API timing, config explorer (dev profile only)</description>

    <dependencies>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-base</artifactId>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-ds-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-web-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure-processor</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

### 4.2 SQL Log Interceptor

**File**: `mate-starters/mate-devtools-starter/src/main/java/vip/mate/starter/devtools/interceptor/SqlLogEntry.java`

```java
package vip.mate.starter.devtools.interceptor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * A recorded SQL execution entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlLogEntry implements Serializable {

    private String sql;
    private String params;
    private long durationMs;
    private String mapperId;
    private long timestamp;
}
```

**File**: `mate-starters/mate-devtools-starter/src/main/java/vip/mate/starter/devtools/interceptor/SqlLogInterceptor.java`

```java
package vip.mate.starter.devtools.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * MyBatis interceptor that records the most recent SQL statements in memory.
 * Only active in dev profile.
 */
@Slf4j
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare",
               args = {Connection.class, Integer.class})
})
public class SqlLogInterceptor implements Interceptor {

    private static final int MAX_ENTRIES = 100;

    private final LinkedList<SqlLogEntry> entries = new LinkedList<>();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler handler = (StatementHandler) invocation.getTarget();
        BoundSql boundSql = handler.getBoundSql();

        // Extract mapperId via reflection
        MetaObject metaObject = SystemMetaObject.forObject(handler);
        String mapperId = "";
        try {
            MappedStatement ms = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
            mapperId = ms.getId();
        } catch (Exception ignored) {
            // Not all handlers have the delegate
        }

        long start = System.currentTimeMillis();
        Object result = invocation.proceed();
        long duration = System.currentTimeMillis() - start;

        SqlLogEntry entry = SqlLogEntry.builder()
                .sql(boundSql.getSql().replaceAll("\\s+", " ").trim())
                .params(String.valueOf(boundSql.getParameterObject()))
                .durationMs(duration)
                .mapperId(mapperId)
                .timestamp(System.currentTimeMillis())
                .build();

        synchronized (entries) {
            entries.addFirst(entry);
            if (entries.size() > MAX_ENTRIES) {
                entries.removeLast();
            }
        }

        log.debug("[SQL] {}ms | {}", duration, entry.getSql());
        return result;
    }

    /**
     * Get the most recent N entries.
     */
    public List<SqlLogEntry> getRecent(int limit) {
        synchronized (entries) {
            int count = Math.min(limit, entries.size());
            return new ArrayList<>(entries.subList(0, count));
        }
    }

    /**
     * Get all entries.
     */
    public List<SqlLogEntry> getAll() {
        synchronized (entries) {
            return Collections.unmodifiableList(new ArrayList<>(entries));
        }
    }
}
```

### 4.3 API Timing Filter

**File**: `mate-starters/mate-devtools-starter/src/main/java/vip/mate/starter/devtools/filter/ApiTimingStats.java`

```java
package vip.mate.starter.devtools.filter;

import lombok.Data;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Timing statistics for a single API endpoint.
 */
@Data
public class ApiTimingStats implements Serializable {

    private String uri;
    private long count;
    private long totalMs;
    private long minMs = Long.MAX_VALUE;
    private long maxMs = 0;

    /** Circular buffer for percentile calculation */
    private final long[] recentDurations = new long[200];
    private int recentIndex = 0;
    private int recentSize = 0;

    public ApiTimingStats(String uri) {
        this.uri = uri;
    }

    public synchronized void record(long durationMs) {
        count++;
        totalMs += durationMs;
        if (durationMs < minMs) minMs = durationMs;
        if (durationMs > maxMs) maxMs = durationMs;

        recentDurations[recentIndex] = durationMs;
        recentIndex = (recentIndex + 1) % recentDurations.length;
        if (recentSize < recentDurations.length) recentSize++;
    }

    public long getAvgMs() {
        return count == 0 ? 0 : totalMs / count;
    }

    public long getP50() { return percentile(50); }
    public long getP95() { return percentile(95); }
    public long getP99() { return percentile(99); }

    private synchronized long percentile(int p) {
        if (recentSize == 0) return 0;
        long[] sorted = Arrays.copyOf(recentDurations, recentSize);
        Arrays.sort(sorted);
        int idx = (int) Math.ceil(p / 100.0 * sorted.length) - 1;
        return sorted[Math.max(0, idx)];
    }
}
```

**File**: `mate-starters/mate-devtools-starter/src/main/java/vip/mate/starter/devtools/filter/ApiTimingFilter.java`

```java
package vip.mate.starter.devtools.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servlet filter that records per-URI response time statistics.
 * Only active in dev profile.
 */
@Slf4j
public class ApiTimingFilter extends OncePerRequestFilter {

    private final Map<String, ApiTimingStats> statsMap = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            String uri = request.getMethod() + " " + request.getRequestURI();
            statsMap.computeIfAbsent(uri, ApiTimingStats::new).record(duration);

            if (duration > 1000) {
                log.warn("[SLOW API] {}ms | {}", duration, uri);
            }
        }
    }

    /**
     * Get timing stats for all endpoints.
     */
    public Map<String, ApiTimingStats> getStats() {
        return Collections.unmodifiableMap(statsMap);
    }
}
```

### 4.4 DevTools Controller

**File**: `mate-starters/mate-devtools-starter/src/main/java/vip/mate/starter/devtools/controller/DevToolsController.java`

```java
package vip.mate.starter.devtools.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.base.result.Result;
import vip.mate.starter.devtools.filter.ApiTimingFilter;
import vip.mate.starter.devtools.filter.ApiTimingStats;
import vip.mate.starter.devtools.interceptor.SqlLogEntry;
import vip.mate.starter.devtools.interceptor.SqlLogInterceptor;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * REST endpoints for development debugging. Only active in "dev" profile.
 */
@RestController
@RequestMapping("/devtools")
@Profile("dev")
@RequiredArgsConstructor
public class DevToolsController {

    private final SqlLogInterceptor sqlLogInterceptor;
    private final ApiTimingFilter apiTimingFilter;
    private final ApplicationContext applicationContext;
    private final Environment environment;

    /** Sensitive config keys to mask in output */
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "secret", "token", "key", "credential", "private"
    );

    // ==================== SQL Log ====================

    @GetMapping("/sql-log")
    public Result<List<SqlLogEntry>> sqlLog(
            @RequestParam(defaultValue = "50") int limit) {
        return Result.ok(sqlLogInterceptor.getRecent(limit));
    }

    // ==================== API Timing ====================

    @GetMapping("/api-timing")
    public Result<Map<String, ApiTimingStats>> apiTiming() {
        return Result.ok(apiTimingFilter.getStats());
    }

    // ==================== Bean Explorer ====================

    @GetMapping("/beans")
    public Result<List<String>> beans(
            @RequestParam(required = false) String filter) {
        String[] names = applicationContext.getBeanDefinitionNames();
        List<String> result = Arrays.stream(names)
                .filter(name -> filter == null || name.toLowerCase().contains(filter.toLowerCase()))
                .sorted()
                .toList();
        return Result.ok(result);
    }

    // ==================== Config Explorer (desensitized) ====================

    @GetMapping("/config")
    public Result<Map<String, String>> config() {
        Map<String, String> props = new TreeMap<>();
        if (environment instanceof AbstractEnvironment abstractEnv) {
            abstractEnv.getPropertySources().forEach(ps -> {
                if (ps instanceof EnumerablePropertySource<?> eps) {
                    for (String key : eps.getPropertyNames()) {
                        String value = environment.getProperty(key);
                        props.put(key, desensitize(key, value));
                    }
                }
            });
        }
        return Result.ok(props);
    }

    // ==================== Thread Pool Stats ====================

    @GetMapping("/thread-pool")
    public Result<Map<String, Map<String, Object>>> threadPools() {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();

        applicationContext.getBeansOfType(ThreadPoolExecutor.class)
                .forEach((name, executor) -> {
                    Map<String, Object> stats = new LinkedHashMap<>();
                    stats.put("corePoolSize", executor.getCorePoolSize());
                    stats.put("maximumPoolSize", executor.getMaximumPoolSize());
                    stats.put("activeCount", executor.getActiveCount());
                    stats.put("poolSize", executor.getPoolSize());
                    stats.put("queueSize", executor.getQueue().size());
                    stats.put("completedTaskCount", executor.getCompletedTaskCount());
                    stats.put("taskCount", executor.getTaskCount());
                    result.put(name, stats);
                });

        return Result.ok(result);
    }

    // ==================== Dynamic Log Level ====================

    @PutMapping("/log-level")
    public Result<String> setLogLevel(
            @RequestParam String logger,
            @RequestParam String level) {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logbackLogger = ctx.getLogger(logger);
        Level newLevel = Level.valueOf(level.toUpperCase());
        logbackLogger.setLevel(newLevel);
        return Result.ok("Logger [" + logger + "] set to " + newLevel);
    }

    // ==================== helpers ====================

    private String desensitize(String key, String value) {
        if (value == null) return null;
        String lowerKey = key.toLowerCase();
        for (String sensitive : SENSITIVE_KEYS) {
            if (lowerKey.contains(sensitive)) {
                return "******";
            }
        }
        return value;
    }
}
```

### 4.5 Auto-Configuration

**File**: `mate-starters/mate-devtools-starter/src/main/java/vip/mate/starter/devtools/config/DevToolsAutoConfiguration.java`

```java
package vip.mate.starter.devtools.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import vip.mate.starter.devtools.filter.ApiTimingFilter;
import vip.mate.starter.devtools.interceptor.SqlLogInterceptor;

/**
 * Auto-configuration for developer tools.
 * Only active when:
 * - Profile is "dev"
 * - Property mate.devtools.enabled=true
 *
 * This ensures devtools are NEVER available in production.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "mate.devtools", name = "enabled", havingValue = "true")
@Profile("dev")
public class DevToolsAutoConfiguration {

    /**
     * MyBatis interceptor that records the last 100 SQL statements.
     */
    @Bean
    @ConditionalOnMissingBean
    public SqlLogInterceptor sqlLogInterceptor() {
        return new SqlLogInterceptor();
    }

    /**
     * Servlet filter that records per-endpoint P50/P95/P99 timing stats.
     */
    @Bean
    @ConditionalOnMissingBean
    public ApiTimingFilter apiTimingFilter() {
        return new ApiTimingFilter();
    }
}
```

### 4.6 Spring Boot Auto-Configuration Registration

**File**: `mate-starters/mate-devtools-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
vip.mate.starter.devtools.config.DevToolsAutoConfiguration
```

### 4.7 application.yml (dev profile)

```yaml
# Enable devtools (only effective when profile=dev)
mate:
  devtools:
    enabled: true
```

### 4.8 Usage Example

```bash
# View recent SQL executions
GET /devtools/sql-log?limit=20

# View API response time percentiles
GET /devtools/api-timing

# Search beans by name
GET /devtools/beans?filter=user

# View all configuration (sensitive values masked)
GET /devtools/config

# View thread pool stats
GET /devtools/thread-pool

# Dynamically change log level
PUT /devtools/log-level?logger=vip.mate.biz.system&level=DEBUG
```

---

## New Starter/Module Summary

| Starter | Core Capability |
|---------|-----------------|
| mate-idempotent-starter | @Idempotent (PARAM/TOKEN/HEADER three modes) |
| mate-gray-starter | Gateway + Dubbo gray routing |
| mate-devtools-starter | SQL log / API timing / Bean viewer / Config viewer / Dynamic log level (dev only) |
| AsyncTaskExecutor | mate-base interface for async task tracking |
| VirtualThreadAsyncTaskExecutor | Java 21 virtual thread implementation |
| TaskProgress | Progress model (taskId, state, total, current, message) |

## Verification Plan

1. **Idempotent**: POST /orders with same orderNo twice -> second returns duplicate rejection
2. **Idempotent (TOKEN)**: GET /api/v1/idempotent/token -> POST /payment with X-Idempotent-Token -> second POST rejected
3. **Gray**: Save rule with 10% traffic -> verify hash-based routing via X-Gray-Tag header
4. **Gray (Dubbo)**: Gray-tagged provider receives only gray traffic, normal provider handles the rest
5. **DevTools**: GET /devtools/sql-log returns recent SQL with duration
6. **DevTools**: PUT /devtools/log-level?logger=vip.mate&level=DEBUG -> verify log output changes
7. **AsyncTask**: Submit long import -> poll getProgress -> observe PENDING -> RUNNING -> SUCCESS
