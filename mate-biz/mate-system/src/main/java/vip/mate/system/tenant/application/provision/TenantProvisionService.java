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
package vip.mate.system.tenant.application.provision;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.mate.base.exception.BizException;
import vip.mate.starter.tenant.core.TenantHelper;
import vip.mate.system.admin.application.command.AdminCommandService;
import vip.mate.system.admin.application.command.RoleCommandService;
import vip.mate.system.admin.types.exception.AdminErrorCode;
import vip.mate.system.domain.adapter.port.DomainEventPublisher;
import vip.mate.system.tenant.domain.event.TenantProvisionedEvent;

import java.util.List;

/**
 * Provisions a freshly-created tenant so it is immediately usable: a
 * tenant-scoped admin role, an admin account bound to that role, a sensible
 * default menu set, and a {@code tenant.provisioned} domain event.
 *
 * <p>Invoked from {@code TenantCommandService.create} after the tenant row is
 * saved. The caller's create() is already {@code @Transactional}, so every
 * insert here joins that transaction; any exception propagates and rolls the
 * whole tenant creation back.
 *
 * @author mateaix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantProvisionService {

    /** Default password for the first admin of a new tenant. */
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    /**
     * Default menus granted to the tenant admin role. Mirrors the V1.3.1 demo
     * seed: system dir (100) + user mgmt (101/105/106) + dict + config + monitor
     * + logs (200/201/202/203). Deliberately excludes admin/role/menu mgmt
     * (102/103/104) and tenant/package mgmt (107/110) — those are platform-level.
     */
    private static final List<String> DEFAULT_TENANT_MENU_IDS =
            List.of("100", "101", "105", "106", "200", "201", "202", "203");

    private final RoleCommandService roleCommandService;
    private final AdminCommandService adminCommandService;
    private final DomainEventPublisher eventPublisher;

    /**
     * Provision the tenant identified by {@code tenantId}. Runs the inserts as
     * the target tenant via {@link TenantHelper#runAsTenant} so rows are stamped
     * with the right tenant even when the caller runs as the super-tenant.
     */
    public void provision(String tenantId, String tenantCode, String contactName) {
        TenantHelper.runAsTenant(tenantId, () -> {
            // 1. tenant-scoped admin role (role_key is globally unique → prefix with code)
            String roleId = roleCommandService.createRoleForTenant(
                    tenantCode.toUpperCase() + "_ADMIN", "企业管理员", 1, tenantId);

            // 2. grant the default menu set to the role
            roleCommandService.assignMenus(roleId, DEFAULT_TENANT_MENU_IDS);

            // 3. first admin for the tenant
            String username = tenantCode + "_admin";
            String nick = (contactName == null || contactName.isBlank())
                    ? tenantCode + " 管理员"
                    : contactName;
            String adminId;
            try {
                adminId = adminCommandService.createAdmin(
                        username, DEFAULT_ADMIN_PASSWORD, nick, null, null, null, tenantId);
            } catch (BizException e) {
                if (AdminErrorCode.DUPLICATE_ADMIN_USERNAME.getCode().equals(e.getCode())) {
                    throw new BizException(e.getCode(),
                            "Cannot provision tenant: admin username already taken: " + username);
                }
                throw e;
            }

            // 4. bind the admin to the role
            adminCommandService.assignRoles(adminId, List.of(roleId));

            // 5. publish synchronously so it stays inside the create() transaction
            eventPublisher.publish(new TenantProvisionedEvent(
                    tenantId, tenantCode, username, adminId, roleId));

            log.info("[tenant] provisioned tenant={} admin={} role={}", tenantId, username, roleId);
        });
    }
}
