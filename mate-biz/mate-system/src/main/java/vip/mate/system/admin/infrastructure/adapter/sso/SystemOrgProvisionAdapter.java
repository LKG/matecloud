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
package vip.mate.system.admin.infrastructure.adapter.sso;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vip.mate.system.admin.application.command.AdminCommandService;
import vip.mate.system.admin.application.command.DeptCommandService;
import vip.mate.starter.sso.port.OrgProvisionPort;
import vip.mate.starter.sso.port.ProviderRef;
import vip.mate.starter.sso.spi.model.ExternalDept;
import vip.mate.starter.sso.spi.model.ExternalUser;

/**
 * Platform-side {@link OrgProvisionPort}: lands SSO organization sync into
 * {@code mate_dept} + {@code mate_admin}. Reuses the existing DDD command
 * services (no table bypass). Only active when {@code mate.feature.sso.enabled=true}.
 *
 * <p><b>Privilege note:</b> synced admins are created with <b>no roles</b> (zero
 * permission) — an administrator must assign roles in 角色/用户管理 before they
 * can do anything. This prevents an org sync from silently granting access.
 *
 * <p>For student-style scenarios (bulk learners), the LMS service implements its
 * own {@code OrgProvisionPort} against {@code mate_lms_member} instead.
 *
 * @author mateaix
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mate.feature.sso", name = "enabled", havingValue = "true")
public class SystemOrgProvisionAdapter implements OrgProvisionPort {

    private final DeptCommandService deptCommandService;
    private final AdminCommandService adminCommandService;

    @Override
    public String upsertDept(ExternalDept dept, String parentLocalId, String existingLocalId, ProviderRef ref) {
        if (existingLocalId != null) {
            deptCommandService.updateDept(existingLocalId, dept.name(), dept.sort(),
                    null, null, null, null);
            return existingLocalId;
        }
        return deptCommandService.createDept(parentLocalId, dept.name(), dept.sort(), null, null, null);
    }

    @Override
    public String provisionUser(ExternalUser user, List<String> localDeptIds, String existingLocalId, ProviderRef ref) {
        String nick = (user.name() == null || user.name().isBlank()) ? user.externalId() : user.name();
        String firstDept = (localDeptIds == null || localDeptIds.isEmpty()) ? null : localDeptIds.get(0);
        if (existingLocalId != null) {
            adminCommandService.updateAdmin(existingLocalId, nick, user.avatar(), firstDept,
                    user.mobile(), user.email(), nick);
            return existingLocalId;
        }
        String username = buildUsername(ref.provider(), user.externalId());
        String id = adminCommandService.createAdmin(username, randomPassword(), nick,
                user.mobile(), user.email(), nick);
        if (firstDept != null) {
            adminCommandService.updateAdmin(id, nick, user.avatar(), firstDept,
                    user.mobile(), user.email(), nick);
        }
        // No roles assigned on purpose — zero privilege until an admin grants roles.
        return id;
    }

    @Override
    public void disableUser(String localUserId, ProviderRef ref) {
        try {
            adminCommandService.disableAdmin(localUserId);
        } catch (Exception e) {
            log.warn("[sso] disableAdmin {} failed: {}", localUserId, e.getMessage());
        }
    }

    @Override
    public void unlinkUser(String localUserId, ProviderRef ref) {
        // Keep the local admin; the starter removes the identity mapping. No-op.
    }

    @Override
    public void removeDept(String localDeptId, ProviderRef ref) {
        try {
            deptCommandService.deleteDept(localDeptId);
        } catch (Exception e) {
            // Likely has children / still referenced — leave it, just log.
            log.warn("[sso] removeDept {} skipped: {}", localDeptId, e.getMessage());
        }
    }

    /** Build a unique, schema-valid username (4-32 [a-zA-Z0-9_]) from the external id. */
    private String buildUsername(String provider, String externalId) {
        String prefix = provider != null && provider.startsWith("wechat") ? "ww_" : "ext_";
        String s = (prefix + externalId).replaceAll("[^a-zA-Z0-9_]", "_");
        if (s.length() > 32) {
            s = s.substring(0, 32);
        }
        while (s.length() < 4) {
            s = s + "0";
        }
        return s;
    }

    private String randomPassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
