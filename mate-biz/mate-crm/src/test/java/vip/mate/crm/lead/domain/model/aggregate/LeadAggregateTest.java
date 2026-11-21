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
package vip.mate.crm.lead.domain.model.aggregate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vip.mate.base.exception.BizException;
import vip.mate.crm.lead.domain.model.entity.Lead;
import vip.mate.crm.lead.types.enums.LeadStatus;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LeadAggregate lifecycle tests")
class LeadAggregateTest {

    private LeadAggregate following() {
        return LeadAggregate.create("展会线索", "张三", "13900000000", null, null,
                "互联网", "EXHIBITION", "北京", "备注", null,
                "u1", "张三", "d1", "u1", "张三");
    }

    @Test
    @DisplayName("create() yields FOLLOWING with generated id")
    void create_ok() {
        LeadAggregate agg = following();
        assertNotNull(agg.getId());
        assertEquals(LeadStatus.FOLLOWING.name(), agg.getLead().getStatus());
        assertEquals("EXHIBITION", agg.getLead().getSource());
    }

    @Test
    @DisplayName("create() rejects blank required fields")
    void create_validates() {
        assertThrows(BizException.class, () -> LeadAggregate.create("", "张三", null, null, null,
                null, null, null, null, null, "u1", "张三", "d1", "u1", "张三"));
        assertThrows(BizException.class, () -> LeadAggregate.create("名称", " ", null, null, null,
                null, null, null, null, null, "u1", "张三", "d1", "u1", "张三"));
    }

    @Test
    @DisplayName("convert records customer id and reaches terminal CONVERTED")
    void convert_ok() {
        LeadAggregate agg = following();
        agg.markConverted("c999");
        assertEquals(LeadStatus.CONVERTED.name(), agg.getLead().getStatus());
        assertEquals("c999", agg.getLead().getConvertedCustomerId());
    }

    @Test
    @DisplayName("terminal lead cannot be edited / re-converted / discarded")
    void terminal_locked() {
        LeadAggregate agg = following();
        agg.markConverted("c1");
        assertThrows(BizException.class, () -> agg.updateInfo("x", "y", null, null, null, null, null, null, null, null));
        assertThrows(BizException.class, () -> agg.markConverted("c2"));
        assertThrows(BizException.class, agg::discard);

        LeadAggregate agg2 = following();
        agg2.discard();
        assertEquals(LeadStatus.INVALID.name(), agg2.getLead().getStatus());
        assertThrows(BizException.class, agg2::discard);
    }

    @Test
    @DisplayName("changeOwner requires non-blank owner")
    void changeOwner() {
        LeadAggregate agg = following();
        agg.changeOwner("u2", "李四", "d2");
        assertEquals("u2", agg.getLead().getOwnerId());
        assertThrows(BizException.class, () -> agg.changeOwner("", "李四", "d2"));
    }

    @Test
    @DisplayName("of(null) throws")
    void of_null() {
        assertThrows(BizException.class, () -> LeadAggregate.of(null));
    }
}
