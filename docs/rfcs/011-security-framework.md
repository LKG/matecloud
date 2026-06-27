# RFC-011: Security Framework

- **Status**: Draft
- **Created**: 2026-04-12
- **Author**: MateCloud Team
- **Wave**: 4
- **Dependencies**: RFC-004 (sa-token-starter, cache-starter), RFC-007 (mate-auth)

## Overview

This RFC provides complete implementation specifications for two new starters: `mate-datascope-starter` (data permission) and `mate-security-starter` (API security, audit, desensitization, field encryption). Each includes full pom.xml, Java source, auto-configuration, and SQL DDL.

---

## Part 1: mate-datascope-starter

Package: `vip.mate.starter.datascope`

### 1.1 pom.xml

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

    <artifactId>mate-datascope-starter</artifactId>
    <packaging>jar</packaging>
    <name>mate-datascope-starter</name>
    <description>Data permission (data scope) auto-configuration starter</description>

    <dependencies>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-base</artifactId>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-sa-token-starter</artifactId>
            <exclusions>
                <exclusion>
                    <groupId>cn.dev33</groupId>
                    <artifactId>sa-token-reactor-spring-boot3-starter</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-ds-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
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

### 1.2 DataScope.java

```java
package vip.mate.starter.datascope;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Data permission scope levels.
 */
@Getter
@AllArgsConstructor
public enum DataScope {

    ALL(1, "All data"),
    DEPT(2, "Current department only"),
    DEPT_AND_CHILD(3, "Current department and children"),
    SELF(4, "Self only"),
    CUSTOM(5, "Custom department list");

    private final int code;
    private final String description;

    public static DataScope of(int code) {
        for (DataScope scope : values()) {
            if (scope.code == code) return scope;
        }
        return ALL;
    }
}
```

### 1.3 DataScopeContext.java

```java
package vip.mate.starter.datascope;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Context carrying the current request's data scope information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataScopeContext {

    private DataScope scope;
    private String deptAlias;
    private String userAlias;
    private String userId;
    private String deptId;
    private String customDeptIds;
}
```

### 1.4 DataScopeHolder.java

```java
package vip.mate.starter.datascope;

/**
 * ThreadLocal holder for DataScopeContext. Set by AOP before query, cleared after.
 */
public final class DataScopeHolder {

    private static final ThreadLocal<DataScopeContext> CONTEXT = new ThreadLocal<>();

    private DataScopeHolder() {
    }

    public static void set(DataScopeContext context) {
        CONTEXT.set(context);
    }

    public static DataScopeContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
```

### 1.5 @DataPermission Annotation

```java
package vip.mate.starter.datascope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable data permission filtering on a query method.
 * The MyBatis Plus interceptor will automatically append WHERE conditions
 * based on the current user's data scope.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {

    /**
     * Department column alias in SQL. e.g. "dept_id" or "u.dept_id"
     */
    String deptAlias() default "dept_id";

    /**
     * User/creator column alias in SQL. e.g. "creator_id" or "u.creator_id"
     */
    String userAlias() default "creator_id";
}
```

### 1.6 DataScopeAspect.java

```java
package vip.mate.starter.datascope;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that populates DataScopeHolder before annotated query methods
 * and clears it after execution.
 */
@Slf4j
@Aspect
@Component
public class DataScopeAspect {

    @Before("@annotation(dp)")
    public void before(JoinPoint jp, DataPermission dp) {
        try {
            Object loginId = StpUtil.getLoginId();
            if (loginId == null) return;

            String userId = loginId.toString();
            // Read data scope from Sa-Token session (set during login)
            Object scopeObj = StpUtil.getSession().get("dataScope");
            Object deptIdObj = StpUtil.getSession().get("deptId");
            Object customDeptIdsObj = StpUtil.getSession().get("customDeptIds");

            int scopeCode = scopeObj != null ? Integer.parseInt(scopeObj.toString()) : 1;
            DataScope scope = DataScope.of(scopeCode);

            if (scope == DataScope.ALL) {
                // No filtering needed
                return;
            }

            DataScopeContext ctx = DataScopeContext.builder()
                    .scope(scope)
                    .deptAlias(dp.deptAlias())
                    .userAlias(dp.userAlias())
                    .userId(userId)
                    .deptId(deptIdObj != null ? deptIdObj.toString() : null)
                    .customDeptIds(customDeptIdsObj != null ? customDeptIdsObj.toString() : null)
                    .build();
            DataScopeHolder.set(ctx);
        } catch (Exception e) {
            log.warn("DataScope aspect failed to resolve context: {}", e.getMessage());
        }
    }

    @After("@annotation(dp)")
    public void after(JoinPoint jp, DataPermission dp) {
        DataScopeHolder.clear();
    }
}
```

