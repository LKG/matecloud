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
package vip.mate.starter.security.repeatsubmit;

import cn.hutool.crypto.digest.DigestUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vip.mate.base.exception.BizException;
import vip.mate.starter.security.SecurityErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;

/**
 * Implements {@link RepeatSubmit} by hashing method signature + args + user
 * and storing a Redis lock for the configured window.
 *
 * @author mateaix
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class RepeatSubmitAspect {

    private static final String KEY_PREFIX = "repeatSubmit:";

    private final RedissonClient redissonClient;

    @Around("@annotation(repeatSubmit)")
    public Object around(ProceedingJoinPoint pjp, RepeatSubmit repeatSubmit) throws Throwable {
        String key = KEY_PREFIX + buildKey(pjp);
        RBucket<String> bucket = redissonClient.getBucket(key);
        boolean acquired = bucket.setIfAbsent("1", Duration.ofSeconds(repeatSubmit.seconds()));
        if (!acquired) {
            throw new BizException(SecurityErrorCode.REPEAT_SUBMIT);
        }
        return pjp.proceed();
    }

    private String buildKey(ProceedingJoinPoint pjp) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String method = sig.getDeclaringType().getSimpleName() + "." + sig.getName();
        String args = Arrays.deepToString(pjp.getArgs());
        String uri = "";
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                uri = request.getRequestURI();
            }
        } catch (Exception ignored) {
        }
        // Scope the dedup key to the caller. Without this, two different users
        // submitting a semantically identical request inside the same window
        // share one key, so the second user is wrongly rejected as a repeat
        // submit (and the key is publicly guessable — a lightweight DoS vector).
        return DigestUtil.md5Hex(getPrincipal() + "|" + method + "|" + uri + "|" + args);
    }

    /**
     * Identifies the caller: the Sa-Token login id when authenticated, otherwise
     * the gateway-set {@code X-Real-IP} (never the client-supplied XFF). Resolved
     * reflectively so this starter keeps no hard dependency on Sa-Token.
     */
    private String getPrincipal() {
        try {
            Class<?> stpUtilClass = Class.forName("cn.dev33.satoken.stp.StpUtil");
            Object id = stpUtilClass.getMethod("getLoginIdDefaultNull").invoke(null);
            if (id != null) {
                return "u:" + id;
            }
        } catch (Throwable ignored) {
        }
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String realIp = request.getHeader("X-Real-IP");
                if (realIp != null && !realIp.isBlank()) {
                    return "ip:" + realIp.trim();
                }
                return "ip:" + request.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return "anonymous";
    }
}
