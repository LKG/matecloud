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
 * 回款记录实体(实际到账登记)。
 *
 * @author mateaix
 */
@Data
@Builder
public class Repayment {

    private String id;
    private String number;
    private String contractId;
    private String contractNum;
    /** 关联回款计划ID(可空, 空表示非计划内回款) */
    private String planId;
    private String customerId;
    private BigDecimal money;
    /** CASH/TRANSFER/CHECK/OTHER */
    private String paymentMethod;
    private LocalDate returnTime;
    private String remark;
    private String attachment;
    private String ownerId;
    private String ownerName;
    private String deptId;
    private String creatorId;
    private String creatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
