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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import vip.mate.base.exception.BizException;
import vip.mate.starter.tenant.core.TenantProperties;
import vip.mate.system.infrastructure.dao.UserDao;
import vip.mate.system.tenant.domain.adapter.repository.TenantRepository;
import vip.mate.system.tenant.domain.model.aggregate.TenantAggregate;
import vip.mate.system.tenant.domain.model.entity.Tenant;
import vip.mate.system.tenant.domain.model.entity.TenantPackage;
import vip.mate.system.tenant.domain.model.valobj.TenantStatus;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantQuotaService tests")
class TenantQuotaServiceTest {

    private static final String TENANT_ID = "1001";
    private static final String SUPER_TENANT = "1";
    private static final String PACKAGE_ID = "2";

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserDao userDao;
    @Mock
    private RedissonClient redissonClient;

    private TenantProperties tenantProperties;
    private TenantQuotaService service;

    @BeforeEach
    void setUp() {
        tenantProperties = new TenantProperties();
        tenantProperties.setEnabled(true);
        tenantProperties.setSuperTenantId(SUPER_TENANT);
        tenantProperties.setGraceDays(7);
        service = new TenantQuotaService(tenantRepository, userDao, redissonClient, tenantProperties);
    }

    // ---- fixtures ----------------------------------------------------------

    private TenantAggregate tenant(TenantStatus status, LocalDateTime expireAt) {
        Tenant t = Tenant.builder()
                .tenantCode("acme")
                .tenantName("Acme")
                .packageId(PACKAGE_ID)
                .status(status)
                .expireAt(expireAt)
                .build();
        t.setId(TENANT_ID);
        return TenantAggregate.of(t);
    }

    private TenantPackage pkg(int maxUsers, String features, int aiEnabled, int aiQuotaDaily) {
        return TenantPackage.builder()
                .id(PACKAGE_ID)
                .packageCode("PRO")
                .packageName("Professional")
                .maxUsers(maxUsers)
                .features(features)
                .aiEnabled(aiEnabled)
                .aiQuotaDaily(aiQuotaDaily)
                .build();
    }

    private void stubTenantAndPackage(TenantAggregate agg, TenantPackage pkg) {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(agg);
        when(tenantRepository.findPackageById(PACKAGE_ID)).thenReturn(pkg);
    }

    // ==================== maxUsers ====================

    @Nested
    @DisplayName("assertCanAddUser")
    class AddUser {

        @Test
        @DisplayName("under the limit → ok")
        void underLimit_ok() {
            stubTenantAndPackage(tenant(TenantStatus.ACTIVE, null), pkg(10, "core", 0, 0));
            when(userDao.countByTenantId(TENANT_ID)).thenReturn(5L);
            assertDoesNotThrow(() -> service.assertCanAddUser(TENANT_ID));
        }

        @Test
        @DisplayName("at/over the limit → BizException")
        void atLimit_throws() {
            stubTenantAndPackage(tenant(TenantStatus.ACTIVE, null), pkg(10, "core", 0, 0));
            when(userDao.countByTenantId(TENANT_ID)).thenReturn(10L);
            assertThrows(BizException.class, () -> service.assertCanAddUser(TENANT_ID));
        }

        @Test
        @DisplayName("maxUsers = 0 (unlimited) → ok without counting")
        void unlimited_ok() {
            stubTenantAndPackage(tenant(TenantStatus.ACTIVE, null), pkg(0, "core", 0, 0));
            assertDoesNotThrow(() -> service.assertCanAddUser(TENANT_ID));
            verify(userDao, never()).countByTenantId(anyString());
        }

        @Test
        @DisplayName("super tenant → ok, no lookups")
        void superTenant_ok() {
            assertDoesNotThrow(() -> service.assertCanAddUser(SUPER_TENANT));
            verifyNoInteractions(tenantRepository, userDao);
        }

        @Test
        @DisplayName("enforcement disabled → ok, no lookups")
        void disabled_ok() {
            tenantProperties.setEnabled(false);
            assertDoesNotThrow(() -> service.assertCanAddUser(TENANT_ID));
            verifyNoInteractions(tenantRepository, userDao);
        }

        @Test
        @DisplayName("blank tenant → ok, no lookups")
        void blankTenant_ok() {
            assertDoesNotThrow(() -> service.assertCanAddUser("  "));
            verifyNoInteractions(tenantRepository, userDao);
        }
    }

    // ==================== feature ====================

    @Nested
    @DisplayName("hasFeature / assertFeature")
    class Feature {

        @Test
        @DisplayName("feature present → hasFeature true, assertFeature ok")
        void present() {
            stubTenantAndPackage(tenant(TenantStatus.ACTIVE, null), pkg(10, "core,audit", 0, 0));
            assertTrue(service.hasFeature(TENANT_ID, "audit"));
            assertDoesNotThrow(() -> service.assertFeature(TENANT_ID, "audit"));
        }

