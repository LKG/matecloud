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
package vip.mate.system.tenant.application.quota;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import vip.mate.base.exception.BizException;
import vip.mate.starter.tenant.core.TenantHelper;
import vip.mate.starter.tenant.core.TenantProperties;
import vip.mate.system.infrastructure.dao.UserDao;
import vip.mate.system.tenant.domain.adapter.repository.TenantRepository;
import vip.mate.system.tenant.domain.model.aggregate.TenantAggregate;
import vip.mate.system.tenant.domain.model.entity.TenantPackage;
import vip.mate.system.tenant.domain.model.valobj.TenantStatus;
import vip.mate.system.tenant.types.exception.TenantErrorCode;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Platform-side (mate-system local) tenant quota enforcement (RFC-028).
 *
 * <p>Every assert/check method is a no-op when enforcement is inactive
 * ({@link #isEnforcementActive(String)}): the tenant master switch is off, the
 * tenant id is blank, or the tenant is the configured super tenant (which
 * bypasses all quotas). This keeps single-tenant / dev deployments unaffected.
 *
 * <p>Scope is deliberately local — no gateway/reactive-Dubbo coupling. The AI
 * hook ({@link #recordAndCheckAiCall(String)}) is reusable but not wired into
 * mate-ai here.
 *
 * @author mateaix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantQuotaService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final TenantRepository tenantRepository;
    private final UserDao userDao;
    private final RedissonClient redissonClient;
    private final TenantProperties tenantProperties;

    /**
     * Whether quota enforcement should run for the given tenant. False when the
     * tenant chain is disabled, the id is blank, or the id is the super tenant.
     */
    public boolean isEnforcementActive(String tenantId) {
        if (!tenantProperties.isEnabled()) {
            return false;
        }
        if (tenantId == null || tenantId.isBlank()) {
            return false;
        }
        String superTenant = tenantProperties.getSuperTenantId();
        return superTenant == null || superTenant.isBlank() || !superTenant.equals(tenantId);
    }

    // ==================== user quota ====================

    /**
     * Reject when the tenant has reached its package's {@code maxUsers} cap.
     * No-op when enforcement is inactive or {@code maxUsers <= 0} (unlimited).
     * The count runs with the tenant-line filter disabled so the explicit
     * {@code WHERE tenant_id = ?} is the only scope (avoids double-filtering /
     * fail-closed behaviour when the row-level interceptor is active).
     */
    public void assertCanAddUser(String tenantId) {
        if (!isEnforcementActive(tenantId)) {
            return;
        }
        TenantPackage pkg = resolvePackage(tenantId);
        Integer maxUsers = pkg.getMaxUsers();
        if (maxUsers == null || maxUsers <= 0) {
            return; // unlimited
        }
        long current = TenantHelper.withoutTenant(() -> userDao.countByTenantId(tenantId));
        if (current >= maxUsers) {
            throw new BizException(TenantErrorCode.USER_QUOTA_EXCEEDED);
        }
    }

    // ==================== feature gate ====================

    /**
     * Whether the tenant's package carries the given feature flag. Returns true
     * when enforcement is inactive (no gate in single-tenant/dev mode).
     */
    public boolean hasFeature(String tenantId, String feature) {
        if (!isEnforcementActive(tenantId)) {
            return true;
        }
        return resolvePackage(tenantId).hasFeature(feature);
    }

    /** Reject when the tenant's package lacks the given feature. No-op when inactive. */
    public void assertFeature(String tenantId, String feature) {
        if (!isEnforcementActive(tenantId)) {
            return;
        }
        if (!resolvePackage(tenantId).hasFeature(feature)) {
            throw new BizException(TenantErrorCode.FEATURE_NOT_AVAILABLE.getCode(),
                    TenantErrorCode.FEATURE_NOT_AVAILABLE.getMessage() + ": " + feature);
        }
    }

    // ==================== status gate ====================

    /**
     * Hard gate on tenant status. SUSPENDED/DELETED are rejected outright.
     * Expired tenants (status EXPIRED, or ACTIVE-but-past-expiry) are allowed
     * only inside the read-only grace window; past it they must renew.
     * No-op when enforcement is inactive.
     */
    public void assertActiveTenant(String tenantId) {
        if (!isEnforcementActive(tenantId)) {
            return;
        }
        TenantAggregate aggregate = mustFindTenant(tenantId);
        TenantStatus status = aggregate.getTenant().getStatus();
        if (status == TenantStatus.SUSPENDED) {
            throw new BizException(TenantErrorCode.TENANT_SUSPENDED);
        }
        if (status == TenantStatus.DELETED) {
            throw new BizException(TenantErrorCode.TENANT_DELETED);
        }
        LocalDateTime now = LocalDateTime.now();
        boolean expired = status == TenantStatus.EXPIRED || aggregate.isExpired(now);
        if (expired && !aggregate.inGracePeriod(now, tenantProperties.getGraceDays())) {
            throw new BizException(TenantErrorCode.TENANT_EXPIRED);
        }
    }

    // ==================== AI quota ====================

    /**
     * Record one AI call for the tenant and enforce the daily cap. Returns the
     * new running count for today. No-op (returns 0) when enforcement inactive.
     * Throws when the package has no AI, or the daily cap is exceeded.
     */
    public long recordAndCheckAiCall(String tenantId) {
        if (!isEnforcementActive(tenantId)) {
            return 0L;
        }
        TenantPackage pkg = resolvePackage(tenantId);
        if (!pkg.isAiEnabled()) {
            throw new BizException(TenantErrorCode.AI_NOT_AVAILABLE);
        }
        String key = "tenant:quota:ai:" + tenantId + ":" + LocalDate.now().format(DAY);
        RAtomicLong counter = redissonClient.getAtomicLong(key);
        long count = counter.incrementAndGet();
        if (count == 1L) {
            // First increment of the day — bound the key's lifetime so stale
            // day-buckets self-evict (~2 days covers clock skew / TZ edges).
            counter.expire(Duration.ofDays(2));
        }
        if (!pkg.isAiQuotaUnlimited() && count > pkg.getAiQuotaDaily()) {
            throw new BizException(TenantErrorCode.AI_QUOTA_EXCEEDED);
        }
        return count;
    }

    // ==================== helpers ====================

    private TenantPackage resolvePackage(String tenantId) {
        TenantAggregate aggregate = mustFindTenant(tenantId);
        String packageId = aggregate.getTenant().getPackageId();
        TenantPackage pkg = packageId == null ? null : tenantRepository.findPackageById(packageId);
        if (pkg == null) {
            throw new BizException(TenantErrorCode.PACKAGE_NOT_FOUND.getCode(),
                    TenantErrorCode.PACKAGE_NOT_FOUND.getMessage() + ": " + packageId);
        }
        return pkg;
    }

    private TenantAggregate mustFindTenant(String tenantId) {
        TenantAggregate aggregate = tenantRepository.findById(tenantId);
        if (aggregate == null) {
            throw new BizException(TenantErrorCode.TENANT_NOT_FOUND.getCode(),
                    TenantErrorCode.TENANT_NOT_FOUND.getMessage() + ": " + tenantId);
        }
        return aggregate;
    }
}
