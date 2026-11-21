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
package vip.mate.crm.aftersale.domain.model.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 售后工单实体(状态流转只经 {@code AfterSaleAggregate})。
 *
 * @author mateaix
 */
@Data
@Builder
public class AfterSale {

    private String id;
    private String ticketNo;
    private String title;
    private String customerId;
    private String customerName;
    private String contractId;
    /** COMPLAINT/REPAIR/CONSULT/SERVICE */
    private String type;
    /** HIGH/MEDIUM/LOW */
    private String priority;
    /** OPEN/PROCESSING/RESOLVED/CLOSED (见 AfterSaleStatus) */
    private String status;
    private String description;
    private String handlerId;
    private String handlerName;
    private LocalDateTime resolveTime;
    private LocalDateTime closeTime;
    /** 满意度 1-5 */
    private Integer rating;
    private String ratingRemark;
    private String ownerId;
    private String ownerName;
    private String deptId;
    private String creatorId;
    private String creatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
