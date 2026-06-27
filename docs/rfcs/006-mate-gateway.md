# RFC-006: mate-gateway -- API Gateway Service

| Field       | Value                                |
|-------------|--------------------------------------|
| RFC         | 006                                  |
| Title       | API Gateway Service (mate-gateway)   |
| Status      | Draft                                |
| Created     | 2026-04-11                           |
| Module      | mate-gateway                         |
| Port        | 9010                                 |
| Package     | vip.mate.gateway                     |

---

## 1. Overview

`mate-gateway` is the unified API entry point for the entire mate-cloud platform. Built on **Spring Cloud Gateway (WebFlux)**, it handles:

- Dynamic route configuration via Nacos
- Sa-Token-based authentication / authorization (reactive filter)
- Path-level auth strategies: public, login-required, admin-only
- Service discovery and load balancing via Nacos + Spring Cloud LoadBalancer

---

## 2. Module Structure

```
mate-gateway/
  pom.xml
  src/main/java/vip/mate/gateway/
    MateGatewayApplication.java
    config/
      AuthStrategyConfig.java
      DynamicStrategyFactory.java
      SaTokenGatewayConfig.java
    auth/
      StpUserInterfaceImpl.java
  src/main/resources/
    bootstrap.yml
```

---

## 3. pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>matecloud</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>mate-gateway</artifactId>
    <packaging>jar</packaging>
    <description>Mate Cloud - API Gateway</description>

    <dependencies>
        <!-- Spring Cloud Gateway (WebFlux based) -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway-server-webflux</artifactId>
        </dependency>

        <!-- Spring Cloud LoadBalancer -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        </dependency>

        <!-- Mate Common Base (Result, ErrorCode, BaseEntity, etc.) -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-base</artifactId>
        </dependency>

        <!-- Mate API (RPC interfaces, DTOs) -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-api</artifactId>
        </dependency>

        <!-- Mate Nacos Starter (discovery + config) -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-nacos-starter</artifactId>
        </dependency>

        <!-- Mate Sa-Token Starter (authentication) -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-sa-token-starter</artifactId>
        </dependency>

        <!-- Mate Cache Starter (Redis for Sa-Token session) -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-cache-starter</artifactId>
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

## 4. bootstrap.yml

```yaml
server:
  port: 9010

spring:
  application:
    name: mate-gateway
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}
        namespace: ${NACOS_NAMESPACE:dev}
        group: DEFAULT_GROUP
      config:
        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}
        namespace: ${NACOS_NAMESPACE:dev}
        group: DEFAULT_GROUP
        file-extension: yml
  config:
    import:
      - optional:nacos:${spring.application.name}.yml
      - optional:nacos:mate-common.yml?group=DEFAULT_GROUP&refreshEnabled=true

logging:
  level:
    vip.mate.gateway: debug
    org.springframework.cloud.gateway: debug
```

---

## 5. Nacos Gateway Routes Configuration

The following YAML is stored in Nacos as `mate-gateway.yml`:

```yaml
spring:
  cloud:
    gateway:
      routes:
        # ---- Auth Service ----
        - id: mate-auth
          uri: lb://mate-auth
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - StripPrefix=0

        # ---- System Service ----
        - id: mate-system
          uri: lb://mate-system
          predicates:
            - Path=/api/v1/users/**,/api/v1/roles/**,/api/v1/menus/**,/api/v1/depts/**
          filters:
            - StripPrefix=0

        # ---- File Service (future) ----
        - id: mate-file
          uri: lb://mate-file
          predicates:
            - Path=/api/v1/files/**
          filters:
            - StripPrefix=0

      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Origin Access-Control-Allow-Credentials, RETAIN_UNIQUE

      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origins: "*"
            allowed-methods: "*"
            allowed-headers: "*"
            allow-credentials: false
            max-age: 3600

# Sa-Token config (shared via mate-common.yml or inline)
sa-token:
  token-name: Authorization
  token-prefix: Bearer
  timeout: 86400
  active-timeout: 1800
  is-concurrent: true
  token-style: uuid
  is-read-cookie: false
  is-read-header: true
  is-log: false
```

---

## 6. Source Code

### 6.1 MateGatewayApplication.java

```java
package vip.mate.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Mate Cloud API Gateway entry point.
 * <p>
 * WebFlux-based gateway -- do NOT include spring-boot-starter-web.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class MateGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(MateGatewayApplication.class, args);
    }
}
```

