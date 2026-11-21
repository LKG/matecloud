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
package vip.mate.crm.approval.domain.model.aggregate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vip.mate.base.exception.BizException;
import vip.mate.crm.approval.domain.model.entity.ApprovalNode;
import vip.mate.crm.approval.types.enums.ApprovalStatus;
import vip.mate.crm.common.CrmCurrentUser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ApprovalAggregate node-chain status machine tests")
class ApprovalAggregateTest {

    private final CrmCurrentUser applicant = new CrmCurrentUser("u1", "alice", "张三", "d1");
    private final CrmCurrentUser boss = new CrmCurrentUser("u2", "bob", "主管", "d1");
    private final CrmCurrentUser finance = new CrmCurrentUser("u3", "eve", "财务", "d1");

    /** 两级链: 部门主管(u2) → 财务(u3)。 */
    private List<ApprovalNode> chain() {
        return List.of(
                ApprovalNode.of("部门主管", "u2", "主管"),
                ApprovalNode.of("财务", "u3", "财务"));
    }

    private ApprovalAggregate submitted() {
        return ApprovalAggregate.submit("CONTRACT", "c1", "HT001", "园区合同", chain(), applicant);
    }

    @Test
    @DisplayName("submit: PENDING at first node, submit record added")
    void submit_ok() {
        ApprovalAggregate agg = submitted();
        assertEquals(ApprovalStatus.PENDING.name(), agg.getApproval().getStatus());
        assertEquals(0, agg.getApproval().getCurrentNode());
        assertEquals(1, agg.getRecords().size());
        assertEquals("SUBMIT", agg.getRecords().get(0).getAction());
    }

    @Test
    @DisplayName("approve逐级: node0 -> node1 -> APPROVED")
    void approve_through_chain() {
        ApprovalAggregate agg = submitted();
        agg.approve(boss, "同意");
        assertEquals(ApprovalStatus.PENDING.name(), agg.getApproval().getStatus());
        assertEquals(1, agg.getApproval().getCurrentNode());
        agg.approve(finance, "打款无误");
        assertEquals(ApprovalStatus.APPROVED.name(), agg.getApproval().getStatus());
        assertEquals(-1, agg.getApproval().getCurrentNode());
        assertTrue(agg.isApproved());
        assertTrue(agg.isFinished());
    }

    @Test
    @DisplayName("non-current approver rejected at node0")
    void approve_wrong_approver() {
        ApprovalAggregate agg = submitted();
        assertThrows(BizException.class, () -> agg.approve(finance, "越级"));
    }

    @Test
    @DisplayName("reject by current approver -> REJECTED")
    void reject_ok() {
        ApprovalAggregate agg = submitted();
        agg.reject(boss, "金额存疑");
        assertEquals(ApprovalStatus.REJECTED.name(), agg.getApproval().getStatus());
        assertEquals(-1, agg.getApproval().getCurrentNode());
    }

    @Test
    @DisplayName("cancel only allowed for applicant")
    void cancel_gate() {
        ApprovalAggregate agg = submitted();
        assertThrows(BizException.class, () -> agg.cancel(boss, "非申请人"));
        agg.cancel(applicant, "撤回");
        assertEquals(ApprovalStatus.CANCELED.name(), agg.getApproval().getStatus());
    }

    @Test
    @DisplayName("no operation after finished")
    void gate_after_finished() {
        ApprovalAggregate agg = submitted();
        agg.approve(boss, null);
        agg.approve(finance, null);
        assertThrows(BizException.class, () -> agg.approve(boss, null));
        assertThrows(BizException.class, () -> agg.reject(boss, null));
    }

    @Test
    @DisplayName("empty chain rejected on submit")
    void empty_chain() {
        assertThrows(BizException.class,
                () -> ApprovalAggregate.submit("CONTRACT", "c1", "HT", "t", List.of(), applicant));
    }
}
