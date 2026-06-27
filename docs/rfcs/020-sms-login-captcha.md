# RFC-020: SMS Login + Captcha Verification

| Field       | Value                                          |
|-------------|------------------------------------------------|
| **RFC**     | 020                                            |
| **Title**   | SMS Login + Captcha Verification               |
| **Status**  | Draft                                          |
| **Created** | 2026-04-11                                     |
| **Module**  | mate-auth                                      |
| **Package** | vip.mate.auth                                  |

---

## 1. Overview

Extends `mate-auth` (RFC-007) with two additional authentication flows:

1. **SMS Login** -- send a 6-digit code to mobile, verify it, then login via Sa-Token
2. **Admin Captcha** -- generate a base64 captcha image for admin portal login protection

Both flows use Redis for code/captcha storage with TTL-based expiration, and include rate limiting to prevent abuse.

---

## 2. Module Structure (Additions to mate-auth)

```
mate-auth/
  src/main/java/vip/mate/auth/
    trigger/
      controller/
        SmsAuthController.java          <-- NEW
        CaptchaController.java          <-- NEW
    application/
      command/
        SmsLoginCommand.java            <-- NEW
        SmsSendCommand.java             <-- NEW
        CaptchaVerifyCommand.java       <-- NEW
        SmsAuthCommandService.java      <-- NEW
    domain/
      service/
        ISmsAuthDomainService.java      <-- NEW
        impl/
          SmsAuthDomainServiceImpl.java <-- NEW
    infrastructure/
      adapter/
        SmsGatewayAdapter.java          <-- NEW
        CaptchaService.java             <-- NEW
      config/
        SmsRateLimitConfig.java         <-- NEW
```

---

## 3. pom.xml Additions

Add to `mate-auth/pom.xml`:

```xml
<!-- Hutool (for captcha generation) -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-captcha</artifactId>
    <version>${hutool.version}</version>
</dependency>

<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-core</artifactId>
    <version>${hutool.version}</version>
</dependency>
```

The following dependencies are already present via `mate-cache-starter`:
- `spring-boot-starter-data-redis`
- `redisson-spring-boot-starter`

---

## 4. Source Code

### 4.1 SmsSendCommand.java

```java
package vip.mate.auth.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Command for requesting an SMS verification code.
 */
@Data
public class SmsSendCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Mobile phone number (Chinese format: 11 digits starting with 1).
     */
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid mobile number format")
    private String mobile;
}
```

### 4.2 SmsLoginCommand.java

```java
package vip.mate.auth.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Command for SMS code login.
 */
@Data
public class SmsLoginCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Mobile phone number.
     */
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid mobile number format")
    private String mobile;

    /**
     * 6-digit SMS verification code.
     */
    @NotBlank(message = "SMS code is required")
    @Pattern(regexp = "^\\d{6}$", message = "SMS code must be 6 digits")
    private String code;
}
```

### 4.3 CaptchaVerifyCommand.java

```java
package vip.mate.auth.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Command for verifying a captcha before admin login.
 */
@Data
public class CaptchaVerifyCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Unique key identifying the captcha (returned when captcha was generated).
     */
    @NotBlank(message = "Captcha key is required")
    private String captchaKey;

    /**
     * User-entered captcha text.
     */
    @NotBlank(message = "Captcha code is required")
    private String captchaCode;
}
```

### 4.4 SmsRateLimitConfig.java

```java
package vip.mate.auth.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Rate limiting configuration for SMS code sending.
 */
@Data
@Component
@ConfigurationProperties(prefix = "mate.auth.sms")
public class SmsRateLimitConfig {

    /**
     * Minimum interval (seconds) between SMS sends to the same mobile.
     * Default: 60 seconds.
     */
    private long mobileIntervalSeconds = 60;

    /**
     * Maximum SMS sends per IP per hour.
     * Default: 10.
     */
    private int ipMaxPerHour = 10;

    /**
     * SMS code TTL in seconds.
     * Default: 300 (5 minutes).
     */
    private long codeTtlSeconds = 300;

    /**
     * SMS code length.
     * Default: 6.
     */
    private int codeLength = 6;
}
```

