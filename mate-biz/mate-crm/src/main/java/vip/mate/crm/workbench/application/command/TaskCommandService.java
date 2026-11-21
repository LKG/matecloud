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
package vip.mate.crm.workbench.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.base.exception.BizException;
import vip.mate.crm.common.CrmCurrentUser;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.types.exception.CrmErrorCode;
import vip.mate.crm.workbench.domain.adapter.repository.ITaskRepository;
import vip.mate.crm.workbench.domain.model.aggregate.TaskAggregate;

import java.time.LocalDateTime;

/**
 * 待办任务写侧应用服务: 新增/编辑/完成/取消/删除。AI 生成任务复用 {@link #generate}。
 *
 * @author mateaix
 */
@Service
@RequiredArgsConstructor
public class TaskCommandService {

    private final ITaskRepository taskRepository;
    private final CurrentUserResolver currentUserResolver;

    @Transactional
    public String create(TaskCommand cmd) {
        CrmCurrentUser operator = currentUserResolver.require();
        TaskAggregate agg = TaskAggregate.create(cmd.getTitle(), cmd.getType(), cmd.getBizType(),
                cmd.getBizId(), cmd.getBizName(), cmd.getContent(), cmd.getDueTime(), cmd.getPriority(),
                cmd.getSource(), operator);
        taskRepository.save(agg);
        return agg.getId();
    }

    /** AI 生成待办(负责人可指定; 缺省当前用户)。 */
    @Transactional
    public String generate(String title, String bizType, String bizId, String bizName,
                           String content, LocalDateTime dueTime, String priority, CrmCurrentUser operator) {
        TaskAggregate agg = TaskAggregate.create(title, "FOLLOW", bizType, bizId, bizName, content,
                dueTime, priority, TaskAggregate.SOURCE_AI, operator);
        taskRepository.save(agg);
        return agg.getId();
    }

    @Transactional
    public void update(String id, TaskCommand cmd) {
        TaskAggregate agg = findOrThrow(id);
        agg.updateInfo(cmd.getTitle(), cmd.getContent(), cmd.getDueTime(), cmd.getPriority());
        taskRepository.update(agg);
    }

    @Transactional
    public void complete(String id) {
        TaskAggregate agg = findOrThrow(id);
        agg.complete();
        taskRepository.update(agg);
    }

    @Transactional
    public void cancel(String id) {
        TaskAggregate agg = findOrThrow(id);
        agg.cancel();
        taskRepository.update(agg);
    }

    @Transactional
    public void delete(String id) {
        findOrThrow(id);
        taskRepository.deleteById(id);
    }

    private TaskAggregate findOrThrow(String id) {
        TaskAggregate agg = taskRepository.findById(id);
        if (agg == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        return agg;
    }
}
