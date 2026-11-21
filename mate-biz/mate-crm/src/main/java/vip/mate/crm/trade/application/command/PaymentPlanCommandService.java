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
package vip.mate.crm.trade.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.base.exception.BizException;
import vip.mate.crm.common.CrmCurrentUser;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.trade.domain.adapter.repository.IPaymentPlanRepository;
import vip.mate.crm.trade.domain.model.aggregate.PaymentPlanAggregate;
import vip.mate.crm.types.exception.CrmErrorCode;

/**
 * 回款计划写侧应用服务: 期次创建/编辑/删除编排。
 *
 * @author mateaix
 */
@Service
@RequiredArgsConstructor
public class PaymentPlanCommandService {

    private final IPaymentPlanRepository paymentPlanRepository;
    private final CurrentUserResolver currentUserResolver;

    @Transactional
    public String create(PaymentPlanCommand cmd) {
        CrmCurrentUser operator = currentUserResolver.require();
        PaymentPlanAggregate agg = PaymentPlanAggregate.create(
                cmd.getContractId(), cmd.getContractNum(), cmd.getCustomerId(), cmd.getTermNo(),
                cmd.getMoney(), cmd.getCheckingDate(), cmd.getReturnType(), cmd.getRemark(), operator);
        paymentPlanRepository.savePlan(agg);
        return agg.getId();
    }

    @Transactional
    public void update(String id, PaymentPlanCommand cmd) {
        PaymentPlanAggregate agg = findOrThrow(id);
        agg.updateInfo(cmd.getMoney(), cmd.getCheckingDate(), cmd.getReturnType(), cmd.getRemark());
        paymentPlanRepository.updatePlan(agg);
    }

    @Transactional
    public void delete(String id) {
        PaymentPlanAggregate agg = findOrThrow(id);
        agg.assertDeletable();
        paymentPlanRepository.deletePlan(id);
    }

    private PaymentPlanAggregate findOrThrow(String id) {
        PaymentPlanAggregate agg = paymentPlanRepository.findPlanById(id);
        if (agg == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        return agg;
    }
}
