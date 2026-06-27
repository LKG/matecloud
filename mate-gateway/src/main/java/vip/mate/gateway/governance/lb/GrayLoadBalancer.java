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
package vip.mate.gateway.governance.lb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.SelectedInstanceCallback;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;
import vip.mate.gateway.governance.model.GrayRule;
import vip.mate.gateway.governance.store.GovernanceRuleStore;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 按版本灰度的负载均衡器,替换默认的 {@code RoundRobinLoadBalancer}。
 *
 * <p>每次选实例时读取 {@link GovernanceRuleStore} 中该服务的 {@link GrayRule}(纯内存快照,非阻塞):</p>
 * <ul>
 *   <li>{@code OFF} / 规则未启用 —— 全实例轮询(行为与官方一致)。</li>
 *   <li>{@code HEADER} —— 取请求头 {@code X-Service-Version},只在 {@code metadata.version} 完全相等的
 *       实例里轮询;无匹配则尝试降级到 {@code fallbackVersion};仍无则空响应(交由上层 503)。</li>
 *   <li>{@code WEIGHTED} —— 在各版本之间按权重随机加权挑一个版本,再在该版本实例内轮询。</li>
 * </ul>
 *
 * <p>实例的版本取自 Nacos 注册 metadata 的 {@code version} 键。该类不做任何阻塞调用,完全运行在 Reactor 线程上。</p>
 *
 * @author mateaix
 */
@Slf4j
public class GrayLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    /** 实例 metadata 中表示版本的键。 */
    private static final String METADATA_VERSION = "version";

    private final ObjectProvider<ServiceInstanceListSupplier> supplierProvider;
    private final String serviceId;
    private final GovernanceRuleStore ruleStore;
    private final AtomicInteger position = new AtomicInteger(ThreadLocalRandom.current().nextInt(1000));

    public GrayLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> supplierProvider,
                            String serviceId,
                            GovernanceRuleStore ruleStore) {
        this.supplierProvider = supplierProvider;
        this.serviceId = serviceId;
        this.ruleStore = ruleStore;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier =
                supplierProvider.getIfAvailable(NoopServiceInstanceListSupplier::new);
        return supplier.get(request).next()
                .map(instances -> processResponse(supplier, instances, request));
    }

    /**
     * 选实例并回调 {@link SelectedInstanceCallback} —— 与官方 RoundRobin/Random LB 一致,通知供应链
     * (如 hint/统计/粘性会话 supplier)本次最终选中的实例,否则这些增强会失效。
     */
    private Response<ServiceInstance> processResponse(ServiceInstanceListSupplier supplier,
                                                      List<ServiceInstance> instances,
                                                      Request<?> request) {
        Response<ServiceInstance> response = select(instances, request);
        if (supplier instanceof SelectedInstanceCallback callback && response.hasServer()) {
            callback.selectedServiceInstance(response.getServer());
        }
        return response;
    }

    private Response<ServiceInstance> select(List<ServiceInstance> instances, Request<?> request) {
        if (instances.isEmpty()) {
            log.warn("[gateway-gray] no instances for {}", serviceId);
            return new EmptyResponse();
        }
        GrayRule rule = ruleStore.gray(serviceId);
        if (!rule.active()) {
            return roundRobin(instances);
        }

        return switch (rule.mode()) {
            case HEADER -> byHeader(instances, rule, request);
            case WEIGHTED -> byWeight(instances, rule);
            case OFF -> roundRobin(instances);
        };
    }

    /** 按请求头版本隔离;命中为空时按兜底版本降级。 */
    private Response<ServiceInstance> byHeader(List<ServiceInstance> instances, GrayRule rule, Request<?> request) {
        String wanted = headerVersion(request, rule.headerName());
        if (wanted == null || wanted.isBlank()) {
            // 调用方未声明版本 —— 不强制隔离,退回全实例轮询。
            return roundRobin(instances);
        }
        List<ServiceInstance> matched = ofVersion(instances, wanted);
        if (!matched.isEmpty()) {
            return roundRobin(matched);
        }
        List<ServiceInstance> fallback = ofVersion(instances, rule.fallbackVersion());
        if (!fallback.isEmpty()) {
            log.debug("[gateway-gray] {} version {} unavailable, fallback -> {}", serviceId, wanted, rule.fallbackVersion());
            return roundRobin(fallback);
        }
        log.warn("[gateway-gray] {} version {} unavailable and no fallback instance", serviceId, wanted);
        return new EmptyResponse();
    }

    /** 按版本权重加权随机。 */
    private Response<ServiceInstance> byWeight(List<ServiceInstance> instances, GrayRule rule) {
        Map<String, List<ServiceInstance>> byVer = instances.stream()
                .collect(Collectors.groupingBy(GrayLoadBalancer::versionOf));

        int totalWeight = 0;
        for (Map.Entry<String, Integer> w : rule.weights().entrySet()) {
            if (byVer.containsKey(w.getKey()) && w.getValue() != null && w.getValue() > 0) {
                totalWeight += w.getValue();
            }
        }
        if (totalWeight <= 0) {
            // 配置的版本都没存活实例 —— 退回全实例轮询,避免打不出去。
            return roundRobin(instances);
        }
        int hit = ThreadLocalRandom.current().nextInt(totalWeight);
        for (Map.Entry<String, Integer> w : rule.weights().entrySet()) {
            List<ServiceInstance> pool = byVer.get(w.getKey());
            if (pool == null || w.getValue() == null || w.getValue() <= 0) {
                continue;
            }
            hit -= w.getValue();
            if (hit < 0) {
                return roundRobin(pool);
            }
        }
        return roundRobin(instances);
    }

    /** 经典轮询(原子自增取模),与官方 RoundRobinLoadBalancer 行为一致。 */
    private Response<ServiceInstance> roundRobin(List<ServiceInstance> pool) {
        // 单实例直接返回,不推进游标——避免对已被上游 supplier 过滤成单实例的池做无谓自增。
        if (pool.size() == 1) {
            return new DefaultResponse(pool.get(0));
        }
        // 用 & Integer.MAX_VALUE 而非 Math.abs:后者遇 Integer.MIN_VALUE 仍返回负数,会导致取模为负越界。
        int idx = (position.incrementAndGet() & Integer.MAX_VALUE) % pool.size();
        return new DefaultResponse(pool.get(idx));
    }

    private static List<ServiceInstance> ofVersion(List<ServiceInstance> instances, String version) {
        if (version == null || version.isBlank()) {
            return List.of();
        }
        return instances.stream().filter(i -> version.equals(versionOf(i))).toList();
    }

    private static String versionOf(ServiceInstance instance) {
        Map<String, String> md = instance.getMetadata();
        String v = md == null ? null : md.get(METADATA_VERSION);
        return v == null ? "" : v;
    }

    /** 从负载均衡 Request 上下文里取下游请求头(SCG 透传的原始请求头)。 */
    private static String headerVersion(Request<?> request, String headerName) {
        if (request.getContext() instanceof RequestDataContext ctx) {
            RequestData data = ctx.getClientRequest();
            HttpHeaders headers = data == null ? null : data.getHeaders();
            if (headers != null) {
                return headers.getFirst(headerName);
            }
        }
        return null;
    }
}
