# RFC-007: mate-auth -- Authentication Service

| Field       | Value                                 |
|-------------|---------------------------------------|
| RFC         | 007                                   |
| Title       | Authentication Service (mate-auth)    |
| Status      | Draft (base auth; SMS+Captcha in RFC-020) |
| Created     | 2026-04-11                            |
| Module      | mate-auth                             |
| Port        | 9020                                  |
| Package     | vip.mate.auth                         |

---

## 1. Overview

`mate-auth` provides centralized authentication for the mate-cloud platform:

- **Login** via mobile + password, returning a Sa-Token session
- **Logout** to invalidate the current session
- **User info** retrieval for the currently authenticated user
- DDD 4-layer architecture (trigger / application / domain / infrastructure)
- Dubbo RPC integration to call `IRpcUserService` from `mate-system`

---

## 2. Module Structure

```
mate-auth/
  pom.xml
  src/main/java/vip/mate/auth/
    MateAuthApplication.java
    trigger/
      controller/
        AuthController.java
    application/
      command/
        LoginCommand.java
        AuthCommandService.java
    domain/
      service/
        IAuthDomainService.java
        AuthDomainServiceImpl.java
    infrastructure/
      adapter/
        UserRpcAdapter.java
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

    <artifactId>mate-auth</artifactId>
    <packaging>jar</packaging>
    <description>Mate Cloud - Authentication Service</description>

    <dependencies>
        <!-- Mate Common Base -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-base</artifactId>
        </dependency>

        <!-- Mate API (IRpcUserService, DTOs) -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-api</artifactId>
        </dependency>

        <!-- Mate DataSource Starter (MyBatis Plus + Druid) -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-ds-starter</artifactId>
        </dependency>

        <!-- Mate Web Starter (Spring MVC + Jackson) -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-web-starter</artifactId>
        </dependency>

        <!-- Mate Nacos Starter (discovery + config) -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-nacos-starter</artifactId>
        </dependency>

        <!-- Mate RPC Starter (Dubbo) -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-rpc-starter</artifactId>
        </dependency>

        <!-- Mate Cache Starter (Redis) -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-cache-starter</artifactId>
        </dependency>

        <!-- Mate Sa-Token Starter (exclude reactor for servlet context) -->
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
  port: 9020

spring:
  application:
    name: mate-auth
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

# Dubbo
dubbo:
  application:
    name: mate-auth
    qos-enable: false
  protocol:
    name: dubbo
    port: -1
  registry:
    address: nacos://${NACOS_SERVER_ADDR:127.0.0.1:8848}
    parameters:
      namespace: ${NACOS_NAMESPACE:dev}
  consumer:
    check: false
    timeout: 5000
    retries: 1

logging:
  level:
    vip.mate.auth: debug
```

---

## 5. Source Code

### 5.1 MateAuthApplication.java

```java
package vip.mate.auth;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Mate Cloud Authentication Service entry point.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableDubbo
public class MateAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(MateAuthApplication.class, args);
    }
}
```

### 5.2 LoginCommand.java

```java
package vip.mate.auth.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Login request command object.
 */
@Data
public class LoginCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Mobile phone number.
     */
    @NotBlank(message = "Mobile number is required")
    private String mobile;

    /**
     * Password (plain-text from client, will be verified against BCrypt hash).
     */
    @NotBlank(message = "Password is required")
    private String password;
}
```

### 5.3 AuthCommandService.java

