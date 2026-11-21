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
package vip.mate.crm.workbench.domain.adapter.repository;

import vip.mate.base.result.PageResult;
import vip.mate.crm.common.CrmScope;
import vip.mate.crm.workbench.domain.model.aggregate.TaskAggregate;
import vip.mate.crm.workbench.domain.model.entity.Task;

import java.util.List;

/**
 * 待办任务仓储接口(domain 定义, infrastructure 实现)。
 *
 * @author mateaix
 */
public interface ITaskRepository {

    void save(TaskAggregate aggregate);

    void update(TaskAggregate aggregate);

    TaskAggregate findById(String id);

    void deleteById(String id);

    PageResult<Task> page(String keyword, String status, String type, Boolean onlyOpen,
                          CrmScope scope, int pageNum, int pageSize);

    /** 今日待办(负责人=userId, 未完成)。 */
    List<Task> listTodayOpen(String ownerId);

    /** 某业务对象关联的未完成任务(AI 建议/详情用)。 */
    List<Task> listOpenByBiz(String bizType, String bizId);
}
