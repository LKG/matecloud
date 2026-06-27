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
package vip.mate.gateway.route;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import vip.mate.gateway.governance.model.TimeoutRule;
import vip.mate.gateway.governance.store.GovernanceRuleStore;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * P2 — Pluggable dynamic routing.
 *
 * <p>Builds gateway routes from Nacos discovery metadata: any registered service that declares a
 * {@code gateway-path} metadata entry (comma-separated path patterns) gets a {@code lb://} route
 * built automatically. A new business service therefore needs <em>no</em> change in mate-gateway —
 * it only declares its path(s) in its own {@code application.yml}:
 *
 * <pre>
 * spring.cloud.nacos.discovery.metadata.gateway-path: /api/v1/foo/**
 * </pre>
 *
 * <p>This is purely additive. Static routes in {@code application.yml} remain as a fallback; dynamic
 * route ids are prefixed with {@code dyn-} so they never collide with the static ids. Per-service
 * errors are isolated (logged + skipped) so a single bad instance can never fail gateway startup or
 * the route refresh.
 *
 * @author mateaix
 */
@Slf4j
@Component
public class DiscoveryMetadataRouteLocator implements RouteDefinitionLocator {

    /** Discovery metadata key a service sets to opt into auto-routing. */
    private static final String METADATA_KEY = "gateway-path";

    /** SCG 按路由级覆盖超时的 metadata 键(NettyRoutingFilter 读取,单位毫秒)。 */
    private static final String RESPONSE_TIMEOUT_KEY = "response-timeout";
    private static final String CONNECT_TIMEOUT_KEY = "connect-timeout";

    private final ReactiveDiscoveryClient discoveryClient;
    private final GovernanceRuleStore ruleStore;

    /**
     * serviceId → 上次已记录的路由签名。仅当路由<b>首次出现或发生变化</b>时打一条 INFO ——
     * 否则每 30s 的定时刷新(且 {@link #getRouteDefinitions()} 的 Flux 每次刷新被多次订阅)
     * 会把每个服务的路由在稳态下反复刷屏, 产生大量无谓日志。新服务上线 / 路径或超时变化仍会记录一次。
     */
    private final Map<String, String> loggedRoutes = new ConcurrentHashMap<>();

    public DiscoveryMetadataRouteLocator(ReactiveDiscoveryClient discoveryClient, GovernanceRuleStore ruleStore) {
        this.discoveryClient = discoveryClient;
        this.ruleStore = ruleStore;
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return discoveryClient.getServices()
                // For each serviceId, look at its first instance's metadata.
                .flatMap(serviceId -> discoveryClient.getInstances(serviceId)
                        .next()
                        .mapNotNull(instance -> {
                            String paths = instance.getMetadata() == null
                                    ? null
                                    : instance.getMetadata().get(METADATA_KEY);
                            if (paths == null || paths.isBlank()) {
                                // No opt-in metadata (e.g. the gateway itself) — skip.
                                return null;
                            }
                            return buildRoute(serviceId, paths.trim());
                        })
                        // A single failing/empty instance must never break the whole stream.
                        .onErrorResume(ex -> {
                            log.warn("[gateway] skip dynamic route for {} due to error: {}",
                                    serviceId, ex.toString());
                            return Flux.<RouteDefinition>empty().next();
                        }))
                .onErrorContinue((ex, obj) ->
                        log.warn("[gateway] dynamic route discovery error, continuing: {}", ex.toString()));
    }

    /**
     * Build a single {@link RouteDefinition} for {@code serviceId} from a comma-separated list of
     * path patterns. The {@code Path} predicate args use the {@code _genkey_N} convention so multiple
     * patterns map to a single predicate (same as Spring Cloud Gateway's own shortcut parsing).
     */
    private RouteDefinition buildRoute(String serviceId, String paths) {
        RouteDefinition route = new RouteDefinition();
        route.setId("dyn-" + serviceId);
        route.setUri(URI.create("lb://" + serviceId));
        // 动态路由优先级高于 application.yml 里 order=0 的静态兜底路由:同 path 同时命中时,
        // 携带超时 metadata 的动态路由胜出,避免超时治理被静态路由旁路。
        route.setOrder(-1);

        PredicateDefinition path = new PredicateDefinition();
        path.setName("Path");
        String[] patterns = paths.split(",");
        int index = 0;
        for (String pattern : patterns) {
            String p = pattern.trim();
            if (!p.isEmpty()) {
                path.addArg("_genkey_" + index++, p);
            }
        }
        route.setPredicates(List.of(path));

        // 超时治理:该服务若配了启用的超时规则,写入路由级 metadata,SCG 据此对本路由覆盖全局默认。
        TimeoutRule timeout = ruleStore.timeout(serviceId);
        String descriptor;
        if (timeout.enabled()) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put(RESPONSE_TIMEOUT_KEY, timeout.responseTimeoutMs());
            metadata.put(CONNECT_TIMEOUT_KEY, timeout.connectTimeoutMs());
            route.setMetadata(metadata);
            descriptor = paths + " (timeout connect=" + timeout.connectTimeoutMs()
                    + "ms response=" + timeout.responseTimeoutMs() + "ms)";
        } else {
            descriptor = paths;
        }
        // 仅在首次出现或路由签名变化时记录, 稳态刷新静默(见 loggedRoutes)。
        if (!descriptor.equals(loggedRoutes.put(serviceId, descriptor))) {
            log.info("[gateway] dynamic route for {} -> {}", serviceId, descriptor);
        }
        return route;
    }
}
