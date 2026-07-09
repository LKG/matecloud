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
package vip.mate.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines three tiers of auth strategies for gateway routes.
 *
 * @author mateaix
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mate.gateway.auth")
public class AuthStrategyConfig {

    private List<String> publicPaths = new ArrayList<>(List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/api/v1/auth/captcha",
            "/api/v1/auth/sms/**",
            // actuator: 仅健康探针/版本信息无鉴权放行; prometheus/metrics 等敏感端点纳入 adminPaths
            "/actuator/health",
            "/actuator/info",
            "/doc.html",
            "/webjars/**",
            "/swagger-resources/**",
            "/v3/api-docs/**"
    ));

    private List<String> loginPaths = new ArrayList<>(List.of(
            "/api/v1/**"
    ));

    private List<String> adminPaths = new ArrayList<>(List.of(
            "/api/v1/users/delete/**",
            "/api/v1/roles/**",
            "/api/v1/menus/**",
            // actuator 敏感端点(prometheus/metrics/env/beans...): 经网关需超管;
            // 监控应走内网直连各服务端口抓取, 不经业务网关无鉴权暴露
            "/actuator/**"
    ));
}
