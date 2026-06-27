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
package vip.mate.starter.security.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import vip.mate.starter.security.desensitize.SensitiveDataUtil;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * AOP aspect that publishes an {@link AuditLogEvent} on every annotated method call.
 *
 * @author mateaix
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class AuditLogAspect {

    private final ApplicationEventPublisher eventPublisher;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String method = sig.getDeclaringType().getSimpleName() + "." + sig.getName();
        String ip = "unknown";
        String url = "";
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                ip = request.getRemoteAddr();
                url = request.getRequestURI();
            }
        } catch (Exception ignored) {
        }

        Object result = null;
        String errorMsg = null;
        boolean success = true;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            success = false;
            errorMsg = t.getMessage();
            throw t;
        } finally {
            long cost = System.currentTimeMillis() - start;
            AuditLogEvent event = AuditLogEvent.builder()
                    .module(auditLog.module())
                    .operation(auditLog.operation())
                    .method(method)
                    .url(url)
                    .params(truncate(SensitiveDataUtil.maskToJson(pjp.getArgs()), 4096))
                    .result(result != null ? truncate(SensitiveDataUtil.maskToJson(result), 4096) : null)
                    .ip(ip)
                    .costTime(cost)
                    .createdAt(LocalDateTime.now())
                    .success(success)
                    .errorMsg(errorMsg)
                    .build();
            try {
                eventPublisher.publishEvent(event);
            } catch (Exception e) {
                log.warn("[audit] Failed to publish event: {}", e.getMessage());
            }
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
