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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.HandlerInterceptor;
import vip.mate.starter.tenant.core.TenantContext;
import vip.mate.starter.tenant.core.TenantProperties;
import vip.mate.system.tenant.application.quota.TenantQuotaService;

/**
 * Servlet interceptor that gates every request on the tenant's lifecycle
 * status (RFC-028). Runs after Sa-Token auth so unauthenticated requests are
 * already rejected; by the time we reach here the starter's TenantWebFilter has
 * populated {@link TenantContext}.
 *
 * <p>When tenant support is disabled, or there is no resolved tenant (auth /
 * login endpoints), this is a no-op. A {@link vip.mate.base.exception.BizException}
 * thrown by {@link TenantQuotaService#assertActiveTenant(String)} propagates to
 * the global exception handler for rendering.
 *
 * @author mateaix
 */
@RequiredArgsConstructor
public class TenantStatusInterceptor implements HandlerInterceptor {

    private final TenantProperties tenantProperties;
    private final TenantQuotaService tenantQuotaService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!tenantProperties.isEnabled()) {
            return true;
        }
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            // No tenant resolved yet (login/auth/public endpoints) — nothing to gate.
            return true;
        }
        String superTenant = tenantProperties.getSuperTenantId();
        if (superTenant != null && !superTenant.isBlank() && superTenant.equals(tenantId)) {
            return true;
        }
        // Throws BizException on suspended/deleted/expired-past-grace; the
        // global handler renders it as a structured failure response.
        tenantQuotaService.assertActiveTenant(tenantId);
        return true;
    }
}