### 6.2 AuthStrategyConfig.java

```java
package vip.mate.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines three tiers of auth strategies for gateway routes.
 * <p>
 * Configuration is loaded from Nacos (mate-gateway.yml) under prefix {@code mate.gateway.auth}.
 *
 * <pre>
 * mate:
 *   gateway:
 *     auth:
 *       public-paths:
 *         - /api/v1/auth/login
 *         - /api/v1/auth/captcha
 *         - /actuator/**
 *         - /doc.html
 *         - /webjars/**
 *       login-paths:
 *         - /api/v1/**
 *       admin-paths:
 *         - /api/v1/users/delete/**
 *         - /api/v1/roles/**
 *         - /api/v1/menus/**
 * </pre>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mate.gateway.auth")
public class AuthStrategyConfig {

    /**
     * Paths that require NO authentication (anonymous access).
     */
    private List<String> publicPaths = new ArrayList<>(List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/captcha",
            "/actuator/**",
            "/doc.html",
            "/webjars/**",
            "/swagger-resources/**",
            "/v3/api-docs/**"
    ));

    /**
     * Paths that require a valid login session (any authenticated user).
     */
    private List<String> loginPaths = new ArrayList<>(List.of(
            "/api/v1/**"
    ));

    /**
     * Paths restricted to admin role only.
     */
    private List<String> adminPaths = new ArrayList<>(List.of(
            "/api/v1/users/delete/**",
            "/api/v1/roles/**",
            "/api/v1/menus/**"
    ));
}
```

### 6.3 DynamicStrategyFactory.java

```java
package vip.mate.gateway.config;

import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Selects the correct auth strategy per request path.
 * <p>
 * Strategy priority:
 * <ol>
 *   <li>Public paths &rarr; skip authentication</li>
 *   <li>Admin paths &rarr; require login + admin role</li>
 *   <li>Login paths &rarr; require login</li>
 * </ol>
 *
 * Used inside {@link SaTokenGatewayConfig} to configure the reactive filter chain.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicStrategyFactory {

    private final AuthStrategyConfig authStrategyConfig;

    /**
     * Apply auth strategies to the Sa-Token reactive filter.
     *
     * @param filter the SaReactorFilter to configure
     */
    public void applyStrategies(SaReactorFilter filter) {
        // 1. Public paths -- no authentication required
        filter.addExclude(authStrategyConfig.getPublicPaths().toArray(new String[0]));

        // 2. Set the main auth function
        filter.setAuth(obj -> {
            // Admin paths -- must have ADMIN role
            SaRouter.match(authStrategyConfig.getAdminPaths())
                    .check(r -> {
                        StpUtil.checkLogin();
                        StpUtil.checkRole("ADMIN");
                    });

            // Login paths -- must be logged in
            SaRouter.match(authStrategyConfig.getLoginPaths())
                    .check(r -> StpUtil.checkLogin());
        });

        log.info("[Gateway] Auth strategies applied - public: {}, login: {}, admin: {}",
                authStrategyConfig.getPublicPaths().size(),
                authStrategyConfig.getLoginPaths().size(),
                authStrategyConfig.getAdminPaths().size());
    }
}
```

### 6.4 SaTokenGatewayConfig.java

```java
package vip.mate.gateway.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vip.mate.base.result.Result;
import vip.mate.base.enums.ErrorCode;

/**
 * Sa-Token WebFlux (Reactor) filter integration for Spring Cloud Gateway.
 * <p>
 * Registers a {@link SaReactorFilter} bean that intercepts every request
 * before it reaches downstream services. Auth strategy rules are delegated
 * to {@link DynamicStrategyFactory}.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SaTokenGatewayConfig {

    private final DynamicStrategyFactory dynamicStrategyFactory;
    private final ObjectMapper objectMapper;

    @Bean
    public SaReactorFilter saReactorFilter() {
        SaReactorFilter filter = new SaReactorFilter();

        // Apply path-based auth strategies
        dynamicStrategyFactory.applyStrategies(filter);

        // Global error handler -- returns standardized JSON
        filter.setError(e -> {
            // Set response content-type
            SaHolder.getResponse()
                    .setHeader("Content-Type", "application/json;charset=UTF-8");

            Result<?> result;
            if (e instanceof NotLoginException) {
                result = Result.fail(ErrorCode.UNAUTHORIZED.getCode(), "Please login first");
                log.warn("[Gateway] Unauthorized access attempt: {}", e.getMessage());
            } else if (e instanceof NotRoleException) {
                result = Result.fail(ErrorCode.FORBIDDEN.getCode(), "Insufficient permissions");
                log.warn("[Gateway] Forbidden access attempt: {}", e.getMessage());
            } else {
                result = Result.fail(ErrorCode.INTERNAL_ERROR.getCode(), "Gateway authentication error");
                log.error("[Gateway] Authentication error", e);
            }

            try {
                return objectMapper.writeValueAsString(result);
            } catch (Exception ex) {
                log.error("[Gateway] Failed to serialize error response", ex);
                return "{\"code\":500,\"msg\":\"Internal Server Error\",\"data\":null}";
            }
        });

        // Before-auth hook: log request path (debug)
        filter.setBeforeAuth(obj -> {
            log.debug("[Gateway] Incoming request: {}", SaHolder.getRequest().getRequestPath());
        });

        return filter;
    }
}
```