```java
package vip.mate.auth.application.command;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import vip.mate.api.dto.UserInfoDTO;
import vip.mate.auth.domain.service.IAuthDomainService;
import vip.mate.base.exception.BizException;
import vip.mate.base.enums.ErrorCode;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Application-layer command service for authentication operations.
 * <p>
 * Orchestrates domain service calls and Sa-Token session management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthCommandService {

    private static final String ROLE_KEY_PREFIX = "mate:user:role:";
    private static final String PERM_KEY_PREFIX = "mate:user:perm:";
    private static final long CACHE_EXPIRE_SECONDS = 86400L;

    private final IAuthDomainService authDomainService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Authenticate user and create Sa-Token session.
     *
     * @param command login command containing mobile + password
     * @return Sa-Token token value
     */
    public String login(LoginCommand command) {
        // 1. Validate credentials via domain service (calls RPC internally)
        UserInfoDTO userInfo = authDomainService.validateCredentials(
                command.getMobile(), command.getPassword());

        // 2. Create Sa-Token session
        StpUtil.login(userInfo.getUserId());
        String tokenValue = StpUtil.getTokenValue();

        // 3. Cache roles and permissions in Redis for gateway StpInterface
        cacheUserRolesAndPermissions(userInfo);

        log.info("[Auth] User logged in: userId={}, mobile={}",
                userInfo.getUserId(), command.getMobile());

        return tokenValue;
    }

    /**
     * Logout current user and clean up cached data.
     */
    public void logout() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId != null) {
            // Clean up cached roles/permissions
            stringRedisTemplate.delete(ROLE_KEY_PREFIX + loginId);
            stringRedisTemplate.delete(PERM_KEY_PREFIX + loginId);

            StpUtil.logout();
            log.info("[Auth] User logged out: loginId={}", loginId);
        }
    }

    /**
     * Cache user roles and permissions to Redis for gateway auth checks.
     */
    private void cacheUserRolesAndPermissions(UserInfoDTO userInfo) {
        String userId = String.valueOf(userInfo.getUserId());

        // Cache roles
        List<String> roles = userInfo.getRoles();
        if (roles != null && !roles.isEmpty()) {
            String roleKey = ROLE_KEY_PREFIX + userId;
            stringRedisTemplate.delete(roleKey);
            stringRedisTemplate.opsForSet().add(roleKey, roles.toArray(new String[0]));
            stringRedisTemplate.expire(roleKey, CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        }

        // Cache permissions
        List<String> permissions = userInfo.getPermissions();
        if (permissions != null && !permissions.isEmpty()) {
            String permKey = PERM_KEY_PREFIX + userId;
            stringRedisTemplate.delete(permKey);
            stringRedisTemplate.opsForSet().add(permKey, permissions.toArray(new String[0]));
            stringRedisTemplate.expire(permKey, CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        }
    }
}
```

### 5.4 AuthController.java

```java
package vip.mate.auth.trigger.controller;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vip.mate.api.dto.UserInfoDTO;
import vip.mate.auth.application.command.AuthCommandService;
import vip.mate.auth.application.command.LoginCommand;
import vip.mate.auth.domain.service.IAuthDomainService;
import vip.mate.base.result.Result;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication API endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthCommandService authCommandService;
    private final IAuthDomainService authDomainService;

    /**
     * User login.
     *
     * @param command login credentials
     * @return token value
     */
    @PostMapping("/login")
    public Result<Map<String, String>> login(@Valid @RequestBody LoginCommand command) {
        String tokenValue = authCommandService.login(command);

        Map<String, String> data = new HashMap<>(2);
        data.put("token", tokenValue);
        data.put("tokenName", StpUtil.getTokenName());

        return Result.ok(data);
    }

    /**
     * User logout.
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authCommandService.logout();
        return Result.ok();
    }

    /**
     * Get current user info.
     *
     * @return user info DTO
     */
    @GetMapping("/info")
    public Result<UserInfoDTO> info() {
        long userId = StpUtil.getLoginIdAsLong();
        UserInfoDTO userInfo = authDomainService.getUserInfo(userId);
        return Result.ok(userInfo);
    }
}
```

### 5.5 IAuthDomainService.java

```java
package vip.mate.auth.domain.service;

import vip.mate.api.dto.UserInfoDTO;

/**
 * Authentication domain service interface.
 */
public interface IAuthDomainService {

    /**
     * Validate user credentials (mobile + password).
     *
     * @param mobile   mobile phone number
     * @param password raw password
     * @return user info DTO if credentials are valid
     * @throws vip.mate.base.exception.BizException if credentials are invalid
     */
    UserInfoDTO validateCredentials(String mobile, String password);

    /**
     * Get user info by userId (delegates to RPC).
     *
     * @param userId user id
     * @return user info DTO
     */
    UserInfoDTO getUserInfo(Long userId);
}
```

### 5.6 AuthDomainServiceImpl.java

