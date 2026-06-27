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
import vip.mate.starter.test.annotation.MateIntegrationTest;
import vip.mate.starter.test.container.MysqlContainerInitializer;
import vip.mate.starter.test.container.RedisContainerInitializer;
import vip.mate.system.tenant.application.command.TenantCommand;
import vip.mate.system.tenant.application.command.TenantCommandService;
import vip.mate.system.tenant.application.lifecycle.TenantLifecycleService;
import vip.mate.system.tenant.application.query.TenantQueryService;
import vip.mate.system.tenant.domain.model.entity.Tenant;
import vip.mate.system.tenant.domain.model.valobj.TenantStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test for the time-based tenant lifecycle sweep (RFC-028): the
 * ACTIVE → EXPIRED transition driven by
 * {@link TenantLifecycleService#expireOverdueTenants()}.
 *
 * <p>Boots the full mate-system Spring context (random port) against MySQL +
 * Redis Testcontainers with all Flyway migrations applied. Each test creates its
 * own tenant (unique code) and drives the assertions off it, so it never depends
 * on exact global counts — the seeded demo tenant 1002 (globex) expires 3 days
 * out (future) and is therefore untouched by the sweep.
 *
 * <p>Requires Docker. Disabled in CI by default; enable via {@code -Dintegration-test=true}.
 */
@Disabled("Requires Docker for Testcontainers — enable with -Dintegration-test=true")
@MateIntegrationTest
@ContextConfiguration(initializers = {
        MysqlContainerInitializer.class,
        RedisContainerInitializer.class,
})
class TenantLifecycleIntegrationTest {

    @Autowired
    private TenantCommandService tenantCommandService;

    @Autowired
    private TenantLifecycleService tenantLifecycleService;

    @Autowired
    private TenantQueryService tenantQueryService;

    @Test
    void expireOverdueTenants_flipsActiveToExpired() {
        String code = "itexp" + System.nanoTime();
        TenantCommand cmd = new TenantCommand();
        cmd.setTenantCode(code);
        cmd.setTenantName("Expiring Co " + code);
        cmd.setContactName("到期联系人");
        cmd.setPackageId("1"); // BASIC package
        String tenantId = tenantCommandService.create(cmd);

        // renew on an ACTIVE tenant just sets expireAt — push it into the past
        // so the sweep treats it as overdue.
        tenantCommandService.renew(tenantId, LocalDateTime.now().minusDays(1));

        // Still ACTIVE — only the sweep performs the transition.
        assertThat(tenantQueryService.findById(tenantId).getStatus())
                .isEqualTo(TenantStatus.ACTIVE);

        int expiredCount = tenantLifecycleService.expireOverdueTenants();
        assertThat(expiredCount).isGreaterThanOrEqualTo(1);

        Tenant after = tenantQueryService.findById(tenantId);
        assertThat(after.getStatus()).isEqualTo(TenantStatus.EXPIRED);
    }

    @Test
    void expireOverdueTenants_leavesHealthyTenantUntouched() {
        String code = "ithealthy" + System.nanoTime();
        TenantCommand cmd = new TenantCommand();
        cmd.setTenantCode(code);
        cmd.setTenantName("Healthy Co " + code);
        cmd.setContactName("健康联系人");
        cmd.setPackageId("1");
        String tenantId = tenantCommandService.create(cmd);

        // Future expiry — the sweep must not touch it.
        tenantCommandService.renew(tenantId, LocalDateTime.now().plusDays(30));

        tenantLifecycleService.expireOverdueTenants();

        Tenant after = tenantQueryService.findById(tenantId);
        assertThat(after.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }
}
