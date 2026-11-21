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
import vip.mate.base.response.ResponseCode;
import vip.mate.crm.common.CrmCurrentUser;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.trade.domain.adapter.repository.IContractRepository;
import vip.mate.crm.trade.domain.adapter.repository.IPaymentPlanRepository;
import vip.mate.crm.trade.domain.model.aggregate.ContractAggregate;
import vip.mate.crm.trade.domain.model.aggregate.PaymentPlanAggregate;
import vip.mate.crm.trade.domain.model.entity.Repayment;
import vip.mate.crm.types.exception.CrmErrorCode;
import vip.mate.starter.distribute.util.SnowflakeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 回款登记写侧应用服务: 登记一笔回款联动回款计划期次与合同累计已回款; 支持撤销回退。
 *
 * @author mateaix
 */
@Service
@RequiredArgsConstructor
public class RepaymentCommandService {

    private final IPaymentPlanRepository paymentPlanRepository;
    private final IContractRepository contractRepository;
    private final CurrentUserResolver currentUserResolver;

    @Transactional
    public String register(RepaymentCommand cmd) {
        CrmCurrentUser operator = currentUserResolver.require();
        if (cmd.getMoney() == null || cmd.getMoney().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "回款金额必须大于 0");
        }
        ContractAggregate contract = findContractOrThrow(cmd.getContractId());

        // 计划期次联动(可选): 先做期次级校验与累加
        if (cmd.getPlanId() != null && !cmd.getPlanId().isBlank()) {
            PaymentPlanAggregate plan = paymentPlanRepository.findPlanById(cmd.getPlanId());
            if (plan == null) {
                throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
            }
            plan.registerRepayment(cmd.getMoney());
            paymentPlanRepository.updatePlan(plan);
        }

        Repayment repayment = Repayment.builder()
                .number("HK" + SnowflakeUtil.newSnowflakeId())
                .contractId(contract.getId())
                .contractNum(contract.getContract().getContractNum())
                .planId(cmd.getPlanId())
                .customerId(contract.getContract().getCustomerId())
                .money(cmd.getMoney())
                .paymentMethod(cmd.getPaymentMethod() == null || cmd.getPaymentMethod().isBlank()
                        ? "TRANSFER" : cmd.getPaymentMethod())
                .returnTime(cmd.getReturnTime() == null ? LocalDate.now() : cmd.getReturnTime())
                .remark(cmd.getRemark())
                .attachment(cmd.getAttachment())
                .ownerId(operator.userId())
                .ownerName(operator.displayName())
                .deptId(operator.deptId())
                .creatorId(operator.userId())
                .creatorName(operator.displayName())
                .build();
        repayment.setId(SnowflakeUtil.newSnowflakeId());
        paymentPlanRepository.insertRepayment(repayment);

        // 合同累计已回款(合同级不超过合同额)
        contract.addReceived(cmd.getMoney());
        contractRepository.updateReceivedMoney(contract.getId(), contract.getContract().getReceivedMoney());
        return repayment.getId();
    }

    /** 撤销回款登记: 回退计划期次与合同累计已回款, 删除回款记录。 */
    @Transactional
    public void delete(String repaymentId) {
        Repayment repayment = paymentPlanRepository.findRepaymentById(repaymentId);
        if (repayment == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        if (repayment.getPlanId() != null && !repayment.getPlanId().isBlank()) {
            PaymentPlanAggregate plan = paymentPlanRepository.findPlanById(repayment.getPlanId());
            if (plan != null) {
                plan.rollbackRepayment(repayment.getMoney());
                paymentPlanRepository.updatePlan(plan);
            }
        }
        ContractAggregate contract = contractRepository.findById(repayment.getContractId());
        if (contract != null) {
            contract.reduceReceived(repayment.getMoney());
            contractRepository.updateReceivedMoney(contract.getId(), contract.getContract().getReceivedMoney());
        }
        paymentPlanRepository.deleteRepayment(repaymentId);
    }

    private ContractAggregate findContractOrThrow(String id) {
        ContractAggregate agg = contractRepository.findById(id);
        if (agg == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        return agg;
    }
}
