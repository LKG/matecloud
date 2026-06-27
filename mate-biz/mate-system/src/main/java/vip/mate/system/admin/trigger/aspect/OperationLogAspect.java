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
package vip.mate.system.admin.trigger.aspect;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import vip.mate.base.constant.AuthHeaders;
import vip.mate.starter.security.desensitize.SensitiveDataUtil;
import vip.mate.system.admin.infrastructure.dao.po.OperationLogPO;
import vip.mate.system.admin.trigger.annotation.OperationLog;
import vip.mate.system.admin.trigger.event.OperationLogEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * AOP aspect for {@link OperationLog}-annotated controller methods.
 *
 * <p>Captures request metadata synchronously, then publishes an
 * {@link OperationLogEvent} for async persistence — the DB insert
 * never blocks the request thread.
 *
 * <p>User identity resolution: prefers gateway-forwarded headers
 * ({@code X-User-Id}, {@code X-User-Name}); falls back to Sa-Token
 * session when running without gateway (dev / standalone).
 *
 * <p>Sensitive/non-serialisable arguments (ServletRequest, MultipartFile,
 * BindingResult, etc.) are filtered out before JSON serialisation.
 *
 * @author mateaix
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final ApplicationEventPublisher eventPublisher;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog opLog) throws Throwable {
        long start = System.currentTimeMillis();
        OperationLogPO logPO = new OperationLogPO();
        logPO.setModule(opLog.module());
        logPO.setOperationType(opLog.type());
        logPO.setDescription(opLog.description());

        // ---- request metadata ----
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            logPO.setRequestMethod(request.getMethod());
            logPO.setRequestUrl(request.getRequestURI());
            logPO.setClientIp(getClientIp(request));
            logPO.setUserAgent(truncate(request.getHeader("User-Agent"), 500));

            // User identity: gateway headers first, then Sa-Token
            String userId = request.getHeader(AuthHeaders.USER_ID);
            String username = request.getHeader(AuthHeaders.USER_NAME);
            if (userId == null || userId.isBlank()) {
                try {
                    Object loginId = StpUtil.getLoginIdDefaultNull();
                    if (loginId != null) {
                        userId = loginId.toString();
                        Object sessionName = StpUtil.getSession().get("username");
                        if (sessionName != null) username = sessionName.toString();
                    }
                } catch (Exception ignored) {
                    // Not logged in or Sa-Token unavailable
                }
            }
            logPO.setUserId(userId);
            logPO.setUsername(username);
        }

        // ---- serialise safe arguments (sensitive fields masked) ----
        try {
            Object[] safeArgs = filterArgs(joinPoint.getArgs());
            String params = SensitiveDataUtil.maskToJson(safeArgs);
            logPO.setRequestParams(truncate(params, 2000));
        } catch (Exception e) {
            logPO.setRequestParams("[serialization error]");
        }

        // ---- execute target ----
        Object result;
        try {
            result = joinPoint.proceed();
            logPO.setStatus(0);
            try {
                String resultStr = SensitiveDataUtil.maskToJson(result);
                logPO.setResponseResult(truncate(resultStr, 2000));
            } catch (Exception ignored) {}
        } catch (Throwable ex) {
            logPO.setStatus(1);
            logPO.setErrorMsg(truncate(ex.getMessage(), 500));
            throw ex;
        } finally {
            logPO.setDuration(System.currentTimeMillis() - start);
            // Publish async — listener persists in a separate thread
            try {
                eventPublisher.publishEvent(new OperationLogEvent(logPO));
            } catch (Exception e) {
                log.error("[OperationLog] Failed to publish operation log event", e);
            }
        }
        return result;
    }

    /**
     * Filter out non-serialisable / sensitive argument types.
     */
    private Object[] filterArgs(Object[] args) {
        if (args == null || args.length == 0) return args;
        List<Object> safe = new ArrayList<>(args.length);
        for (Object arg : args) {
            if (arg == null) {
                safe.add(null);
            } else if (arg instanceof ServletRequest
                    || arg instanceof ServletResponse
                    || arg instanceof MultipartFile
                    || arg instanceof BindingResult) {
                safe.add("[filtered:" + arg.getClass().getSimpleName() + "]");
            } else {
                safe.add(arg);
            }
        }
        return safe.toArray();
    }

    private String getClientIp(HttpServletRequest request) {
        // Trust only the gateway-set X-Real-IP (SecurityHeaderFilter), not the
        // client-supplied X-Forwarded-For, so audit IPs can't be forged.
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }

    private String truncate(String str, int maxLen) {
        return str != null && str.length() > maxLen ? str.substring(0, maxLen) : str;
    }
}
