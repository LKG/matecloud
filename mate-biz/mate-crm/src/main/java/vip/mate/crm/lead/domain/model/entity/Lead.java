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
package vip.mate.crm.lead.domain.model.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 线索实体(纯数据, 状态变更只经 {@code LeadAggregate})。
 *
 * @author mateaix
 */
@Data
@Builder
public class Lead {

    private String id;
    private String leadName;
    private String contactName;
    private String mobile;
    private String telephone;
    private String email;
    private String industry;
    /** 线索来源: SEARCH/OFFICIAL_WEBSITE/EXHIBITION/REFERRAL/PHONE/OTHER */
    private String source;
    /** FOLLOWING/CONVERTED/INVALID (见 LeadStatus) */
    private String status;
    private String address;
    private String remark;
    private LocalDateTime nextTime;
    /** 转化后客户ID */
    private String convertedCustomerId;
    private String ownerId;
    private String ownerName;
    private String deptId;
    private String creatorId;
    private String creatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
