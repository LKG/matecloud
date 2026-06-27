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

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;

/**
 * Counts real API traffic so the admin workbench's "API 调用" trend reflects
 * actual gateway throughput — not the operation-log table it used to read,
 * which only records {@code @OperationLog}-annotated admin mutations and so
 * was near-empty and mislabeled.
 *
 * <p>The gateway is the single choke point every {@code /api/**} request passes
 * through, making it the only place a platform-wide call count is meaningful.
 * Each request bumps a per-day Redis counter ({@code mate:metrics:api:yyyy-MM-dd});
 * {@code mate-system}'s dashboard reads the last 7 keys back (same Redis db 0,
 * shared via mate-cache-starter). Counters self-expire after 8 days.
 *
 * <p>Metrics must never affect routing: the increment is fire-and-forget on
 * Redisson's own threads (async, not chained into the request Mono), and any
 * failure is swallowed. CORS preflight (OPTIONS) and non-{@code /api} paths
 * (actuator probes, static proxying) are not counted.
 *
 * @author mateaix
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mate.monitor.api-metrics.enabled", matchIfMissing = true)
public class ApiCallMetricsFilter implements GlobalFilter, Ordered {

    /** Shared with mate-system's dashboard reader — keep both sides in sync. */
    public static final String KEY_PREFIX = "mate:metrics:api:";
    private static final Duration TTL = Duration.ofDays(8);

    private final RedissonClient redisson;

    public ApiCallMetricsFilter(RedissonClient redisson) {
        this.redisson = redisson;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/api/") && !HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            bump();
        }
        return chain.filter(exchange);
    }

    private void bump() {
        try {
            RAtomicLong counter = redisson.getAtomicLong(KEY_PREFIX + LocalDate.now());
            counter.incrementAndGetAsync().whenComplete((value, ex) -> {
                // Set the TTL only on the day's first hit so the key prunes itself.
                if (ex == null && value != null && value == 1L) {
                    counter.expireAsync(TTL);
                }
            });
        } catch (Exception e) {
            // Counting is best-effort — never let it break the request path.
            if (log.isDebugEnabled()) {
                log.debug("[metrics] api-call counter increment skipped: {}", e.getMessage());
            }
        }
    }

    @Override
    public int getOrder() {
        // Count every incoming API request, before auth/rate-limit can short-circuit it.
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
