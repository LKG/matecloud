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
package vip.mate.system.admin.trigger.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vip.mate.system.admin.application.command.DeptCommandService;
import vip.mate.system.admin.application.query.IDeptQueryService;
import vip.mate.system.admin.domain.permission.model.entity.Dept;
import vip.mate.system.admin.trigger.annotation.OperationLog;
import vip.mate.system.admin.types.security.Perms;
import vip.mate.base.result.Result;

import java.util.List;

/**
 * Department (organization unit) management.
 *
 * <p>Path: {@code /api/v1/admin/depts}. A self-referencing hierarchy that also
 * anchors data-permission (data scope) filtering — see {@code @DataPermission}.
 *
 * <p>Auth: requires login; mutations gated by {@code sys:dept:*} permission
 * codes. The tree read is login-only so it can feed dept select dropdowns
 * across the admin UI.
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/admin/depts")
@RequiredArgsConstructor
@SaCheckLogin
public class DeptController {

    private final DeptCommandService commandService;
    private final IDeptQueryService queryService;

    @GetMapping("/tree")
    public Result<List<Dept>> tree() {
        return Result.ok(queryService.findTree());
    }

    @GetMapping
    public Result<List<Dept>> list() {
        return Result.ok(queryService.findAll());
    }

    @PostMapping
    @SaCheckPermission(Perms.DEPT_ADD)
    @OperationLog(module = "部门", type = "新增")
    public Result<String> create(@Valid @RequestBody CreateDeptReq req) {
        return Result.ok(commandService.createDept(
                req.parentId(), req.deptName(), req.sort(), req.leader(), req.phone(), req.email()));
    }

    @PutMapping("/{id}")
    @SaCheckPermission(Perms.DEPT_EDIT)
    @OperationLog(module = "部门", type = "修改")
    public Result<Void> update(@PathVariable String id, @RequestBody UpdateDeptReq req) {
        commandService.updateDept(id, req.deptName(), req.sort(),
                req.leader(), req.phone(), req.email(), req.status());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission(Perms.DEPT_DELETE)
    @OperationLog(module = "部门", type = "删除")
    public Result<Void> delete(@PathVariable String id) {
        commandService.deleteDept(id);
        return Result.ok();
    }

    record CreateDeptReq(String parentId, @NotBlank String deptName, Integer sort,
                         String leader, String phone, String email) {}

    record UpdateDeptReq(@NotBlank String deptName, Integer sort,
                         String leader, String phone, String email, Integer status) {}
}
