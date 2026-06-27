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
package vip.mate.gateway.governance.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.redisson.api.RRateLimiterReactive;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import vip.mate.base.response.ResponseCode;
import vip.mate.base.result.Result;
import vip.mate.gateway.governance.model.RateLimitRule;
import vip.mate.gateway.governance.store.GovernanceRuleStore;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 全局限流 filter,基于 Redisson 分布式令牌桶({@link RRateLimiterReactive},全程非阻塞)。
 *
 * <p>对每个请求解析目标服务,读 {@link GovernanceRuleStore} 中该服务的 {@link RateLimitRule};启用则按
 * 配置维度(IP/USER/PATH/SERVICE)取一个限流键尝试获取令牌,失败返回 429。</p>
 *
 * <p>状态生命周期(防无限增长):限流键内嵌 {@code rate:interval},规则改动后自然切换到新桶;每个桶在 Redis
 * 上挂 {@link #REDIS_TTL} 过期,空闲后自动回收;本地用有界 Caffeine({@link #initialized},maxSize + TTL)
 * 记录「已初始化过的桶」以跳过重复 {@code trySetRate},既不阻塞热路径又不会让 JVM set 无限膨胀。</p>
 *
 * <p>IP 维度防伪造:默认只信网关直连 {@code remoteAddress},完全忽略客户端可伪造的 {@code X-Forwarded-For};
 * 仅当部署在可信反代后(配置 {@code mate.gateway.ratelimit.trusted-hops=N})才从 XFF 右起第 N 跳取真实客户端 IP,
 * 攻击者无法越过可信代理伪造该位。</p>
 *
 * @author mateaix
 */
@Slf4j
@Component
public class RateLimitGlobalFilter implements GlobalFilter, Ordered {

    private static final String KEY_PREFIX = "mate:gw:rl:";
    /** 限流桶在 Redis 的存活时长;须大于 {@link #initialized} 的 TTL,保证活跃桶在本地标记过期前被续命。 */
    private static final Duration REDIS_TTL = Duration.ofMinutes(15);
    private static final Duration LOCAL_TTL = Duration.ofMinutes(10);
    private static final long LOCAL_MAX_SIZE = 100_000L;

    private final RedissonClient redisson;
    private final GovernanceRuleStore ruleStore;
    private final ObjectMapper objectMapper;

    /** 可信反代跳数;0 = 不信任任何 XFF,只用直连地址(默认,防伪造)。 */
    private final int trustedHops;

    /** 有界本地标记:记录已在 Redis 初始化过 rate 的桶名,避免每请求重复 trySetRate。 */
    private final Cache<String, Boolean> initialized = Caffeine.newBuilder()
            .maximumSize(LOCAL_MAX_SIZE)
            .expireAfterWrite(LOCAL_TTL)
            .build();

    public RateLimitGlobalFilter(RedissonClient redisson,
                                 GovernanceRuleStore ruleStore,
                                 ObjectMapper objectMapper,
                                 @Value("${mate.gateway.ratelimit.trusted-hops:0}") int trustedHops) {
        this.redisson = redisson;
        this.ruleStore = ruleStore;
        this.objectMapper = objectMapper;
        this.trustedHops = Math.max(0, trustedHops);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String service = targetService(exchange);
        if (service == null) {
            return chain.filter(exchange);
        }
        RateLimitRule rule = ruleStore.rateLimit(service);
        if (!rule.enabled()) {
            return chain.filter(exchange);
        }

        String name = KEY_PREFIX + service + ':' + rule.key() + ':' + rule.rate() + ':' + rule.interval()
                + ':' + dimensionValue(exchange, rule);
        RRateLimiterReactive limiter = redisson.reactive().getRateLimiter(name);

        Mono<Void> ensureRate = initialized.getIfPresent(name) != null
                ? Mono.empty()
                : limiter.trySetRate(RateType.OVERALL, rule.rate(), Duration.ofSeconds(rule.interval()))
                        // 给桶挂 TTL,空闲后自动回收,防止 Redis key 随高基数维度无限增长。
                        .then(limiter.expire(REDIS_TTL))
                        .doOnSuccess(ok -> initialized.put(name, Boolean.TRUE))
                        .then();

        return ensureRate
                .then(limiter.tryAcquire(1))
                .flatMap(allowed -> Boolean.TRUE.equals(allowed)
                        ? chain.filter(exchange)
                        : reject(exchange, service))
                // 限流组件异常绝不能拖垮请求,降级为放行。
                .onErrorResume(ex -> {
                    log.warn("[gateway-ratelimit] limiter error for {}, passing through: {}", service, ex.toString());
                    return chain.filter(exchange);
                });
    }

    private Mono<Void> reject(ServerWebExchange exchange, String service) {
        log.debug("[gateway-ratelimit] 429 for service {}", service);
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Result<?> body = Result.fail(ResponseCode.TOO_MANY_REQUESTS.getCode(), "Rate limit exceeded: " + service);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = ("{\"code\":\"429\",\"msg\":\"Rate limit exceeded\",\"data\":null}").getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /** 取目标服务名:lb:// 路由的 host 即 serviceId。 */
    private String targetService(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route == null || route.getUri() == null) {
            return null;
        }
        return route.getUri().getHost();
    }

    private String dimensionValue(ServerWebExchange exchange, RateLimitRule rule) {
        ServerHttpRequest req = exchange.getRequest();
        return switch (rule.key()) {
            case IP -> clientIp(exchange);
            case USER -> {
                String token = req.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                yield (token == null || token.isBlank()) ? clientIp(exchange) : Integer.toHexString(token.hashCode());
            }
            case PATH -> req.getPath().value();
            case SERVICE -> "all";
        };
    }

    /**
     * 解析真实客户端 IP。默认(trustedHops=0)只信直连地址,杜绝 {@code X-Forwarded-For} 伪造;配置可信反代跳数后,
     * 从 XFF 右起第 N 跳取值(攻击者注入的伪造项只会出现在更左侧,取不到)。
     */
    private String clientIp(ServerWebExchange exchange) {
        ServerHttpRequest req = exchange.getRequest();
        if (trustedHops > 0) {
            List<String> forwarded = req.getHeaders().get("X-Forwarded-For");
            if (forwarded != null && !forwarded.isEmpty()) {
                List<String> ips = new ArrayList<>();
                for (String header : forwarded) {
                    for (String part : header.split(",")) {
                        String ip = part.trim();
                        if (!ip.isEmpty()) {
                            ips.add(ip);
                        }
                    }
                }
                int idx = ips.size() - trustedHops;
                if (idx >= 0 && idx < ips.size()) {
                    return ips.get(idx);
                }
            }
        }
        InetSocketAddress remote = req.getRemoteAddress();
        return (remote == null || remote.getAddress() == null) ? "unknown" : remote.getAddress().getHostAddress();
    }

    @Override
    public int getOrder() {
        // 早于转发,晚于路由匹配(路由属性由 handler mapping 预置)。
        return HIGHEST_PRECEDENCE + 1000;
    }
}
