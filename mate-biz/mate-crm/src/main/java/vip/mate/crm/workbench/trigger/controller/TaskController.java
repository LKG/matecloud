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
package vip.mate.crm.workbench.trigger.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.base.result.PageResult;
import vip.mate.base.result.Result;
import vip.mate.crm.query.dto.AdvancedQuery;
import vip.mate.crm.workbench.application.command.TaskCommand;
import vip.mate.crm.workbench.application.command.TaskCommandService;
import vip.mate.crm.workbench.application.query.TaskQueryService;
import vip.mate.crm.workbench.domain.model.entity.Task;

import java.util.List;

/**
 * 待办任务 REST 入口(/api/v1/crm/tasks): 今日待办 / 列表 / 完成 / 取消。
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/crm/tasks")
@RequiredArgsConstructor
@SaCheckLogin
public class TaskController {

    private final TaskCommandService commandService;
    private final TaskQueryService queryService;

    @GetMapping
    @SaCheckPermission("crm:task:list")
    public Result<PageResult<Task>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean onlyMine,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(queryService.page(keyword, status, type, onlyMine, pageNum, pageSize));
    }

    /** 条件查询(动态高级筛选)。 */
    @PostMapping("/search")
    @SaCheckPermission("crm:task:list")
    public Result<PageResult<Task>> search(@RequestBody AdvancedQuery query) {
        return Result.ok(queryService.search(query));
    }

    @GetMapping("/today")
    @SaCheckPermission("crm:task:list")
    public Result<List<Task>> today() {
        return Result.ok(queryService.today());
    }

    @GetMapping("/{id}")
    @SaCheckPermission("crm:task:list")
    public Result<Task> detail(@PathVariable String id) {
        return Result.ok(queryService.findById(id));
    }

    @PostMapping
    @SaCheckPermission("crm:task:add")
    public Result<String> create(@RequestBody TaskCommand cmd) {
        return Result.ok(commandService.create(cmd));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("crm:task:add")
    public Result<Void> update(@PathVariable String id, @RequestBody TaskCommand cmd) {
        commandService.update(id, cmd);
        return Result.ok();
    }

    @PutMapping("/{id}/done")
    @SaCheckPermission("crm:task:done")
    public Result<Void> complete(@PathVariable String id) {
        commandService.complete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/cancel")
    @SaCheckPermission("crm:task:done")
    public Result<Void> cancel(@PathVariable String id) {
        commandService.cancel(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("crm:task:delete")
    public Result<Void> delete(@PathVariable String id) {
        commandService.delete(id);
        return Result.ok();
    }
}
