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
package vip.mate.crm.trade.domain.model.aggregate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vip.mate.base.exception.BizException;
import vip.mate.crm.common.CrmCurrentUser;
import vip.mate.crm.trade.domain.model.entity.ContractProduct;
import vip.mate.crm.trade.types.enums.ContractStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ContractAggregate money invariant + status machine tests")
class ContractAggregateTest {

    private final CrmCurrentUser u1 = new CrmCurrentUser("u1", "alice", "张三", "d1");

    private ContractAggregate fresh() {
        // 100 * 2 * 0.9 = 180.00
        List<ContractProduct> lines = List.of(
                ContractProduct.of("p1", "产品A", new BigDecimal("100"), new BigDecimal("2"), new BigDecimal("0.9")));
        return ContractAggregate.create("园区合同", "HT001", "c1", "Acme", "b1", "李四",
                null, null, null, "备注", null, lines, u1);
    }

    @Test
    @DisplayName("create: money = discounted subtotal sum, status DRAFT")
    void create_ok() {
        ContractAggregate agg = fresh();
        assertEquals(0, new BigDecimal("180.00").compareTo(agg.getContract().getMoney()));
        assertEquals(ContractStatus.DRAFT.name(), agg.getContract().getStatus());
    }

    @Test
    @DisplayName("submit -> approve -> archive happy path")
    void lifecycle() {
        ContractAggregate agg = fresh();
        agg.submit();
        assertEquals(ContractStatus.PENDING.name(), agg.getContract().getStatus());
        agg.approve();
        assertEquals(ContractStatus.APPROVED.name(), agg.getContract().getStatus());
        agg.archive();
        assertEquals(ContractStatus.ARCHIVED.name(), agg.getContract().getStatus());
    }

    @Test
    @DisplayName("approve only from PENDING")
    void approve_gate() {
        ContractAggregate agg = fresh();
        assertThrows(BizException.class, agg::approve);
    }

    @Test
    @DisplayName("edit locked once submitted")
    void edit_locked_after_submit() {
        ContractAggregate agg = fresh();
        agg.submit();
        assertThrows(BizException.class,
                () -> agg.updateInfo("x", null, null, null, null, null, null, List.of()));
    }

    @Test
    @DisplayName("addReceived accumulates and cannot exceed money")
    void received_invariant() {
        ContractAggregate agg = fresh();
        agg.addReceived(new BigDecimal("80"));
        agg.addReceived(new BigDecimal("100"));
        assertEquals(0, new BigDecimal("180").compareTo(agg.getContract().getReceivedMoney()));
        assertEquals(0, BigDecimal.ZERO.compareTo(agg.receivable()));
        assertThrows(BizException.class, () -> agg.addReceived(new BigDecimal("1")));
        agg.reduceReceived(new BigDecimal("30"));
        assertEquals(0, new BigDecimal("150").compareTo(agg.getContract().getReceivedMoney()));
    }
}
