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
package vip.mate.gateway.governance.web.dto;

import vip.mate.gateway.governance.model.GrayRule;
import vip.mate.gateway.governance.model.RateLimitRule;
import vip.mate.gateway.governance.model.TimeoutRule;

import java.util.List;

/**
 * 网关治理 REST 出/入参 DTO 集合(JDK21 record)。集中一处便于前端对照字段。
 *
 * @author mateaix
 */
public final class GovernanceDtos {

    private GovernanceDtos() {
    }

    /** 单个版本的实例计数。 */
    public record VersionStat(String version, int count) {
    }

    /** 单个实例明细(灰度时用于确认某版本具体落在哪台)。 */
    public record InstanceView(String host, int port, String version) {
    }

    /** 服务总览:实例/版本分布 + 实例明细 + 三类治理规则现状。 */
    public record ServiceView(
            String service,
            int instanceCount,
            List<VersionStat> versions,
            List<InstanceView> instances,
            GrayRule gray,
            RateLimitRule rateLimit,
            TimeoutRule timeout
    ) {
    }

    /** 路由 dry-run 请求体。 */
    public record RouteTestRequest(String service, String version) {
    }

    /** 路由 dry-run 结果。outcome ∈ {ROUND_ROBIN, MATCHED, FALLBACK, NONE}。 */
    public record RouteTestResult(
            String service,
            String mode,
            String requestedVersion,
            String outcome,
            String hitVersion,
            int hitInstanceCount,
            String detail,
            List<String> targets
    ) {
    }

    /** 全局超时视图:网关层 httpclient 默认值(来自配置),以及各服务的覆盖规则。 */
    public record TimeoutView(
            Integer globalConnectTimeoutMs,
            Integer globalResponseTimeoutMs,
            List<TimeoutRule> overrides
    ) {
    }
}
