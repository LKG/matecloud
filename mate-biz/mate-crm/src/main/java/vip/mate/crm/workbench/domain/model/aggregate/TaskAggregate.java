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
package vip.mate.crm.workbench.domain.model.aggregate;

import lombok.Builder;
import lombok.Getter;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;
import vip.mate.crm.common.CrmCurrentUser;
import vip.mate.crm.types.exception.CrmErrorCode;
import vip.mate.crm.workbench.domain.model.entity.Task;
import vip.mate.crm.workbench.types.enums.TaskStatus;
import vip.mate.starter.distribute.util.SnowflakeUtil;

import java.time.LocalDateTime;

/**
 * 待办任务聚合根: 待办 → 已完成 / 已取消。AI 可生成(source=AI)。
 *
 * <p>不变式: 仅待办任务可完成或取消; 完成时回填 finishTime。
 *
 * @author mateaix
 */
@Getter
@Builder
public class TaskAggregate {

    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_AI = "AI";

    private final Task task;

    public static TaskAggregate create(String title, String type, String bizType, String bizId, String bizName,
                                       String content, LocalDateTime dueTime, String priority,
                                       String source, CrmCurrentUser operator) {
        if (title == null || title.isBlank()) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "任务标题不能为空");
        }
        Task task = Task.builder()
                .title(title)
                .type(type == null || type.isBlank() ? "FOLLOW" : type)
                .bizType(bizType)
                .bizId(bizId)
                .bizName(bizName)
                .content(content)
                .dueTime(dueTime)
                .priority(priority == null || priority.isBlank() ? "MEDIUM" : priority)
                .status(TaskStatus.OPEN.name())
                .source(source == null || source.isBlank() ? SOURCE_MANUAL : source)
                .ownerId(operator.userId())
                .ownerName(operator.displayName())
                .deptId(operator.deptId())
                .creatorId(operator.userId())
                .creatorName(operator.displayName())
                .build();
        task.setId(SnowflakeUtil.newSnowflakeId());
        return TaskAggregate.builder().task(task).build();
    }

    public static TaskAggregate of(Task task) {
        if (task == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        return TaskAggregate.builder().task(task).build();
    }

    public void updateInfo(String title, String content, LocalDateTime dueTime, String priority) {
        assertOpen();
        if (title != null && !title.isBlank()) {
            task.setTitle(title);
        }
        task.setContent(content);
        task.setDueTime(dueTime);
        if (priority != null && !priority.isBlank()) {
            task.setPriority(priority);
        }
    }

    /** 完成: 待办 → 已完成。 */
    public void complete() {
        assertOpen();
        task.setStatus(TaskStatus.DONE.name());
        task.setFinishTime(LocalDateTime.now());
    }

    /** 取消: 待办 → 已取消。 */
    public void cancel() {
        assertOpen();
        task.setStatus(TaskStatus.CANCELED.name());
    }

    /** 是否逾期(待办且截止时间已过)。 */
    public boolean isOverdue() {
        return TaskStatus.of(task.getStatus()).isOpen()
                && task.getDueTime() != null && task.getDueTime().isBefore(LocalDateTime.now());
    }

    public String getId() {
        return task.getId();
    }

    private void assertOpen() {
        if (!TaskStatus.of(task.getStatus()).isOpen()) {
            throw new BizException(CrmErrorCode.NO_PERMISSION.getCode(), "任务已结束，不可再操作");
        }
    }
}
