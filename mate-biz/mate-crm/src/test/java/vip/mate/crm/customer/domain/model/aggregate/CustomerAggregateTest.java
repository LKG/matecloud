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
package vip.mate.crm.customer.domain.model.aggregate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vip.mate.base.exception.BizException;
import vip.mate.crm.common.CrmCurrentUser;
import vip.mate.crm.customer.domain.model.entity.Customer;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CustomerAggregate pool-flow tests")
class CustomerAggregateTest {

    private final CrmCurrentUser u1 = new CrmCurrentUser("u1", "alice", "张三", "d1");

    private CustomerAggregate privateCustomer() {
        return CustomerAggregate.create(" Acme", "互联网", "A", "REFERRAL", "INITIAL",
                "13900000000", "北京", null, "备注", false, u1, 0, 200);
    }

    private CustomerAggregate publicCustomer() {
        Customer c = Customer.builder()
                .customerName("Beta").poolStatus(CustomerAggregate.POOL_PUBLIC).status("INITIAL").level("C").build();
        c.setId("c1");
        return CustomerAggregate.of(c);
    }

    @Test
    @DisplayName("create to private sea stamps owner + claim time")
    void create_private() {
        CustomerAggregate agg = privateCustomer();
        assertEquals(CustomerAggregate.POOL_PRIVATE, agg.getCustomer().getPoolStatus());
        assertEquals("u1", agg.getCustomer().getOwnerId());
        assertNotNull(agg.getCustomer().getClaimTime());
    }

    @Test
    @DisplayName("create to public sea has no owner")
    void create_public() {
        CustomerAggregate agg = CustomerAggregate.create("Gamma", null, null, null, null,
                null, null, null, null, true, u1, 0, 200);
        assertEquals(CustomerAggregate.POOL_PUBLIC, agg.getCustomer().getPoolStatus());
        assertNull(agg.getCustomer().getOwnerId());
    }

    @Test
    @DisplayName("hold limit enforced on private create")
    void create_hold_limit() {
        assertThrows(BizException.class, () -> CustomerAggregate.create("X", null, null, null, null,
                null, null, null, null, false, u1, 200, 200));
    }

    @Test
    @DisplayName("claim moves public -> private with owner")
    void claim_ok() {
        CustomerAggregate agg = publicCustomer();
        agg.claim(u1, 5, 200);
        assertEquals(CustomerAggregate.POOL_PRIVATE, agg.getCustomer().getPoolStatus());
        assertEquals("u1", agg.getCustomer().getOwnerId());
    }

    @Test
    @DisplayName("claim on already-private customer rejected")
    void claim_conflict() {
        CustomerAggregate agg = privateCustomer();
        assertThrows(BizException.class, () -> agg.claim(u1, 0, 200));
    }

    @Test
    @DisplayName("release clears owner and marks public")
    void release_ok() {
        CustomerAggregate agg = privateCustomer();
        agg.release("手动释放");
        assertEquals(CustomerAggregate.POOL_PUBLIC, agg.getCustomer().getPoolStatus());
        assertNull(agg.getCustomer().getOwnerId());
    }

    @Test
    @DisplayName("release on public customer rejected")
    void release_conflict() {
        CustomerAggregate agg = publicCustomer();
        assertThrows(BizException.class, () -> agg.release("x"));
    }

    @Test
    @DisplayName("transfer reassigns owner (private only)")
    void transfer_ok() {
        CustomerAggregate agg = privateCustomer();
        agg.transfer("u2", "李四", "d2");
        assertEquals("u2", agg.getCustomer().getOwnerId());
        assertThrows(BizException.class, () -> agg.transfer("", "李四", "d2"));
    }

    @Test
    @DisplayName("touchFollow refreshes recent follow time")
    void touchFollow() {
        CustomerAggregate agg = privateCustomer();
        agg.touchFollow(null, null);
        assertNotNull(agg.getCustomer().getRecentFollowTime());
    }
}
