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
package vip.mate.crm.approval.domain.adapter.repository;

import vip.mate.base.result.PageResult;
import vip.mate.crm.approval.domain.model.aggregate.ApprovalAggregate;
import vip.mate.crm.approval.domain.model.entity.Approval;
import vip.mate.crm.common.CrmScope;

/**
 * 审批实例仓储接口(聚合含节点链快照与操作记录)。
 *
 * @author mateaix
 */
public interface IApprovalRepository {

    void save(ApprovalAggregate aggregate);

    void update(ApprovalAggregate aggregate);

    ApprovalAggregate findById(String id);

    void deleteById(String id);

    PageResult<Approval> page(String keyword, String status, String bizType, String applicantId,
                              CrmScope scope, int pageNum, int pageSize);

    /** 业务单据是否已存在在途审批(PENDING)。 */
    boolean existsPending(String bizType, String bizId);
}
