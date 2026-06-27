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
package vip.mate.gateway.governance.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import vip.mate.gateway.governance.model.GrayRule;
import vip.mate.gateway.governance.model.RateLimitRule;
import vip.mate.gateway.governance.model.TimeoutRule;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网关治理规则的唯一事实来源。
 *
 * <p>三类规则(灰度 / 限流 / 超时)以 JSON 持久化在 Redis 的三张 {@link RMap} 中,survive 重启;
 * 同时各维护一份进程内 {@link ConcurrentHashMap} 快照,供负载均衡器与限流 filter 在<b>请求热路径</b>上
 * 非阻塞读取(绝不在 Reactor 事件循环里打 Redis)。</p>
 *
 * <p>多网关实例间的一致性:任一实例写规则后,通过 {@link RTopic} 广播变更类型,其余实例收到后只重载
 * 对应那张表的快照。启动时 {@link #init()} 全量加载并订阅。</p>
 *
 * <p>读方法(get*)只碰内存快照,可在 reactive 链路直接调用;写方法(put*)走 Redisson 阻塞 API,
 * 由 controller 包到 {@code boundedElastic} 调度器执行。</p>
 *
 * @author mateaix
 */
@Slf4j
@Component
public class GovernanceRuleStore {

    private static final String MAP_GRAY = "mate:gateway:gray";
    private static final String MAP_RATELIMIT = "mate:gateway:ratelimit";
    private static final String MAP_TIMEOUT = "mate:gateway:timeout";
    private static final String TOPIC_CHANGED = "mate:gateway:rules:changed";

    private final RedissonClient redisson;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    private final Map<String, GrayRule> grayCache = new ConcurrentHashMap<>();
    private final Map<String, RateLimitRule> rateLimitCache = new ConcurrentHashMap<>();
    private final Map<String, TimeoutRule> timeoutCache = new ConcurrentHashMap<>();

    public GovernanceRuleStore(RedissonClient redisson, ObjectMapper objectMapper,
                               ApplicationEventPublisher eventPublisher) {
        this.redisson = redisson;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    public void init() {
        reloadAll();
        RTopic topic = redisson.getTopic(TOPIC_CHANGED);
        topic.addListener(String.class, (channel, type) -> {
            log.debug("[gateway-governance] received change notice: {}", type);
            switch (type) {
                case "gray" -> reload(MAP_GRAY, grayCache, GrayRule.class);
                case "ratelimit" -> reload(MAP_RATELIMIT, rateLimitCache, RateLimitRule.class);
                // 超时规则写在路由 metadata 里,本节点重载快照后还须重建路由表才生效——
                // 由本监听器统一触发,确保「保存即生效」覆盖所有网关实例(而非只有发起保存的那台)。
                case "timeout" -> {
                    reload(MAP_TIMEOUT, timeoutCache, TimeoutRule.class);
                    refreshRoutes();
                }
                default -> {
                    reloadAll();
                    refreshRoutes();
                }
            }
        });
        log.info("[gateway-governance] rule store ready — gray:{} ratelimit:{} timeout:{}",
                grayCache.size(), rateLimitCache.size(), timeoutCache.size());
    }

    private void reloadAll() {
        reload(MAP_GRAY, grayCache, GrayRule.class);
        reload(MAP_RATELIMIT, rateLimitCache, RateLimitRule.class);
        reload(MAP_TIMEOUT, timeoutCache, TimeoutRule.class);
    }

    /** 触发本节点重建网关路由表(使超时 metadata 变更立即生效)。 */
    private void refreshRoutes() {
        try {
            eventPublisher.publishEvent(new RefreshRoutesEvent(this));
        } catch (Exception ex) {
            log.warn("[gateway-governance] route refresh publish failed: {}", ex.getMessage());
        }
    }

    private <T> void reload(String mapName, Map<String, T> cache, Class<T> type) {
        try {
            RMap<String, String> map = redisson.getMap(mapName);
            Map<String, T> fresh = new ConcurrentHashMap<>();
            map.readAllEntrySet().forEach(e -> {
                try {
                    fresh.put(e.getKey(), objectMapper.readValue(e.getValue(), type));
                } catch (Exception ex) {
                    log.warn("[gateway-governance] skip malformed rule {}/{}: {}", mapName, e.getKey(), ex.getMessage());
                }
            });
            cache.clear();
            cache.putAll(fresh);
        } catch (Exception ex) {
            log.warn("[gateway-governance] reload {} failed (using existing snapshot): {}", mapName, ex.getMessage());
        }
    }

    // ---------------------------------------------------------------- 读(热路径,非阻塞)

    /** 灰度规则,缺省返回该服务的「关闭」规则。 */
    public GrayRule gray(String service) {
        return grayCache.getOrDefault(service, GrayRule.off(service));
    }

    public RateLimitRule rateLimit(String service) {
        return rateLimitCache.getOrDefault(service, RateLimitRule.off(service));
    }

    public TimeoutRule timeout(String service) {
        return timeoutCache.getOrDefault(service, TimeoutRule.off(service));
    }

    /** 当前已配置(非默认)的灰度规则全集,供 controller 列表展示。 */
    public List<GrayRule> allGray() {
        return List.copyOf(grayCache.values());
    }

    public List<RateLimitRule> allRateLimit() {
        return List.copyOf(rateLimitCache.values());
    }

    public List<TimeoutRule> allTimeout() {
        return List.copyOf(timeoutCache.values());
    }

    // ---------------------------------------------------------------- 写(controller 经 boundedElastic 调用)

    public void putGray(GrayRule rule) {
        write(MAP_GRAY, rule.service(), rule, grayCache, "gray");
    }

    public void putRateLimit(RateLimitRule rule) {
        write(MAP_RATELIMIT, rule.service(), rule, rateLimitCache, "ratelimit");
    }

    public void putTimeout(TimeoutRule rule) {
        write(MAP_TIMEOUT, rule.service(), rule, timeoutCache, "timeout");
    }

    private <T> void write(String mapName, String service, T rule, Map<String, T> cache, String type) {
        try {
            String json = objectMapper.writeValueAsString(rule);
            redisson.<String, String>getMap(mapName).put(service, json);
            cache.put(service, rule);
            redisson.getTopic(TOPIC_CHANGED).publish(type);
        } catch (Exception ex) {
            throw new IllegalStateException("persist rule failed: " + ex.getMessage(), ex);
        }
    }
}