### 4.5 SmsGatewayAdapter.java

```java
package vip.mate.auth.infrastructure.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Infrastructure adapter for sending SMS messages via external gateway.
 * <p>
 * In production, this would integrate with a real SMS provider
 * (Aliyun SMS, Tencent SMS, etc.). For development, it logs the code.
 * <p>
 * To switch to a real provider, implement the {@code sendSms} method
 * using the provider's SDK and configure credentials via Nacos.
 */
@Slf4j
@Component
public class SmsGatewayAdapter {

    /**
     * Send an SMS verification code to the given mobile number.
     *
     * @param mobile the target mobile number
     * @param code   the verification code
     * @return true if the SMS was sent successfully
     */
    public boolean sendSms(String mobile, String code) {
        // TODO: Replace with real SMS provider integration
        // Example for Aliyun SMS:
        //   Client client = new Client(accessKeyId, accessKeySecret);
        //   SendSmsRequest request = new SendSmsRequest()
        //       .setPhoneNumbers(mobile)
        //       .setSignName("MateCloud")
        //       .setTemplateCode("SMS_XXXXXX")
        //       .setTemplateParam("{\"code\":\"" + code + "\"}");
        //   client.sendSms(request);

        log.info("[SMS] Sending code to mobile={}, code={} (DEV MODE - not actually sent)", mobile, code);
        return true;
    }
}
```

### 4.6 CaptchaService.java

```java
package vip.mate.auth.infrastructure.adapter;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.lang.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Captcha generation and verification service using Hutool captcha.
 * <p>
 * Generates a line captcha image, stores the answer in Redis with a 5-min TTL,
 * and returns the base64-encoded image + a unique captcha key to the client.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CaptchaService {

    private static final String CAPTCHA_KEY_PREFIX = "captcha:code:";
    private static final long CAPTCHA_TTL_SECONDS = 300;
    private static final int CAPTCHA_WIDTH = 200;
    private static final int CAPTCHA_HEIGHT = 60;
    private static final int CAPTCHA_CODE_COUNT = 4;
    private static final int CAPTCHA_LINE_COUNT = 80;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Generate a new captcha.
     *
     * @return map containing "captchaKey" and "captchaImage" (base64-encoded PNG)
     */
    public Map<String, String> generateCaptcha() {
        // 1. Create line captcha using Hutool
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(
                CAPTCHA_WIDTH, CAPTCHA_HEIGHT, CAPTCHA_CODE_COUNT, CAPTCHA_LINE_COUNT);

        // 2. Generate a unique key for this captcha
        String captchaKey = UUID.fastUUID().toString(true);
        String captchaCode = captcha.getCode().toLowerCase();

        // 3. Store in Redis with TTL
        String redisKey = CAPTCHA_KEY_PREFIX + captchaKey;
        stringRedisTemplate.opsForValue().set(redisKey, captchaCode, CAPTCHA_TTL_SECONDS, TimeUnit.SECONDS);

        // 4. Get base64 image
        String base64Image = captcha.getImageBase64Data();

        log.debug("[Captcha] Generated captcha: key={}, code={}", captchaKey, captchaCode);

        Map<String, String> result = new HashMap<>(4);
        result.put("captchaKey", captchaKey);
        result.put("captchaImage", base64Image);
        return result;
    }

    /**
     * Verify a captcha code against the stored answer.
     *
     * @param captchaKey  the unique captcha key
     * @param captchaCode the user-entered code
     * @return true if the code matches
     */
    public boolean verifyCaptcha(String captchaKey, String captchaCode) {
        String redisKey = CAPTCHA_KEY_PREFIX + captchaKey;
        String storedCode = stringRedisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            log.warn("[Captcha] Captcha expired or not found: key={}", captchaKey);
            return false;
        }

        // Delete after verification (one-time use)
        stringRedisTemplate.delete(redisKey);

        boolean match = storedCode.equalsIgnoreCase(captchaCode);
        if (!match) {
            log.warn("[Captcha] Captcha mismatch: key={}, expected={}, got={}",
                    captchaKey, storedCode, captchaCode);
        }
        return match;
    }
}
```

