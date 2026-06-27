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
package vip.mate.auth.infrastructure.adapter.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vip.mate.auth.domain.event.UserLoggedInEvent;
import vip.mate.auth.domain.event.UserLoginFailedEvent;
import vip.mate.auth.infrastructure.dao.LoginLogDao;
import vip.mate.auth.infrastructure.dao.po.LoginLogPO;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Persists login events to {@code mate_login_log}.
 *
 * <p>Why a {@link EventListener} (and not just a direct DAO call from
 * {@code AbstractLoginStrategy})? Two reasons:
 * <ul>
 *   <li>Decoupling — the auth pipeline doesn't know which sinks consume the
 *       event. Tomorrow we can add a Prometheus counter listener, an MQ
 *       publisher, or both, without touching the strategy code.</li>
 *   <li>Failure isolation — if the database hiccups, login itself still
 *       succeeds; the audit row just isn't written.</li>
 * </ul>
 *
 * <p>Client IP / user-agent aren't carried on the event payload (events are
 * emitted from the application layer, which doesn't see HttpServletRequest).
 * We pull them via {@link RequestContextHolder} here, which works because the
 * listener runs synchronously on the same request thread. If we ever switch
 * to {@code @Async} (recommended for prod under load) the request context
 * needs to be propagated explicitly via {@code RequestContextFilter}.
 *
 * @author mateaix
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginLogPersistenceListener {

    private final LoginLogDao loginLogDao;

    @EventListener
    public void onSuccess(UserLoggedInEvent event) {
        try {
            LoginLogPO po = baseFrom(event.username(),
                    event.loginType() == null ? "PASSWORD" : event.loginType().name());
            po.setStatus(0);
            po.setCreatedAt(toLocalDateTime(event.at()));
            loginLogDao.insert(po);
        } catch (Exception e) {
            // Audit failures must never break the login flow.
            log.warn("[audit] Failed to persist login-success log: {}", e.getMessage());
        }
    }

    @EventListener
    public void onFailure(UserLoginFailedEvent event) {
        try {
            LoginLogPO po = baseFrom(event.account(),
                    event.loginType() == null ? "PASSWORD" : event.loginType().name());
            po.setStatus(1);
            po.setFailMsg(safeTrunc(event.reason(), 256));
            po.setCreatedAt(toLocalDateTime(event.at()));
            loginLogDao.insert(po);
        } catch (Exception e) {
            log.warn("[audit] Failed to persist login-failure log: {}", e.getMessage());
        }
    }

    /**
     * Common PO construction: pulls IP + User-Agent from the current servlet
     * request when one is bound to this thread. When the event is published
     * outside an HTTP context (e.g. an admin-triggered impersonation in the
     * future) both fields stay null — the column allows it.
     */
    private LoginLogPO baseFrom(String username, String loginType) {
        LoginLogPO po = new LoginLogPO();
        po.setUsername(username);
        po.setLoginType(loginType);
        HttpServletRequest req = currentRequest();
        if (req != null) {
            po.setClientIp(extractClientIp(req));
            po.setUserAgent(safeTrunc(req.getHeader("User-Agent"), 512));
        }
        return po;
    }

    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        return attrs instanceof ServletRequestAttributes sra ? sra.getRequest() : null;
    }

    /**
     * Walk forwarded-for headers in priority order so a request through the
     * gateway / load balancer still attributes the real client IP. Falls back
     * to the socket peer when no proxy header is present.
     */
    private String extractClientIp(HttpServletRequest req) {
        for (String h : new String[]{"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP"}) {
            String v = req.getHeader(h);
            if (v != null && !v.isBlank() && !"unknown".equalsIgnoreCase(v)) {
                // X-Forwarded-For can be a comma-separated chain; take the first.
                int comma = v.indexOf(',');
                return safeTrunc(comma > 0 ? v.substring(0, comma).trim() : v.trim(), 64);
            }
        }
        return safeTrunc(req.getRemoteAddr(), 64);
    }

    private static String safeTrunc(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null
                ? LocalDateTime.now()
                : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
