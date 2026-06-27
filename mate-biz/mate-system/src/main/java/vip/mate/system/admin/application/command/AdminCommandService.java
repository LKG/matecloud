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
package vip.mate.system.admin.application.command;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.system.admin.domain.permission.adapter.repository.AdminRepository;
import vip.mate.system.admin.domain.permission.model.aggregate.AdminAggregate;
import vip.mate.system.admin.domain.permission.model.entity.Admin;
import vip.mate.system.admin.types.excel.AdminImportRow;
import vip.mate.system.admin.types.exception.AdminErrorCode;
import vip.mate.base.exception.BizException;
import vip.mate.base.result.BatchResult;
import vip.mate.starter.excel.model.ImportResult;
import vip.mate.starter.tenant.core.TenantContext;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCommandService {

    private static final String DEFAULT_IMPORT_PASSWORD = "123456";
    /** Fallback tenant for single-tenant deployments / when no context is present. */
    private static final String SYSTEM_TENANT_ID = "1";
    /** username pattern: letters/digits/underscore, 4-32 (matches Admin.create downstream). */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{4,32}$");

    private final AdminRepository adminRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Self-reference for {@link #importBatch} so each row hits its own
     * {@code REQUIRES_NEW} transaction. Same Spring AOP gotcha as
     * UserCommandService — without going through the proxy a same-class
     * method call collapses every row into one outer transaction and the
     * first failure rolls back the lot.
     */
    @Lazy
    @Autowired
    private AdminCommandService self;

    /** Batch-import / minimal create (no contact fields). */
    @Transactional
    public String createAdmin(String username, String password, String nickName) {
        return createAdmin(username, password, nickName, null, null, null);
    }

    @Transactional
    public String createAdmin(String username, String password, String nickName,
                              String mobile, String email, String realName) {
        return createAdmin(username, password, nickName, mobile, email, realName, resolveCurrentTenant());
    }

    /**
     * Create an admin explicitly owned by {@code tenantId}. Used by tenant
     * provisioning, which runs in the platform (super-tenant) context but must
     * stamp the freshly-created tenant onto its first admin so that admin can
     * log into their own tenant. Falls back to the system tenant when blank.
     */
    @Transactional
    public String createAdmin(String username, String password, String nickName,
                              String mobile, String email, String realName, String tenantId) {
        if (adminRepository.existsByUsername(username)) {
            throw BizException.of(AdminErrorCode.DUPLICATE_ADMIN_USERNAME);
        }
        String owningTenant = (tenantId == null || tenantId.isBlank()) ? SYSTEM_TENANT_ID : tenantId;
        Admin admin = Admin.create(username, passwordEncoder.encode(password), nickName, owningTenant);
        admin.setMobile(mobile);
        admin.setEmail(email);
        admin.setRealName(realName);
        AdminAggregate agg = AdminAggregate.builder().admin(admin).build();
        adminRepository.save(agg);
        return admin.getId();
    }

    /** Current request tenant, or the system tenant when no context is bound. */
    private String resolveCurrentTenant() {
        String current = TenantContext.getTenantId();
        return (current == null || current.isBlank()) ? SYSTEM_TENANT_ID : current;
    }

    @Transactional
    public void updateAdmin(String id, String nickName, String avatar, String deptId,
                            String mobile, String email, String realName) {
        AdminAggregate agg = findOrThrow(id);
        Admin admin = agg.getAdmin();
        admin.setNickName(nickName);
        admin.setAvatar(avatar);
        admin.setDeptId(deptId);
        admin.setMobile(mobile);
        admin.setEmail(email);
        admin.setRealName(realName);
        adminRepository.update(agg);
    }

    @Transactional
    public void assignRoles(String adminId, List<String> roleIds) {
        findOrThrow(adminId);
        adminRepository.assignRoles(adminId, roleIds);
    }

    @Transactional
    public void disableAdmin(String id) {
        AdminAggregate agg = findOrThrow(id);
        agg.getAdmin().disable();
        adminRepository.update(agg);
    }

    @Transactional
    public void enableAdmin(String id) {
        AdminAggregate agg = findOrThrow(id);
        agg.getAdmin().enable();
        adminRepository.update(agg);
    }

    @Transactional
    public void deleteAdmin(String id) {
        findOrThrow(id);
        // Real logical delete (deleted=1). The previous update(emptyAdmin) path
        // only reset status to 0 via toPO's null-coalescing and never deleted.
        adminRepository.delete(id);
    }

    /**
     * Self-service profile update: current admin edits THEIR OWN profile fields.
     * Intentionally does NOT touch deptId (department changes are an admin-level
     * operation, not self-service).
     */
    @Transactional
    public void updateMyProfile(String id, String nickName, String avatar,
                                String mobile, String email, String realName) {
        AdminAggregate agg = findOrThrow(id);
        Admin admin = agg.getAdmin();
        admin.setNickName(nickName);
        admin.setAvatar(avatar);
        admin.setMobile(mobile);
        admin.setEmail(email);
        admin.setRealName(realName);
        adminRepository.update(agg);
    }

    /**
     * Self-service password change: the admin verifies their CURRENT password
     * and chooses a new one. Used by the personal-center page after the
     * mate_admin refactor — distinct from {@link #resetPassword}, which is
     * an administrator resetting SOMEONE ELSE's password (no old-password check).
     */
    @Transactional
    public void changePassword(String id, String rawOldPassword, String rawNewPassword) {
        if (rawNewPassword == null || rawNewPassword.length() < 6) {
            throw BizException.of(AdminErrorCode.PASSWORD_TOO_SHORT);
        }
        AdminAggregate agg = findOrThrow(id);
        Admin admin = agg.getAdmin();
        if (rawOldPassword == null
                || !passwordEncoder.matches(rawOldPassword, admin.getPassword())) {
            throw BizException.of(AdminErrorCode.PASSWORD_INCORRECT);
        }
        admin.changePassword(passwordEncoder.encode(rawNewPassword));
        adminRepository.update(agg);
        log.info("Admin {} self-changed password", id);
    }

    /**
     * Reset another admin's password to a caller-provided value.
     *
     * <p>Authorization (only {@code sys:admin:reset} can call this) is enforced
     * upstream by Sa-Token. Callers should NOT attempt to do their own
     * permission check here.
     */
    @Transactional
    public void resetPassword(String id, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw BizException.of(AdminErrorCode.PASSWORD_TOO_SHORT);
        }
        AdminAggregate agg = findOrThrow(id);
        agg.getAdmin().changePassword(passwordEncoder.encode(newPassword));
        adminRepository.update(agg);
        // Kick every active session for this admin so the old token stops
        // working immediately. Best-effort: a Redis hiccup here must not
        // mask the password change itself, which is already persisted.
        try {
            // Sa-Token's kickout marks every active token for the adminId as
            // invalid (NOT_TOKEN on the next request). Forces re-login.
            StpUtil.kickout(id);
        } catch (Exception e) {
            log.warn("Failed to invalidate sessions for adminId={}: {}", id, e.getMessage());
        }
        log.info("Admin {} password reset — all sessions invalidated", id);
    }

    private AdminAggregate findOrThrow(String id) {
        AdminAggregate agg = adminRepository.findById(id);
        if (agg == null) throw BizException.of(AdminErrorCode.ADMIN_NOT_EXIST);
        return agg;
    }

    // ========================= IMPORT =========================

    /**
     * Bulk create admins from parsed Excel rows. Same shape as
     * UserCommandService.importBatch: per-row REQUIRES_NEW so a duplicate
     * username on row 5 doesn't roll back rows 1-4.
     */
    public ImportResult importBatch(List<AdminImportRow> rows) {
        ImportResult result = ImportResult.builder()
                .total(rows == null ? 0 : rows.size())
                .success(0)
                .fail(0)
                .build();
        if (rows == null || rows.isEmpty()) return result;

        for (int i = 0; i < rows.size(); i++) {
            AdminImportRow row = rows.get(i);
            int excelRow = i + 2; // header at row 1
            try {
                self.importSingleAdmin(row);
                result.incrementSuccess();
            } catch (Exception e) {
                String msg = (e instanceof BizException biz) ? biz.getMsg() : e.getMessage();
                result.addError(String.format("Row %d: %s", excelRow, msg));
                log.warn("Import admin row {} failed: {}", excelRow, msg);
            }
        }
        log.info("Admin import finished: total={}, success={}, fail={}",
                result.getTotal(), result.getSuccess(), result.getFail());
        return result;
    }

    /**
     * One-row import on its own transaction. Re-uses {@link #createAdmin} so
     * the duplicate-username check + BCrypt encoding stay in lockstep with
     * the interactive "create admin" path.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void importSingleAdmin(AdminImportRow row) {
        if (row == null) {
            throw new BizException("IMPORT_ROW_NULL", "Empty row");
        }
        if (row.getUsername() == null || row.getUsername().isBlank()) {
            throw new BizException("IMPORT_USERNAME_BLANK", "Username is required");
        }
        if (!USERNAME_PATTERN.matcher(row.getUsername()).matches()) {
            throw new BizException("IMPORT_USERNAME_INVALID",
                    "Username must be 4-32 letters/digits/underscores: " + row.getUsername());
        }
        String password = (row.getPassword() == null || row.getPassword().isBlank())
                ? DEFAULT_IMPORT_PASSWORD
                : row.getPassword();
        if (password.length() < 6 || password.length() > 64) {
            throw BizException.of(AdminErrorCode.PASSWORD_TOO_SHORT);
        }
        String nick = (row.getNickName() == null || row.getNickName().isBlank())
                ? row.getUsername()
                : row.getNickName();
        createAdmin(row.getUsername(), password, nick);
    }

    // ========================= BATCH OPS =========================

    // Route per-id calls through the `self` proxy so each invocation crosses the
    // Spring AOP boundary and actually gets its own @Transactional (a plain
    // `this::` reference is self-invocation and would bypass the proxy).
    public BatchResult batchEnable(List<String> ids) { return runBatch(ids, self::enableAdmin); }

    public BatchResult batchDisable(List<String> ids) { return runBatch(ids, self::disableAdmin); }

    public BatchResult batchDelete(List<String> ids) { return runBatch(ids, self::deleteAdmin); }

    /**
     * Per-id loop with isolated failures. Same shape as
     * UserCommandService#runBatch — both intentionally avoid wrapping the
     * loop in {@code @Transactional} so a single bad id doesn't roll the
     * whole batch back. Each per-id call carries its own transaction.
     */
    private BatchResult runBatch(List<String> ids, Consumer<String> action) {
        BatchResult result = BatchResult.empty();
        if (ids == null || ids.isEmpty()) return result;
        for (String id : ids) {
            try {
                action.accept(id);
                result.incrementSuccess();
            } catch (Exception e) {
                String msg = (e instanceof BizException biz) ? biz.getMsg() : e.getMessage();
                result.addFailure(id, msg);
                log.warn("Admin batch op failed for id={}: {}", id, msg);
            }
        }
        log.info("Admin batch op finished: success={}, fail={}",
                result.getSuccessCount(), result.getFailCount());
        return result;
    }
}
