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

import org.junit.jupiter.api.Test;
import vip.mate.base.exception.BizException;
import vip.mate.system.domain.model.valobj.UserStatus;
import vip.mate.system.types.exception.UserErrorCode;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private User createActiveUser() {
        return User.create("testuser", "encoded_pwd", "13800138000", "test@example.com", "Test User");
    }

    @Test
    void create_validParams_returnsActiveUser() {
        User user = createActiveUser();
        assertNotNull(user.getId());
        assertEquals(32, user.getId().length()); // UUID without dashes
        assertEquals("testuser", user.getUsername());
        assertEquals("encoded_pwd", user.getPassword());
        assertEquals("13800138000", user.getMobile());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("Test User", user.getRealName());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void freeze_activeUser_statusBecomeFrozen() {
        User user = createActiveUser();
        user.freeze();
        assertEquals(UserStatus.FROZEN, user.getStatus());
        assertTrue(user.isFrozen());
    }

    @Test
    void freeze_frozenUser_throwsException() {
        User user = createActiveUser();
        user.freeze();
        BizException ex = assertThrows(BizException.class, user::freeze);
        assertEquals(UserErrorCode.USER_IS_FROZEN.getCode(), ex.getCode());
    }

    @Test
    void freeze_deletedUser_throwsException() {
        User user = createActiveUser();
        user.delete();
        BizException ex = assertThrows(BizException.class, user::freeze);
        assertEquals(UserErrorCode.USER_IS_DELETED.getCode(), ex.getCode());
    }

    @Test
    void unfreeze_frozenUser_statusBecomeActive() {
        User user = createActiveUser();
        user.freeze();
        user.unfreeze();
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(user.isActive());
    }

    @Test
    void unfreeze_activeUser_throwsException() {
        User user = createActiveUser();
        BizException ex = assertThrows(BizException.class, user::unfreeze);
        assertEquals(UserErrorCode.USER_STATUS_INVALID.getCode(), ex.getCode());
    }

    @Test
    void delete_activeUser_statusBecomeDeleted() {
        User user = createActiveUser();
        user.delete();
        assertEquals(UserStatus.DELETED, user.getStatus());
        assertTrue(user.isDomainDeleted());
    }

    @Test
    void delete_alreadyDeleted_throwsException() {
        User user = createActiveUser();
        user.delete();
        BizException ex = assertThrows(BizException.class, user::delete);
        assertEquals(UserErrorCode.USER_IS_DELETED.getCode(), ex.getCode());
    }

    @Test
    void changeRealName_activeUser_nameUpdated() {
        User user = createActiveUser();
        user.changeRealName("New Name");
        assertEquals("New Name", user.getRealName());
    }

    @Test
    void changeRealName_frozenUser_throwsException() {
        User user = createActiveUser();
        user.freeze();
        assertThrows(BizException.class, () -> user.changeRealName("New Name"));
    }

    @Test
    void changePassword_activeUser_passwordUpdated() {
        User user = createActiveUser();
        user.changePassword("new_encoded_pwd");
        assertEquals("new_encoded_pwd", user.getPassword());
    }

    @Test
    void changePassword_frozenUser_throwsException() {
        User user = createActiveUser();
        user.freeze();
        assertThrows(BizException.class, () -> user.changePassword("new_pwd"));
    }

    @Test
    void adminResetPassword_frozenUser_passwordUpdated() {
        User user = createActiveUser();
        user.freeze();
        // admin reset bypasses active check
        user.adminResetPassword("admin_reset_pwd");
        assertEquals("admin_reset_pwd", user.getPassword());
    }

    @Test
    void adminResetPassword_deletedUser_throwsException() {
        User user = createActiveUser();
        user.delete();
        BizException ex = assertThrows(BizException.class,
                () -> user.adminResetPassword("pwd"));
        assertEquals(UserErrorCode.USER_IS_DELETED.getCode(), ex.getCode());
    }

    @Test
    void updateProfile_partialUpdate_onlyNonNullFieldsChanged() {
        User user = createActiveUser();
        user.updateProfile(null, "13900139000", null, null, 1);
        assertEquals("Test User", user.getRealName()); // unchanged
        assertEquals("13900139000", user.getMobile()); // updated
        assertEquals("test@example.com", user.getEmail()); // unchanged
        assertEquals(1, user.getGender()); // updated
    }

    @Test
    void updateProfile_frozenUser_throwsException() {
        User user = createActiveUser();
        user.freeze();
        assertThrows(BizException.class,
                () -> user.updateProfile("name", null, null, null, null));
    }
}