### 1.7 DataScopeInterceptor.java

```java
package vip.mate.starter.datascope;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Field;

/**
 * MyBatis Plus InnerInterceptor that dynamically appends data scope WHERE conditions
 * based on the DataScopeContext set by the AOP aspect.
 */
@Slf4j
public class DataScopeInterceptor implements InnerInterceptor {

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms,
                            Object parameter, RowBounds rowBounds,
                            ResultHandler resultHandler, BoundSql boundSql) {
        DataScopeContext ctx = DataScopeHolder.get();
        if (ctx == null || ctx.getScope() == DataScope.ALL) {
            return;
        }

        String originalSql = boundSql.getSql();
        String scopeSql = buildScopeSql(originalSql, ctx);

        try {
            Field sqlField = BoundSql.class.getDeclaredField("sql");
            sqlField.setAccessible(true);
            sqlField.set(boundSql, scopeSql);
        } catch (Exception e) {
            log.error("Failed to set data scope SQL: {}", e.getMessage(), e);
        }
    }

    private String buildScopeSql(String originalSql, DataScopeContext ctx) {
        return switch (ctx.getScope()) {
            case DEPT -> originalSql + " AND " + ctx.getDeptAlias()
                    + " = '" + ctx.getDeptId() + "'";
            case DEPT_AND_CHILD -> originalSql + " AND " + ctx.getDeptAlias()
                    + " IN (SELECT id FROM mate_dept WHERE id = '" + ctx.getDeptId()
                    + "' OR ancestors LIKE CONCAT('" + ctx.getDeptId() + "', ',%'))";
            case SELF -> originalSql + " AND " + ctx.getUserAlias()
                    + " = '" + ctx.getUserId() + "'";
            case CUSTOM -> {
                String deptIds = ctx.getCustomDeptIds();
                if (deptIds == null || deptIds.isBlank()) {
                    yield originalSql + " AND 1 = 0";
                }
                yield originalSql + " AND " + ctx.getDeptAlias()
                        + " IN (" + deptIds + ")";
            }
            default -> originalSql;
        };
    }
}
```

### 1.8 DataScopeAutoConfiguration.java

```java
package vip.mate.starter.datascope;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for data scope (data permission) support.
 * Registers the MyBatis Plus interceptor and AOP aspect.
 */
@AutoConfiguration
@ConditionalOnClass(MybatisPlusInterceptor.class)
public class DataScopeAutoConfiguration {

    @Bean
    public DataScopeInterceptor dataScopeInterceptor() {
        return new DataScopeInterceptor();
    }

    @Bean
    public DataScopeAspect dataScopeAspect() {
        return new DataScopeAspect();
    }
}
```

### 1.9 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports

```text
vip.mate.starter.datascope.DataScopeAutoConfiguration
```

---

## Part 2: mate-security-starter

Package: `vip.mate.starter.security`

### 2.1 pom.xml

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

    <artifactId>mate-security-starter</artifactId>
    <packaging>jar</packaging>
    <name>mate-security-starter</name>
    <description>Security starter - API sign, repeat-submit, rate-limit, desensitize, audit, field-encrypt</description>

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
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-annotation</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-extension</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-core</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
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

### 2.2 SecurityErrorCode.java

