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
package vip.mate.crm.aftersale.domain.model.aggregate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vip.mate.base.exception.BizException;
import vip.mate.crm.aftersale.types.enums.AfterSaleStatus;
import vip.mate.crm.common.CrmCurrentUser;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AfterSaleAggregate ticket status machine tests")
class AfterSaleAggregateTest {

    private final CrmCurrentUser agent = new CrmCurrentUser("u1", "alice", "客服", "d1");
    private final CrmCurrentUser handler = new CrmCurrentUser("u2", "bob", "工程师", "d1");

    private AfterSaleAggregate opened() {
        return AfterSaleAggregate.create("设备故障", "c1", "Acme", "ct1", "REPAIR", "HIGH", "无法开机", agent);
    }

    @Test
    @DisplayName("create: OPEN with create log")
    void create_ok() {
        AfterSaleAggregate agg = opened();
        assertEquals(AfterSaleStatus.OPEN.name(), agg.getAfterSale().getStatus());
        assertNotNull(agg.getAfterSale().getTicketNo());
        assertEquals(1, agg.getLogs().size());
    }

    @Test
    @DisplayName("happy path: accept -> process -> resolve -> rate(closes)")
    void lifecycle() {
        AfterSaleAggregate agg = opened();
        agg.accept(handler);
        assertEquals(AfterSaleStatus.PROCESSING.name(), agg.getAfterSale().getStatus());
        agg.process("远程排查", handler);
        agg.resolve("更换电源", handler);
        assertEquals(AfterSaleStatus.RESOLVED.name(), agg.getAfterSale().getStatus());
        agg.rate(5, "很满意", agent);
        assertEquals(AfterSaleStatus.CLOSED.name(), agg.getAfterSale().getStatus());
        assertEquals(5, agg.getLogs().size());
    }

    @Test
    @DisplayName("process requires PROCESSING (cannot before accept)")
    void process_gate() {
        AfterSaleAggregate agg = opened();
        assertThrows(BizException.class, () -> agg.process("跳过受理", handler));
    }

    @Test
    @DisplayName("rate only from RESOLVED")
    void rate_gate() {
        AfterSaleAggregate agg = opened();
        agg.accept(handler);
        assertThrows(BizException.class, () -> agg.rate(5, null, agent));
    }

    @Test
    @DisplayName("rating must be 1-5")
    void rating_range() {
        AfterSaleAggregate agg = opened();
        agg.accept(handler);
        agg.resolve("好了", handler);
        assertThrows(BizException.class, () -> agg.rate(6, null, agent));
    }

    @Test
    @DisplayName("no operation after closed")
    void closed_gate() {
        AfterSaleAggregate agg = opened();
        agg.close("直接关闭", agent);
        assertThrows(BizException.class, () -> agg.accept(handler));
        assertThrows(BizException.class, () -> agg.close("再关", agent));
    }
}