### 4.7 ISmsAuthDomainService.java

```java
package vip.mate.auth.domain.service;

import vip.mate.api.dto.UserInfoDTO;

/**
 * Domain service interface for SMS-based authentication.
 */
public interface ISmsAuthDomainService {

    /**
     * Generate and store an SMS verification code for the given mobile.
     *
     * @param mobile the mobile number
     * @return the generated code (for logging/testing; not exposed to client)
     */
    String generateSmsCode(String mobile);

    /**
     * Verify the SMS code for the given mobile number.
     *
     * @param mobile the mobile number
     * @param code   the code entered by the user
     * @return true if the code is valid
     */
    boolean verifySmsCode(String mobile, String code);

    /**
     * Lookup user by mobile. If user does not exist, auto-create a new one.
     *
     * @param mobile the mobile number
     * @return user info DTO (never null)
     */
    UserInfoDTO lookupOrCreateUser(String mobile);
}
```

### 4.8 SmsAuthDomainServiceImpl.java

```java
package vip.mate.auth.domain.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import vip.mate.api.dto.UserInfoDTO;
import vip.mate.auth.domain.service.ISmsAuthDomainService;
import vip.mate.auth.infrastructure.adapter.UserRpcAdapter;
import vip.mate.auth.infrastructure.config.SmsRateLimitConfig;
import vip.mate.base.exception.BizException;
import vip.mate.base.enums.ErrorCode;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * Domain service implementation for SMS-based authentication.
 * <p>
 * Handles code generation, Redis storage, verification, and user lookup/creation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsAuthDomainServiceImpl implements ISmsAuthDomainService {

    private static final String SMS_CODE_KEY_PREFIX = "sms:code:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate stringRedisTemplate;
    private final SmsRateLimitConfig rateLimitConfig;
    private final UserRpcAdapter userRpcAdapter;

    @Override
    public String generateSmsCode(String mobile) {
        // Generate a random N-digit code
        int length = rateLimitConfig.getCodeLength();
        int bound = (int) Math.pow(10, length);
        String code = String.format("%0" + length + "d", RANDOM.nextInt(bound));

        // Store in Redis with TTL
        String redisKey = SMS_CODE_KEY_PREFIX + mobile;
        stringRedisTemplate.opsForValue().set(
                redisKey, code,
                rateLimitConfig.getCodeTtlSeconds(), TimeUnit.SECONDS);

        log.info("[SMS] Generated code for mobile={}, ttl={}s", mobile, rateLimitConfig.getCodeTtlSeconds());
        return code;
    }

    @Override
    public boolean verifySmsCode(String mobile, String code) {
        String redisKey = SMS_CODE_KEY_PREFIX + mobile;
        String storedCode = stringRedisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            log.warn("[SMS] Code expired or not found for mobile={}", mobile);
            return false;
        }

        if (!storedCode.equals(code)) {
            log.warn("[SMS] Code mismatch for mobile={}, expected={}, got={}", mobile, storedCode, code);
            return false;
        }

        // Delete after successful verification (one-time use)
        stringRedisTemplate.delete(redisKey);
        log.info("[SMS] Code verified successfully for mobile={}", mobile);
        return true;
    }

    @Override
    public UserInfoDTO lookupOrCreateUser(String mobile) {
        // Try to find existing user
        UserInfoDTO userInfo = userRpcAdapter.findByMobile(mobile);

        if (userInfo != null) {
            // Check status
            if (!"ACTIVE".equals(userInfo.getStatus())) {
                throw new BizException(ErrorCode.FORBIDDEN, "Account is frozen or deleted");
            }
            userInfo.setPassword(null);
            return userInfo;
        }

        // Auto-create user for SMS login (new user registration)
        log.info("[SMS] User not found for mobile={}, auto-creating", mobile);
        UserInfoDTO newUser = userRpcAdapter.createByMobile(mobile);
        if (newUser == null) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to create user");
        }
        newUser.setPassword(null);
        return newUser;
    }
}
```

### 4.9 SmsAuthCommandService.java

