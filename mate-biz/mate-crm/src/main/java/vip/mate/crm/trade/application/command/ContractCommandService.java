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
import vip.mate.crm.business.domain.adapter.repository.IBusinessRepository;
import vip.mate.crm.business.domain.model.aggregate.BusinessAggregate;
import vip.mate.crm.business.domain.model.entity.BusinessProduct;
import vip.mate.crm.common.CrmCurrentUser;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.trade.application.command.ContractCommand.ProductLine;
import vip.mate.crm.trade.domain.adapter.repository.IContractRepository;
import vip.mate.crm.trade.domain.model.aggregate.ContractAggregate;
import vip.mate.crm.trade.domain.model.entity.ContractProduct;
import vip.mate.crm.types.exception.CrmErrorCode;
import vip.mate.starter.distribute.util.SnowflakeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 合同写侧应用服务: 草稿创建/编辑、赢单转合同、提交审批、审批回写、归档、删除编排。
 *
 * @author mateaix
 */
@Service
@RequiredArgsConstructor
public class ContractCommandService {

    private final IContractRepository contractRepository;
    private final IBusinessRepository businessRepository;
    private final CurrentUserResolver currentUserResolver;

    @Transactional
    public String create(ContractCommand cmd) {
        CrmCurrentUser operator = currentUserResolver.require();
        assertNumUnique(cmd.getContractNum(), null);
        ContractAggregate agg = ContractAggregate.create(
                cmd.getContractName(), cmd.getContractNum(), cmd.getCustomerId(), cmd.getCustomerName(),
                cmd.getBusinessId(), cmd.getContactName(), cmd.getStartTime(), cmd.getEndTime(), cmd.getSignTime(),
                cmd.getRemark(), cmd.getAttachment(), toLines(cmd.getProducts()), operator);
        contractRepository.save(agg);
        return agg.getId();
    }

    /** 赢单商机转合同草稿: 沿用商机产品行(无折扣), 自动生成合同编号。 */
    @Transactional
    public String createFromBusiness(String businessId) {
        CrmCurrentUser operator = currentUserResolver.require();
        BusinessAggregate business = businessRepository.findById(businessId);
        if (business == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        if (!business.isWon()) {
            throw new BizException(CrmErrorCode.BUSINESS_NOT_WON);
        }
        List<ContractProduct> lines = business.getProducts().stream()
                .map(this::fromBusinessProduct).toList();
        ContractAggregate agg = ContractAggregate.create(
                business.getBusiness().getBusinessName(), "HT" + SnowflakeUtil.newSnowflakeId(),
                business.getBusiness().getCustomerId(), business.getBusiness().getCustomerName(),
                businessId, business.getBusiness().getContactName(),
                LocalDate.now(), null, LocalDate.now(), null, null, lines, operator);
        contractRepository.save(agg);
        return agg.getId();
    }

    @Transactional
    public void update(String id, ContractCommand cmd) {
        assertNumUnique(cmd.getContractNum(), id);
        ContractAggregate agg = findOrThrow(id);
        agg.updateInfo(cmd.getContractName(), cmd.getContactName(), cmd.getStartTime(), cmd.getEndTime(),
                cmd.getSignTime(), cmd.getRemark(), cmd.getAttachment(), toLines(cmd.getProducts()));
        contractRepository.update(agg);
    }

    /** 提交审批: 状态迁移到 PENDING; 审批实例创建由 approval 上下文编排。 */
    @Transactional
    public void submit(String id) {
        ContractAggregate agg = findOrThrow(id);
        agg.submit();
        contractRepository.update(agg);
    }

    /** 审批通过回写(供 approval 上下文回调)。 */
    @Transactional
    public void approve(String id) {
        ContractAggregate agg = findOrThrow(id);
        agg.approve();
        contractRepository.update(agg);
    }

    /** 审批驳回回写(供 approval 上下文回调)。 */
    @Transactional
    public void reject(String id) {
        ContractAggregate agg = findOrThrow(id);
        agg.reject();
        contractRepository.update(agg);
    }

    @Transactional
    public void archive(String id) {
        ContractAggregate agg = findOrThrow(id);
        agg.archive();
        contractRepository.update(agg);
    }

    @Transactional
    public void delete(String id) {
        ContractAggregate agg = findOrThrow(id);
        if (!agg.isDeletable()) {
            throw new BizException(CrmErrorCode.CONTRACT_LOCKED);
        }
        contractRepository.deleteById(id);
    }

    private ContractProduct fromBusinessProduct(BusinessProduct p) {
        return ContractProduct.of(p.getProductId(), p.getProductName(),
                p.getUnitPrice(), p.getQuantity(), BigDecimal.ONE);
    }

    private static List<ContractProduct> toLines(List<ProductLine> lines) {
        if (lines == null) {
            return List.of();
        }
        return lines.stream()
                .map(l -> ContractProduct.of(l.getProductId(), l.getProductName(),
                        l.getUnitPrice(), l.getQuantity(), l.getDiscount()))
                .toList();
    }

    private void assertNumUnique(String contractNum, String excludeId) {
        if (contractRepository.existsByNum(contractNum, excludeId)) {
            throw new BizException("CRMB050", "合同编号已存在, 请更换");
        }
    }

    private ContractAggregate findOrThrow(String id) {
        ContractAggregate agg = contractRepository.findById(id);
        if (agg == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        return agg;
    }
}