```java
package vip.mate.starter.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import vip.mate.base.exception.ErrorCode;

@Getter
@AllArgsConstructor
public enum SecurityErrorCode implements ErrorCode {

    SIGN_EXPIRED("S0001", "API signature expired"),
    REPLAY_ATTACK("S0002", "Replay attack detected (duplicate nonce)"),
    SIGN_INVALID("S0003", "API signature verification failed"),
    APP_KEY_NOT_FOUND("S0004", "App key not found or disabled"),
    REPEAT_SUBMIT("S0010", "Repeat submission, please wait"),
    RATE_LIMITED("S0020", "Request rate exceeded, please slow down");

    private final String code;
    private final String message;
}
```

### 2.3 @ApiSign Annotation

```java
package vip.mate.starter.security.sign;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to require API signature verification on an endpoint.
 * Signature algorithm: MD5(appKey + timestamp + nonce + body + appSecret)
 * Required headers: X-App-Key, X-Timestamp, X-Nonce, X-Sign
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiSign {

    /**
     * Signature validity period in seconds. Default 300s (5 minutes).
     */
    long timeout() default 300;
}
```

### 2.4 ApiSignInterceptor.java

```java
package vip.mate.starter.security.sign;

import cn.hutool.crypto.digest.DigestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import vip.mate.base.exception.BizException;
import vip.mate.starter.cache.RedissonService;
import vip.mate.starter.security.SecurityErrorCode;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * HandlerInterceptor that validates API signature for methods annotated with @ApiSign.
 */
@Slf4j
@RequiredArgsConstructor
public class ApiSignInterceptor implements HandlerInterceptor {

    private final RedissonService redissonService;
    private final AppKeyProvider appKeyProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }
        ApiSign sign = hm.getMethodAnnotation(ApiSign.class);
        if (sign == null) {
            return true;
        }

        String appKey = request.getHeader("X-App-Key");
        String timestamp = request.getHeader("X-Timestamp");
        String nonce = request.getHeader("X-Nonce");
        String signature = request.getHeader("X-Sign");

        if (appKey == null || timestamp == null || nonce == null || signature == null) {
            throw new BizException(SecurityErrorCode.SIGN_INVALID);
        }

        // 1. Time window validation
        long now = System.currentTimeMillis() / 1000;
        long ts = Long.parseLong(timestamp);
        if (Math.abs(now - ts) > sign.timeout()) {
            throw new BizException(SecurityErrorCode.SIGN_EXPIRED);
        }

        // 2. Nonce replay prevention (Redis SET NX with TTL)
        String nonceKey = "security:nonce:" + nonce;
        boolean isNew = redissonService.getClient().getBucket(nonceKey)
                .trySet("1", Duration.ofSeconds(sign.timeout()));
        if (!isNew) {
            throw new BizException(SecurityErrorCode.REPLAY_ATTACK);
        }

        // 3. Signature verification
        String appSecret = appKeyProvider.getSecret(appKey);
        if (appSecret == null) {
            throw new BizException(SecurityErrorCode.APP_KEY_NOT_FOUND);
        }

        // Read request body (requires CachedBodyRequestWrapper)
        String body = "";
        if (request instanceof CachedBodyRequestWrapper wrapper) {
            body = StreamUtils.copyToString(wrapper.getInputStream(), StandardCharsets.UTF_8);
        }

        String expected = DigestUtil.md5Hex(appKey + timestamp + nonce + body + appSecret);
        if (!expected.equalsIgnoreCase(signature)) {
            throw new BizException(SecurityErrorCode.SIGN_INVALID);
        }

        return true;
    }
}
```

### 2.5 AppKeyProvider.java

```java
package vip.mate.starter.security.sign;

/**
 * SPI for resolving app secret by app key.
 * Implementors typically query from database or cache.
 */
public interface AppKeyProvider {

    /**
     * Returns the app secret for the given app key, or null if not found/disabled.
     */
    String getSecret(String appKey);
}
```

### 2.6 CachedBodyRequestWrapper.java

