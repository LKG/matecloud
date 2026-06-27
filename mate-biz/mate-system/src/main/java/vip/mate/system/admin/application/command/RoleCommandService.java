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
package vip.mate.system.admin.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.system.admin.domain.permission.adapter.repository.RoleRepository;
import vip.mate.system.admin.domain.permission.model.aggregate.RoleAggregate;
import vip.mate.system.admin.domain.permission.model.entity.Role;
import vip.mate.system.admin.types.exception.AdminErrorCode;
import vip.mate.base.exception.BizException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleCommandService {

    private final RoleRepository roleRepository;

    @Transactional
    public String createRole(String roleKey, String roleName, Integer sort) {
        if (roleRepository.existsByKey(roleKey)) {
            throw new BizException(AdminErrorCode.DUPLICATE_ROLE_KEY.getCode(), "角色标识已存在: " + roleKey);
        }
        Role role = Role.create(roleKey, roleName, sort);
        roleRepository.save(RoleAggregate.builder().role(role).build());
        return role.getId();
    }

    /**
     * Create a role explicitly owned by {@code tenantId}. Used by tenant
     * provisioning, which runs in the platform (super-tenant) context but must
     * stamp the freshly-created tenant onto its admin role so role management
     * stays scoped to that tenant.
     */
    @Transactional
    public String createRoleForTenant(String roleKey, String roleName, Integer sort, String tenantId) {
        if (roleRepository.existsByKey(roleKey)) {
            throw new BizException(AdminErrorCode.DUPLICATE_ROLE_KEY.getCode(), "角色标识已存在: " + roleKey);
        }
        Role role = Role.create(roleKey, roleName, sort, tenantId);
        roleRepository.save(RoleAggregate.builder().role(role).build());
        return role.getId();
    }

    @Transactional
    public void updateRole(String id, String roleName, Integer sort) {
        RoleAggregate agg = findOrThrow(id);
        agg.getRole().setRoleName(roleName);
        agg.getRole().setSort(sort);
        roleRepository.update(agg);
    }

    @Transactional
    public void updateDataScope(String id, Integer dataScope, String customDeptIds) {
        RoleAggregate agg = findOrThrow(id);
        agg.getRole().setDataScope(dataScope == null ? 1 : dataScope);
        // customDeptIds only meaningful for CUSTOM(5); clear otherwise.
        agg.getRole().setCustomDeptIds(dataScope != null && dataScope == 5 ? customDeptIds : null);
        roleRepository.update(agg);
    }

    @Transactional
    public void assignMenus(String roleId, List<String> menuIds) {
        findOrThrow(roleId);
        roleRepository.assignMenus(roleId, menuIds);
    }

    @Transactional
    public void deleteRole(String id) {
        findOrThrow(id);
        // Clean up role-menu associations before deleting
        roleRepository.assignMenus(id, List.of());
        roleRepository.deleteById(id);
    }

    private RoleAggregate findOrThrow(String id) {
        RoleAggregate agg = roleRepository.findById(id);
        if (agg == null) throw BizException.of(AdminErrorCode.ROLE_NOT_EXIST);
        return agg;
    }
}
