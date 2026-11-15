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
package vip.mate.monolith.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vip.mate.base.constant.AuthHeaders;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Monolith replacement for the gateway's {@code HeaderRelayFilter} +
 * {@code SecurityHeaderFilter} pair — in monolith mode this JVM IS the edge,
 * so it must relay the authenticated identity downstream in exactly the same
 * headers the browser-facing gateway would.
 *
 * <p>Without this filter, service-scoped guards that trust only the relayed
 * identity headers ({@code NoticeAuthInterceptor} on {@code /api/v1/notice/**},
 * {@code AiAuthInterceptor} on {@code /api/v1/ai/**}) never see an
 * {@code X-User-Id} — no browser sends one — and 401 every request even with a
 * perfectly valid Sa-Token session. The frontend axios layer then treats
 * "401 survives a silent token refresh" as a dead session and logs the user
 * out (the message-center bounce of 2026-11-16).
 *
 * <p>Two responsibilities, mirroring the gateway:</p>
 * <ul>
 *   <li><b>Relay</b>: resolve the bearer token via a stateless session-store
 *       lookup ({@link StpUtil#getLoginIdByToken(String)}, same call the
 *       gateway makes) and inject the {@link AuthHeaders#ALL} identity set
 *       from the Sa-Token session;</li>
 *   <li><b>Strip</b>: identity headers are only ever trusted from this filter,
 *       so any {@link AuthHeaders#ALL} / {@link AuthHeaders#INTERNAL} header
 *       a client tries to smuggle in is removed when no valid token backs it
 *       (the gateway's inbound-strip guarantee, which no other monolith
 *       component provides).</li>
 * </ul>
 *
 * <p>No HMAC signing here: {@code mate.gateway.internal.signature-required=false}
 * in monolith mode (see mate-infra-local.yml) — the single-JVM, single-entry
 * attack surface the signature defends against does not exist.</p>
 *
 * @author mateaix
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnProperty(name = "mate.rpc.mode", havingValue = "local")
public class MonolithHeaderRelayFilter extends OncePerRequestFilter {

    /** Header names this filter owns: relayed identity + gateway-internal ones. */
    private static final List<String> TRUSTED_HEADERS =
            Stream.concat(AuthHeaders.ALL.stream(), AuthHeaders.INTERNAL.stream()).toList();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Map<String, String> identity = resolveIdentity(request);
        if (identity != null) {
            chain.doFilter(new IdentityHeaderWrapper(request, identity), response);
        } else if (hasTrustedHeader(request)) {
            // Forged/relayed headers without a valid token behind them — strip.
            chain.doFilter(new IdentityHeaderWrapper(request, Map.of()), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * Builds the relay header set for the request's bearer token, or
     * {@code null} when the token is absent / expired / invalid.
     */
    private Map<String, String> resolveIdentity(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return null;
        }
        Object loginId;
        try {
            loginId = StpUtil.getLoginIdByToken(token);
        } catch (Exception e) {
            log.trace("[monolith-relay] token resolution failed: {}", e.getMessage());
            return null;
        }
        if (loginId == null) {
            return null;
        }
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(AuthHeaders.USER_ID, loginId.toString());
        try {
            var session = StpUtil.getSessionByLoginId(loginId);
            putIfPresent(headers, AuthHeaders.USER_NAME, session.get("username"));
            putIfPresent(headers, AuthHeaders.TENANT_ID, session.get("tenantId"));
            putIfPresent(headers, AuthHeaders.DEPT_ID, session.get("deptId"));
            putIfPresent(headers, AuthHeaders.WORKSPACE_ID, session.get("workspaceId"));
            putIfPresent(headers, AuthHeaders.ROLES, session.get("roles"));
            putIfPresent(headers, AuthHeaders.DATA_SCOPE, session.get("dataScope"));
        } catch (Exception sessionEx) {
            // Session may not exist yet (e.g. right at login time) — X-User-Id suffices.
            log.trace("[monolith-relay] session lookup failed for loginId={}: {}",
                    loginId, sessionEx.getMessage());
        }
        return headers;
    }

    private static String extractToken(HttpServletRequest request) {
        String raw = request.getHeader("Authorization");
        if (raw == null || raw.isBlank()) {
            // sa-token default: token-name header, then same-named parameter
            raw = request.getHeader(StpUtil.getTokenName());
        }
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String prefix = SaManager.getConfig().getTokenPrefix();
        if (prefix != null && !prefix.isBlank() && raw.startsWith(prefix)) {
            raw = raw.substring(prefix.length());
        }
        return raw.isBlank() ? null : raw.trim();
    }

    /** roles is stored as a collection in the session — comma-join like the gateway. */
    private static void putIfPresent(Map<String, String> headers, String name, Object value) {
        if (value == null) {
            return;
        }
        String text = value instanceof Iterable<?> it
                ? String.join(",", StreamSupport.stream(it.spliterator(), false)
                        .map(String::valueOf).toList())
                : value.toString();
        if (!text.isBlank()) {
            headers.put(name, text);
        }
    }

    private static boolean hasTrustedHeader(HttpServletRequest request) {
        return TRUSTED_HEADERS.stream().anyMatch(h -> request.getHeader(h) != null);
    }

    /**
     * Overrides ONLY the trusted identity headers (inject or strip, per the
     * supplied map) and passes every other header through untouched.
     */
    private static final class IdentityHeaderWrapper extends HttpServletRequestWrapper {

        private final Map<String, String> overrides;

        IdentityHeaderWrapper(HttpServletRequest request, Map<String, String> overrides) {
            super(request);
            this.overrides = overrides;
        }

        @Override
        public String getHeader(String name) {
            if (isTrusted(name)) {
                return overrides.get(name);
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (isTrusted(name)) {
                String value = overrides.get(name);
                return value == null ? Collections.emptyEnumeration()
                        : Collections.enumeration(List.of(value));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = new java.util.ArrayList<>(Collections.list(super.getHeaderNames()));
            names.removeAll(TRUSTED_HEADERS);
            names.addAll(overrides.keySet());
            return Collections.enumeration(names);
        }

        private static boolean isTrusted(String name) {
            return TRUSTED_HEADERS.stream().anyMatch(t -> t.equalsIgnoreCase(name));
        }
    }
}
