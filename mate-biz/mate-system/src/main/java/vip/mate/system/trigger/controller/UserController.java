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
package vip.mate.system.trigger.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
import vip.mate.api.system.command.RegisterUserCommand;
import vip.mate.api.system.response.UserInfoResponse;
import vip.mate.base.exception.BizException;
import vip.mate.base.result.BatchResult;
import vip.mate.base.result.Result;
import vip.mate.starter.excel.annotation.ExcelExport;
import vip.mate.starter.excel.annotation.ExcelImport;
import vip.mate.starter.excel.model.ImportResult;
import vip.mate.system.application.command.UserCommandService;
import vip.mate.system.application.query.IUserQueryService;
import vip.mate.system.types.exception.UserErrorCode;
import vip.mate.system.types.excel.UserExportRow;
import vip.mate.system.types.excel.UserImportRow;
import vip.mate.system.types.security.Perms;

import vip.mate.base.result.PageResult;

import java.util.List;

/**
 * End-user account management (distinct from {@code AdminController} which
 * manages admin staff accounts). Covers create / update / freeze / unfreeze /
 * delete / reset-password / Excel import-export plus self-service password
 * change for the currently logged-in user.
 *
 * <p>Path: {@code /api/v1/users}.
 * <p>Auth: every endpoint requires a Sa-Token session; mutating endpoints
 * additionally require fine-grained permissions defined in {@link Perms}.
 * <p>Tenant: the mate-tenant-starter interceptor scopes every query to the
 * caller's tenant — admin staff cannot read users from other tenants.
 *
 * @author MateCloud
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SaCheckLogin
public class UserController {

    private final UserCommandService userCommandService;
    private final IUserQueryService userQueryService;

    /** 创建用户 */
    @PostMapping
    @SaCheckPermission(Perms.USER_ADD)
    public Result<String> createUser(@Valid @RequestBody RegisterUserCommand command) {
        return Result.ok(userCommandService.createUser(command));
    }

    /** 用户列表（分页） */
    @GetMapping
    @SaCheckPermission(Perms.USER_LIST)
    public Result<PageResult<UserInfoResponse>> listUsers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.ok(userQueryService.pageQuery(keyword, pageNum, pageSize));
    }

    /**
     * Excel export. {@code ExcelResponseBodyAdvice} (mate-excel-starter)
     * intercepts methods annotated with {@code @ExcelExport} whose return is
     * a {@code List<T>}, and writes an {@code .xlsx} stream to the HTTP
     * response instead of the usual JSON {@code Result}.
     *
     * <p>The endpoint does NOT return {@code Result<...>} — the advice
     * bypasses Spring's body converter entirely — so the controller signature
     * is just the raw list.
     */
    @GetMapping("/export")
    @SaCheckPermission(Perms.USER_LIST)
    @ExcelExport(fileName = "users", dataClass = UserExportRow.class, sheetName = "Users")
    public List<UserExportRow> exportUsers(@RequestParam(required = false) String keyword) {
        return userQueryService.exportRows(keyword);
    }

    /**
     * Template download — the admin clicks "Download template" in the import
     * dialog and gets a single-row {@code .xlsx} with just the header. Easiest
     * way to hand them a correctly-shaped file without hand-authoring an
     * example row.
     *
     * <p>Implemented as a one-row {@code @ExcelExport} returning an empty list
     * so EasyExcel writes just the header row from the {@link UserImportRow}
     * {@code @ExcelProperty} annotations — no need for a separate template
     * renderer.
     */
    @GetMapping("/import/template")
    @SaCheckPermission(Perms.USER_ADD)
    @ExcelExport(fileName = "users-import-template", dataClass = UserImportRow.class, sheetName = "Users")
    public List<UserImportRow> importTemplate() {
        return List.of();
    }

    /**
     * Bulk import from an uploaded {@code .xlsx}. The {@link ExcelImport}
     * argument resolver (mate-excel-starter) reads the multipart file, binds
     * each data row to a {@link UserImportRow}, and hands the list over.
     * The command service then processes each row on its own transaction
     * boundary so partial failure is tolerated.
     *
     * <p>Response is {@link ImportResult} with {@code total/success/fail} and
     * a list of row-level error messages for the UI to surface.
     */
    @PostMapping("/import")
    @SaCheckPermission(Perms.USER_ADD)
    public Result<ImportResult> importUsers(
            @ExcelImport(dataClass = UserImportRow.class) List<UserImportRow> rows) {
        return Result.ok(userCommandService.importBatch(rows));
    }

    /**
     * Self-service password change.
     *
     * <p>Mapped on a literal path so it sits BEFORE {@code /{id}} in routing
     * order (Spring's {@code AntPathMatcher} prefers the most specific match,
     * but the literal "password" segment also reads more clearly than
     * {@code /{id}/password} for the "current user" use case). The user id is
     * resolved from the Sa-Token session — the request body intentionally
     * does NOT carry a userId, preventing cross-user password changes.
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordReq req) {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            throw BizException.of(UserErrorCode.NOT_LOGGED_IN);
        }
        userCommandService.changePassword(loginId.toString(), req.oldPassword(), req.newPassword());
        return Result.ok();
    }

    /** 用户详情 */
    @GetMapping("/{id}")
    @SaCheckPermission(Perms.USER_LIST)
    public Result<UserInfoResponse> getUser(@PathVariable String id) {
        return Result.ok(userQueryService.findById(id));
    }

    /** Generic profile update — accepts a partial body, null fields are skipped. */
    @PutMapping("/{id}")
    @SaCheckPermission(Perms.USER_EDIT)
    public Result<Void> updateUser(@PathVariable String id, @RequestBody UpdateUserReq req) {
        userCommandService.updateProfile(id, req.realName(), req.mobile(),
                req.email(), req.avatar(), req.gender());
        return Result.ok();
    }

    /** Kept for backward compat — frontend new code uses {@link #updateUser}. */
    @PutMapping("/{id}/realName")
    @SaCheckPermission(Perms.USER_EDIT)
    public Result<Void> changeRealName(@PathVariable String id, @RequestParam String realName) {
        userCommandService.changeRealName(id, realName);
        return Result.ok();
    }

    /** 冻结用户 */
    @PutMapping("/{id}/freeze")
    @SaCheckPermission(Perms.USER_EDIT)
    public Result<Void> freeze(@PathVariable String id) {
        userCommandService.freezeUser(id);
        return Result.ok();
    }

    /** 解冻用户 */
    @PutMapping("/{id}/unfreeze")
    @SaCheckPermission(Perms.USER_EDIT)
    public Result<Void> unfreeze(@PathVariable String id) {
        userCommandService.unfreezeUser(id);
        return Result.ok();
    }

    /** Admin reset of someone else's password. Path is /{id}/password/reset. */
    @PutMapping("/{id}/password/reset")
    @SaCheckPermission(Perms.USER_RESET)
    public Result<Void> resetPassword(@PathVariable String id, @Valid @RequestBody ResetPasswordReq req) {
        userCommandService.resetPassword(id, req.newPassword());
        return Result.ok();
    }

    /** 删除用户 */
    @DeleteMapping("/{id}")
    @SaCheckPermission(Perms.USER_DELETE)
    public Result<Void> delete(@PathVariable String id) {
        userCommandService.deleteUser(id);
        return Result.ok();
    }

    // ---------- Batch ops ----------

    /**
     * Batch freeze. POST instead of PUT because the body is a list of ids
     * (PUT semantically replaces, POST adds an action — RFC 9110). Returns
     * {@link BatchResult} so the UI can show "8 ok / 2 failed" plus a
     * per-id error map.
     */
    @PostMapping("/batch-freeze")
    @SaCheckPermission(Perms.USER_EDIT)
    public Result<BatchResult> batchFreeze(@RequestBody List<String> ids) {
        return Result.ok(userCommandService.batchFreeze(ids));
    }

    /** 批量解冻用户 */
    @PostMapping("/batch-unfreeze")
    @SaCheckPermission(Perms.USER_EDIT)
    public Result<BatchResult> batchUnfreeze(@RequestBody List<String> ids) {
        return Result.ok(userCommandService.batchUnfreeze(ids));
    }

    /** 批量删除用户 */
    @PostMapping("/batch-delete")
    @SaCheckPermission(Perms.USER_DELETE)
    public Result<BatchResult> batchDelete(@RequestBody List<String> ids) {
        return Result.ok(userCommandService.batchDelete(ids));
    }

    record ChangePasswordReq(@NotBlank String oldPassword,
                             @NotBlank @Size(min = 6, max = 64) String newPassword) {}
    record ResetPasswordReq(@NotBlank @Size(min = 6, max = 64) String newPassword) {}
    record UpdateUserReq(String realName, String mobile, String email, String avatar, Integer gender) {}
}
