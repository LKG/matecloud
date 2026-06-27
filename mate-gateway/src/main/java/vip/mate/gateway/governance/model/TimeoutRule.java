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
 * 单个服务的超时覆盖规则。启用后由 {@code DiscoveryMetadataRouteLocator} 写入该服务动态路由的
 * {@code response-timeout} / {@code connect-timeout} 元数据,Spring Cloud Gateway 据此对该路由生效,
 * 覆盖全局默认值。
 *
 * @param service          目标服务名
 * @param enabled          是否启用覆盖(false 时走全局默认)
 * @param connectTimeoutMs 连接超时(毫秒)
 * @param responseTimeoutMs 响应超时(毫秒)
 *
 * @author mateaix
 */
public record TimeoutRule(
        String service,
        boolean enabled,
        int connectTimeoutMs,
        int responseTimeoutMs
) {

    public TimeoutRule {
        if (connectTimeoutMs <= 0) {
            connectTimeoutMs = 1000;
        }
        if (responseTimeoutMs <= 0) {
            responseTimeoutMs = 5000;
        }
    }

    public static TimeoutRule off(String service) {
        return new TimeoutRule(service, false, 1000, 5000);
    }
}
