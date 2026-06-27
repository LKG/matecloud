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
package vip.mate.system.domain.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import vip.mate.base.model.entity.BaseEntity;
import vip.mate.system.domain.model.valobj.UserStatus;
import vip.mate.system.types.exception.UserErrorCode;
import vip.mate.system.types.exception.UserException;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {

    private String tenantId;
    private String username;
    private String password;
    private String mobile;
    private String email;
    private String realName;
    private String avatar;
    private Integer gender;
    private UserStatus status;
    private LocalDateTime lastLoginTime;

    public static User create(String username, String password, String mobile, String email, String realName) {
        return User.builder()
                .id(UUID.randomUUID().toString().replace("-", ""))
                .username(username)
                .password(password)
                .mobile(mobile)
                .email(email)
                .realName(realName)
                .status(UserStatus.ACTIVE)
                .build();
    }

    public void changeRealName(String newRealName) {
        checkActive();
        this.realName = newRealName;
    }

    /**
     * Apply a partial profile update. {@code null} fields leave the existing
     * value untouched, so callers can pass only what they want to change.
     */
    public void updateProfile(String realName, String mobile, String email,
                              String avatar, Integer gender) {
        checkActive();
        if (realName != null) this.realName = realName;
        if (mobile != null) this.mobile = mobile;
        if (email != null) this.email = email;
        if (avatar != null) this.avatar = avatar;
        if (gender != null) this.gender = gender;
    }

    /**
     * Self-service password change. Active accounts only — a frozen user
     * cannot change their own password (must contact an admin to unfreeze
     * or to {@link #adminResetPassword} on their behalf).
     * <p>
     * Caller MUST pass an already-hashed value (BCrypt) — domain entities
     * don't know about encoding.
     */
    public void changePassword(String encodedPassword) {
        checkActive();
        this.password = encodedPassword;
    }

    /**
     * Admin-driven password reset. Bypasses the {@link #checkActive()} guard
     * so an admin can recover a frozen account; deleted accounts are still
     * rejected because resetting a tombstone makes no sense.
     */
    public void adminResetPassword(String encodedPassword) {
        if (isDomainDeleted()) {
            throw UserException.of(UserErrorCode.USER_IS_DELETED);
        }
        this.password = encodedPassword;
    }

    public void freeze() {
        checkActive();
        this.status = UserStatus.FROZEN;
    }

    public void unfreeze() {
        if (!status.isFrozen()) {
            throw UserException.of(UserErrorCode.USER_STATUS_INVALID);
        }
        this.status = UserStatus.ACTIVE;
    }

    public void delete() {
        if (status.isDeleted()) {
            throw UserException.of(UserErrorCode.USER_IS_DELETED);
        }
        this.status = UserStatus.DELETED;
    }

    public void updateLastLoginTime() {
        this.lastLoginTime = LocalDateTime.now();
    }

    public boolean isActive() { return status == UserStatus.ACTIVE; }
    public boolean isFrozen() { return status == UserStatus.FROZEN; }
    public boolean isDomainDeleted() { return status == UserStatus.DELETED; }

    private void checkActive() {
        if (isFrozen()) throw UserException.of(UserErrorCode.USER_IS_FROZEN);
        if (isDomainDeleted()) throw UserException.of(UserErrorCode.USER_IS_DELETED);
    }
}