```java
package vip.mate.starter.security.sign;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Wraps HttpServletRequest to cache the body so it can be read multiple times.
 */
public class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream bais = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                // No async support needed
            }

            @Override
            public int read() {
                return bais.read();
            }
        };
    }
}
```

### 2.7 CachedBodyFilter.java

```java
package vip.mate.starter.security.sign;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that wraps the request for body caching when API sign is needed.
 */
public class CachedBodyFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {
        // Only wrap if Content-Type suggests a body
        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            CachedBodyRequestWrapper wrappedRequest = new CachedBodyRequestWrapper(request);
            filterChain.doFilter(wrappedRequest, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
```

### 2.8 @RepeatSubmit Annotation

```java
package vip.mate.starter.security.repeat;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Prevents duplicate form submissions within a configurable interval.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit {

    /**
     * Lock interval in seconds. Default 5 seconds.
     */
    long interval() default 5;

    /**
     * Custom error message.
     */
    String message() default "Repeat submission, please wait";
}
```

### 2.9 RepeatSubmitAspect.java

```java
package vip.mate.starter.security.repeat;

import cn.hutool.crypto.digest.DigestUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import vip.mate.base.exception.BizException;
import vip.mate.starter.cache.RedissonService;
import vip.mate.starter.security.SecurityErrorCode;

import java.time.Duration;

/**
 * AOP aspect that enforces repeat-submit prevention using Redis locks.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RepeatSubmitAspect {

    private final RedissonService redissonService;

    @Around("@annotation(rs)")
    public Object around(ProceedingJoinPoint pjp, RepeatSubmit rs) throws Throwable {
        String userId = resolveUserId();
        String methodKey = DigestUtil.md5Hex(pjp.getSignature().toLongString());
        String key = "security:repeat:" + userId + ":" + methodKey;

        boolean acquired = redissonService.getClient().getBucket(key)
                .trySet("1", Duration.ofSeconds(rs.interval()));
        if (!acquired) {
            throw new BizException(SecurityErrorCode.REPEAT_SUBMIT.getCode(), rs.message());
        }

        return pjp.proceed();
    }

    private String resolveUserId() {
        try {
            Object loginId = cn.dev33.satoken.stp.StpUtil.getLoginId();
            return loginId != null ? loginId.toString() : "anonymous";
        } catch (Exception e) {
            return "anonymous";
        }
    }
}
```

### 2.10 @RateLimit Annotation

```java
package vip.mate.starter.security.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rate limiting annotation using Redisson RRateLimiter.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * Maximum requests per second.
     */
    int qps() default 10;

    /**
     * Limit key SpEL expression. Empty means use method signature.
     */
    String key() default "";

    /**
     * Limiting dimension.
     */
    LimitType type() default LimitType.IP;
}
```

### 2.11 LimitType.java

```java
package vip.mate.starter.security.ratelimit;

public enum LimitType {
    /**
     * Rate limit per client IP address.
     */
    IP,
    /**
     * Rate limit per authenticated user.
     */
    USER,
    /**
     * Global rate limit shared across all requests.
     */
    GLOBAL
}
```

### 2.12 RateLimitAspect.java

```java
package vip.mate.starter.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vip.mate.base.exception.BizException;
import vip.mate.starter.security.SecurityErrorCode;

/**
 * AOP aspect that enforces rate limiting using Redisson RRateLimiter.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedissonClient redissonClient;

    @Before("@annotation(rl)")
    public void before(JoinPoint jp, RateLimit rl) {
        String key = buildKey(jp, rl);
        String redisKey = "security:rate:" + key;

        RRateLimiter limiter = redissonClient.getRateLimiter(redisKey);
        limiter.trySetRate(RateType.OVERALL, rl.qps(), 1, RateIntervalUnit.SECONDS);

        if (!limiter.tryAcquire()) {
            throw new BizException(SecurityErrorCode.RATE_LIMITED);
        }
    }

    private String buildKey(JoinPoint jp, RateLimit rl) {
        String base = jp.getSignature().toShortString();

        return switch (rl.type()) {
            case IP -> {
                String ip = getClientIp();
                yield base + ":" + ip;
            }
            case USER -> {
                String userId;
                try {
                    Object loginId = cn.dev33.satoken.stp.StpUtil.getLoginId();
                    userId = loginId != null ? loginId.toString() : "anonymous";
                } catch (Exception e) {
                    userId = "anonymous";
                }
                yield base + ":" + userId;
            }
            case GLOBAL -> base + ":global";
        };
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isBlank()) {
                    ip = request.getRemoteAddr();
                } else {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }
}
```

