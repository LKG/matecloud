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
package vip.mate.gateway.governance.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import vip.mate.base.result.Result;
import vip.mate.gateway.governance.model.GrayRule;
import vip.mate.gateway.governance.model.RateLimitRule;
import vip.mate.gateway.governance.model.RouteMode;
import vip.mate.gateway.governance.model.TimeoutRule;
import vip.mate.gateway.governance.store.GovernanceRuleStore;
import vip.mate.gateway.governance.web.dto.GovernanceDtos.InstanceView;
import vip.mate.gateway.governance.web.dto.GovernanceDtos.RouteTestRequest;
import vip.mate.gateway.governance.web.dto.GovernanceDtos.RouteTestResult;
import vip.mate.gateway.governance.web.dto.GovernanceDtos.ServiceView;
import vip.mate.gateway.governance.web.dto.GovernanceDtos.TimeoutView;
import vip.mate.gateway.governance.web.dto.GovernanceDtos.VersionStat;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网关治理控制台后端,直接挂在网关本地({@code /api/v1/gateway/**})—— 只有网关进程持有实时的服务发现、
 * 路由表与负载均衡状态,故管理面不外置到其它服务。
 *
 * <p>该前缀在 {@code application.yml} 的 {@code admin-paths} 中,经 Sa-Token 限定 {@code ROLE_ADMIN}。
 * 由于没有任何 {@code lb://} 路由匹配此前缀,请求会落到本 {@code @RestController} 而非被转发。</p>
 *
 * <p>读接口只碰 {@link GovernanceRuleStore} 内存快照与服务发现(非阻塞);写接口的 Redisson 持久化调用
 * 统一调度到 {@code boundedElastic},不阻塞事件循环。</p>
 *
 * @author mateaix
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/gateway")
public class GatewayGovernanceController {

    private static final String METADATA_VERSION = "version";
    /** 网关自身的服务名,治理列表里排除。 */
    private static final String SELF = "mate-gateway";
    private static final String CONNECT_TIMEOUT_PROP =
            "spring.cloud.gateway.server.webflux.httpclient.connect-timeout";
    private static final String RESPONSE_TIMEOUT_PROP =
            "spring.cloud.gateway.server.webflux.httpclient.response-timeout";

    private final ReactiveDiscoveryClient discoveryClient;
    private final GovernanceRuleStore ruleStore;
    private final Environment environment;

    public GatewayGovernanceController(ReactiveDiscoveryClient discoveryClient,
                                       GovernanceRuleStore ruleStore,
                                       Environment environment) {
        this.discoveryClient = discoveryClient;
        this.ruleStore = ruleStore;
        this.environment = environment;
    }

    // ---------------------------------------------------------------- 总览 / 服务列表

    /** 已注册服务 + 实例版本分布 + 三类规则现状。灰度发布页与各页共用。 */
    @GetMapping("/services")
    public Mono<Result<List<ServiceView>>> services() {
        return discoveryClient.getServices()
                .filter(GatewayGovernanceController::isHttpService)
                .flatMap(service -> discoveryClient.getInstances(service)
                        .collectList()
                        .map(instances -> toServiceView(service, instances)))
                .sort(Comparator.comparing(ServiceView::service))
                .collectList()
                .map(Result::ok);
    }

    /**
     * 只保留网关真正会做 HTTP 负载均衡的服务(Spring Cloud 应用名),剔除 Dubbo 在 Nacos 里的伴生注册:
     * 接口级 {@code providers:* / consumers:*}、应用级 {@code *-dubbo} 元数据服务,以及网关自身。
     * 这些都不是 {@code lb://} 路由目标,治理页面不应展示。
     */
    private static boolean isHttpService(String service) {
        if (service == null || service.isBlank()) {
            return false;
        }
        String s = service.toLowerCase();
        return !s.equals(SELF)
                && !s.startsWith("providers:")
                && !s.startsWith("consumers:")
                && !s.endsWith("-dubbo");
    }

    private ServiceView toServiceView(String service, List<ServiceInstance> instances) {
        Map<String, Long> byVersion = instances.stream()
                .collect(Collectors.groupingBy(GatewayGovernanceController::versionOf, Collectors.counting()));
        List<VersionStat> versions = byVersion.entrySet().stream()
                .map(e -> new VersionStat(e.getKey(), e.getValue().intValue()))
                .sorted(Comparator.comparing(VersionStat::version))
                .toList();
        List<InstanceView> instanceViews = instances.stream()
                .map(i -> new InstanceView(i.getHost(), i.getPort(), versionOf(i)))
                .sorted(Comparator.comparing(InstanceView::version).thenComparing(InstanceView::host))
                .toList();
        return new ServiceView(service, instances.size(), versions, instanceViews,
                ruleStore.gray(service), ruleStore.rateLimit(service), ruleStore.timeout(service));
    }

    // ---------------------------------------------------------------- 灰度发布

    @GetMapping("/gray")
    public Mono<Result<List<GrayRule>>> grayRules() {
        return Mono.just(Result.ok(ruleStore.allGray()));
    }

    @PutMapping("/gray")
    public Mono<Result<Void>> saveGray(@RequestBody GrayRule rule) {
        String error = validateGray(rule);
        if (error != null) {
            return Mono.just(Result.<Void>fail(error));
        }
        return Mono.fromRunnable(() -> ruleStore.putGray(rule))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(Result.<Void>ok())
                .doOnSuccess(r -> log.info("[gateway-governance] gray rule saved: {}", rule.service()));
    }

    /** 灰度规则保存前校验;返回 null 表示通过,否则返回错误提示。 */
    private static String validateGray(GrayRule rule) {
        if (rule.enabled() && rule.mode() == RouteMode.WEIGHTED) {
            boolean anyPositive = rule.weights().values().stream().anyMatch(w -> w != null && w > 0);
            if (!anyPositive) {
                return "权重灰度至少需为一个版本配置正权重";
            }
        }
        return null;
    }

    /** dry-run:不发真流量,按当前实例与规则推演某个版本请求会命中哪里。 */
    @PostMapping("/gray/test")
    public Mono<Result<RouteTestResult>> testRoute(@RequestBody RouteTestRequest req) {
        return discoveryClient.getInstances(req.service())
                .collectList()
                .map(instances -> simulate(req, instances))
                .map(Result::ok);
    }

    private RouteTestResult simulate(RouteTestRequest req, List<ServiceInstance> instances) {
        GrayRule rule = ruleStore.gray(req.service());
        String want = req.version();
        if (!rule.active()) {
            return new RouteTestResult(req.service(), RouteMode.OFF.name(), want, "ROUND_ROBIN",
                    "*", instances.size(), "未启用灰度,在全部 " + instances.size() + " 个实例间轮询",
                    addrs(instances));
        }
        if (rule.mode() == RouteMode.WEIGHTED) {
            return new RouteTestResult(req.service(), rule.mode().name(), want, "ROUND_ROBIN",
                    "weighted", instances.size(),
                    "按权重灰度:" + rule.weights() + ",在各版本间加权分流", addrs(instances));
        }
        // HEADER
        List<ServiceInstance> matched = ofVersion(instances, want);
        if (!matched.isEmpty()) {
            return new RouteTestResult(req.service(), rule.mode().name(), want, "MATCHED",
                    want, matched.size(), "命中 " + matched.size() + " 个 " + want + " 实例", addrs(matched));
        }
        List<ServiceInstance> fallback = ofVersion(instances, rule.fallbackVersion());
        if (!fallback.isEmpty()) {
            return new RouteTestResult(req.service(), rule.mode().name(), want, "FALLBACK",
                    rule.fallbackVersion(), fallback.size(),
                    "无 " + want + " 实例,降级到兜底 " + rule.fallbackVersion() + "(" + fallback.size() + " 个)",
                    addrs(fallback));
        }
        return new RouteTestResult(req.service(), rule.mode().name(), want, "NONE",
                "", 0, "无匹配实例且兜底不可用 → 503,建议配置降级版本", List.of());
    }

    /** 某版本的实例子集。 */
    private static List<ServiceInstance> ofVersion(List<ServiceInstance> instances, String version) {
        if (version == null || version.isBlank()) {
            return List.of();
        }
        return instances.stream().filter(i -> version.equals(versionOf(i))).toList();
    }

    /** 实例落点地址(host:port (version)),最多展示 20 个,避免大集群刷屏。 */
    private static List<String> addrs(List<ServiceInstance> instances) {
        return instances.stream()
                .limit(20)
                .map(i -> i.getHost() + ":" + i.getPort() + " (" + versionOf(i) + ")")
                .toList();
    }

    // ---------------------------------------------------------------- 限流

    @GetMapping("/ratelimit")
    public Mono<Result<List<RateLimitRule>>> rateLimitRules() {
        return Mono.just(Result.ok(ruleStore.allRateLimit()));
    }

    @PutMapping("/ratelimit")
    public Mono<Result<Void>> saveRateLimit(@RequestBody RateLimitRule rule) {
        return Mono.fromRunnable(() -> ruleStore.putRateLimit(rule))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(Result.<Void>ok())
                .doOnSuccess(r -> log.info("[gateway-governance] rate-limit rule saved: {}", rule.service()));
    }

    // ---------------------------------------------------------------- 超时

    @GetMapping("/timeout")
    public Mono<Result<TimeoutView>> timeoutView() {
        Integer connect = environment.getProperty(CONNECT_TIMEOUT_PROP, Integer.class);
        Integer response = environment.getProperty(RESPONSE_TIMEOUT_PROP, Integer.class);
        return Mono.just(Result.ok(new TimeoutView(connect, response, ruleStore.allTimeout())));
    }

    @PutMapping("/timeout")
    public Mono<Result<Void>> saveTimeout(@RequestBody TimeoutRule rule) {
        // 超时改动写进路由 metadata 才生效:putTimeout 持久化后广播 "timeout",由各网关实例的
        // GovernanceRuleStore 监听器统一触发路由重建(含本实例),实现「保存即生效」且覆盖多实例。
        return Mono.fromRunnable(() -> ruleStore.putTimeout(rule))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(r -> log.info("[gateway-governance] timeout rule saved: {}", rule.service()))
                .thenReturn(Result.ok());
    }

    // ---------------------------------------------------------------- helpers

    private static String versionOf(ServiceInstance instance) {
        Map<String, String> md = instance.getMetadata();
        String v = md == null ? null : md.get(METADATA_VERSION);
        return (v == null || v.isBlank()) ? "-" : v;
    }
}
