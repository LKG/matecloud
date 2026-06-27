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
package vip.mate.system.tenant.domain.event;

import lombok.Getter;
import vip.mate.system.domain.event.BaseDomainEvent;

/**
 * Raised once a freshly-created tenant has been provisioned with its admin
 * role, admin account and default menu grants — i.e. the tenant is usable and
 * someone can log in. Published synchronously inside the tenant-create
 * transaction so downstream listeners observe a fully-committed tenant.
 *
 * @author mateaix
 */
@Getter
public class TenantProvisionedEvent extends BaseDomainEvent {

    private final String tenantId;
    private final String tenantCode;
    private final String adminUsername;
    private final String adminId;
    private final String roleId;

    public TenantProvisionedEvent(String tenantId, String tenantCode,
                                  String adminUsername, String adminId, String roleId) {
        super(tenantId, "TenantAggregate");
        this.tenantId = tenantId;
        this.tenantCode = tenantCode;
        this.adminUsername = adminUsername;
        this.adminId = adminId;
        this.roleId = roleId;
    }

    @Override
    public String getEventType() { return "TENANT_PROVISIONED"; }

    @Override
    public String getDescription() {
        return String.format("Tenant provisioned: %s (admin=%s)", tenantCode, adminUsername);
    }
}