```java
package vip.mate.auth.application.command;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import vip.mate.api.dto.UserInfoDTO;
import vip.mate.auth.domain.service.ISmsAuthDomainService;
import vip.mate.auth.infrastructure.adapter.SmsGatewayAdapter;
import vip.mate.auth.infrastructure.config.SmsRateLimitConfig;
import vip.mate.base.exception.BizException;
import vip.mate.base.enums.ErrorCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Application-layer command service for SMS authentication.
 * <p>
 * Orchestrates rate limiting, code sending, verification, and Sa-Token session creation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsAuthCommandService {

    private static final String SMS_RATE_MOBILE_PREFIX = "sms:rate:mobile:";
    private static final String SMS_RATE_IP_PREFIX = "sms:rate:ip:";
    private static final String ROLE_KEY_PREFIX = "mate:user:role:";
    private static final String PERM_KEY_PREFIX = "mate:user:perm:";
    private static final long CACHE_EXPIRE_SECONDS = 86400L;

    private final ISmsAuthDomainService smsAuthDomainService;
    private final SmsGatewayAdapter smsGatewayAdapter;
    private final SmsRateLimitConfig rateLimitConfig;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Send SMS verification code to the specified mobile.
     * <p>
     * Applies rate limiting:
     * - Same mobile: max 1 per minute
     * - Same IP: max 10 per hour
     *
     * @param command the SMS send command
     * @param request the HTTP request (for IP extraction)
     */
    public void sendSmsCode(SmsSendCommand command, HttpServletRequest request) {
        String mobile = command.getMobile();
        String clientIp = getClientIp(request);

        // 1. Check mobile rate limit (1 per minute)
        checkMobileRateLimit(mobile);

        // 2. Check IP rate limit (10 per hour)
        checkIpRateLimit(clientIp);

        // 3. Generate code via domain service
        String code = smsAuthDomainService.generateSmsCode(mobile);

        // 4. Send via SMS gateway
        boolean sent = smsGatewayAdapter.sendSms(mobile, code);
        if (!sent) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to send SMS");
        }

        // 5. Record rate limit markers
        recordMobileRateLimit(mobile);
        recordIpRateLimit(clientIp);

        log.info("[SMS] Code sent successfully: mobile={}, ip={}", mobile, clientIp);
    }

    /**
     * Verify SMS code and login.
     * <p>
     * Flow: verify code -> lookup/create user via Dubbo RPC -> Sa-Token login -> return token.
     *
     * @param command the SMS login command
     * @return map with "token" and "tokenName"
     */
    public Map<String, String> smsLogin(SmsLoginCommand command) {
        String mobile = command.getMobile();
        String code = command.getCode();

        // 1. Verify the SMS code
        boolean valid = smsAuthDomainService.verifySmsCode(mobile, code);
        if (!valid) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Invalid or expired SMS code");
        }

        // 2. Lookup or create user
        UserInfoDTO userInfo = smsAuthDomainService.lookupOrCreateUser(mobile);

        // 3. Create Sa-Token session
        StpUtil.login(userInfo.getUserId());
        String tokenValue = StpUtil.getTokenValue();

        // 4. Cache roles and permissions
        cacheUserRolesAndPermissions(userInfo);

        log.info("[SMS] SMS login successful: userId={}, mobile={}", userInfo.getUserId(), mobile);

        Map<String, String> result = new HashMap<>(4);
        result.put("token", tokenValue);
        result.put("tokenName", StpUtil.getTokenName());
        return result;
    }

    // ======================== Rate Limiting ========================

    private void checkMobileRateLimit(String mobile) {
        String key = SMS_RATE_MOBILE_PREFIX + mobile;
        Boolean exists = stringRedisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS,
                    "SMS code already sent, please wait " + rateLimitConfig.getMobileIntervalSeconds() + " seconds");
        }
    }

    private void checkIpRateLimit(String ip) {
        String key = SMS_RATE_IP_PREFIX + ip;
        String countStr = stringRedisTemplate.opsForValue().get(key);
        if (countStr != null) {
            int count = Integer.parseInt(countStr);
            if (count >= rateLimitConfig.getIpMaxPerHour()) {
                throw new BizException(ErrorCode.TOO_MANY_REQUESTS,
                        "Too many SMS requests from this IP, please try again later");
            }
        }
    }

    private void recordMobileRateLimit(String mobile) {
        String key = SMS_RATE_MOBILE_PREFIX + mobile;
        stringRedisTemplate.opsForValue().set(key, "1",
                rateLimitConfig.getMobileIntervalSeconds(), TimeUnit.SECONDS);
    }

    private void recordIpRateLimit(String ip) {
        String key = SMS_RATE_IP_PREFIX + ip;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            // First request from this IP in this window, set 1-hour expiry
            stringRedisTemplate.expire(key, 1, TimeUnit.HOURS);
        }
    }

    // ======================== Helpers ========================

    private void cacheUserRolesAndPermissions(UserInfoDTO userInfo) {
        String userId = String.valueOf(userInfo.getUserId());

        List<String> roles = userInfo.getRoles();
        if (roles != null && !roles.isEmpty()) {
            String roleKey = ROLE_KEY_PREFIX + userId;
            stringRedisTemplate.delete(roleKey);
            stringRedisTemplate.opsForSet().add(roleKey, roles.toArray(new String[0]));
            stringRedisTemplate.expire(roleKey, CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        }

        List<String> permissions = userInfo.getPermissions();
        if (permissions != null && !permissions.isEmpty()) {
            String permKey = PERM_KEY_PREFIX + userId;
            stringRedisTemplate.delete(permKey);
            stringRedisTemplate.opsForSet().add(permKey, permissions.toArray(new String[0]));
            stringRedisTemplate.expire(permKey, CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Take the first IP if X-Forwarded-For contains multiple
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
```