### 2.13 @Desensitize Annotation

```java
package vip.mate.starter.security.desensitize;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Field-level desensitization annotation. Used on String fields in response DTOs.
 * Jackson will automatically mask the value during serialization.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside
@JsonSerialize(using = DesensitizeSerializer.class)
public @interface Desensitize {

    DesensitizeType type();
}
```

### 2.14 DesensitizeType.java

```java
package vip.mate.starter.security.desensitize;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DesensitizeType {

    MOBILE("Mobile number, e.g. 138****0000"),
    ID_CARD("ID card, e.g. 110***********1234"),
    EMAIL("Email, e.g. a**@example.com"),
    BANK_CARD("Bank card, e.g. 6222 **** **** 1234"),
    NAME("Name, e.g. John *"),
    ADDRESS("Address, e.g. 123 Main St ****");

    private final String description;
}
```

### 2.15 DesensitizeUtil.java

```java
package vip.mate.starter.security.desensitize;

/**
 * Utility for masking sensitive string data.
 */
public final class DesensitizeUtil {

    private DesensitizeUtil() {
    }

    public static String desensitize(String value, DesensitizeType type) {
        if (value == null || value.isBlank()) return value;

        return switch (type) {
            case MOBILE -> maskMobile(value);
            case ID_CARD -> maskIdCard(value);
            case EMAIL -> maskEmail(value);
            case BANK_CARD -> maskBankCard(value);
            case NAME -> maskName(value);
            case ADDRESS -> maskAddress(value);
        };
    }

    private static String maskMobile(String mobile) {
        if (mobile.length() < 7) return mask(mobile, 0, mobile.length());
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }

    private static String maskIdCard(String idCard) {
        if (idCard.length() < 7) return mask(idCard, 0, idCard.length());
        return idCard.substring(0, 3) + "*".repeat(idCard.length() - 7) + idCard.substring(idCard.length() - 4);
    }

    private static String maskEmail(String email) {
        int atIdx = email.indexOf('@');
        if (atIdx <= 1) return email;
        return email.charAt(0) + "*".repeat(atIdx - 1) + email.substring(atIdx);
    }

    private static String maskBankCard(String card) {
        if (card.length() < 8) return mask(card, 0, card.length());
        return card.substring(0, 4) + " **** **** " + card.substring(card.length() - 4);
    }

    private static String maskName(String name) {
        if (name.length() <= 1) return name;
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    private static String maskAddress(String address) {
        if (address.length() <= 6) return address;
        int showLen = Math.min(address.length() / 2, 10);
        return address.substring(0, showLen) + "****";
    }

    private static String mask(String value, int start, int end) {
        return "*".repeat(Math.max(0, end - start));
    }
}
```

### 2.16 DesensitizeSerializer.java

```java
package vip.mate.starter.security.desensitize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

/**
 * Jackson serializer that masks String fields annotated with @Desensitize.
 */
public class DesensitizeSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private DesensitizeType type;

    public DesensitizeSerializer() {
    }

    public DesensitizeSerializer(DesensitizeType type) {
        this.type = type;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (type != null) {
            gen.writeString(DesensitizeUtil.desensitize(value, type));
        } else {
            gen.writeString(value);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        if (property != null) {
            Desensitize annotation = property.getAnnotation(Desensitize.class);
            if (annotation == null) {
                annotation = property.getContextAnnotation(Desensitize.class);
            }
            if (annotation != null) {
                return new DesensitizeSerializer(annotation.type());
            }
        }
        return this;
    }
}
```

### 2.17 @AuditLog Annotation

