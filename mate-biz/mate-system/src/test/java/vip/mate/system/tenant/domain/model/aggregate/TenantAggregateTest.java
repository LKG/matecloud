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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vip.mate.base.exception.BizException;
import vip.mate.system.tenant.domain.model.entity.Tenant;
import vip.mate.system.tenant.domain.model.valobj.TenantStatus;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantAggregate lifecycle tests")
class TenantAggregateTest {

    private TenantAggregate active() {
        Tenant t = Tenant.builder()
                .tenantCode("acme")
                .tenantName("Acme")
                .packageId("2")
                .status(TenantStatus.ACTIVE)
                .build();
        t.setId("1001");
        return TenantAggregate.of(t);
    }

    // ---- create() ----------------------------------------------------------

    @Test
    @DisplayName("create() builds an ACTIVE tenant with a generated id")
    void create_ok() {
        TenantAggregate agg = TenantAggregate.create("acme", "Acme", "2", "Wang", "13900000000");
        assertNotNull(agg.getId());
        assertEquals(TenantStatus.ACTIVE, agg.getTenant().getStatus());
        assertEquals("acme", agg.getTenant().getTenantCode());
    }

    @Test
    @DisplayName("create() rejects blank required fields")
    void create_validatesRequiredFields() {
        assertThrows(BizException.class, () -> TenantAggregate.create("", "Acme", "2", null, null));
        assertThrows(BizException.class, () -> TenantAggregate.create("acme", " ", "2", null, null));
        assertThrows(BizException.class, () -> TenantAggregate.create("acme", "Acme", "", null, null));
    }

    // ---- suspend / activate / renew / delete -------------------------------

    @Test
    @DisplayName("suspend() then activate() returns to ACTIVE when not expired")
    void suspendThenActivate() {
        TenantAggregate agg = active();
        agg.suspend();
        assertEquals(TenantStatus.SUSPENDED, agg.getTenant().getStatus());
        agg.activate();
        assertEquals(TenantStatus.ACTIVE, agg.getTenant().getStatus());
    }

    @Test
    @DisplayName("activate() flips to EXPIRED when expireAt is already past")
    void activate_pastExpiry_marksExpired() {
        TenantAggregate agg = active();
        agg.getTenant().setExpireAt(LocalDateTime.now().minusDays(1));
        agg.activate();
        assertEquals(TenantStatus.EXPIRED, agg.getTenant().getStatus());
    }

    @Test
    @DisplayName("renew() extends expiry and reactivates an EXPIRED tenant")
    void renew_reactivatesExpired() {
        TenantAggregate agg = active();
        agg.getTenant().setStatus(TenantStatus.EXPIRED);
        LocalDateTime future = LocalDateTime.now().plusDays(30);
        agg.renew(future);
        assertEquals(future, agg.getTenant().getExpireAt());
        assertEquals(TenantStatus.ACTIVE, agg.getTenant().getStatus());
    }

    @Test
    @DisplayName("renew() requires a non-null expiry")
    void renew_requiresExpiry() {
        assertThrows(BizException.class, () -> active().renew(null));
    }

    @Test
    @DisplayName("operations on a DELETED tenant are rejected")
    void deletedTenant_isGuarded() {
        TenantAggregate agg = active();
        agg.delete();
        assertEquals(TenantStatus.DELETED, agg.getTenant().getStatus());
        assertThrows(BizException.class, agg::suspend);
        assertThrows(BizException.class, agg::activate);
        assertThrows(BizException.class, () -> agg.changePackage("3"));
    }

    // ---- expiry lifecycle (RFC-028) ----------------------------------------

    @Test
    @DisplayName("isExpired() reflects the expireAt boundary")
    void isExpired() {
        TenantAggregate agg = active();
        LocalDateTime now = LocalDateTime.of(2026, 6, 2, 0, 0);
        assertFalse(agg.isExpired(now), "no expiry set ⇒ never expired");

        agg.getTenant().setExpireAt(now.minusSeconds(1));
        assertTrue(agg.isExpired(now));

        agg.getTenant().setExpireAt(now.plusSeconds(1));
        assertFalse(agg.isExpired(now));
    }

    @Test
    @DisplayName("inGracePeriod() is true only while expired and within the window")
    void inGracePeriod() {
        TenantAggregate agg = active();
        LocalDateTime expiry = LocalDateTime.of(2026, 6, 1, 0, 0);
        agg.getTenant().setExpireAt(expiry);

        assertFalse(agg.inGracePeriod(expiry.minusDays(1), 7), "not yet expired ⇒ not in grace");
        assertTrue(agg.inGracePeriod(expiry.plusDays(3), 7), "3 days after expiry, 7-day grace");
        assertTrue(agg.inGracePeriod(expiry.plusDays(7), 7), "exactly at the grace boundary");
        assertFalse(agg.inGracePeriod(expiry.plusDays(8), 7), "past the grace window");
    }

    @Test
    @DisplayName("markExpiredIfDue() flips ACTIVE→EXPIRED only when overdue")
    void markExpiredIfDue() {
        TenantAggregate agg = active();
        LocalDateTime now = LocalDateTime.of(2026, 6, 2, 0, 0);

        agg.getTenant().setExpireAt(now.plusDays(1));
        assertFalse(agg.markExpiredIfDue(now), "future expiry ⇒ no change");
        assertEquals(TenantStatus.ACTIVE, agg.getTenant().getStatus());

        agg.getTenant().setExpireAt(now.minusDays(1));
        assertTrue(agg.markExpiredIfDue(now), "overdue ⇒ flipped");
        assertEquals(TenantStatus.EXPIRED, agg.getTenant().getStatus());

        assertFalse(agg.markExpiredIfDue(now), "already EXPIRED ⇒ no second flip");
    }

    @Test
    @DisplayName("markExpiredIfDue() never touches SUSPENDED or DELETED tenants")
    void markExpiredIfDue_skipsNonActive() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 2, 0, 0);

        TenantAggregate suspended = active();
        suspended.getTenant().setExpireAt(now.minusDays(1));
        suspended.suspend();
        assertFalse(suspended.markExpiredIfDue(now));
        assertEquals(TenantStatus.SUSPENDED, suspended.getTenant().getStatus());

        TenantAggregate deleted = active();
        deleted.getTenant().setExpireAt(now.minusDays(1));
        deleted.delete();
        assertFalse(deleted.markExpiredIfDue(now));
        assertEquals(TenantStatus.DELETED, deleted.getTenant().getStatus());
    }
}
