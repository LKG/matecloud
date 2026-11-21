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
 * 合同实体(纯数据, 状态/金额不变式只经 {@code ContractAggregate})。
 *
 * @author mateaix
 */
@Data
@Builder
public class Contract {

    private String id;
    private String contractNum;
    private String contractName;
    private String customerId;
    private String customerName;
    /** 来源商机ID(赢单转合同) */
    private String businessId;
    private String contactName;
    /** 合同总额(=产品行折后合计, 不变式) */
    private BigDecimal money;
    /** 累计已回款 */
    private BigDecimal receivedMoney;
    private LocalDate startTime;
    private LocalDate endTime;
    private LocalDate signTime;
    /** DRAFT/PENDING/APPROVED/REJECTED/ARCHIVED (见 ContractStatus) */
    private String status;
    private String remark;
    private String attachment;
    private String ownerId;
    private String ownerName;
    private String deptId;
    private String creatorId;
    private String creatorName;
    private Integer lockVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