### 4.10 SmsAuthController.java

```java
package vip.mate.auth.trigger.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.auth.application.command.SmsAuthCommandService;
import vip.mate.auth.application.command.SmsLoginCommand;
import vip.mate.auth.application.command.SmsSendCommand;
import vip.mate.base.result.Result;

import java.util.Map;

/**
 * SMS authentication API endpoints.
 * <p>
 * Provides two endpoints:
 * <ul>
 *   <li>POST /api/v1/auth/sms/send -- send SMS verification code</li>
 *   <li>POST /api/v1/auth/sms/login -- verify code and login</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth/sms")
@RequiredArgsConstructor
public class SmsAuthController {

    private final SmsAuthCommandService smsAuthCommandService;

    /**
     * Send SMS verification code to the specified mobile number.
     * <p>
     * Rate limits:
     * - Same mobile: max 1 per minute
     * - Same IP: max 10 per hour
     *
     * @param command the send command containing the mobile number
     * @param request the HTTP request (for IP-based rate limiting)
     * @return success result
     */
    @PostMapping("/send")
    public Result<Void> sendCode(@Valid @RequestBody SmsSendCommand command,
                                 HttpServletRequest request) {
        smsAuthCommandService.sendSmsCode(command, request);
        return Result.ok();
    }

    /**
     * Verify SMS code and login.
     * <p>
     * Flow: verify code -> lookup/create user -> Sa-Token login -> return token.
     *
     * @param command the login command containing mobile + code
     * @return token info
     */
    @PostMapping("/login")
    public Result<Map<String, String>> login(@Valid @RequestBody SmsLoginCommand command) {
        Map<String, String> tokenInfo = smsAuthCommandService.smsLogin(command);
        return Result.ok(tokenInfo);
    }
}
```

### 4.11 CaptchaController.java

```java
package vip.mate.auth.trigger.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.auth.infrastructure.adapter.CaptchaService;
import vip.mate.base.result.Result;

import java.util.Map;

/**
 * Captcha API endpoint for admin login protection.
 * <p>
 * Generates a line captcha image and returns it as base64 with a unique key.
 * The client must send back captchaKey + captchaCode during admin login.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    /**
     * Generate a new captcha image.
     *
     * @return map containing:
     *         - captchaKey: unique identifier to reference this captcha
     *         - captchaImage: base64-encoded PNG image (data URI format)
     */
    @GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha() {
        Map<String, String> captchaData = captchaService.generateCaptcha();
        return Result.ok(captchaData);
    }
}
```

