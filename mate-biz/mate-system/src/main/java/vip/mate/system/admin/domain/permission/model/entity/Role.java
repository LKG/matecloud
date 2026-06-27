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
package vip.mate.system.admin.domain.permission.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import vip.mate.base.model.entity.BaseEntity;

import java.util.UUID;

/**
 * Role domain entity.
 *
 * @author mateaix
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Role extends BaseEntity {

    private String roleKey;
    private String roleName;
    private Integer sort;
    private Integer status;
    /** Data scope: 1=ALL 2=DEPT 3=DEPT_AND_CHILD 4=SELF 5=CUSTOM (see DataScope). Default ALL. */
    private Integer dataScope;
    /** Comma-separated dept ids when dataScope=5 (CUSTOM). */
    private String customDeptIds;
    /**
     * Owning tenant id. {@code null} resolves to the system tenant ("1") at the
     * persistence boundary; tenant provisioning stamps the freshly-created tenant
     * onto its admin role so role management stays scoped to that tenant.
     */
    private String tenantId;

    /**
     * Factory method to create a new Role owned by the given tenant. Pass
     * {@code null}/blank for single-tenant deployments — the persistence layer
     * falls back to the system tenant.
     */
    public static Role create(String roleKey, String roleName, Integer sort, String tenantId) {
        return Role.builder()
                .id(UUID.randomUUID().toString().replace("-", ""))
                .roleKey(roleKey)
                .roleName(roleName)
                .sort(sort)
                .status(0)
                .dataScope(1)
                .tenantId(tenantId)
                .build();
    }

    /**
     * Backward-compatible factory: creates a Role with no explicit tenant
     * (resolves to the system tenant downstream).
     */
    public static Role create(String roleKey, String roleName, Integer sort) {
        return create(roleKey, roleName, sort, null);
    }
}
