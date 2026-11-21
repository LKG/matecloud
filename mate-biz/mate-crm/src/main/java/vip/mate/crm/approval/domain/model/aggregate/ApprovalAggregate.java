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

import lombok.Builder;
import lombok.Getter;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;
import vip.mate.crm.approval.domain.model.entity.Approval;
import vip.mate.crm.approval.domain.model.entity.ApprovalNode;
import vip.mate.crm.approval.domain.model.entity.ApprovalRecord;
import vip.mate.crm.approval.types.enums.ApprovalStatus;
import vip.mate.crm.common.CrmCurrentUser;
import vip.mate.crm.types.exception.CrmErrorCode;
import vip.mate.starter.distribute.util.SnowflakeUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 审批实例聚合根: 通用节点链状态机(草稿提交 → 逐级审批 → 通过/驳回/撤回)。
 *
 * <p>不变式: 仅审批中(PENDING)可操作; 审批动作必须由当前节点审批人执行; 通过最后节点即整体通过;
 * 撤回仅限申请人且在审批中。业务单据状态回写由应用层回调对应上下文(如合同)。
 *
 * @author mateaix
 */
@Getter
@Builder
public class ApprovalAggregate {

    private final Approval approval;

    @Builder.Default
    private final List<ApprovalNode> nodes = new ArrayList<>();

    @Builder.Default
    private final List<ApprovalRecord> records = new ArrayList<>();

    public static ApprovalAggregate submit(String bizType, String bizId, String bizNo, String title,
                                            List<ApprovalNode> nodes, CrmCurrentUser applicant) {
        if (bizType == null || bizType.isBlank() || bizId == null || bizId.isBlank()) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "审批必须关联业务单据");
        }
        List<ApprovalNode> chain = nodes == null ? new ArrayList<>() : new ArrayList<>(nodes);
        if (chain.isEmpty()) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "审批链不能为空");
        }
        Approval approval = Approval.builder()
                .bizType(bizType)
                .bizId(bizId)
                .bizNo(bizNo)
                .title(title)
                .applicantId(applicant.userId())
                .applicantName(applicant.displayName())
                .deptId(applicant.deptId())
                .status(ApprovalStatus.PENDING.name())
                .currentNode(0)
                .build();
        approval.setId(SnowflakeUtil.newSnowflakeId());
        ApprovalAggregate agg = ApprovalAggregate.builder().approval(approval).nodes(chain).build();
        agg.addRecord(-1, "提交", applicant.userId(), applicant.displayName(), "SUBMIT", null);
        return agg;
    }

    public static ApprovalAggregate of(Approval approval, List<ApprovalNode> nodes, List<ApprovalRecord> records) {
        if (approval == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        return ApprovalAggregate.builder()
                .approval(approval)
                .nodes(nodes == null ? new ArrayList<>() : new ArrayList<>(nodes))
                .records(records == null ? new ArrayList<>() : new ArrayList<>(records))
                .build();
    }

    /** 当前节点审批人通过; 末节点通过则整体通过。 */
    public void approve(CrmCurrentUser operator, String remark) {
        assertPending();
        ApprovalNode current = currentNode();
        assertCurrentApprover(current, operator);
        int idx = approval.getCurrentNode();
        addRecord(idx, current.getNodeName(), operator.userId(), operator.displayName(), "APPROVE", remark);
        int next = idx + 1;
        if (next >= nodes.size()) {
            approval.setStatus(ApprovalStatus.APPROVED.name());
            approval.setCurrentNode(-1);
        } else {
            approval.setCurrentNode(next);
        }
    }

    /** 当前节点审批人驳回; 整体驳回。 */
    public void reject(CrmCurrentUser operator, String remark) {
        assertPending();
        ApprovalNode current = currentNode();
        assertCurrentApprover(current, operator);
        addRecord(approval.getCurrentNode(), current.getNodeName(), operator.userId(), operator.displayName(),
                "REJECT", remark);
        approval.setStatus(ApprovalStatus.REJECTED.name());
        approval.setCurrentNode(-1);
    }

    /** 申请人撤回。 */
    public void cancel(CrmCurrentUser operator, String remark) {
        assertPending();
        if (!operator.userId().equals(approval.getApplicantId())) {
            throw new BizException(CrmErrorCode.NO_PERMISSION.getCode(), "仅申请人可撤回审批");
        }
        addRecord(approval.getCurrentNode(), "撤回", operator.userId(), operator.displayName(), "CANCEL", remark);
        approval.setStatus(ApprovalStatus.CANCELED.name());
        approval.setCurrentNode(-1);
    }

    public boolean isApproved() {
        return ApprovalStatus.of(approval.getStatus()) == ApprovalStatus.APPROVED;
    }

    public boolean isFinished() {
        return ApprovalStatus.of(approval.getStatus()) != ApprovalStatus.PENDING;
    }

    public String getId() {
        return approval.getId();
    }

    private ApprovalNode currentNode() {
        int idx = approval.getCurrentNode() == null ? 0 : approval.getCurrentNode();
        if (idx < 0 || idx >= nodes.size()) {
            throw new BizException(CrmErrorCode.APPROVAL_NOT_CURRENT_NODE);
        }
        return nodes.get(idx);
    }

    private void assertPending() {
        if (!ApprovalStatus.of(approval.getStatus()).isPending()) {
            throw new BizException(CrmErrorCode.APPROVAL_NOT_PENDING);
        }
    }

    private void assertCurrentApprover(ApprovalNode node, CrmCurrentUser operator) {
        if (node.getApproverId() != null && !node.getApproverId().equals(operator.userId())) {
            throw new BizException(CrmErrorCode.APPROVAL_NOT_CURRENT_NODE);
        }
    }

    private void addRecord(int idx, String nodeName, String approverId, String approverName,
                           String action, String remark) {
        ApprovalRecord record = ApprovalRecord.builder()
                .id(SnowflakeUtil.newSnowflakeId())
                .approvalId(approval.getId())
                .nodeIndex(idx)
                .nodeName(nodeName)
                .approverId(approverId)
                .approverName(approverName)
                .action(action)
                .remark(remark)
                .actedAt(LocalDateTime.now())
                .build();
        records.add(record);
    }
}