### 4.12 mate-api Addition: UserRpcAdapter.createByMobile

Add to `UserRpcAdapter.java`:

```java
/**
 * Auto-create a new user by mobile number (for SMS-login registration).
 *
 * @param mobile the mobile number
 * @return newly created user info DTO
 */
public UserInfoDTO createByMobile(String mobile) {
    try {
        return rpcUserService.createByMobile(mobile);
    } catch (Exception e) {
        log.error("[Auth] RPC call createByMobile failed: mobile={}", mobile, e);
        throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to create user via RPC");
    }
}
```

### 4.13 mate-api Addition: IRpcUserService.createByMobile

Add to `IRpcUserService.java` in `mate-common/mate-api`:

```java
/**
 * Auto-create a new user by mobile number.
 * Used by SMS login when the mobile is not yet registered.
 *
 * @param mobile the mobile number
 * @return newly created user info DTO
 */
UserInfoDTO createByMobile(String mobile);
```

---

## 5. Configuration

### 5.1 Nacos Config (mate-auth.yml)

```yaml
# SMS authentication configuration
mate:
  auth:
    sms:
      mobile-interval-seconds: 60
      ip-max-per-hour: 10
      code-ttl-seconds: 300
      code-length: 6
```

### 5.2 Gateway Route Addition

Add to gateway route configuration for anonymous access:

```yaml
# In mate-gateway route config, add these to the whitelist:
mate:
  gateway:
    whitelist:
      - /api/v1/auth/sms/send
      - /api/v1/auth/sms/login
      - /api/v1/auth/captcha
```

---

## 6. Redis Key Design

| Key Pattern                  | Value       | TTL     | Purpose                            |
|------------------------------|-------------|---------|-------------------------------------|
| `sms:code:{mobile}`         | 6-digit code | 5 min  | SMS verification code storage       |
| `sms:rate:mobile:{mobile}`  | "1"         | 60 sec  | Per-mobile rate limit marker        |
| `sms:rate:ip:{ip}`          | count (int) | 1 hour  | Per-IP hourly request counter       |
| `captcha:code:{captchaKey}` | 4-char code | 5 min   | Captcha answer storage              |

---

## 7. API Reference

### POST /api/v1/auth/sms/send

Send SMS verification code.

**Request:**
```json
{
  "mobile": "13800138000"
}
```

**Response (200):**
```json
{
  "code": 0,
  "msg": "success",
  "data": null
}
```

**Error (429 - rate limited):**
```json
{
  "code": 42900,
  "msg": "SMS code already sent, please wait 60 seconds",
  "data": null
}
```

### POST /api/v1/auth/sms/login

Verify SMS code and login.

**Request:**
```json
{
  "mobile": "13800138000",
  "code": "123456"
}
```

**Response (200):**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "tokenName": "satoken"
  }
}
```

### GET /api/v1/auth/captcha

Generate captcha image.

**Response (200):**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "captchaKey": "a1b2c3d4e5f6",
    "captchaImage": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."
  }
}
```

---

## 8. Sequence Diagrams

### SMS Login Flow

```
Client          SmsAuthController    SmsAuthCommandService    SmsAuthDomainService    Redis    SmsGateway    UserRpcAdapter
  |                    |                      |                       |                  |          |              |
  |--- POST /send ---->|                      |                       |                  |          |              |
  |                    |--- sendSmsCode() --->|                       |                  |          |              |
  |                    |                      |-- checkMobileRate --->|                  |          |              |
  |                    |                      |                       |--- GET key ----->|          |              |
  |                    |                      |-- checkIpRate ------->|                  |          |              |
  |                    |                      |                       |--- GET key ----->|          |              |
  |                    |                      |-- generateSmsCode --->|                  |          |              |
  |                    |                      |                       |--- SET code ---->|          |              |
  |                    |                      |-- sendSms ----------->|                  |          |              |
  |                    |                      |                       |                  |--------->|              |
  |<--- 200 OK --------|                      |                       |                  |          |              |
  |                    |                      |                       |                  |          |              |
  |--- POST /login --->|                      |                       |                  |          |              |
  |                    |--- smsLogin() ------>|                       |                  |          |              |
  |                    |                      |-- verifySmsCode ----->|                  |          |              |
  |                    |                      |                       |--- GET+DEL ----->|          |              |
  |                    |                      |-- lookupOrCreate ---->|                  |          |              |
  |                    |                      |                       |                  |          |              |
  |                    |                      |                       |--- findByMobile ---------->|              |
  |                    |                      |-- StpUtil.login() --->|                  |          |              |
  |<--- token ----------|                      |                       |                  |          |              |
```

