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
import vip.mate.system.admin.domain.permission.model.valobj.AdminStatus;
import vip.mate.base.model.entity.BaseEntity;

import java.util.UUID;

/**
 * Admin domain entity.
 *
 * @author mateaix
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Admin extends BaseEntity {

    private String username;
    private String password;
    private String mobile;
    private String email;
    private String nickName;
    private String realName;
    private String avatar;
    private AdminStatus status;
    /** Owning department id — anchors data-permission (data scope) filtering. */
    private String deptId;
    /**
     * Owning tenant id. {@code null} resolves to the system tenant ("1") at the
     * persistence boundary; the auth path copies it into the Sa-Token session so
     * every downstream request carries the right tenant.
     */
    private String tenantId;

    /**
     * Factory method to create a new Admin with ACTIVE status, owned by the
     * given tenant. Pass {@code null}/blank for single-tenant deployments —
     * the persistence layer falls back to the system tenant.
     */
    public static Admin create(String username, String encodedPassword, String nickName, String tenantId) {
        return Admin.builder()
                .id(UUID.randomUUID().toString().replace("-", ""))
                .username(username)
                .password(encodedPassword)
                .nickName(nickName)
                .status(AdminStatus.ACTIVE)
                .tenantId(tenantId)
                .build();
    }

    /**
     * Backward-compatible factory: creates an Admin with no explicit tenant
     * (resolves to the system tenant downstream).
     */
    public static Admin create(String username, String encodedPassword, String nickName) {
        return create(username, encodedPassword, nickName, null);
    }

    /**
     * Change password. Caller must pass a BCrypt-encoded value.
     */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void disable() {
        this.status = AdminStatus.DISABLED;
    }

    public void enable() {
        this.status = AdminStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == AdminStatus.ACTIVE;
    }

    public boolean isDisabled() {
        return status == AdminStatus.DISABLED;
    }
}
