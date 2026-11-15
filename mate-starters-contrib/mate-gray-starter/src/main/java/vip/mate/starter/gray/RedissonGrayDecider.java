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
package vip.mate.starter.gray;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis (Redisson) 规则源的 {@link GrayDecider} 实现 —— 单体路线 B 的默认决策器。
 *
 * <p>每个特性一份规则，落在两个 StringCodec 键上（可直接 redis-cli 运维）：</p>
 * <ul>
 *   <li>{@code {keyPrefix}{feature}} —— HASH：{@code enabled} / {@code grayVersion} / {@code percentage}</li>
 *   <li>{@code {keyPrefix}{feature}:whitelist} —— SET：白名单 userId，命中即进灰度</li>
 * </ul>
 *
 * <p>规则带 {@link GrayProperties#getCacheTtlMillis()} 的进程内快照缓存，到期惰性回源；
 * Redis 不可用 / 规则缺失 / 解析失败一律降级为 {@link GrayRule#none} —— 全量稳定版，
 * 灰度故障绝不影响主线。</p>
 *
 * @author mateaix
 */
@Slf4j
public class RedissonGrayDecider implements GrayDecider {

    private final RedissonClient redisson;
    private final GrayProperties props;
    private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<>();

    public RedissonGrayDecider(RedissonClient redisson, GrayProperties props) {
        this.redisson = redisson;
        this.props = props;
    }

    @Override
    public String decide(String feature, String userId) {
        GrayRule rule = ruleFor(feature);
        return rule.hit(userId) ? rule.grayVersion() : null;
    }

    private GrayRule ruleFor(String feature) {
        long now = System.currentTimeMillis();
        Cached hit = cache.get(feature);
        if (hit != null && now - hit.loadedAt < props.getCacheTtlMillis()) {
            return hit.rule;
        }
        GrayRule fresh = load(feature);
        cache.put(feature, new Cached(fresh, now));
        return fresh;
    }

    /** 从 Redis 拉一份规则快照；任何异常都吞掉并降级为"无规则"。 */
    private GrayRule load(String feature) {
        String key = props.getKeyPrefix() + feature;
        try {
            RMap<String, String> map = redisson.getMap(key, StringCodec.INSTANCE);
            Map<String, String> fields = map.readAllMap();
            if (fields.isEmpty()) {
                return GrayRule.none(feature);
            }
            boolean enabled = Boolean.parseBoolean(fields.getOrDefault("enabled", "false"));
            String grayVersion = fields.getOrDefault("grayVersion", "");
            int percentage = parsePercent(fields.get("percentage"));
            RSet<String> wl = redisson.getSet(key + ":whitelist", StringCodec.INSTANCE);
            Set<String> whitelist = wl.isEmpty() ? Set.of() : Set.copyOf(wl.readAll());
            return new GrayRule(feature, enabled, grayVersion, percentage, whitelist);
        } catch (Exception e) {
            log.warn("[mate-gray] load rule '{}' failed, fallback to stable: {}", feature, e.getMessage());
            return GrayRule.none(feature);
        }
    }

    private static int parsePercent(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            int p = Integer.parseInt(raw.trim());
            return Math.max(0, Math.min(100, p));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private record Cached(GrayRule rule, long loadedAt) {
    }
}
