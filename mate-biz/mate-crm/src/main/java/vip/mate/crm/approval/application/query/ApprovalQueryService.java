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
package vip.mate.crm.approval.application.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.base.result.PageResult;
import vip.mate.crm.approval.domain.adapter.repository.IApprovalRepository;
import vip.mate.crm.approval.domain.model.aggregate.ApprovalAggregate;
import vip.mate.crm.approval.domain.model.entity.Approval;
import vip.mate.crm.approval.domain.model.entity.ApprovalNode;
import vip.mate.crm.approval.domain.model.entity.ApprovalRecord;
import vip.mate.crm.approval.types.enums.ApprovalStatus;
import vip.mate.crm.common.CurrentUserResolver;

import java.util.List;

/** 审批读侧查询服务: 我发起的 / 待我审批(在途) / 明细(节点链 + 记录)。 */
@Service
@RequiredArgsConstructor
public class ApprovalQueryService {

    private final IApprovalRepository approvalRepository;
    private final CurrentUserResolver currentUserResolver;

    /** 我发起的。 */
    public PageResult<Approval> pageMine(String keyword, String status, String bizType, int pageNum, int pageSize) {
        String applicantId = currentUserResolver.require().userId();
        return approvalRepository.page(keyword, status, bizType, applicantId,
                currentUserResolver.scope(), pageNum, pageSize);
    }

    /** 待我审批(在途列表)。 */
    public PageResult<Approval> pageTodo(String keyword, String bizType, int pageNum, int pageSize) {
        return approvalRepository.page(keyword, ApprovalStatus.PENDING.name(), bizType, null,
                currentUserResolver.scope(), pageNum, pageSize);
    }

    public Approval findById(String id) {
        ApprovalAggregate agg = approvalRepository.findById(id);
        return agg == null ? null : agg.getApproval();
    }

    public ApprovalDetail detail(String id) {
        ApprovalAggregate agg = approvalRepository.findById(id);
        if (agg == null) {
            return null;
        }
        return new ApprovalDetail(agg.getApproval(), agg.getNodes(), agg.getRecords());
    }

    /** 审批明细读模型。 */
    public record ApprovalDetail(Approval approval, List<ApprovalNode> nodes, List<ApprovalRecord> records) {
    }
}
