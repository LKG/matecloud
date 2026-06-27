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
package vip.mate.system.application.command;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.api.system.command.RegisterUserCommand;
import vip.mate.base.exception.BizException;
import vip.mate.base.result.BatchResult;
import vip.mate.starter.excel.model.ImportResult;
import vip.mate.starter.tenant.core.TenantContext;
import vip.mate.system.domain.adapter.port.DomainEventPublisher;
import vip.mate.system.domain.adapter.repository.UserRepository;
import vip.mate.system.domain.event.UserCreatedEvent;
import vip.mate.system.domain.model.aggregate.UserAggregate;
import vip.mate.system.domain.service.IUserDomainService;
import vip.mate.system.tenant.application.quota.TenantQuotaService;
import vip.mate.system.types.excel.UserImportRow;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCommandService {

    private static final String DEFAULT_IMPORT_PASSWORD = "123456";
    /** Mirrors RegisterUserCommand.username — letters/digits/underscore, 4-32. */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{4,32}$");
    /** Mirrors RegisterUserCommand.mobile — CN mobile. */
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private final UserRepository userRepository;
    private final IUserDomainService userDomainService;
    private final DomainEventPublisher eventPublisher;
    private final TenantQuotaService tenantQuotaService;

    /**
     * Self-reference used only by {@link #importBatch} so each row lands on
     * its own {@code REQUIRES_NEW} transaction boundary. Same-class method
     * calls bypass the Spring AOP proxy, which would collapse all rows into
     * one outer transaction and roll back the entire sheet on the first bad
     * row. {@code @Lazy} sidesteps the chicken-and-egg bean wiring.
     */
    @Lazy
    @Autowired
    private UserCommandService self;

    @Transactional(rollbackFor = Exception.class)
    public String createUser(RegisterUserCommand command) {
        // Enforce the tenant's package user cap before persisting. No-op when
        // there is no tenant context (e.g. the cross-tenant registration path)
        // or when enforcement is otherwise inactive.
        tenantQuotaService.assertCanAddUser(TenantContext.getTenantId());

        UserAggregate aggregate = userDomainService.createUser(
                command.getUsername(),
                command.getPassword(),
                command.getMobile(),
                command.getEmail(),
                command.getRealName());
        userRepository.save(aggregate);

        // Publish on the transactional thread; any listener must use
        // @TransactionalEventListener(AFTER_COMMIT) so it fires only after the
        // user row actually commits (no phantom events on rollback).
        eventPublisher.publish(new UserCreatedEvent(
                aggregate.getId(),
                command.getUsername(),
                command.getMobile()));

        log.info("User created, id={}, username={}, mobile={}",
                aggregate.getId(), command.getUsername(), command.getMobile());
        return aggregate.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeRealName(String userId, String realName) {
        UserAggregate aggregate = userDomainService.changeRealName(userId, realName);
        userRepository.update(aggregate);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(String userId, String realName, String mobile,
                              String email, String avatar, Integer gender) {
        UserAggregate aggregate = userDomainService.updateProfile(
                userId, realName, mobile, email, avatar, gender);
        userRepository.update(aggregate);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(String userId, String oldPassword, String newPassword) {
        UserAggregate aggregate = userDomainService.changePassword(userId, oldPassword, newPassword);
        userRepository.update(aggregate);
        // Force re-login on every active session for this user. Calling outside
        // the transaction would risk session removal then write rollback; we
        // accept the small race because Sa-Token logout is in-Redis and idempotent.
        invalidateSessions(userId);
        log.info("User {} changed password — all sessions invalidated", userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String userId, String newPassword) {
        UserAggregate aggregate = userDomainService.resetPassword(userId, newPassword);
        userRepository.update(aggregate);
        invalidateSessions(userId);
        log.info("Admin reset password for user {} — all sessions invalidated", userId);
    }

    /**
     * Best-effort kick of every Sa-Token session bound to {@code loginId}.
     * Uses {@code logoutByLoginId} which is a no-op if the user is not
     * currently logged in anywhere. We swallow exceptions because session
     * cache problems must not block the password change itself — the new
     * password is already persisted before we get here.
     */
    private void invalidateSessions(String loginId) {
        try {
            // Sa-Token's kickout marks every active token for the loginId as
            // invalid (NOT_TOKEN response on the next request). The user will
            // see "Session expired" and be redirected to login.
            StpUtil.kickout(loginId);
        } catch (Exception e) {
            log.warn("Failed to invalidate sessions for loginId={}: {}", loginId, e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void freezeUser(String userId) {
        UserAggregate aggregate = userDomainService.freezeUser(userId);
        userRepository.update(aggregate);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unfreezeUser(String userId) {
        UserAggregate aggregate = userDomainService.unfreezeUser(userId);
        userRepository.update(aggregate);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(String userId) {
        // Go through the aggregate so the delete enforces the domain invariant
        // (can't delete an already-deleted user), moves status -> DELETED, and
        // records a DELETE operate-stream — consistent with every other state
        // change. Persist that first (row still deleted=0 so @TableLogic lets the
        // UPDATE through), THEN apply the logical delete (deleted=1).
        UserAggregate aggregate = userDomainService.deleteUser(userId);
        userRepository.update(aggregate);
        userRepository.delete(userId);
    }

    // ========================= IMPORT =========================

    /**
     * Bulk create users from parsed Excel rows. Every row lives on its own
     * {@code REQUIRES_NEW} transaction (see {@link #importSingleUser}) so
     * one bad row only rolls back its own insert — the rest of the sheet
     * still lands, and the caller receives a summary plus per-row error
     * messages that reference the Excel row index (1-based, header = row 1).
     */
    public ImportResult importBatch(List<UserImportRow> rows) {
        ImportResult result = ImportResult.builder()
                .total(rows == null ? 0 : rows.size())
                .success(0)
                .fail(0)
                .build();
        if (rows == null || rows.isEmpty()) {
            return result;
        }

        for (int i = 0; i < rows.size(); i++) {
            UserImportRow row = rows.get(i);
            int excelRow = i + 2; // header is row 1, data starts at row 2
            try {
                self.importSingleUser(row);
                result.incrementSuccess();
            } catch (Exception e) {
                // Prefer BizException.msg when available — it's already the
                // user-facing copy; otherwise fall back to the exception message.
                String msg = (e instanceof BizException biz)
                        ? biz.getMsg() : e.getMessage();
                result.addError(String.format("Row %d: %s", excelRow, msg));
                log.warn("Import user row {} failed: {}", excelRow, msg);
            }
        }
        log.info("User import finished: total={}, success={}, fail={}",
                result.getTotal(), result.getSuccess(), result.getFail());
        return result;
    }

    /**
     * One-row import in its own transaction. Not meant to be called directly
     * by anything except {@link #importBatch} through the {@link #self}
     * proxy, which is why it's {@code public} but javadoc-tagged internal.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void importSingleUser(UserImportRow row) {
        validateImportRow(row);
        String password = (row.getPassword() == null || row.getPassword().isBlank())
                ? DEFAULT_IMPORT_PASSWORD
                : row.getPassword();
        RegisterUserCommand command = RegisterUserCommand.builder()
                .username(row.getUsername())
                .password(password)
                .mobile(row.getMobile())
                .email(row.getEmail())
                .realName(row.getRealName())
                .build();
        // Re-use the full createUser pipeline so operate-stream + domain
        // event publication match the interactive create path.
        createUser(command);
    }

    private void validateImportRow(UserImportRow row) {
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
        if (row.getMobile() != null && !row.getMobile().isBlank()
                && !MOBILE_PATTERN.matcher(row.getMobile()).matches()) {
            throw new BizException("IMPORT_MOBILE_INVALID",
                    "Invalid mobile: " + row.getMobile());
        }
        if (row.getPassword() != null && !row.getPassword().isBlank()
                && (row.getPassword().length() < 6 || row.getPassword().length() > 64)) {
            throw new BizException("IMPORT_PASSWORD_INVALID",
                    "Password must be 6-64 chars");
        }
    }

    // ========================= BATCH OPS =========================

    // Route per-id calls through the `self` proxy so each invocation crosses the
    // Spring AOP boundary and actually gets its own @Transactional (a plain
    // `this::` reference is self-invocation and would bypass the proxy).
    public BatchResult batchFreeze(List<String> ids) { return runBatch(ids, self::freezeUser); }

    public BatchResult batchUnfreeze(List<String> ids) { return runBatch(ids, self::unfreezeUser); }

    public BatchResult batchDelete(List<String> ids) { return runBatch(ids, self::deleteUser); }

    /**
     * Loop a per-id action and collect a {@link BatchResult}. Each id runs
     * through the existing single-id command, which already carries its own
     * {@code @Transactional}; if a row throws we capture the message and
     * keep going so the caller gets a per-id error map back.
     *
     * <p>We deliberately do NOT wrap the loop in another {@code @Transactional}
     * — that would coalesce all writes onto one outer transaction and the
     * first failure would roll the whole batch back. Sa-Token kickout (called
     * by {@link #changePassword}) similarly relies on no outer transaction.
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
                log.warn("Batch op failed for id={}: {}", id, msg);
            }
        }
        log.info("Batch op finished: success={}, fail={}",
                result.getSuccessCount(), result.getFailCount());
        return result;
    }
}
