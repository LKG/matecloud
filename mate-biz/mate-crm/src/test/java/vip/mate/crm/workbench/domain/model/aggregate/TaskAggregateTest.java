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
package vip.mate.crm.workbench.domain.model.aggregate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vip.mate.base.exception.BizException;
import vip.mate.crm.common.CrmCurrentUser;
import vip.mate.crm.workbench.types.enums.TaskStatus;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TaskAggregate todo status machine tests")
class TaskAggregateTest {

    private final CrmCurrentUser u1 = new CrmCurrentUser("u1", "alice", "张三", "d1");

    private TaskAggregate open() {
        return TaskAggregate.create("回访客户A", "FOLLOW", "CUSTOMER", "c1", "客户A",
                "电话回访", LocalDateTime.now().plusDays(1), "HIGH", null, u1);
    }

    @Test
    @DisplayName("create: OPEN, manual source, owner filled")
    void create_ok() {
        TaskAggregate agg = open();
        assertEquals(TaskStatus.OPEN.name(), agg.getTask().getStatus());
        assertEquals(TaskAggregate.SOURCE_MANUAL, agg.getTask().getSource());
        assertEquals("u1", agg.getTask().getOwnerId());
    }

    @Test
    @DisplayName("complete: OPEN -> DONE with finish time")
    void complete_ok() {
        TaskAggregate agg = open();
        agg.complete();
        assertEquals(TaskStatus.DONE.name(), agg.getTask().getStatus());
        assertNotNull(agg.getTask().getFinishTime());
    }

    @Test
    @DisplayName("no double transition after done")
    void done_gate() {
        TaskAggregate agg = open();
        agg.complete();
        assertThrows(BizException.class, agg::complete);
        assertThrows(BizException.class, agg::cancel);
        assertThrows(BizException.class, () -> agg.updateInfo("x", null, null, null));
    }

    @Test
    @DisplayName("cancel: OPEN -> CANCELED")
    void cancel_ok() {
        TaskAggregate agg = open();
        agg.cancel();
        assertEquals(TaskStatus.CANCELED.name(), agg.getTask().getStatus());
    }

    @Test
    @DisplayName("overdue detection")
    void overdue() {
        TaskAggregate agg = TaskAggregate.create("逾期任务", "FOLLOW", null, null, null,
                null, LocalDateTime.now().minusHours(1), "LOW", null, u1);
        assertTrue(agg.isOverdue());
    }
}
