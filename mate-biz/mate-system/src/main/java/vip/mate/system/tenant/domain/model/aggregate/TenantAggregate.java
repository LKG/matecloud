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
package vip.mate.system.tenant.domain.model.aggregate;

import lombok.Builder;
import lombok.Data;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;
import vip.mate.starter.distribute.util.SnowflakeUtil;
import vip.mate.system.tenant.domain.model.entity.Tenant;
import vip.mate.system.tenant.domain.model.valobj.TenantStatus;

import java.time.LocalDateTime;

/**
 * Aggregate root for the tenant lifecycle. State changes are routed through
 * domain methods so business invariants stay in one place.
 *
 * @author mateaix
 */
@Data
@Builder
public class TenantAggregate {

    private Tenant tenant;

    public static TenantAggregate create(String tenantCode, String tenantName, String packageId,
                                         String contactName, String contactPhone) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "tenantCode is required");
        }
        if (tenantName == null || tenantName.isBlank()) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "tenantName is required");
        }
        if (packageId == null || packageId.isBlank()) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "packageId is required");
        }
        Tenant tenant = Tenant.builder()
                .tenantCode(tenantCode)
                .tenantName(tenantName)
                .packageId(packageId)
                .contactName(contactName)
                .contactPhone(contactPhone)
                .status(TenantStatus.ACTIVE)
                .build();
        tenant.setId(SnowflakeUtil.newSnowflakeId());
        return TenantAggregate.builder().tenant(tenant).build();
    }

    public static TenantAggregate of(Tenant tenant) {
        if (tenant == null) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "tenant cannot be null");
        }
        return TenantAggregate.builder().tenant(tenant).build();
    }

    public String getId() {
        return tenant.getId();
    }

    public void suspend() {
        ensureNotDeleted();
        this.tenant.setStatus(TenantStatus.SUSPENDED);
    }

    public void activate() {
        ensureNotDeleted();
        if (this.tenant.getExpireAt() != null && this.tenant.getExpireAt().isBefore(LocalDateTime.now())) {
            this.tenant.setStatus(TenantStatus.EXPIRED);
            return;
        }
        this.tenant.setStatus(TenantStatus.ACTIVE);
    }

    public void renew(LocalDateTime newExpireAt) {
        ensureNotDeleted();
        if (newExpireAt == null) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "newExpireAt is required");
        }
        this.tenant.setExpireAt(newExpireAt);
        if (this.tenant.getStatus() == TenantStatus.EXPIRED) {
            this.tenant.setStatus(TenantStatus.ACTIVE);
        }
    }

    public void changePackage(String packageId) {
        ensureNotDeleted();
        if (packageId == null || packageId.isBlank()) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "packageId is required");
        }
        this.tenant.setPackageId(packageId);
    }

    public void updateProfile(String tenantName, String contactName, String contactPhone,
                              String contactEmail, String domain, String remark) {
        ensureNotDeleted();
        if (tenantName != null && !tenantName.isBlank()) this.tenant.setTenantName(tenantName);
        if (contactName != null) this.tenant.setContactName(contactName);
        if (contactPhone != null) this.tenant.setContactPhone(contactPhone);
        if (contactEmail != null) this.tenant.setContactEmail(contactEmail);
        if (domain != null) this.tenant.setDomain(domain);
        if (remark != null) this.tenant.setRemark(remark);
    }

    public void delete() {
        this.tenant.setStatus(TenantStatus.DELETED);
    }

    /**
     * Whether the tenant is past its expiry instant.
     *
     * @param now reference instant (passed in for testability)
     * @return true when expireAt is set and {@code now} is strictly after it
     */
    public boolean isExpired(LocalDateTime now) {
        LocalDateTime expireAt = this.tenant.getExpireAt();
        return expireAt != null && now != null && now.isAfter(expireAt);
    }

    /**
     * Whether the tenant is expired but still inside the read-only grace window
     * (per RFC-028: 宽限期 {@code graceDays} 天只读).
     *
     * @param now       reference instant
     * @param graceDays length of the grace window in days after expiry
     * @return true when expired and {@code now} is within {@code graceDays} after expireAt
     */
    public boolean inGracePeriod(LocalDateTime now, int graceDays) {
        if (!isExpired(now)) {
            return false;
        }
        return !now.isAfter(this.tenant.getExpireAt().plusDays(graceDays));
    }

    /**
     * Transition ACTIVE → EXPIRED when the tenant is past due. DELETED tenants
     * are never touched ({@link #ensureNotDeleted()} semantics); non-ACTIVE
     * tenants are left untouched as well.
     *
     * @param now reference instant
     * @return true when the status was flipped to EXPIRED, false otherwise
     */
    public boolean markExpiredIfDue(LocalDateTime now) {
        if (this.tenant.getStatus() == TenantStatus.DELETED) {
            return false;
        }
        if (this.tenant.getStatus() == TenantStatus.ACTIVE && isExpired(now)) {
            this.tenant.setStatus(TenantStatus.EXPIRED);
            return true;
        }
        return false;
    }

    private void ensureNotDeleted() {
        if (this.tenant.getStatus() == TenantStatus.DELETED) {
            throw new BizException(ResponseCode.OPERATION_NOT_ALLOWED.getCode(),
                    "Tenant has been deleted");
        }
    }
}
