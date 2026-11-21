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
package vip.mate.crm.trade.domain.adapter.repository;

import vip.mate.base.result.PageResult;
import vip.mate.crm.common.CrmScope;
import vip.mate.crm.query.dto.AdvancedQuery;
import vip.mate.crm.trade.domain.model.aggregate.PaymentPlanAggregate;
import vip.mate.crm.trade.domain.model.entity.PaymentPlan;
import vip.mate.crm.trade.domain.model.entity.Repayment;

import java.time.LocalDate;
import java.util.List;

/**
 * 回款计划 + 回款记录仓储接口(同属回款上下文)。
 *
 * @author mateaix
 */
public interface IPaymentPlanRepository {

    void savePlan(PaymentPlanAggregate aggregate);

    void updatePlan(PaymentPlanAggregate aggregate);

    PaymentPlanAggregate findPlanById(String id);

    void deletePlan(String id);

    PageResult<PaymentPlan> pagePlans(String keyword, String contractId, Integer status,
                                      String ownerId, CrmScope scope, int pageNum, int pageSize);

    List<PaymentPlan> listByContract(String contractId);

    /** job: 未结清且计划日期早于 today 的期次(逾期提醒/状态刷新)。 */
    List<PaymentPlanAggregate> findDuePlans(LocalDate today, int limit);

    // ---- 回款记录 ----

    void insertRepayment(Repayment repayment);

    Repayment findRepaymentById(String id);

    void deleteRepayment(String id);

    PageResult<Repayment> pageRepayments(String keyword, String contractId, String ownerId,
                                         CrmScope scope, int pageNum, int pageSize);

    /** 回款记录条件查询(动态高级筛选)。 */
    PageResult<Repayment> searchRepayments(AdvancedQuery query, CrmScope scope);

    List<Repayment> listRepaymentsByPlan(String planId);
}
