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
import vip.mate.system.admin.application.command.RoleCommandService;
import vip.mate.system.admin.application.query.IRoleQueryService;
import vip.mate.system.admin.application.query.IRoleQueryService.RoleVO;
import vip.mate.system.admin.trigger.annotation.OperationLog;
import vip.mate.system.admin.types.security.Perms;
import vip.mate.base.result.PageResult;
import vip.mate.base.result.Result;

import java.util.List;

/**
 * Role CRUD plus role ↔ menu / role ↔ admin assignment.
 *
 * <p>Path: {@code /api/v1/admin/roles}.
 * <p>A role's permission set is the union of permissions on the menus assigned
 * to that role (mate_role_menu join). The cached permission list in Redis
 * (key {@code mate:user:perm:<userId>}) is rebuilt at login — change a role
 * assignment and the affected user must re-login to see the new permissions.
 *
 * @author MateCloud
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@SaCheckLogin
public class RoleController {

    private final RoleCommandService commandService;
    private final IRoleQueryService queryService;

    @PostMapping
    @SaCheckPermission(Perms.ROLE_ADD)
    @OperationLog(module = "角色", type = "新增")
    public Result<String> create(@Valid @RequestBody CreateRoleReq req) {
        return Result.ok(commandService.createRole(req.roleKey(), req.roleName(), req.sort()));
    }

    @GetMapping
    @SaCheckPermission(Perms.ROLE_LIST)
    public Result<PageResult<RoleVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.ok(queryService.page(pageNum, pageSize, keyword));
    }

    @GetMapping("/{id}")
    @SaCheckPermission(Perms.ROLE_LIST)
    public Result<RoleVO> getById(@PathVariable String id) {
        return Result.ok(queryService.findById(id));
    }

    @PutMapping("/{id}")
    @SaCheckPermission(Perms.ROLE_EDIT)
    @OperationLog(module = "角色", type = "修改")
    public Result<Void> update(@PathVariable String id, @RequestBody UpdateRoleReq req) {
        commandService.updateRole(id, req.roleName(), req.sort());
        return Result.ok();
    }

    @PutMapping("/{id}/menus")
    @SaCheckPermission(Perms.ROLE_EDIT)
    @OperationLog(module = "角色", type = "授权", description = "分配菜单权限")
    public Result<Void> assignMenus(@PathVariable String id, @RequestBody List<String> menuIds) {
        commandService.assignMenus(id, menuIds);
        return Result.ok();
    }

    @PutMapping("/{id}/data-scope")
    @SaCheckPermission(Perms.ROLE_EDIT)
    @OperationLog(module = "角色", type = "授权", description = "设置数据范围")
    public Result<Void> updateDataScope(@PathVariable String id, @RequestBody DataScopeReq req) {
        commandService.updateDataScope(id, req.dataScope(), req.customDeptIds());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission(Perms.ROLE_DELETE)
    @OperationLog(module = "角色", type = "删除")
    public Result<Void> delete(@PathVariable String id) {
        commandService.deleteRole(id);
        return Result.ok();
    }

    record CreateRoleReq(@NotBlank String roleKey, @NotBlank String roleName, Integer sort) {}
    record UpdateRoleReq(String roleName, Integer sort) {}
    record DataScopeReq(Integer dataScope, String customDeptIds) {}
}
