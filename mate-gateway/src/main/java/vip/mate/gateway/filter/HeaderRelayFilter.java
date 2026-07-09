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
package vip.mate.gateway.filter;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import vip.mate.base.constant.AuthHeaders;
import vip.mate.base.security.GatewaySignature;

import java.util.stream.StreamSupport;

/**
 * Relays the authenticated user/tenant context to downstream services as
 * plaintext headers. Downstream services must NEVER trust these headers if
 * they didn't come from the gateway — the entry path is locked by network
 * policy + each service's own X-User-Id check (see
 * {@code AiAuthInterceptor}, {@code NoticeAuthInterceptor}).
 *
 * <h3>Why we read the token directly instead of {@code StpUtil.isLogin()}</h3>
 * Sa-Token's reactive integration ({@code SaReactorFilter}) propagates the
 * login context via Reactor's {@code Context} — i.e. it's only visible while
 * we're inside a reactive operator chain that subscribed with that context.
 * In a Spring Cloud Gateway {@link GlobalFilter#filter} the synchronous
 * prelude runs OUTSIDE that context, so {@code StpUtil.isLogin()} returns
 * {@code false} and {@code X-User-Id} is silently dropped. The result is
 * services like mate-ai (which require {@code X-User-Id}) get 401 even
 * though Sa-Token already validated the token upstream.
 *
 * <p>{@code StpUtil.getLoginIdByToken(tokenValue)} is a stateless lookup
 * against Sa-Token's session store (Redis here, since we use
 * {@code sa-token-redis-jackson}) and works regardless of reactive context.
 * That's what we use below.
 *
 * @author mateaix
 */
@Slf4j
@Component
public class HeaderRelayFilter implements GlobalFilter, Ordered {

    private static final String HEADER_USER_ID = AuthHeaders.USER_ID;
    private static final String HEADER_USER_NAME = AuthHeaders.USER_NAME;
    private static final String HEADER_TENANT_ID = AuthHeaders.TENANT_ID;

    /** Matches sa-token.token-prefix in mate-defaults.yml. */
    private static final String TOKEN_PREFIX = "Bearer ";
    /** Matches sa-token.token-name in mate-defaults.yml. */
    private static final String TOKEN_HEADER = HttpHeaders.AUTHORIZATION;

    /** dev 默认值 —— 生产必须覆盖, 否则签名密钥公开等于没签。 */
    private static final String DEV_DEFAULT_SECRET =
            "dev-only-gateway-internal-secret-change-in-prod-min-32";

    /** 网关↔下游共享签名密钥。空则拒绝启动 (fail-fast)。 */
    private final String internalSecret;

    public HeaderRelayFilter(@Value("${mate.gateway.internal.secret:}") String internalSecret) {
        if (internalSecret == null || internalSecret.isBlank()) {
            throw new IllegalStateException(
                    "mate.gateway.internal.secret 未配置 —— 网关无法为下游请求签名, 拒绝启动 "
                    + "(通过环境变量 MATE_GATEWAY_INTERNAL_SECRET 设置强随机值)");
        }
        if (DEV_DEFAULT_SECRET.equals(internalSecret)) {
            log.warn("[Gateway][security] mate.gateway.internal.secret 仍为 dev 默认值, "
                    + "生产环境必须通过 MATE_GATEWAY_INTERNAL_SECRET 覆盖为强随机值 (>=32 字节)");
        }
        this.internalSecret = internalSecret;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = extractToken(exchange);
        if (token == null) {
            return chain.filter(exchange);
        }
        // The token/session lookups below hit Sa-Token's BLOCKING Redis DAO.
        // Run them on boundedElastic so they never block a Netty event-loop thread.
        return Mono.fromCallable(() -> buildRelayedRequest(exchange, token))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(request -> chain.filter(exchange.mutate().request(request).build()));
    }