```java
package vip.mate.starter.security.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method for audit logging. The aspect will record
 * who did what, when, from where, with what parameters, and the result.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * Business module name.
     */
    String module();

    /**
     * Operation description.
     */
    String operation();

    /**
     * Whether to save request parameters.
     */
    boolean saveRequestData() default true;

    /**
     * Whether to save response data.
     */
    boolean saveResponseData() default true;
}
```

### 2.18 AuditEvent.java

```java
package vip.mate.starter.security.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Event object published asynchronously after audit logging.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String module;
    private String operation;
    private String method;
    private String requestArgs;
    private String responseData;
    private String status;
    private String errorMsg;
    private Long costTime;
    private String operatorId;
    private String operatorIp;
    private Date operateTime;
}
```

### 2.19 AuditLogAspect.java

```java
package vip.mate.starter.security.audit;

import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;

/**
 * AOP aspect that captures audit logs and publishes them as Spring events
 * for async persistence.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final ApplicationEventPublisher eventPublisher;

    @Around("@annotation(al)")
    public Object around(ProceedingJoinPoint pjp, AuditLog al) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        String status = "SUCCESS";
        String errorMsg = null;

        try {
            result = pjp.proceed();
        } catch (Throwable e) {
            status = "FAIL";
            errorMsg = e.getMessage();
            throw e;
        } finally {
            long cost = System.currentTimeMillis() - start;

            String operatorId = resolveOperatorId();
            String operatorIp = resolveClientIp();

            AuditEvent event = AuditEvent.builder()
                    .module(al.module())
                    .operation(al.operation())
                    .method(pjp.getSignature().toShortString())
                    .requestArgs(al.saveRequestData() ? truncate(JSON.toJSONString(pjp.getArgs())) : null)
                    .responseData(al.saveResponseData() && result != null ? truncate(JSON.toJSONString(result)) : null)
                    .status(status)
                    .errorMsg(errorMsg)
                    .costTime(cost)
                    .operatorId(operatorId)
                    .operatorIp(operatorIp)
                    .operateTime(new Date())
                    .build();

            try {
                eventPublisher.publishEvent(event);
            } catch (Exception e) {
                log.error("Failed to publish audit event: {}", e.getMessage());
            }
        }
        return result;
    }

    private String resolveOperatorId() {
        try {
            Object loginId = cn.dev33.satoken.stp.StpUtil.getLoginId();
            return loginId != null ? loginId.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isBlank()) {
                    ip = request.getRemoteAddr();
                } else {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String truncate(String text) {
        if (text == null) return null;
        return text.length() > 2000 ? text.substring(0, 2000) + "..." : text;
    }
}
```

### 2.20 EncryptTypeHandler.java

```java
package vip.mate.starter.security.encrypt;

import cn.hutool.crypto.symmetric.AES;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis Plus TypeHandler for transparent AES encryption/decryption of database fields.
 * Encrypts on write, decrypts on read. The AES key is read from system property
 * or environment variable MATE_ENCRYPT_KEY, defaulting to a built-in key.
 *
 * Usage in PO class:
 * <pre>
 * {@code @TableField(typeHandler = EncryptTypeHandler.class)}
 * private String mobile;
 * </pre>
 */
@Slf4j
@MappedTypes(String.class)
public class EncryptTypeHandler extends BaseTypeHandler<String> {

    private static final String DEFAULT_KEY = "mate-cloud-aes!!";  // 16 bytes for AES-128
    private static final AES AES_INSTANCE;

    static {
        String key = System.getProperty("mate.encrypt.key",
                System.getenv("MATE_ENCRYPT_KEY") != null
                        ? System.getenv("MATE_ENCRYPT_KEY")
                        : DEFAULT_KEY);
        AES_INSTANCE = new AES(key.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter,
                                     JdbcType jdbcType) throws SQLException {
        ps.setString(i, AES_INSTANCE.encryptHex(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return decrypt(value);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return decrypt(value);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return decrypt(value);
    }

    private String decrypt(String value) {
        if (value == null || value.isBlank()) return value;
        try {
            return AES_INSTANCE.decryptStr(value);
        } catch (Exception e) {
            log.warn("Failed to decrypt field value, returning raw: {}", e.getMessage());
            return value;
        }
    }
}
```

