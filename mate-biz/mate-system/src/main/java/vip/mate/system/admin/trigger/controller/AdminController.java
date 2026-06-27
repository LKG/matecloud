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
import cn.dev33.satoken.stp.StpUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vip.mate.system.admin.application.command.AdminCommandService;
import vip.mate.system.admin.application.query.IAdminQueryService;
import vip.mate.system.admin.application.query.IAdminQueryService.AdminVO;
import vip.mate.system.admin.trigger.annotation.OperationLog;
import vip.mate.system.admin.types.excel.AdminExportRow;
import vip.mate.system.admin.types.excel.AdminImportRow;
import vip.mate.system.admin.types.security.Perms;
import vip.mate.base.result.BatchResult;
import vip.mate.base.result.PageResult;
import vip.mate.base.result.Result;
import vip.mate.starter.excel.annotation.ExcelExport;
import vip.mate.starter.excel.annotation.ExcelImport;
import vip.mate.starter.excel.model.ImportResult;

import java.util.List;

/**
 * Admin user CRUD + role assignment + Excel import/export.
 *
 * <p>Path: {@code /api/v1/admin/admins}.
 * <p>Auth: requires Sa-Token login on every endpoint; per-action permissions
 * enforced through {@link SaCheckPermission} (see {@link Perms}).
 * <p>Tenant: writes inherit the caller's tenant from the Sa-Token session;
 * reads filter by tenant via the mate-tenant-starter MyBatis interceptor.
 *
 * @author MateCloud
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/admin/admins")
@RequiredArgsConstructor
@SaCheckLogin
public class AdminController {

    private final AdminCommandService commandService;
    private final IAdminQueryService queryService;

    /** 创建管理员 */
    @PostMapping
    @SaCheckPermission(Perms.ADMIN_ADD)
    @OperationLog(module = "管理员", type = "新增")
    public Result<String> create(@Valid @RequestBody CreateAdminReq req) {
        return Result.ok(commandService.createAdmin(req.username(), req.password(), req.nickName(),
                req.mobile(), req.email(), req.realName()));
    }

    /** 管理员详情 */
    @GetMapping("/{id}")
    @SaCheckPermission(Perms.ADMIN_LIST)
    public Result<AdminVO> getById(@PathVariable String id) {
        return Result.ok(queryService.findById(id));
    }

    /** 管理员列表（分页） */
    @GetMapping
    @SaCheckPermission(Perms.ADMIN_LIST)
    public Result<PageResult<AdminVO>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.ok(queryService.pageQuery(keyword, pageNum, pageSize));
    }

    /** 更新管理员信息 */
    @PutMapping("/{id}")
    @SaCheckPermission(Perms.ADMIN_EDIT)
    @OperationLog(module = "管理员", type = "修改")
    public Result<Void> update(@PathVariable String id, @RequestBody UpdateAdminReq req) {
        commandService.updateAdmin(id, req.nickName(), req.avatar(), req.deptId(),
                req.mobile(), req.email(), req.realName());
        return Result.ok();
    }

    /** 分配角色 */
    @PutMapping("/{id}/roles")
    @SaCheckPermission(Perms.ADMIN_EDIT)
    @OperationLog(module = "管理员", type = "授权", description = "分配角色")
    public Result<Void> assignRoles(@PathVariable String id, @RequestBody List<String> roleIds) {
        commandService.assignRoles(id, roleIds);
        return Result.ok();
    }

    /** 禁用管理员 */
    @PutMapping("/{id}/disable")
    @SaCheckPermission(Perms.ADMIN_EDIT)
    @OperationLog(module = "管理员", type = "修改", description = "禁用管理员")
    public Result<Void> disable(@PathVariable String id) {
        commandService.disableAdmin(id);
        return Result.ok();
    }

    /** 启用管理员 */
    @PutMapping("/{id}/enable")
    @SaCheckPermission(Perms.ADMIN_EDIT)
    @OperationLog(module = "管理员", type = "修改", description = "启用管理员")
    public Result<Void> enable(@PathVariable String id) {
        commandService.enableAdmin(id);
        return Result.ok();
    }

    /** 删除管理员 */
    @DeleteMapping("/{id}")
    @SaCheckPermission(Perms.ADMIN_DELETE)
    @OperationLog(module = "管理员", type = "删除")
    public Result<Void> delete(@PathVariable String id) {
        commandService.deleteAdmin(id);
        return Result.ok();
    }

    /** Reset another admin's password — caller supplies the new value. */
    @PutMapping("/{id}/password/reset")
    @SaCheckPermission(Perms.ADMIN_RESET)
    @OperationLog(module = "管理员", type = "修改", description = "重置密码")
    public Result<Void> resetPassword(@PathVariable String id, @Valid @RequestBody ResetPasswordReq req) {
        commandService.resetPassword(id, req.newPassword());
        return Result.ok();
    }

    // ---------- Self-service (个人中心) ----------
    // Target the CURRENT login (mate_admin) — NOT mate_user. Before the
    // mate_admin auth refactor, profile/password lived on /users/* which
    // queried mate_user; that no longer matches the logged-in subject.

    /** Self-service profile update: current admin edits THEIR OWN profile.
     *  Uses updateMyProfile (NOT updateAdmin) so deptId is preserved. */
    @PutMapping("/profile")
    @OperationLog(module = "管理员", type = "修改", description = "更新个人资料")
    public Result<Void> updateMyProfile(@RequestBody UpdateMyProfileReq req) {
        Object loginId = StpUtil.getLoginId();
        commandService.updateMyProfile(loginId.toString(),
                req.nickName(), req.avatar(), req.mobile(), req.email(), req.realName());
        return Result.ok();
    }

    /** Self-service password change: verifies CURRENT password, sets new one. */
    @PutMapping("/password")
    @OperationLog(module = "管理员", type = "修改", description = "修改密码")
    public Result<Void> changeMyPassword(@Valid @RequestBody ChangePasswordReq req) {
        Object loginId = StpUtil.getLoginId();
        commandService.changePassword(loginId.toString(), req.oldPassword(), req.newPassword());
        return Result.ok();
    }

    // ---------- Excel ----------

    /**
     * Excel export. {@link vip.mate.starter.excel.advice.ExcelResponseBodyAdvice}
     * intercepts the {@code List<T>} return and writes an .xlsx stream — the
     * controller doesn't return {@code Result<...>} for this method.
     */
    @GetMapping("/export")
    @SaCheckPermission(Perms.ADMIN_LIST)
    @ExcelExport(fileName = "admins", dataClass = AdminExportRow.class, sheetName = "Admins")
    public List<AdminExportRow> exportAdmins() {
        return queryService.exportRows();
    }

    /** Header-only template — empty list lets EasyExcel emit just row 1. */
    @GetMapping("/import/template")
    @SaCheckPermission(Perms.ADMIN_ADD)
    @ExcelExport(fileName = "admins-import-template", dataClass = AdminImportRow.class, sheetName = "Admins")
    public List<AdminImportRow> importTemplate() {
        return List.of();
    }

    /** Bulk import. Per-row failures don't abort the batch — see {@link AdminCommandService#importBatch}. */
    @PostMapping("/import")
    @SaCheckPermission(Perms.ADMIN_ADD)
    @OperationLog(module = "管理员", type = "导入")
    public Result<ImportResult> importAdmins(
            @ExcelImport(dataClass = AdminImportRow.class) List<AdminImportRow> rows) {
        return Result.ok(commandService.importBatch(rows));
    }

    // ---------- Batch ops ----------

    /** 批量启用管理员 */
    @PostMapping("/batch-enable")
    @SaCheckPermission(Perms.ADMIN_EDIT)
    @OperationLog(module = "管理员", type = "修改", description = "批量启用")
    public Result<BatchResult> batchEnable(@RequestBody List<String> ids) {
        return Result.ok(commandService.batchEnable(ids));
    }

    /** 批量禁用管理员 */
    @PostMapping("/batch-disable")
    @SaCheckPermission(Perms.ADMIN_EDIT)
    @OperationLog(module = "管理员", type = "修改", description = "批量禁用")
    public Result<BatchResult> batchDisable(@RequestBody List<String> ids) {
        return Result.ok(commandService.batchDisable(ids));
    }

    /** 批量删除管理员 */
    @PostMapping("/batch-delete")
    @SaCheckPermission(Perms.ADMIN_DELETE)
    @OperationLog(module = "管理员", type = "删除", description = "批量删除")
    public Result<BatchResult> batchDelete(@RequestBody List<String> ids) {
        return Result.ok(commandService.batchDelete(ids));
    }

    record CreateAdminReq(@NotBlank @Size(min = 3, max = 32) String username,
                          @NotBlank @Size(min = 6, max = 64) String password,
                          String nickName,
                          String mobile, String email, String realName) {}
    record UpdateAdminReq(String nickName, String avatar, String deptId,
                          String mobile, String email, String realName) {}
    record ResetPasswordReq(@NotBlank @Size(min = 6, max = 64) String newPassword) {}

    // ---- self-service (个人中心) DTOs ----
    record UpdateMyProfileReq(String nickName, String avatar,
                              String mobile, String email, String realName) {}
    record ChangePasswordReq(@NotBlank String oldPassword,
                             @NotBlank @Size(min = 6, max = 64) String newPassword) {}
}
