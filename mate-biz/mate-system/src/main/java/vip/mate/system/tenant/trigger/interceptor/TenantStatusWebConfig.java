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
package vip.mate.system.tenant.trigger.interceptor;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import vip.mate.starter.tenant.core.TenantProperties;
import vip.mate.system.tenant.application.quota.TenantQuotaService;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers {@link TenantStatusInterceptor} so every business request is gated
 * on the tenant's lifecycle status (RFC-028).
 *
 * <p>Ordered with a high {@code order()} so it runs AFTER the Sa-Token route
 * interceptor (registered by the sa-token starter) — unauthenticated requests
 * are rejected before we ever look at tenant status. Actuator, login/auth and
 * error paths are excluded, plus any {@code mate.tenant.ignore-urls} patterns.
 *
 * @author mateaix
 */
@Configuration
@RequiredArgsConstructor
public class TenantStatusWebConfig implements WebMvcConfigurer {

    private final TenantProperties tenantProperties;
    private final TenantQuotaService tenantQuotaService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> excludes = new ArrayList<>(List.of(
                "/actuator/**",
                "/error",
                "/api/v1/auth/**",
                "/auth/**",
                "/login/**"));
        if (tenantProperties.getIgnoreUrls() != null) {
            excludes.addAll(tenantProperties.getIgnoreUrls());
        }
        registry.addInterceptor(new TenantStatusInterceptor(tenantProperties, tenantQuotaService))
                .addPathPatterns("/**")
                .excludePathPatterns(excludes)
                .order(Integer.MAX_VALUE);
    }
}
