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
package vip.mate.crm.customer.domain.model.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户实体(纯数据, 状态/公海流转只经 {@code CustomerAggregate})。
 *
 * @author mateaix
 */
@Data
@Builder
public class Customer {

    private String id;
    private String customerName;
    private String industry;
    /** A重点/B普通/C其他 */
    private String level;
    private String source;
    /** INITIAL/NEGOTIATING/OPPORTUNITY/DORMANT/CHURNED (见 CustomerStatus) */
    private String status;
    private String phone;
    private String address;
    private String website;
    private String remark;
    /** 1=公海 0=私海 */
    private Integer poolStatus;
    /** 负责人(公海客户为空) */
    private String ownerId;
    private String ownerName;
    private String deptId;
    private LocalDateTime claimTime;
    private LocalDateTime toPoolTime;
    private String toPoolReason;
    private LocalDateTime recentFollowTime;
    private LocalDateTime nextTime;
    private String creatorId;
    private String creatorName;
    private Integer lockVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
