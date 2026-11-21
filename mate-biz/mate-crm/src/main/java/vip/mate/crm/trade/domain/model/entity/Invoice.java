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
 * 发票实体。状态见 {@code InvoiceStatus}(0申请/1已开/2驳回/3作废)。
 *
 * @author mateaix
 */
@Data
@Builder
public class Invoice {

    private String id;
    private String contractId;
    private String contractNum;
    private String customerId;
    /** GENERAL普票/SPECIAL专票/ELECTRONIC电子票 */
    private String invoiceType;
    private String title;
    private String taxNo;
    private String bankName;
    private String bankAccount;
    private String addressInfo;
    private BigDecimal money;
    private BigDecimal taxRate;
    private BigDecimal taxMoney;
    /** 0已申请 1已开票 2已驳回 3已作废 (见 InvoiceStatus) */
    private Integer status;
    private String invoiceNo;
    private LocalDate invoiceDate;
    private String remark;
    private String ownerId;
    private String ownerName;
    private String deptId;
    private String creatorId;
    private String creatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
