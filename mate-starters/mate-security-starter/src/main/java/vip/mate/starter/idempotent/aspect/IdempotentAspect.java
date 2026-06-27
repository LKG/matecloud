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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vip.mate.base.exception.BizException;
import vip.mate.starter.idempotent.annotation.Idempotent;
import vip.mate.starter.idempotent.config.IdempotentProperties;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Objects;

/**
 * AOP aspect enforcing idempotency via Redis setIfAbsent.
 *
 * @author mateaix
 */
@Slf4j
@Aspect
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
            bucket.delete();
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

    private String resolveTokenKey() {
        HttpServletRequest request = getCurrentRequest();
        String token = request.getHeader("X-Idempotent-Token");
        if (token == null || token.isBlank()) {
            throw new BizException("IDEMPOTENT_TOKEN_MISSING", "Missing X-Idempotent-Token header");
        }
        String tokenRedisKey = properties.getKeyPrefix() + "token:" + token;
        RBucket<String> tokenBucket = redissonClient.getBucket(tokenRedisKey);
        String val = tokenBucket.getAndDelete();
        if (val == null) {
            throw new BizException("IDEMPOTENT_TOKEN_INVALID", "Invalid or expired token");
        }
        return "token:" + token;
    }

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
            throw new BizException("IDEMPOTENT_ERROR", "No HTTP request context");
        }
        return attrs.getRequest();
    }
}
