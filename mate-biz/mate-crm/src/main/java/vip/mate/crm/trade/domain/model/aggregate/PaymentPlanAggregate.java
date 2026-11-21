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
package vip.mate.crm.trade.domain.model.aggregate;

import lombok.Builder;
import lombok.Getter;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;
import vip.mate.crm.common.CrmCurrentUser;
import vip.mate.crm.trade.domain.model.entity.PaymentPlan;
import vip.mate.crm.trade.domain.model.entity.Repayment;
import vip.mate.crm.trade.types.enums.PlanStatus;
import vip.mate.crm.types.exception.CrmErrorCode;
import vip.mate.starter.distribute.util.SnowflakeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 回款计划聚合根(单期 + 其下回款记录)。
 *
 * <p>状态推进: 待回款 → 部分回款 → 已回款; 逾期未回款由 job 置为逾期。登记回款累加
 * receivedMoney, 达到计划金额即结清; 已回款的期次不可再修改计划要素(PLAN_LOCKED)。
 *
 * @author mateaix
 */
@Getter
@Builder
public class PaymentPlanAggregate {

    private final PaymentPlan plan;

    @Builder.Default
    private final List<Repayment> repayments = new ArrayList<>();

    public static PaymentPlanAggregate create(String contractId, String contractNum, String customerId,
                                              Integer termNo, BigDecimal money, LocalDate checkingDate,
                                              String returnType, String remark, CrmCurrentUser operator) {
        if (contractId == null || contractId.isBlank()) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "回款计划必须关联合同");
        }
        if (money == null || money.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "计划回款金额必须大于 0");
        }
        if (checkingDate == null) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "计划回款日期不能为空");
        }
        PaymentPlan plan = PaymentPlan.builder()
                .contractId(contractId)
                .contractNum(contractNum)
                .customerId(customerId)
                .termNo(termNo == null || termNo <= 0 ? 1 : termNo)
                .money(money)
                .receivedMoney(BigDecimal.ZERO)
                .checkingDate(checkingDate)
                .returnType(returnType == null || returnType.isBlank() ? "TRANSFER" : returnType)
                .status(PlanStatus.PENDING.code())
                .remark(remark)
                .ownerId(operator.userId())
                .ownerName(operator.displayName())
                .deptId(operator.deptId())
                .creatorId(operator.userId())
                .creatorName(operator.displayName())
                .build();
        plan.setId(SnowflakeUtil.newSnowflakeId());
        return PaymentPlanAggregate.builder().plan(plan).build();
    }

    public static PaymentPlanAggregate of(PaymentPlan plan, List<Repayment> repayments) {
        if (plan == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        List<Repayment> list = repayments == null ? new ArrayList<>() : new ArrayList<>(repayments);
        return PaymentPlanAggregate.builder().plan(plan).repayments(list).build();
    }

    /** 登记一笔回款到本期, 累加已回款并重算状态。 */
    public void registerRepayment(BigDecimal money) {
        if (money == null || money.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "回款金额必须大于 0");
        }
        PlanStatus current = PlanStatus.of(code());
        if (current.isSettled()) {
            throw new BizException(CrmErrorCode.PLAN_ALREADY_SETTLED);
        }
        BigDecimal received = nvl(plan.getReceivedMoney()).add(money);
        if (received.compareTo(nvl(plan.getMoney())) > 0) {
            throw new BizException(CrmErrorCode.CONTRACT_RECEIVABLE_EXCEED.getCode(), "回款金额超出该期计划金额");
        }
        plan.setReceivedMoney(received);
        plan.setStatus(received.compareTo(nvl(plan.getMoney())) >= 0
                ? PlanStatus.SETTLED.code() : PlanStatus.PARTIAL.code());
    }

    /** 回退一笔回款(撤销登记用): 扣减已回款并重算待回款/部分状态。 */
    public void rollbackRepayment(BigDecimal money) {
        if (money == null || money.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal received = nvl(plan.getReceivedMoney()).subtract(money);
        if (received.compareTo(BigDecimal.ZERO) < 0) {
            received = BigDecimal.ZERO;
        }
        plan.setReceivedMoney(received);
        plan.setStatus(received.compareTo(BigDecimal.ZERO) <= 0
                ? PlanStatus.PENDING.code() : PlanStatus.PARTIAL.code());
    }

    /** job: 逾期检测(未结清且计划日期已过 → 逾期)。 */
    public boolean markOverdueIfDue(LocalDate today) {
        PlanStatus current = PlanStatus.of(code());
        if (current.isSettled()) {
            return false;
        }
        if (plan.getCheckingDate() != null && plan.getCheckingDate().isBefore(today) && current != PlanStatus.OVERDUE) {
            plan.setStatus(PlanStatus.OVERDUE.code());
            return true;
        }
        return false;
    }

    /** 编辑期次金额/日期(已发生回款则锁定)。 */
    public void updateInfo(BigDecimal money, LocalDate checkingDate, String returnType, String remark) {
        if (nvl(plan.getReceivedMoney()).compareTo(BigDecimal.ZERO) > 0) {
            throw new BizException(CrmErrorCode.PLAN_LOCKED);
        }
        if (money != null && money.compareTo(BigDecimal.ZERO) > 0) {
            plan.setMoney(money);
        }
        if (checkingDate != null) {
            plan.setCheckingDate(checkingDate);
        }
        if (returnType != null && !returnType.isBlank()) {
            plan.setReturnType(returnType);
        }
        plan.setRemark(remark);
    }

    /** 删除计划(已发生回款则锁定)。 */
    public void assertDeletable() {
        if (nvl(plan.getReceivedMoney()).compareTo(BigDecimal.ZERO) > 0) {
            throw new BizException(CrmErrorCode.PLAN_LOCKED);
        }
    }

    public int code() {
        return plan.getStatus() == null ? PlanStatus.PENDING.code() : plan.getStatus();
    }

    public String getId() {
        return plan.getId();
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
