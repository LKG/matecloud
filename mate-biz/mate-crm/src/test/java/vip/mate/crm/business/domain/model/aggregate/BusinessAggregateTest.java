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
package vip.mate.crm.business.domain.model.aggregate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vip.mate.base.exception.BizException;
import vip.mate.crm.business.domain.model.entity.BusinessProduct;
import vip.mate.crm.business.types.enums.BusinessStage;
import vip.mate.crm.common.CrmCurrentUser;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BusinessAggregate stage funnel tests")
class BusinessAggregateTest {

    private final CrmCurrentUser u1 = new CrmCurrentUser("u1", "alice", "张三", "d1");

    private BusinessAggregate fresh() {
        return BusinessAggregate.create("智慧园区项目", "c1", "Acme", "李四", "REFERRAL",
                50, null, "推进方案", "备注",
                List.of(BusinessProduct.of("p1", "产品A", new BigDecimal("100"), new BigDecimal("2"))), u1);
    }

    @Test
    @DisplayName("create starts INITIAL with money = product subtotal")
    void create_ok() {
        BusinessAggregate agg = fresh();
        assertEquals(BusinessStage.INITIAL.name(), agg.getBusiness().getStage());
        assertEquals(0, new BigDecimal("200").compareTo(agg.getBusiness().getMoney()));
    }

    @Test
    @DisplayName("advance moves one step along the funnel")
    void advance_sequential() {
        BusinessAggregate agg = fresh();
        agg.advance();
        assertEquals(BusinessStage.DEMAND.name(), agg.getBusiness().getStage());
        agg.advance();
        assertEquals(BusinessStage.PLAN.name(), agg.getBusiness().getStage());
        agg.advance();
        assertEquals(BusinessStage.NEGOTIATION.name(), agg.getBusiness().getStage());
    }

    @Test
    @DisplayName("win only from NEGOTIATION")
    void win_gate() {
        BusinessAggregate agg = fresh();
        assertThrows(BizException.class, () -> agg.win("过早"));
        agg.advance();
        agg.advance();
        agg.advance();
        agg.win("中标");
        assertEquals(BusinessStage.WIN.name(), agg.getBusiness().getStage());
        assertEquals(0, agg.getBusiness().getStatusFlag());
    }

    @Test
    @DisplayName("skip level rejected by advanceTo")
    void advanceTo_no_skip() {
        BusinessAggregate agg = fresh();
        assertThrows(BizException.class, () -> agg.advanceTo("NEGOTIATION"));
        agg.advanceTo("DEMAND");
        assertEquals(BusinessStage.DEMAND.name(), agg.getBusiness().getStage());
    }

    @Test
    @DisplayName("lose allowed from any active stage, terminal afterwards")
    void lose_terminal() {
        BusinessAggregate agg = fresh();
        agg.lose("价格不合");
        assertEquals(BusinessStage.LOSE.name(), agg.getBusiness().getStage());
        assertThrows(BizException.class, agg::advance);
        assertThrows(BizException.class, () -> agg.lose("again"));
    }

    @Test
    @DisplayName("terminal business cannot be edited")
    void terminal_no_edit() {
        BusinessAggregate agg = fresh();
        agg.advance();
        agg.advance();
        agg.advance();
        agg.win("done");
        assertThrows(BizException.class, () -> agg.updateInfo("x", null, null, null, null, null, null, null));
    }
}
