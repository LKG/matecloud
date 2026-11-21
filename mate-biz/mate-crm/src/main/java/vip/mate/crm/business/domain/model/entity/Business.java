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
package vip.mate.crm.business.domain.model.entity;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商机实体(阶段漏斗)。
 *
 * @author mateaix
 */
@Data
@Builder
public class Business {

    private String id;
    private String businessName;
    private String customerId;
    private String customerName;
    private String contactName;
    private String source;
    /** INITIAL/DEMAND/PLAN/NEGOTIATION/WIN/LOSE */
    private String stage;
    /** 1=进行中 0=已结束 */
    private Integer statusFlag;
    private BigDecimal money;
    private Integer dealProb;
    private LocalDate dealDate;
    private String nextStep;
    private String endRemark;
    private String remark;
    private String ownerId;
    private String ownerName;
    private String deptId;
    private String creatorId;
    private String creatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