```java
package vip.mate.auth.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import vip.mate.api.dto.UserInfoDTO;
import vip.mate.auth.infrastructure.adapter.UserRpcAdapter;
import vip.mate.base.exception.BizException;
import vip.mate.base.enums.ErrorCode;

/**
 * Authentication domain service implementation.
 * <p>
 * Encapsulates credential validation logic. User data is fetched via
 * the infrastructure adapter (Dubbo RPC to mate-system).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthDomainServiceImpl implements IAuthDomainService {

    private final UserRpcAdapter userRpcAdapter;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public UserInfoDTO validateCredentials(String mobile, String password) {
        // 1. Fetch user by mobile via RPC
        UserInfoDTO userInfo = userRpcAdapter.findByMobile(mobile);
        if (userInfo == null) {
            log.warn("[Auth] User not found: mobile={}", mobile);
            throw new BizException(ErrorCode.UNAUTHORIZED, "Invalid mobile or password");
        }

        // 2. Check user status
        if (!"ACTIVE".equals(userInfo.getStatus())) {
            log.warn("[Auth] User account is not active: userId={}, status={}",
                    userInfo.getUserId(), userInfo.getStatus());
            throw new BizException(ErrorCode.FORBIDDEN, "Account is frozen or deleted");
        }

        // 3. Verify password
        if (!passwordEncoder.matches(password, userInfo.getPassword())) {
            log.warn("[Auth] Invalid password for mobile={}", mobile);
            throw new BizException(ErrorCode.UNAUTHORIZED, "Invalid mobile or password");
        }

        // 4. Clear password before returning
        userInfo.setPassword(null);
        return userInfo;
    }

    @Override
    public UserInfoDTO getUserInfo(Long userId) {
        UserInfoDTO userInfo = userRpcAdapter.findById(userId);
        if (userInfo == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "User not found");
        }
        userInfo.setPassword(null);
        return userInfo;
    }
}
```

### 5.7 UserRpcAdapter.java

```java
package vip.mate.auth.infrastructure.adapter;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;
import vip.mate.api.dto.UserInfoDTO;
import vip.mate.api.service.IRpcUserService;
import vip.mate.base.exception.BizException;
import vip.mate.base.enums.ErrorCode;

/**
 * Infrastructure adapter for user data retrieval via Dubbo RPC.
 * <p>
 * Calls {@link IRpcUserService} provided by mate-system service.
 * Translates RPC exceptions to domain-level BizExceptions.
 */
@Slf4j
@Component
public class UserRpcAdapter {

    @DubboReference(check = false, timeout = 5000, retries = 1)
    private IRpcUserService rpcUserService;

    /**
     * Find user by mobile number.
     *
     * @param mobile mobile phone number
     * @return user info DTO or null if not found
     */
    public UserInfoDTO findByMobile(String mobile) {
        try {
            return rpcUserService.findByMobile(mobile);
        } catch (Exception e) {
            log.error("[Auth] RPC call findByMobile failed: mobile={}", mobile, e);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to query user service");
        }
    }

    /**
     * Find user by ID.
     *
     * @param userId user id
     * @return user info DTO or null if not found
     */
    public UserInfoDTO findById(Long userId) {
        try {
            return rpcUserService.findById(userId);
        } catch (Exception e) {
            log.error("[Auth] RPC call findById failed: userId={}", userId, e);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to query user service");
        }
    }
}
```

---

## 6. mate-api Interfaces (Referenced)

The following interfaces and DTOs live in `mate-common/mate-api` and are shared across services:

### IRpcUserService.java (in mate-api)

```java
package vip.mate.api.service;

import vip.mate.api.dto.UserInfoDTO;

/**
 * Dubbo RPC interface for user operations.
 * Implemented by mate-system, consumed by mate-auth and others.
 */
public interface IRpcUserService {

    UserInfoDTO findByMobile(String mobile);

    UserInfoDTO findById(Long userId);
}
```

### UserInfoDTO.java (in mate-api)

```java
package vip.mate.api.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * User information DTO for cross-service communication.
 */
@Data
public class UserInfoDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String mobile;
    private String nickName;
    private String avatar;
    private String gender;
    private String status;

    /** BCrypt-hashed password (cleared before returning to client). */
    private String password;

    /** Role codes (e.g., ADMIN, USER). */
    private List<String> roles;

    /** Permission codes (e.g., sys:user:list). */
    private List<String> permissions;
}
```

---

## 7. Key Design Decisions

| Decision | Rationale |
|---|---|
| Exclude sa-token-reactor | Auth service is servlet-based (Spring MVC), not WebFlux |
| BCrypt in domain service | Password verification is domain logic, not infrastructure |
| Redis caching of roles/perms | Gateway (WebFlux) cannot call Dubbo; Redis is the shared medium |
| Dubbo RPC via adapter | Infrastructure adapter isolates RPC concerns from domain layer |
| LoginCommand as CQRS command | Clean separation of write operations from query |

---

## 8. Testing Checklist

- [ ] Auth service starts on port 9020 and registers with Nacos
- [ ] POST `/api/v1/auth/login` with valid credentials returns token
- [ ] POST `/api/v1/auth/login` with invalid password returns 401
- [ ] POST `/api/v1/auth/login` with frozen account returns 403
- [ ] POST `/api/v1/auth/logout` invalidates session
- [ ] GET `/api/v1/auth/info` returns current user data
- [ ] Roles and permissions are cached in Redis after login
- [ ] Redis cache is cleaned on logout
