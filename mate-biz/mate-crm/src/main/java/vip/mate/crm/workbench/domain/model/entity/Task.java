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
package vip.mate.crm.workbench.domain.model.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待办任务实体(状态流转只经 {@code TaskAggregate})。
 *
 * @author mateaix
 */
@Data
@Builder
public class Task {

    private String id;
    private String title;
    /** FOLLOW跟进/RETURN_VISIT回访/MEETING会议/OTHER其他 */
    private String type;
    /** 关联对象类型: CUSTOMER/BUSINESS/CONTRACT/LEAD */
    private String bizType;
    private String bizId;
    private String bizName;
    private String content;
    private LocalDateTime dueTime;
    /** HIGH/MEDIUM/LOW */
    private String priority;
    /** OPEN/DONE/CANCELED (见 TaskStatus) */
    private String status;
    private LocalDateTime finishTime;
    /** MANUAL手动/AI智能生成 */
    private String source;
    private String ownerId;
    private String ownerName;
    private String deptId;
    private String creatorId;
    private String creatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
