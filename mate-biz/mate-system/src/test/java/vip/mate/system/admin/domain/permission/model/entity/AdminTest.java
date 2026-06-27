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
package vip.mate.system.admin.domain.permission.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vip.mate.system.admin.domain.permission.model.valobj.AdminStatus;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Admin entity tests")
class AdminTest {

    @Test
    @DisplayName("create() should generate an ID and set ACTIVE status")
    void create_shouldGenerateIdAndSetActiveStatus() {
        Admin admin = Admin.create("testuser", "encoded_pwd", "Test User");

        assertNotNull(admin.getId(), "ID should be generated");
        assertFalse(admin.getId().isEmpty(), "ID should not be empty");
        assertEquals(32, admin.getId().length(), "ID should be a 32-char hex string (UUID without dashes)");
        assertEquals("testuser", admin.getUsername());
        assertEquals("encoded_pwd", admin.getPassword());
        assertEquals("Test User", admin.getNickName());
        assertEquals(AdminStatus.ACTIVE, admin.getStatus());
    }

    @Test
    @DisplayName("create() should produce unique IDs")
    void create_shouldProduceUniqueIds() {
        Admin a1 = Admin.create("user1", "pwd1", "Nick1");
        Admin a2 = Admin.create("user2", "pwd2", "Nick2");

        assertNotEquals(a1.getId(), a2.getId());
    }

    @Test
    @DisplayName("changePassword() should update the password")
    void changePassword_shouldUpdatePassword() {
        Admin admin = Admin.create("user", "old_pwd", "Nick");

        admin.changePassword("new_encoded_pwd");

        assertEquals("new_encoded_pwd", admin.getPassword());
    }

    @Test
    @DisplayName("disable() should set status to DISABLED")
    void disable_shouldSetStatusToDisabled() {
        Admin admin = Admin.create("user", "pwd", "Nick");
        assertTrue(admin.isActive(), "Precondition: should start ACTIVE");

        admin.disable();

        assertEquals(AdminStatus.DISABLED, admin.getStatus());
        assertTrue(admin.isDisabled());
        assertFalse(admin.isActive());
    }

    @Test
    @DisplayName("enable() should set status to ACTIVE")
    void enable_shouldSetStatusToActive() {
        Admin admin = Admin.create("user", "pwd", "Nick");
        admin.disable();
        assertTrue(admin.isDisabled(), "Precondition: should be DISABLED");

        admin.enable();

        assertEquals(AdminStatus.ACTIVE, admin.getStatus());
        assertTrue(admin.isActive());
        assertFalse(admin.isDisabled());
    }

    @Test
    @DisplayName("isActive() returns true only when status is ACTIVE")
    void isActive_returnsCorrectly() {
        Admin admin = Admin.create("user", "pwd", "Nick");
        assertTrue(admin.isActive());

        admin.disable();
        assertFalse(admin.isActive());

        admin.enable();
        assertTrue(admin.isActive());
    }

    @Test
    @DisplayName("isDisabled() returns true only when status is DISABLED")
    void isDisabled_returnsCorrectly() {
        Admin admin = Admin.create("user", "pwd", "Nick");
        assertFalse(admin.isDisabled());

        admin.disable();
        assertTrue(admin.isDisabled());

        admin.enable();
        assertFalse(admin.isDisabled());
    }
}
