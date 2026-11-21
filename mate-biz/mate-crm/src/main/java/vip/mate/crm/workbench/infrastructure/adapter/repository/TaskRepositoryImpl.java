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
package vip.mate.crm.workbench.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.base.result.PageResult;
import vip.mate.crm.common.CrmScope;
import vip.mate.crm.query.dto.AdvancedQuery;
import vip.mate.crm.query.infrastructure.CrmSearchSupport;
import vip.mate.crm.workbench.domain.adapter.repository.ITaskRepository;
import vip.mate.crm.workbench.domain.model.aggregate.TaskAggregate;
import vip.mate.crm.workbench.domain.model.entity.Task;
import vip.mate.crm.workbench.infrastructure.dao.TaskDao;
import vip.mate.crm.workbench.infrastructure.dao.po.TaskPO;
import vip.mate.crm.workbench.types.enums.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 待办任务仓储实现。 */
@Repository
@RequiredArgsConstructor
public class TaskRepositoryImpl implements ITaskRepository {

    private final TaskDao taskDao;
    private final CrmSearchSupport crmSearchSupport;

    @Override
    public void save(TaskAggregate aggregate) {
        taskDao.insert(toPO(aggregate.getTask()));
    }

    @Override
    public void update(TaskAggregate aggregate) {
        taskDao.updateById(toPO(aggregate.getTask()));
    }

    @Override
    public TaskAggregate findById(String id) {
        TaskPO po = taskDao.selectById(id);
        return po == null ? null : TaskAggregate.of(toEntity(po));
    }

    @Override
    public void deleteById(String id) {
        taskDao.deleteById(id);
    }

    @Override
    public PageResult<Task> page(String keyword, String status, String type, Boolean onlyOpen,
                                 CrmScope scope, int pageNum, int pageSize) {
        LambdaQueryWrapper<TaskPO> wrapper = new LambdaQueryWrapper<>();
        if (notBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(TaskPO::getTitle, kw)
                    .or().like(TaskPO::getBizName, kw)
                    .or().like(TaskPO::getContent, kw));
        }
        wrapper.eq(notBlank(status), TaskPO::getStatus, status)
                .eq(notBlank(type), TaskPO::getType, type);
        if (Boolean.TRUE.equals(onlyOpen)) {
            wrapper.eq(TaskPO::getStatus, TaskStatus.OPEN.name());
        }
        (scope == null ? CrmScope.all() : scope).apply(wrapper, TaskPO::getOwnerId, TaskPO::getDeptId);
        wrapper.orderByAsc(TaskPO::getDueTime).orderByDesc(TaskPO::getCreatedAt);

        Page<TaskPO> result = taskDao.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Task> list = result.getRecords().stream().map(this::toEntity).toList();
        return PageResult.of(list, result.getTotal());
    }

    @Override
    public PageResult<Task> search(AdvancedQuery query, CrmScope scope) {
        Page<TaskPO> result = crmSearchSupport.search(taskDao, "task", query, scope, "owner_id", "dept_id");
        List<Task> list = result.getRecords().stream().map(this::toEntity).toList();
        return PageResult.of(list, result.getTotal());
    }

    @Override
    public List<Task> listTodayOpen(String ownerId) {
        LocalDateTime endOfToday = LocalDate.now().atTime(23, 59, 59);
        LambdaQueryWrapper<TaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskPO::getOwnerId, ownerId)
                .eq(TaskPO::getStatus, TaskStatus.OPEN.name())
                .and(w -> w.le(TaskPO::getDueTime, endOfToday).or().isNull(TaskPO::getDueTime))
                .orderByAsc(TaskPO::getDueTime);
        return taskDao.selectList(wrapper).stream().map(this::toEntity).toList();
    }

    @Override
    public List<Task> listOpenByBiz(String bizType, String bizId) {
        LambdaQueryWrapper<TaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskPO::getBizType, bizType)
                .eq(TaskPO::getBizId, bizId)
                .eq(TaskPO::getStatus, TaskStatus.OPEN.name())
                .orderByDesc(TaskPO::getCreatedAt);
        return taskDao.selectList(wrapper).stream().map(this::toEntity).toList();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private TaskPO toPO(Task t) {
        TaskPO po = new TaskPO();
        po.setId(t.getId());
        po.setTitle(t.getTitle());
        po.setType(t.getType());
        po.setBizType(t.getBizType());
        po.setBizId(t.getBizId());
        po.setBizName(t.getBizName());
        po.setContent(t.getContent());
        po.setDueTime(t.getDueTime());
        po.setPriority(t.getPriority());
        po.setStatus(t.getStatus());
        po.setFinishTime(t.getFinishTime());
        po.setSource(t.getSource());
        po.setOwnerId(t.getOwnerId());
        po.setOwnerName(t.getOwnerName());
        po.setDeptId(t.getDeptId());
        po.setCreatorId(t.getCreatorId());
        po.setCreatorName(t.getCreatorName());
        return po;
    }

    private Task toEntity(TaskPO po) {
        return Task.builder()
                .id(po.getId())
                .title(po.getTitle())
                .type(po.getType())
                .bizType(po.getBizType())
                .bizId(po.getBizId())
                .bizName(po.getBizName())
                .content(po.getContent())
                .dueTime(po.getDueTime())
                .priority(po.getPriority())
                .status(po.getStatus())
                .finishTime(po.getFinishTime())
                .source(po.getSource())
                .ownerId(po.getOwnerId())
                .ownerName(po.getOwnerName())
                .deptId(po.getDeptId())
                .creatorId(po.getCreatorId())
                .creatorName(po.getCreatorName())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