### 2.21 SecurityAutoConfiguration.java

```java
package vip.mate.starter.security;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import vip.mate.starter.cache.RedissonService;
import vip.mate.starter.security.audit.AuditLogAspect;
import vip.mate.starter.security.ratelimit.RateLimitAspect;
import vip.mate.starter.security.repeat.RepeatSubmitAspect;
import vip.mate.starter.security.sign.ApiSignInterceptor;
import vip.mate.starter.security.sign.AppKeyProvider;
import vip.mate.starter.security.sign.CachedBodyFilter;

/**
 * Auto-configuration for the security starter.
 * Registers interceptors, AOP aspects, and filters.
 */
@AutoConfiguration
@ConditionalOnClass(RedissonClient.class)
public class SecurityAutoConfiguration implements WebMvcConfigurer {

    private ApiSignInterceptor apiSignInterceptor;

    @Bean
    @ConditionalOnBean(AppKeyProvider.class)
    public ApiSignInterceptor apiSignInterceptor(RedissonService redissonService,
                                                  AppKeyProvider appKeyProvider) {
        this.apiSignInterceptor = new ApiSignInterceptor(redissonService, appKeyProvider);
        return this.apiSignInterceptor;
    }

    @Bean
    public FilterRegistrationBean<CachedBodyFilter> cachedBodyFilter() {
        FilterRegistrationBean<CachedBodyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CachedBodyFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    @Bean
    public RepeatSubmitAspect repeatSubmitAspect(RedissonService redissonService) {
        return new RepeatSubmitAspect(redissonService);
    }

    @Bean
    public RateLimitAspect rateLimitAspect(RedissonClient redissonClient) {
        return new RateLimitAspect(redissonClient);
    }

    @Bean
    public AuditLogAspect auditLogAspect(ApplicationEventPublisher eventPublisher) {
        return new AuditLogAspect(eventPublisher);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (apiSignInterceptor != null) {
            registry.addInterceptor(apiSignInterceptor).addPathPatterns("/**");
        }
    }
}
```

### 2.22 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports

```text
vip.mate.starter.security.SecurityAutoConfiguration
```

---

## Part 3: SQL DDL

