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

import java.util.Map;

/**
 * 单个服务的灰度路由规则(JDK21 record,不可变值对象,可直接 Jackson 序列化进 Redis)。
 *
 * @param service         目标服务名(lb:// 后的 serviceId)
 * @param mode            路由模式,见 {@link RouteMode}
 * @param headerName      版本请求头名,默认 {@code X-Service-Version}
 * @param weights         版本 -> 权重(仅 {@link RouteMode#WEIGHTED} 生效),如 {@code {"1.0.0":80,"2.0.0":20}}
 * @param fallbackVersion 目标版本无存活实例时的降级兜底版本;空串表示不降级
 * @param enabled         规则总开关;false 等价于 {@link RouteMode#OFF}
 *
 * @author mateaix
 */
public record GrayRule(
        String service,
        RouteMode mode,
        String headerName,
        Map<String, Integer> weights,
        String fallbackVersion,
        boolean enabled
) {

    public static final String DEFAULT_HEADER = "X-Service-Version";

    /** 紧凑构造器:补默认头名、空集合保护,保证 record 始终处于合法状态。 */
    public GrayRule {
        if (headerName == null || headerName.isBlank()) {
            headerName = DEFAULT_HEADER;
        }
        weights = weights == null ? Map.of() : Map.copyOf(weights);
        if (fallbackVersion == null) {
            fallbackVersion = "";
        }
        if (mode == null) {
            mode = RouteMode.OFF;
        }
    }

    /** 某服务默认(关闭灰度)的规则。 */
    public static GrayRule off(String service) {
        return new GrayRule(service, RouteMode.OFF, DEFAULT_HEADER, Map.of(), "", false);
    }

    /** 是否实际启用了灰度(开关开 && 非 OFF)。 */
    public boolean active() {
        return enabled && mode != RouteMode.OFF;
    }
}
