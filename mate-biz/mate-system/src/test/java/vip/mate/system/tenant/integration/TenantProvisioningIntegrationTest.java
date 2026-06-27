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
package vip.mate.system.tenant.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import vip.mate.base.exception.BizException;
import vip.mate.starter.tenant.core.TenantHelper;
import vip.mate.starter.test.annotation.MateIntegrationTest;
import vip.mate.starter.test.container.MysqlContainerInitializer;
import vip.mate.starter.test.container.RedisContainerInitializer;
import vip.mate.system.admin.domain.permission.adapter.repository.AdminRepository;
import vip.mate.system.admin.domain.permission.adapter.repository.RoleRepository;
import vip.mate.system.admin.domain.permission.model.aggregate.AdminAggregate;
import vip.mate.system.admin.domain.permission.model.aggregate.RoleAggregate;
import vip.mate.system.tenant.application.command.TenantCommand;
import vip.mate.system.tenant.application.command.TenantCommandService;
import vip.mate.system.tenant.application.query.TenantQueryService;
import vip.mate.system.tenant.domain.model.entity.Tenant;
import vip.mate.system.tenant.domain.model.valobj.TenantStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end provisioning test for the tenant onboarding flow (RFC-028 / RFC-012).
 *
 * <p>Boots the full mate-system Spring context (random port) against MySQL +
 * Redis Testcontainers, runs every Flyway migration (incl. V1.1.3 stored
 * procedures, V1.3.1 demo tenants, V1.3.2 quota), then drives
 * {@link TenantCommandService#create} and asserts the side effects that make a
 * tenant immediately usable:
 *
 * <pre>
 * TenantCommandService.create(cmd)
 *   → tenant row (ACTIVE)
 *   → tenant-scoped admin role  (&lt;CODE&gt;_ADMIN) with default menu grants
 *   → tenant-scoped admin account (&lt;code&gt;_admin) bound to that role
 * </pre>
 *
 * <p>Tenant-scoped tables are read back through
 * {@link TenantHelper#runAsTenant}/{@link TenantHelper#callAsTenant} because the
 * fail-closed tenant-line interceptor rejects no-context reads.
 *
 * <p>Requires Docker. Disabled in CI by default; enable via {@code -Dintegration-test=true}.
 */
@Disabled("Requires Docker for Testcontainers — enable with -Dintegration-test=true")
@MateIntegrationTest
@ContextConfiguration(initializers = {
        MysqlContainerInitializer.class,
        RedisContainerInitializer.class,
})
class TenantProvisioningIntegrationTest {

    @Autowired
    private TenantCommandService tenantCommandService;

    @Autowired
    private TenantQueryService tenantQueryService;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void provision_createsLoginableTenantWithAdminRoleAndMenus() {
        // Unique code so the test is replay-safe against a reused container.
        String code = "it" + System.nanoTime();
        TenantCommand cmd = new TenantCommand();
        cmd.setTenantCode(code);
        cmd.setTenantName("IT Provisioning Co " + code);
        cmd.setContactName("集成测试联系人");
        cmd.setContactPhone("13900001234");
        cmd.setPackageId("2"); // PRO package (seeded by V1.3.2)

        String tenantId = tenantCommandService.create(cmd);
        assertThat(tenantId).isNotBlank();

        // --- the tenant itself: exists, ACTIVE, code preserved ---
        Tenant tenant = tenantQueryService.findById(tenantId);
        assertThat(tenant).isNotNull();
        assertThat(tenant.getTenantCode()).isEqualTo(code);
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(tenant.getPackageId()).isEqualTo("2");

        // --- the provisioned admin: scoped to this tenant, role bound ---
        String adminUsername = code + "_admin";
        AdminAggregate admin = TenantHelper.callAsTenant(tenantId,
                () -> adminRepository.findByUsername(adminUsername));
        assertThat(admin).isNotNull();
        assertThat(admin.getAdmin().getUsername()).isEqualTo(adminUsername);
        assertThat(admin.getAdmin().getTenantId()).isEqualTo(tenantId);
        assertThat(admin.getRoleIds()).isNotEmpty();

        // --- the provisioned role: <CODE>_ADMIN, scoped to this tenant ---
        String roleId = admin.getRoleIds().get(0);
        RoleAggregate role = TenantHelper.callAsTenant(tenantId,
                () -> roleRepository.findById(roleId));
        assertThat(role).isNotNull();
        assertThat(role.getRole().getRoleKey()).isEqualTo(code.toUpperCase() + "_ADMIN");
        assertThat(role.getRole().getTenantId()).isEqualTo(tenantId);

        // --- the role carries the default menu grants (system dir + user mgmt) ---
        assertThat(role.getMenuIds()).contains("100", "101");
    }

    @Test
    void provision_duplicateTenantCodeRejected() {
        String code = "itdup" + System.nanoTime();

        TenantCommand first = new TenantCommand();
        first.setTenantCode(code);
        first.setTenantName("Dup First " + code);
        first.setContactName("联系人一");
        first.setPackageId("2");
        tenantCommandService.create(first);

        TenantCommand second = new TenantCommand();
        second.setTenantCode(code);
        second.setTenantName("Dup Second " + code);
        second.setContactName("联系人二");
        second.setPackageId("2");

        assertThatThrownBy(() -> tenantCommandService.create(second))
                .isInstanceOf(BizException.class);
    }
}
