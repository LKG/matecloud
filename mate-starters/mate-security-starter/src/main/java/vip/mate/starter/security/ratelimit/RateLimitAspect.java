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
package vip.mate.starter.security.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vip.mate.base.exception.BizException;
import vip.mate.starter.security.SecurityErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.Duration;

/**
 * AOP aspect implementing {@link RateLimit} via Redisson rate limiter.
 *
 * @author mateaix
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedissonClient redissonClient;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String key = buildKey(pjp, rateLimit);
        RRateLimiter limiter = redissonClient.getRateLimiter("rateLimit:" + key);
        limiter.trySetRate(RateType.OVERALL, rateLimit.permits(),
                rateLimit.seconds(), RateIntervalUnit.SECONDS);
        // IP/USER scopes mint one Redis key per distinct caller; Redisson's rate
        // limiter keys never expire on their own, so without a TTL they accumulate
        // unbounded. Refresh a sliding expiry (>= the window) on each call so active
        // callers stay limited while idle keys self-clean. GLOBAL scope is a single
        // fixed key and needs no expiry.
        if (rateLimit.scope() != RateLimitScope.GLOBAL) {
            limiter.expire(Duration.ofSeconds(Math.max(rateLimit.seconds() * 2L, 60L)));
        }
        if (!limiter.tryAcquire(1)) {
            throw new BizException(SecurityErrorCode.RATE_LIMITED);
        }
        return pjp.proceed();
    }

    private String buildKey(ProceedingJoinPoint pjp, RateLimit rateLimit) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        String base = method.getDeclaringClass().getSimpleName() + ":" + method.getName();
        if (!rateLimit.key().isBlank()) {
            base = base + ":" + rateLimit.key();
        }
        return switch (rateLimit.scope()) {
            case IP -> base + ":" + getClientIp();
            case USER -> base + ":user:" + getUserId();
            case GLOBAL -> base;
        };
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                // Trust only the gateway-set X-Real-IP (SecurityHeaderFilter), NOT the
                // client-supplied X-Forwarded-For — otherwise a caller could mint a
                // fresh rate-limit bucket per request by spoofing XFF.
                String realIp = request.getHeader("X-Real-IP");
                if (realIp != null && !realIp.isBlank()) {
                    return realIp.trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    private String getUserId() {
        // Best-effort via Sa-Token if present
        try {
            Class<?> stpUtilClass = Class.forName("cn.dev33.satoken.stp.StpUtil");
            Object id = stpUtilClass.getMethod("getLoginIdDefaultNull").invoke(null);
            return id != null ? id.toString() : "anonymous";
        } catch (Throwable t) {
            return "anonymous";
        }
    }
}