    /**
     * Resolves the login context for {@code token} and returns a request carrying
     * the relayed auth headers. Always returns a (possibly header-free) request —
     * never null — so the chain proceeds even when the token is expired/invalid.
     */
    private ServerHttpRequest buildRelayedRequest(ServerWebExchange exchange, String token) {
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();
        // 整段跑在 boundedElastic 上, MDC 不带 traceId —— 用本请求抓到的 id 包住所有日志行(含 catch)。
        try (var ignored = GatewayTrace.scope(exchange)) {
            try {
                Object loginId = StpUtil.getLoginIdByToken(token);
                if (loginId != null) {
                    requestBuilder.header(HEADER_USER_ID, loginId.toString());
                    log.debug("[Gateway] Relaying X-User-Id={} for path={}",
                            loginId, exchange.getRequest().getPath().value());

                    // Optional context from session (stateless Redis lookup keyed by loginId).
                    // Header names come from AuthHeaders — the single source shared with
                    // SecurityHeaderFilter (inbound strip) and downstream resolvers.
                    try {
                        var session = StpUtil.getSessionByLoginId(loginId);
                        relay(requestBuilder, HEADER_USER_NAME, session.get("username"));
                        relay(requestBuilder, HEADER_TENANT_ID, session.get("tenantId"));
                        relay(requestBuilder, AuthHeaders.DEPT_ID, session.get("deptId"));
                        relay(requestBuilder, AuthHeaders.WORKSPACE_ID, session.get("workspaceId"));
                        relay(requestBuilder, AuthHeaders.ROLES, session.get("roles"));
                        relay(requestBuilder, AuthHeaders.DATA_SCOPE, session.get("dataScope"));
                    } catch (Exception sessionEx) {
                        // Session may not exist yet (e.g. on first login response) — non-fatal.
                        log.trace("[Gateway] Session lookup failed for loginId={}: {}",
                                loginId, sessionEx.getMessage());
                    }
                } else {
                    log.trace("[Gateway] Token present but no loginId resolved (expired/invalid)");
                }
            } catch (Exception e) {
                log.trace("[Gateway] Token resolution failed: {}", e.getMessage());
            }
        }
        ServerHttpRequest relayed = requestBuilder.build();
        // 对整套身份头 + 时间戳做 HMAC 签名, 供下游验证「请求确实来自网关」(防绕过网关直连
        // 服务端口伪造 X-User-Id)。仅在已注入身份 (X-User-Id) 时签名; 公开/未登录路径不签,
        // 由下游各自策略处理。
        if (relayed.getHeaders().getFirst(AuthHeaders.USER_ID) != null) {
            long ts = System.currentTimeMillis();
            String sign = GatewaySignature.sign(internalSecret,
                    name -> relayed.getHeaders().getFirst(name), ts);
            return relayed.mutate()
                    .header(AuthHeaders.GATEWAY_TS, Long.toString(ts))
                    .header(AuthHeaders.GATEWAY_SIGN, sign)
                    .build();
        }
        return relayed;
    }

    /** roles 为集合时拼逗号串; null/空 不注入。 */
    private static void relay(ServerHttpRequest.Builder builder, String header, Object value) {
        if (value == null) {
            return;
        }
        String text = value instanceof Iterable<?> it
                ? String.join(",", StreamSupport.stream(it.spliterator(), false)
                        .map(String::valueOf).toList())
                : value.toString();
        if (!text.isBlank()) {
            builder.header(header, text);
        }
    }

    private String extractToken(ServerWebExchange exchange) {
        String raw = exchange.getRequest().getHeaders().getFirst(TOKEN_HEADER);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.startsWith(TOKEN_PREFIX) ? raw.substring(TOKEN_PREFIX.length()) : raw;
    }

    @Override
    public int getOrder() {
        // Must run AFTER SaReactorFilter so token validation has already happened.
        // Sa-Token reactor filter defaults to HIGHEST_PRECEDENCE + 10; we sit comfortably after.
        return -90;
    }
}