### Captcha Flow

```
Client          CaptchaController    CaptchaService    Redis
  |                    |                    |             |
  |--- GET /captcha -->|                    |             |
  |                    |-- generateCaptcha ->|             |
  |                    |                    |-- SET key -->|
  |<--- captchaKey + base64 image ----------|             |
  |                    |                    |             |
  |--- POST /login (with captchaKey + captchaCode) ------>|
  |                    |-- verifyCaptcha -->|             |
  |                    |                    |-- GET+DEL ->|
  |<--- result ---------|                    |             |
```

---

## 9. Admin Login with Captcha Integration

To require captcha for admin login, modify the existing `AuthCommandService.login()`:

```java
/**
 * Authenticate admin user with captcha verification.
 *
 * @param command      login command containing mobile + password
 * @param captchaKey   the captcha key from GET /captcha
 * @param captchaCode  the user-entered captcha code
 * @return Sa-Token token value
 */
public String loginWithCaptcha(LoginCommand command, String captchaKey, String captchaCode) {
    // 1. Verify captcha first
    boolean captchaValid = captchaService.verifyCaptcha(captchaKey, captchaCode);
    if (!captchaValid) {
        throw new BizException(ErrorCode.BAD_REQUEST, "Invalid or expired captcha");
    }

    // 2. Proceed with normal login
    return login(command);
}
```

Add the corresponding endpoint to `AuthController.java`:

```java
/**
 * Admin login with captcha verification.
 */
@PostMapping("/login/admin")
public Result<Map<String, String>> adminLogin(
        @Valid @RequestBody AdminLoginCommand command) {
    String tokenValue = authCommandService.loginWithCaptcha(
            command.toLoginCommand(), command.getCaptchaKey(), command.getCaptchaCode());

    Map<String, String> data = new HashMap<>(2);
    data.put("token", tokenValue);
    data.put("tokenName", StpUtil.getTokenName());
    return Result.ok(data);
}
```

### AdminLoginCommand.java

```java
package vip.mate.auth.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Admin login command with captcha verification.
 */
@Data
public class AdminLoginCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Mobile number is required")
    private String mobile;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Captcha key is required")
    private String captchaKey;

    @NotBlank(message = "Captcha code is required")
    private String captchaCode;

    /**
     * Convert to a standard LoginCommand (mobile + password only).
     */
    public LoginCommand toLoginCommand() {
        LoginCommand cmd = new LoginCommand();
        cmd.setMobile(this.mobile);
        cmd.setPassword(this.password);
        return cmd;
    }
}
```

---

## 10. Testing Checklist

- [ ] POST `/api/v1/auth/sms/send` sends code and stores in Redis with 5-min TTL
- [ ] POST `/api/v1/auth/sms/send` twice within 60s returns 429
- [ ] POST `/api/v1/auth/sms/send` > 10 times from same IP within 1h returns 429
- [ ] POST `/api/v1/auth/sms/login` with valid code returns token
- [ ] POST `/api/v1/auth/sms/login` with expired code returns 401
- [ ] POST `/api/v1/auth/sms/login` with wrong code returns 401
- [ ] POST `/api/v1/auth/sms/login` for new mobile auto-creates user
- [ ] GET `/api/v1/auth/captcha` returns base64 image and captchaKey
- [ ] POST `/api/v1/auth/login/admin` with valid captcha succeeds
- [ ] POST `/api/v1/auth/login/admin` with expired captcha returns 400
- [ ] Captcha is one-time use (second verification with same key fails)
