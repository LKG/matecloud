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
package vip.mate.gateway.governance.model;

/**
 * 单个服务的限流规则(令牌桶)。
 *
 * @param service  目标服务名
 * @param enabled  是否启用
 * @param key      限流维度,见 {@link RateLimitKey}
 * @param rate     时间窗内允许的请求数(令牌数)
 * @param interval 时间窗长度(秒)
 *
 * @author mateaix
 */
public record RateLimitRule(
        String service,
        boolean enabled,
        RateLimitKey key,
        int rate,
        int interval
) {

    public RateLimitRule {
        if (key == null) {
            key = RateLimitKey.IP;
        }
        if (rate <= 0) {
            rate = 100;
        }
        if (interval <= 0) {
            interval = 1;
        }
    }

    public static RateLimitRule off(String service) {
        return new RateLimitRule(service, false, RateLimitKey.IP, 100, 1);
    }
}
