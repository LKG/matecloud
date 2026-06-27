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

import java.time.LocalDateTime;

/**
 * Raised when a tenant is swept from ACTIVE to EXPIRED by the lifecycle job.
 *
 * @author mateaix
 */
@Getter
public class TenantExpiredEvent extends BaseDomainEvent {

    private final String tenantCode;
    private final LocalDateTime expireAt;

    public TenantExpiredEvent(String aggregateId, String tenantCode, LocalDateTime expireAt) {
        super(aggregateId, "TenantAggregate");
        this.tenantCode = tenantCode;
        this.expireAt = expireAt;
    }

    @Override
    public String getEventType() { return "TENANT_EXPIRED"; }

    @Override
    public String getDescription() {
        return String.format("Tenant expired: %s (expireAt=%s)", tenantCode, expireAt);
    }
}