```sql
-- =============================================
-- Security Framework DDL
-- =============================================

CREATE TABLE `mate_dept` (
    `id`            VARCHAR(64)     NOT NULL COMMENT 'Primary key',
    `parent_id`     VARCHAR(64)     NOT NULL DEFAULT '0' COMMENT 'Parent dept ID, 0=root',
    `ancestors`     VARCHAR(512)    DEFAULT NULL COMMENT 'Ancestor chain, e.g. 0,1,2',
    `name`          VARCHAR(128)    NOT NULL COMMENT 'Department name',
    `sort`          INT             NOT NULL DEFAULT 0 COMMENT 'Display order',
    `leader`        VARCHAR(64)     DEFAULT NULL COMMENT 'Department leader',
    `phone`         VARCHAR(20)     DEFAULT NULL COMMENT 'Contact phone',
    `email`         VARCHAR(128)    DEFAULT NULL COMMENT 'Contact email',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT 'Status: 1=active 0=disabled',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT         NOT NULL DEFAULT 0,
    `lock_version`  INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Departments';

CREATE TABLE `mate_audit_log` (
    `id`            VARCHAR(64)     NOT NULL COMMENT 'Primary key',
    `module`        VARCHAR(64)     NOT NULL COMMENT 'Business module',
    `operation`     VARCHAR(128)    NOT NULL COMMENT 'Operation description',
    `method`        VARCHAR(256)    DEFAULT NULL COMMENT 'Method signature',
    `request_args`  TEXT            DEFAULT NULL COMMENT 'Request parameters (JSON)',
    `response_data` TEXT            DEFAULT NULL COMMENT 'Response data (JSON)',
    `status`        VARCHAR(16)     NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAIL',
    `error_msg`     VARCHAR(1024)   DEFAULT NULL COMMENT 'Error message if failed',
    `cost_time`     BIGINT          DEFAULT NULL COMMENT 'Execution time (ms)',
    `operator_id`   VARCHAR(64)     DEFAULT NULL COMMENT 'Operator user ID',
    `operator_ip`   VARCHAR(64)     DEFAULT NULL COMMENT 'Operator IP address',
    `operate_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Operation time',
    PRIMARY KEY (`id`),
    KEY `idx_module` (`module`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Audit logs';

CREATE TABLE `mate_app_key` (
    `id`            VARCHAR(64)     NOT NULL COMMENT 'Primary key',
    `app_key`       VARCHAR(64)     NOT NULL COMMENT 'Application key',
    `app_secret`    VARCHAR(128)    NOT NULL COMMENT 'Application secret',
    `name`          VARCHAR(128)    NOT NULL COMMENT 'Application name',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT 'Status: 1=active 0=disabled',
    `expire_time`   DATETIME        DEFAULT NULL COMMENT 'Expiration time, null=never',
    `remark`        VARCHAR(256)    DEFAULT NULL COMMENT 'Remark',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT         NOT NULL DEFAULT 0,
    `lock_version`  INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_key` (`app_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Open API keys';
```

---

## Part 4: Usage Examples

### Data Permission Usage

```java
// In a query service method
@DataPermission(deptAlias = "u.dept_id", userAlias = "u.creator_id")
public List<UserInfoResponse> pageQuery(UserPageQueryReq req) {
    return userRepository.pageQuery(req);
    // SQL automatically appended with data scope WHERE conditions
}
```

### API Sign Usage

```java
@ApiSign(timeout = 300)
@PostMapping("/open/submit")
public Result<String> openApiSubmit(@RequestBody OrderRequest req) {
    return Result.ok(orderService.submit(req));
}
```

### Repeat Submit Usage

```java
@RepeatSubmit(interval = 5, message = "Please wait before submitting again")
@PostMapping
public Result<String> createUser(@Valid @RequestBody RegisterUserCommand cmd) {
    return Result.ok(userCommandService.createUser(cmd));
}
```

### Rate Limit Usage

```java
@RateLimit(qps = 5, type = LimitType.USER)
@GetMapping("/export")
public void exportData(HttpServletResponse response) {
    // Rate limited to 5 requests per second per user
}
```

### Desensitize Usage

```java
public class UserInfoResponse {

    @Desensitize(type = DesensitizeType.MOBILE)
    private String mobile;       // Output: 138****0000

    @Desensitize(type = DesensitizeType.NAME)
    private String realName;     // Output: J***

    @Desensitize(type = DesensitizeType.EMAIL)
    private String email;        // Output: a**@example.com

    @Desensitize(type = DesensitizeType.ID_CARD)
    private String idCard;       // Output: 110***********1234
}
```

### Audit Log Usage

```java
@AuditLog(module = "User Management", operation = "Create User")
@PostMapping
public Result<String> createUser(@Valid @RequestBody RegisterUserCommand cmd) {
    return Result.ok(userCommandService.createUser(cmd));
}
```

### Field Encryption Usage

```java
@TableName("mate_user")
public class UserPO {

    @TableField(typeHandler = EncryptTypeHandler.class)
    private String mobile;     // DB stores ciphertext, Java reads plaintext

    @TableField(typeHandler = EncryptTypeHandler.class)
    private String idCard;
}
```

---

## Verification Plan

1. **Data Permission**: Same API, admin sees all data, regular user sees own data only
2. **Repeat Submit**: POST twice within 5s, second returns 429 with message
3. **API Sign**: Tamper with parameters after signing, verify signature invalid response
4. **Rate Limit**: Exceed QPS threshold, verify rate limit response
5. **Desensitize**: GET /users returns mobile as 138****0000
6. **Audit Log**: Perform operation, query mate_audit_log for record
7. **Field Encryption**: Query DB directly to see ciphertext, API returns plaintext
