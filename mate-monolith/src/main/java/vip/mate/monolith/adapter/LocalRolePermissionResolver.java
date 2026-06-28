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
package vip.mate.monolith.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vip.mate.auth.domain.adapter.port.RolePermissionResolverPort;
import vip.mate.auth.domain.model.aggregate.AuthUser;
import vip.mate.starter.tenant.core.TenantHelper;
import vip.mate.system.admin.domain.permission.service.IPermissionDomainService;

import java.util.List;

/**
 * In-process {@link RolePermissionResolverPort} for monolith mode: resolves
 * roles/permissions by calling mate-system's {@link IPermissionDomainService}
 * directly instead of over Dubbo. The RBAC tables are global, so lookups run
 * under {@link TenantHelper#withoutTenant} (matching {@code RpcPermissionServiceImpl}).
 * <p>
 * No Redis fail-closed fallback is needed: an in-process call has no transient
 * RPC outage to guard against. {@code SaTokenIssuer} stays unchanged and uses
 * this resolver transparently.
 *
 * @author mateaix
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mate.rpc.mode", havingValue = "local")
public class LocalRolePermissionResolver implements RolePermissionResolverPort {

    private final IPermissionDomainService permissionDomainService;

    @Override
    public List<String> resolveRoleKeys(AuthUser user) {
        List<String> roles = user.getRoleCodes();
        if (roles != null && !roles.isEmpty()) {
            return roles;
        }
        try {
            List<String> resolved = TenantHelper.withoutTenant(
                    () -> permissionDomainService.findRoleKeysByUsername(user.getUsername()));
            return resolved != null ? resolved : List.of();
        } catch (Exception e) {
            log.warn("[monolith] resolveRoleKeys failed for username={}: {}", user.getUsername(), e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<String> resolvePermissions(AuthUser user) {
        List<String> upstream = user.getPermissions();
        if (upstream != null && !upstream.isEmpty()) {
            return upstream;
        }
        try {
            List<String> resolved = TenantHelper.withoutTenant(
                    () -> permissionDomainService.findPermissionsByUsername(user.getUsername()));
            return resolved != null ? resolved : List.of();
        } catch (Exception e) {
            log.warn("[monolith] resolvePermissions failed for username={}: {}", user.getUsername(), e.getMessage());
            return List.of();
        }
    }
}
