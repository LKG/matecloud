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
package vip.mate.gateway.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.util.SaTokenConsts;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import vip.mate.base.response.ResponseCode;
import vip.mate.base.result.Result;
import vip.mate.gateway.filter.GatewayTrace;

/**
 * Sa-Token WebFlux (Reactor) filter integration.
 *
 * <h3>Why the Sa-Token filter is offloaded to {@code boundedElastic}</h3>
 * {@link SaReactorFilter#filter} runs its {@code auth} callback
 * <b>synchronously on the calling thread</b> — which on a WebFlux gateway is a
 * Netty event-loop thread. That callback does {@code StpUtil.checkLogin()} /
 * {@code checkRole()}, both of which hit Redis through Sa-Token's <b>blocking</b>
 * {@code sa-token-redis-jackson} DAO (and {@link vip.mate.gateway.auth.StpUserInterfaceImpl},
 * which uses a blocking {@code StringRedisTemplate}). Blocking the handful of
 * event-loop threads means a Redis stall freezes the whole gateway for every
 * request. We therefore wrap the Sa-Token filter so its blocking auth runs on
 * the {@code boundedElastic} scheduler, off the event loop. Sa-Token's own
 * {@code SaReactorSyncHolder} ThreadLocal is set/used/cleared inside the same
 * synchronous {@code delegate.filter(...)} call, so it stays on one elastic
 * thread — no context leakage.
 *
 * @author mateaix
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SaTokenGatewayConfig {

    private final DynamicStrategyFactory dynamicStrategyFactory;
    private final ObjectMapper objectMapper;

    /**
     * Registers the Sa-Token reactor filter wrapped so its blocking auth never
     * runs on a Netty event-loop thread. Ordered at {@link SaTokenConsts#ASSEMBLY_ORDER}
     * (-100) to match Sa-Token's own default precedence, so it still runs first.
     */
    @Bean
    public WebFilter saReactorFilter() {
        return new BoundedElasticSaReactorFilter(buildSaReactorFilter());
    }

    /**
     * Wraps {@link SaReactorFilter} so its synchronous, blocking auth callback is
     * subscribed on {@code boundedElastic} instead of the event loop. Implements
     * {@link Ordered} directly (mirroring Sa-Token's class-level {@code @Order})
     * so the precedence holds no matter how WebFlux collects filter beans.
     */
    private static final class BoundedElasticSaReactorFilter implements WebFilter, Ordered {

        private final SaReactorFilter delegate;

        private BoundedElasticSaReactorFilter(SaReactorFilter delegate) {
            this.delegate = delegate;
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
            // Mono.defer ensures delegate.filter(...) — which runs the blocking auth
            // synchronously — is invoked at subscription time, on a boundedElastic
            // thread rather than the event loop.
            //
            // 这是网关链上最早跑的环节(WebFilter 在 gateway GlobalFilter 之前), sa-token 的
            // beforeAuth / error 回调都在 delegate.filter(...) 内同步执行。此处从 exchange 上
            // WebFlux 已就绪的 active observation 抓真实 traceId 存进 exchange, 并在 delegate.filter
            // 调用期间绑回当前线程 MDC —— 于是 sa-token 那几行回调日志也带上 traceId, 不再是 [,]。
            return Mono.defer(() -> {
                GatewayTrace.capture(exchange);
                try (var ignored = GatewayTrace.scope(exchange)) {
                    return delegate.filter(exchange, chain);
                }
            }).subscribeOn(Schedulers.boundedElastic());
        }

        @Override
        public int getOrder() {
            return SaTokenConsts.ASSEMBLY_ORDER;
        }
    }

    private SaReactorFilter buildSaReactorFilter() {
        SaReactorFilter filter = new SaReactorFilter();

        dynamicStrategyFactory.applyStrategies(filter);

        filter.setError(e -> {
            // Set proper HTTP status codes so frontend axios interceptor
            // can distinguish 401/403 from normal 200 responses
            if (e instanceof NotLoginException) {
                SaHolder.getResponse().setStatus(401);
            } else if (e instanceof NotRoleException || e instanceof NotPermissionException) {
                SaHolder.getResponse().setStatus(403);
            } else {
                SaHolder.getResponse().setStatus(500);
            }
            SaHolder.getResponse()
                    .setHeader("Content-Type", "application/json;charset=UTF-8");

            Result<?> result;
            if (e instanceof NotLoginException) {
                result = Result.fail(ResponseCode.UNAUTHORIZED.getCode(), "Please login first");
                log.warn("[Gateway] Unauthorized access attempt: {}", e.getMessage());
            } else if (e instanceof NotRoleException || e instanceof NotPermissionException) {
                result = Result.fail(ResponseCode.FORBIDDEN.getCode(), "Insufficient permissions");
                log.warn("[Gateway] Forbidden access attempt: {}", e.getMessage());
            } else {
                result = Result.fail(ResponseCode.INTERNAL_ERROR.getCode(), "Gateway authentication error");
                log.error("[Gateway] Authentication error", e);
            }

            try {
                return objectMapper.writeValueAsString(result);
            } catch (Exception ex) {
                log.error("[Gateway] Failed to serialize error response", ex);
                return "{\"code\":\"500\",\"msg\":\"Internal Server Error\",\"data\":null}";
            }
        });

        filter.setBeforeAuth(obj -> {
            log.debug("[Gateway] Incoming request: {} {}", SaHolder.getRequest().getMethod(), SaHolder.getRequest().getRequestPath());
            // Skip CORS preflight requests (OPTIONS) — browser sends these before POST/PUT/DELETE
            if ("OPTIONS".equalsIgnoreCase(SaHolder.getRequest().getMethod())) {
                SaRouter.back();
            }
        });

        return filter;
    }
}
