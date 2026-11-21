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
package vip.mate.crm.business.domain.model.aggregate;

import lombok.Builder;
import lombok.Getter;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;
import vip.mate.crm.business.domain.model.entity.Business;
import vip.mate.crm.business.domain.model.entity.BusinessProduct;
import vip.mate.crm.business.types.enums.BusinessStage;
import vip.mate.crm.common.CrmCurrentUser;
import vip.mate.crm.types.exception.CrmErrorCode;
import vip.mate.starter.distribute.util.SnowflakeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 商机聚合根: 阶段只可逐级向前推进(INITIAL→DEMAND→PLAN→NEGOTIATION→WIN),
 * LOSE 可从任一在途阶段进入; WIN/LOSE 为终态不可再变更。产品行合计=预计金额。
 *
 * @author mateaix
 */
@Getter
@Builder
public class BusinessAggregate {

    private final Business business;

    @Builder.Default
    private final List<BusinessProduct> products = new ArrayList<>();

    public static BusinessAggregate create(String businessName, String customerId, String customerName,
                                           String contactName, String source, Integer dealProb,
                                           LocalDate dealDate, String nextStep, String remark,
                                           List<BusinessProduct> products, CrmCurrentUser operator) {
        requireText(businessName, "商机名称不能为空");
        if (customerId == null || customerId.isBlank()) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "商机必须关联客户");
        }
        List<BusinessProduct> lines = products == null ? new ArrayList<>() : new ArrayList<>(products);
        Business business = Business.builder()
                .businessName(businessName)
                .customerId(customerId)
                .customerName(customerName)
                .contactName(contactName)
                .source(source)
                .stage(BusinessStage.INITIAL.name())
                .statusFlag(1)
                .money(sumProducts(lines))
                .dealProb(dealProb == null ? 50 : dealProb)
                .dealDate(dealDate)
                .nextStep(nextStep)
                .remark(remark)
                .ownerId(operator.userId())
                .ownerName(operator.displayName())
                .deptId(operator.deptId())
                .creatorId(operator.userId())
                .creatorName(operator.displayName())
                .build();
        business.setId(SnowflakeUtil.newSnowflakeId());
        lines.forEach(p -> p.setBusinessId(business.getId()));
        return BusinessAggregate.builder().business(business).products(lines).build();
    }

    public static BusinessAggregate of(Business business, List<BusinessProduct> products) {
        if (business == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        List<BusinessProduct> lines = products == null ? new ArrayList<>() : new ArrayList<>(products);
        return BusinessAggregate.builder().business(business).products(lines).build();
    }

    /** 编辑基本信息(仅进行中可编辑); 传入产品行时重算金额。 */
    public void updateInfo(String businessName, String contactName, String source, Integer dealProb,
                           LocalDate dealDate, String nextStep, String remark, List<BusinessProduct> products) {
        assertActive();
        requireText(businessName, "商机名称不能为空");
        business.setBusinessName(businessName);
        business.setContactName(contactName);
        business.setSource(source);
        if (dealProb != null) {
            business.setDealProb(dealProb);
        }
        business.setDealDate(dealDate);
        business.setNextStep(nextStep);
        business.setRemark(remark);
        if (products != null) {
            List<BusinessProduct> lines = new ArrayList<>(products);
            lines.forEach(p -> {
                if (p.getId() == null) {
                    p.setId(SnowflakeUtil.newSnowflakeId());
                }
                p.setBusinessId(business.getId());
            });
            this.products.clear();
            this.products.addAll(lines);
            business.setMoney(sumProducts(lines));
        }
    }

    /** 阶段推进: 逐级向前到下一活跃阶段; NEGOTIATION 之后需走 win/lose。 */
    public void advance() {
        BusinessStage current = BusinessStage.of(business.getStage());
        assertNotTerminal(current);
        BusinessStage next = current.nextActive();
        if (next == null) {
            throw new BizException(CrmErrorCode.BUSINESS_STAGE_BACKWARD.getCode(), "已处最后阶段, 请标记赢单或输单");
        }
        business.setStage(next.name());
    }

    /** 直接推进到指定阶段(仅允许下一级活跃阶段, 禁止跳级/回退)。 */
    public void advanceTo(String targetStage) {
        BusinessStage target = BusinessStage.of(targetStage);
        BusinessStage current = BusinessStage.of(business.getStage());
        assertNotTerminal(current);
        if (target == BusinessStage.WIN) {
            win(null);
            return;
        }
        if (target == BusinessStage.LOSE) {
            lose(null);
            return;
        }
        if (target != current.nextActive()) {
            throw new BizException(CrmErrorCode.BUSINESS_STAGE_BACKWARD);
        }
        business.setStage(target.name());
    }

    /** 赢单: 仅可从商务谈判达成。 */
    public void win(String endRemark) {
        BusinessStage current = BusinessStage.of(business.getStage());
        assertNotTerminal(current);
        if (current != BusinessStage.NEGOTIATION) {
            throw new BizException(CrmErrorCode.BUSINESS_STAGE_BACKWARD.getCode(), "只有商务谈判阶段才能赢单");
        }
        business.setStage(BusinessStage.WIN.name());
        business.setStatusFlag(0);
        business.setDealProb(100);
        business.setEndRemark(endRemark);
    }

    /** 输单: 任一在途阶段可标记。 */
    public void lose(String endRemark) {
        BusinessStage current = BusinessStage.of(business.getStage());
        assertNotTerminal(current);
        business.setStage(BusinessStage.LOSE.name());
        business.setStatusFlag(0);
        business.setDealProb(0);
        business.setEndRemark(endRemark);
    }

    public boolean isWon() {
        return BusinessStage.WIN.name().equals(business.getStage());
    }

    public String getId() {
        return business.getId();
    }

    private static void assertNotTerminal(BusinessStage stage) {
        if (stage.isTerminal()) {
            throw new BizException(CrmErrorCode.BUSINESS_FINISHED);
        }
    }

    private void assertActive() {
        BusinessStage current = BusinessStage.of(business.getStage());
        assertNotTerminal(current);
    }

    private static BigDecimal sumProducts(List<BusinessProduct> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (BusinessProduct p : lines) {
            if (p.getSubtotal() != null) {
                total = total.add(p.getSubtotal());
            }
        }
        return total;
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), message);
        }
    }
}
