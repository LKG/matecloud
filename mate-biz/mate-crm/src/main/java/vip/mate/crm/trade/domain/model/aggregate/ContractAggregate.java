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
import vip.mate.crm.trade.domain.model.entity.Contract;
import vip.mate.crm.trade.domain.model.entity.ContractProduct;
import vip.mate.crm.trade.types.enums.ContractStatus;
import vip.mate.crm.types.exception.CrmErrorCode;
import vip.mate.starter.distribute.util.SnowflakeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 合同聚合根: 状态机 草稿→审批中→通过/驳回→归档。
 *
 * <p>不变式: 合同总额 money 恒等于产品行折后 subtotal 合计; 仅在 DRAFT/REJECTED 可改
 * 产品行; 已回款 receivedMoney 累加且不得超过合同总额。审批节点链由 approval 上下文编排,
 * 本聚合只暴露 submit/approve/reject/archive 状态迁移。
 *
 * @author mateaix
 */
@Getter
@Builder
public class ContractAggregate {

    private final Contract contract;

    @Builder.Default
    private final List<ContractProduct> products = new ArrayList<>();

    public static ContractAggregate create(String contractName, String contractNum, String customerId,
                                           String customerName, String businessId, String contactName,
                                           LocalDate startTime, LocalDate endTime, LocalDate signTime,
                                           String remark, String attachment,
                                           List<ContractProduct> products, CrmCurrentUser operator) {
        requireText(contractName, "合同名称不能为空");
        requireText(contractNum, "合同编号不能为空");
        if (customerId == null || customerId.isBlank()) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "合同必须关联客户");
        }
        List<ContractProduct> lines = products == null ? new ArrayList<>() : new ArrayList<>(products);
        Contract contract = Contract.builder()
                .contractName(contractName)
                .contractNum(contractNum)
                .customerId(customerId)
                .customerName(customerName)
                .businessId(businessId)
                .contactName(contactName)
                .money(sumProducts(lines))
                .receivedMoney(BigDecimal.ZERO)
                .startTime(startTime)
                .endTime(endTime)
                .signTime(signTime)
                .status(ContractStatus.DRAFT.name())
                .remark(remark)
                .attachment(attachment)
                .ownerId(operator.userId())
                .ownerName(operator.displayName())
                .deptId(operator.deptId())
                .creatorId(operator.userId())
                .creatorName(operator.displayName())
                .lockVersion(0)
                .build();
        contract.setId(SnowflakeUtil.newSnowflakeId());
        lines.forEach(p -> p.setContractId(contract.getId()));
        return ContractAggregate.builder().contract(contract).products(lines).build();
    }

    public static ContractAggregate of(Contract contract, List<ContractProduct> products) {
        if (contract == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        List<ContractProduct> lines = products == null ? new ArrayList<>() : new ArrayList<>(products);
        return ContractAggregate.builder().contract(contract).products(lines).build();
    }

    /** 编辑基本信息 + 产品行(仅草稿/驳回可编辑); 重算合同总额不变式。 */
    public void updateInfo(String contractName, String contactName, LocalDate startTime, LocalDate endTime,
                           LocalDate signTime, String remark, String attachment, List<ContractProduct> products) {
        assertStatus(ContractStatus.of(contract.getStatus()).isEditable(), CrmErrorCode.CONTRACT_LOCKED);
        requireText(contractName, "合同名称不能为空");
        contract.setContractName(contractName);
        contract.setContactName(contactName);
        contract.setStartTime(startTime);
        contract.setEndTime(endTime);
        contract.setSignTime(signTime);
        contract.setRemark(remark);
        contract.setAttachment(attachment);
        if (products != null) {
            List<ContractProduct> lines = new ArrayList<>(products);
            lines.forEach(p -> {
                if (p.getId() == null) {
                    p.setId(SnowflakeUtil.newSnowflakeId());
                }
                p.setContractId(contract.getId());
            });
            this.products.clear();
            this.products.addAll(lines);
            contract.setMoney(sumProducts(lines));
        }
    }

    /** 提交审批: 草稿/驳回 → 审批中。 */
    public void submit() {
        assertStatus(ContractStatus.of(contract.getStatus()).canSubmit(), CrmErrorCode.CONTRACT_NOT_DRAFT);
        contract.setStatus(ContractStatus.PENDING.name());
    }

    /** 审批通过: 审批中 → 已通过。 */
    public void approve() {
        assertStatus(ContractStatus.of(contract.getStatus()) == ContractStatus.PENDING, CrmErrorCode.CONTRACT_LOCKED);
        contract.setStatus(ContractStatus.APPROVED.name());
    }

    /** 审批驳回: 审批中 → 已驳回。 */
    public void reject() {
        assertStatus(ContractStatus.of(contract.getStatus()) == ContractStatus.PENDING, CrmErrorCode.CONTRACT_LOCKED);
        contract.setStatus(ContractStatus.REJECTED.name());
    }

    /** 归档生效: 已通过 → 已归档。 */
    public void archive() {
        assertStatus(ContractStatus.of(contract.getStatus()) == ContractStatus.APPROVED, CrmErrorCode.CONTRACT_LOCKED);
        contract.setStatus(ContractStatus.ARCHIVED.name());
    }

    /** 回款累加(由回款登记编排调用); 不得超过合同总额。 */
    public void addReceived(BigDecimal money) {
        if (money == null || money.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "回款金额必须大于 0");
        }
        BigDecimal received = nvl(contract.getReceivedMoney()).add(money);
        if (received.compareTo(nvl(contract.getMoney())) > 0) {
            throw new BizException(CrmErrorCode.CONTRACT_RECEIVABLE_EXCEED);
        }
        contract.setReceivedMoney(received);
    }

    /** 回款回退(撤销登记用): 扣减累计已回款, 不低于 0。 */
    public void reduceReceived(BigDecimal money) {
        if (money == null || money.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal received = nvl(contract.getReceivedMoney()).subtract(money);
        if (received.compareTo(BigDecimal.ZERO) < 0) {
            received = BigDecimal.ZERO;
        }
        contract.setReceivedMoney(received);
    }

    /** 未回款余额。 */
    public BigDecimal receivable() {
        return nvl(contract.getMoney()).subtract(nvl(contract.getReceivedMoney()));
    }

    /** 仅草稿/驳回可删除。 */
    public boolean isDeletable() {
        return ContractStatus.of(contract.getStatus()).isEditable();
    }

    public String getId() {
        return contract.getId();
    }

    private static void assertStatus(boolean ok, CrmErrorCode code) {
        if (!ok) {
            throw new BizException(code);
        }
    }

    private static BigDecimal sumProducts(List<ContractProduct> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (ContractProduct p : lines) {
            if (p.getSubtotal() != null) {
                total = total.add(p.getSubtotal());
            }
        }
        return total;
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), message);
        }
    }
}
