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
package vip.mate.crm.approval.domain.model.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批实例实体(通用: 业务类型 + 单据ID + 节点链快照)。
 *
 * @author mateaix
 */
@Data
@Builder
public class Approval {

    private String id;
    /** CONTRACT/INVOICE/DISCOUNT */
    private String bizType;
    private String bizId;
    private String bizNo;
    private String title;
    private String applicantId;
    private String applicantName;
    private String deptId;
    /** PENDING/APPROVED/REJECTED/CANCELED (见 ApprovalStatus) */
    private String status;
    /** 当前审批节点下标(-1=已结束) */
    private Integer currentNode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
