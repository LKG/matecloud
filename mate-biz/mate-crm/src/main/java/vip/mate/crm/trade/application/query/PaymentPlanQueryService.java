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
package vip.mate.crm.trade.application.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.base.result.PageResult;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.trade.domain.adapter.repository.IPaymentPlanRepository;
import vip.mate.crm.trade.domain.model.entity.PaymentPlan;
import vip.mate.crm.trade.domain.model.entity.Repayment;

import java.util.List;

/** 回款读侧查询服务: 回款计划分页 + 回款记录分页 + 合同/计划维度列表。 */
@Service
@RequiredArgsConstructor
public class PaymentPlanQueryService {

    private final IPaymentPlanRepository paymentPlanRepository;
    private final CurrentUserResolver currentUserResolver;

    public PageResult<PaymentPlan> pagePlans(String keyword, String contractId, Integer status,
                                             Boolean mineOnly, int pageNum, int pageSize) {
        String ownerId = Boolean.TRUE.equals(mineOnly) ? currentUserResolver.require().userId() : null;
        return paymentPlanRepository.pagePlans(keyword, contractId, status, ownerId,
                currentUserResolver.scope(), pageNum, pageSize);
    }

    public PaymentPlan findPlanById(String id) {
        var agg = paymentPlanRepository.findPlanById(id);
        return agg == null ? null : agg.getPlan();
    }

    public List<PaymentPlan> listPlansByContract(String contractId) {
        return paymentPlanRepository.listByContract(contractId);
    }

    public PageResult<Repayment> pageRepayments(String keyword, String contractId,
                                                Boolean mineOnly, int pageNum, int pageSize) {
        String ownerId = Boolean.TRUE.equals(mineOnly) ? currentUserResolver.require().userId() : null;
        return paymentPlanRepository.pageRepayments(keyword, contractId, ownerId,
                currentUserResolver.scope(), pageNum, pageSize);
    }

    public List<Repayment> listRepaymentsByPlan(String planId) {
        return paymentPlanRepository.listRepaymentsByPlan(planId);
    }
}
