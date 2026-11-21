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
package vip.mate.crm.trade.domain.model.entity;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 回款计划期次实体(合同分期)。状态见 {@code PlanStatus}。
 *
 * @author mateaix
 */
@Data
@Builder
public class PaymentPlan {

    private String id;
    private String contractId;
    private String contractNum;
    private String customerId;
    /** 期次(从 1 起) */
    private Integer termNo;
    private BigDecimal money;
    private BigDecimal receivedMoney;
    /** 计划回款日期 */
    private LocalDate checkingDate;
    /** CASH/TRANSFER/CHECK/OTHER */
    private String returnType;
    /** 0待回款 1部分 2已回款 3逾期 (见 PlanStatus) */
    private Integer status;
    private String remark;
    private String ownerId;
    private String ownerName;
    private String deptId;
    private String creatorId;
    private String creatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