        @Test
        @DisplayName("feature missing → hasFeature false, assertFeature throws")
        void missing() {
            stubTenantAndPackage(tenant(TenantStatus.ACTIVE, null), pkg(10, "core", 0, 0));
            assertFalse(service.hasFeature(TENANT_ID, "sso"));
            assertThrows(BizException.class, () -> service.assertFeature(TENANT_ID, "sso"));
        }

        @Test
        @DisplayName("enforcement inactive → hasFeature true, assertFeature no-op")
        void inactive() {
            tenantProperties.setEnabled(false);
            assertTrue(service.hasFeature(TENANT_ID, "anything"));
            assertDoesNotThrow(() -> service.assertFeature(TENANT_ID, "anything"));
            verifyNoInteractions(tenantRepository);
        }
    }

    // ==================== status gate ====================

    @Nested
    @DisplayName("assertActiveTenant")
    class StatusGate {

        @Test
        @DisplayName("ACTIVE not expired → ok")
        void active_ok() {
            when(tenantRepository.findById(TENANT_ID))
                    .thenReturn(tenant(TenantStatus.ACTIVE, LocalDateTime.now().plusDays(30)));
            assertDoesNotThrow(() -> service.assertActiveTenant(TENANT_ID));
        }

        @Test
        @DisplayName("SUSPENDED → throws")
        void suspended_throws() {
            when(tenantRepository.findById(TENANT_ID))
                    .thenReturn(tenant(TenantStatus.SUSPENDED, null));
            assertThrows(BizException.class, () -> service.assertActiveTenant(TENANT_ID));
        }

        @Test
        @DisplayName("DELETED → throws")
        void deleted_throws() {
            when(tenantRepository.findById(TENANT_ID))
                    .thenReturn(tenant(TenantStatus.DELETED, null));
            assertThrows(BizException.class, () -> service.assertActiveTenant(TENANT_ID));
        }

        @Test
        @DisplayName("EXPIRED within grace → ok")
        void expiredWithinGrace_ok() {
            when(tenantRepository.findById(TENANT_ID))
                    .thenReturn(tenant(TenantStatus.EXPIRED, LocalDateTime.now().minusDays(3)));
            assertDoesNotThrow(() -> service.assertActiveTenant(TENANT_ID));
        }

        @Test
        @DisplayName("EXPIRED past grace → throws")
        void expiredPastGrace_throws() {
            when(tenantRepository.findById(TENANT_ID))
                    .thenReturn(tenant(TenantStatus.EXPIRED, LocalDateTime.now().minusDays(30)));
            assertThrows(BizException.class, () -> service.assertActiveTenant(TENANT_ID));
        }

        @Test
        @DisplayName("ACTIVE but past expiry, past grace → throws")
        void activeButExpiredPastGrace_throws() {
            when(tenantRepository.findById(TENANT_ID))
                    .thenReturn(tenant(TenantStatus.ACTIVE, LocalDateTime.now().minusDays(30)));
            assertThrows(BizException.class, () -> service.assertActiveTenant(TENANT_ID));
        }
    }

    // ==================== AI quota ====================

    @Nested
    @DisplayName("recordAndCheckAiCall")
    class AiQuota {

        @Mock
        private RAtomicLong counter;

        private void stubCounter(long afterIncrement) {
            when(redissonClient.getAtomicLong(anyString())).thenReturn(counter);
            when(counter.incrementAndGet()).thenReturn(afterIncrement);
        }

        @Test
        @DisplayName("under quota → ok, returns incremented count")
        void underQuota_ok() {
            stubTenantAndPackage(tenant(TenantStatus.ACTIVE, null), pkg(10, "core,ai", 1, 500));
            stubCounter(42L);
            assertEquals(42L, service.recordAndCheckAiCall(TENANT_ID));
        }

        @Test
        @DisplayName("over quota → throws")
        void overQuota_throws() {
            stubTenantAndPackage(tenant(TenantStatus.ACTIVE, null), pkg(10, "core,ai", 1, 500));
            stubCounter(501L);
            assertThrows(BizException.class, () -> service.recordAndCheckAiCall(TENANT_ID));
        }

        @Test
        @DisplayName("ai disabled → throws, no counter touched")
        void aiDisabled_throws() {
            stubTenantAndPackage(tenant(TenantStatus.ACTIVE, null), pkg(10, "core", 0, 0));
            assertThrows(BizException.class, () -> service.recordAndCheckAiCall(TENANT_ID));
            verifyNoInteractions(redissonClient);
        }

        @Test
        @DisplayName("unlimited (ai_quota_daily = 0) with ai enabled → ok")
        void unlimited_ok() {
            stubTenantAndPackage(tenant(TenantStatus.ACTIVE, null), pkg(0, "core,ai", 1, 0));
            stubCounter(99999L);
            assertEquals(99999L, service.recordAndCheckAiCall(TENANT_ID));
        }

        @Test
        @DisplayName("enforcement inactive → returns 0, no work")
        void inactive_returnsZero() {
            tenantProperties.setEnabled(false);
            assertEquals(0L, service.recordAndCheckAiCall(TENANT_ID));
            verifyNoInteractions(redissonClient, tenantRepository);
        }
    }
}
