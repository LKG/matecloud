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
package vip.mate.crm.workbench.application.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.base.result.PageResult;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.workbench.domain.adapter.repository.ITaskRepository;
import vip.mate.crm.workbench.domain.model.aggregate.TaskAggregate;
import vip.mate.crm.workbench.domain.model.entity.Task;

import java.util.List;

/** 待办任务读侧查询服务。 */
@Service
@RequiredArgsConstructor
public class TaskQueryService {

    private final ITaskRepository taskRepository;
    private final CurrentUserResolver currentUserResolver;

    public PageResult<Task> page(String keyword, String status, String type, Boolean onlyMine,
                                 int pageNum, int pageSize) {
        return taskRepository.page(keyword, status, type, onlyMine, currentUserResolver.scope(), pageNum, pageSize);
    }

    /** 我的今日待办(含逾期)。 */
    public List<Task> today() {
        return taskRepository.listTodayOpen(currentUserResolver.require().userId());
    }

    public Task findById(String id) {
        TaskAggregate agg = taskRepository.findById(id);
        return agg == null ? null : agg.getTask();
    }
}
