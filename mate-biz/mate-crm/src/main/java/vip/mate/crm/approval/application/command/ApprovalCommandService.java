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
package vip.mate.crm.approval.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.base.exception.BizException;
import vip.mate.crm.approval.domain.adapter.repository.IApprovalRepository;
import vip.mate.crm.approval.domain.model.aggregate.ApprovalAggregate;
import vip.mate.crm.approval.domain.model.entity.Approval;
import vip.mate.crm.approval.domain.model.entity.ApprovalNode;
import vip.mate.crm.common.CrmCurrentUser;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.trade.application.command.ContractCommandService;
import vip.mate.crm.trade.domain.adapter.repository.IContractRepository;
import vip.mate.crm.trade.domain.model.aggregate.ContractAggregate;
import vip.mate.crm.types.exception.CrmErrorCode;

import java.util.List;

/**
 * 审批写侧应用服务: 合同提交审批 + 审批通过/驳回/撤回编排, 并回调合同状态回写。
 *
 * @author mateaix
 */
@Service
@RequiredArgsConstructor
public class ApprovalCommandService {

    public static final String BIZ_CONTRACT = "CONTRACT";

    private final IApprovalRepository approvalRepository;
    private final IContractRepository contractRepository;
    private final ContractCommandService contractCommandService;
    private final ApprovalChainProvider chainProvider;
    private final CurrentUserResolver currentUserResolver;

    /** 合同提交审批: 建审批实例 + 合同转 PENDING。 */
    @Transactional
    public String submitContract(String contractId) {
        CrmCurrentUser operator = currentUserResolver.require();
        ContractAggregate contract = contractRepository.findById(contractId);
        if (contract == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        if (approvalRepository.existsPending(BIZ_CONTRACT, contractId)) {
            throw new BizException(CrmErrorCode.APPROVAL_ALREADY_DONE);
        }
        List<ApprovalNode> nodes = chainProvider.contractChain();
        ApprovalAggregate agg = ApprovalAggregate.submit(BIZ_CONTRACT, contractId,
                contract.getContract().getContractNum(), contract.getContract().getContractName(), nodes, operator);
        approvalRepository.save(agg);
        contractCommandService.submit(contractId);
        return agg.getId();
    }

    @Transactional
    public void approve(String approvalId, String remark) {
        ApprovalAggregate agg = findOrThrow(approvalId);
        agg.approve(currentUserResolver.require(), remark);
        approvalRepository.update(agg);
        callback(agg);
    }

    @Transactional
    public void reject(String approvalId, String remark) {
        ApprovalAggregate agg = findOrThrow(approvalId);
        agg.reject(currentUserResolver.require(), remark);
        approvalRepository.update(agg);
        callback(agg);
    }

    @Transactional
    public void cancel(String approvalId, String remark) {
        ApprovalAggregate agg = findOrThrow(approvalId);
        agg.cancel(currentUserResolver.require(), remark);
        approvalRepository.update(agg);
        callback(agg);
    }

    /** 审批终态回写业务单据(当前支持合同)。 */
    private void callback(ApprovalAggregate agg) {
        Approval approval = agg.getApproval();
        if (!BIZ_CONTRACT.equals(approval.getBizType())) {
            return;
        }
        vip.mate.crm.approval.types.enums.ApprovalStatus status =
                vip.mate.crm.approval.types.enums.ApprovalStatus.of(approval.getStatus());
        switch (status) {
            case APPROVED -> contractCommandService.approve(approval.getBizId());
            case REJECTED, CANCELED -> contractCommandService.reject(approval.getBizId());
            default -> {
            }
        }
    }

    private ApprovalAggregate findOrThrow(String id) {
        ApprovalAggregate agg = approvalRepository.findById(id);
        if (agg == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        return agg;
    }
}