### 6.5 StpUserInterfaceImpl.java

```java
package vip.mate.gateway.auth;

import cn.dev33.satoken.stp.StpInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Sa-Token permission / role data provider.
 * <p>
 * In the gateway (WebFlux context), we cannot use Dubbo RPC directly.
 * Instead, permission and role data is read from Redis, where it was
 * cached during login by the auth service.
 *
 * <p>Redis key conventions:
 * <ul>
 *   <li>{@code mate:user:role:{loginId}} &rarr; Set of role codes</li>
 *   <li>{@code mate:user:perm:{loginId}} &rarr; Set of permission codes</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StpUserInterfaceImpl implements StpInterface {

    private static final String ROLE_KEY_PREFIX = "mate:user:role:";
    private static final String PERM_KEY_PREFIX = "mate:user:perm:";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Return the permission code list for the given loginId.
     *
     * @param loginId   user id
     * @param loginType login type (default: "login")
     * @return list of permission strings
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        String key = PERM_KEY_PREFIX + loginId;
        Set<String> perms = stringRedisTemplate.opsForSet().members(key);
        if (perms == null || perms.isEmpty()) {
            log.debug("[Gateway] No permissions found for loginId={}", loginId);
            return Collections.emptyList();
        }
        return new ArrayList<>(perms);
    }

    /**
     * Return the role code list for the given loginId.
     *
     * @param loginId   user id
     * @param loginType login type (default: "login")
     * @return list of role strings
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        String key = ROLE_KEY_PREFIX + loginId;
        Set<String> roles = stringRedisTemplate.opsForSet().members(key);
        if (roles == null || roles.isEmpty()) {
            log.debug("[Gateway] No roles found for loginId={}", loginId);
            return Collections.emptyList();
        }
        return new ArrayList<>(roles);
    }
}
```

---

## 7. Nacos Auth Strategy Configuration

Add the following to `mate-gateway.yml` in Nacos:

```yaml
mate:
  gateway:
    auth:
      public-paths:
        - /api/v1/auth/login
        - /api/v1/auth/captcha
        - /actuator/**
        - /doc.html
        - /webjars/**
        - /swagger-resources/**
        - /v3/api-docs/**
      login-paths:
        - /api/v1/**
      admin-paths:
        - /api/v1/users/delete/**
        - /api/v1/roles/**
        - /api/v1/menus/**
```

---

## 8. Key Design Decisions

| Decision | Rationale |
|---|---|
| WebFlux-only (no spring-web) | Gateway uses Netty reactor; mixing servlet causes startup failures |
| Redis-based StpInterface | Gateway cannot use Dubbo RPC (reactive context); Redis is shared across services |
| Path-based strategy tiers | Simple, declarative; easy to extend via Nacos hot-reload |
| DynamicStrategyFactory | Decouples strategy logic from filter wiring; testable in isolation |
| Nacos route config | Routes can be changed at runtime without gateway restart |

---

## 9. Testing Checklist

- [ ] Gateway starts on port 9010 and registers with Nacos
- [ ] Public paths (e.g., `/api/v1/auth/login`) return 200 without token
- [ ] Login paths (e.g., `/api/v1/users`) return 401 without token
- [ ] Login paths return 200 with valid Sa-Token
- [ ] Admin paths return 403 for non-admin users
- [ ] Admin paths return 200 for admin users
- [ ] Routes load from Nacos and forward to downstream services
- [ ] Nacos route changes take effect without restart
