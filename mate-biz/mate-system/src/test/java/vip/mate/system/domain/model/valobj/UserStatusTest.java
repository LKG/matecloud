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
package vip.mate.system.domain.model.valobj;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserStatusTest {

    @Test
    void fromCode_validCodes_returnsCorrectStatus() {
        assertEquals(UserStatus.ACTIVE, UserStatus.fromCode(0));
        assertEquals(UserStatus.FROZEN, UserStatus.fromCode(1));
        assertEquals(UserStatus.DELETED, UserStatus.fromCode(2));
    }

    @Test
    void fromCode_invalidCode_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> UserStatus.fromCode(99));
    }

    @Test
    void fromCode_null_returnsNull() {
        assertNull(UserStatus.fromCode(null));
    }

    @Test
    void isActive_activeStatus_returnsTrue() {
        assertTrue(UserStatus.ACTIVE.isActive());
        assertFalse(UserStatus.FROZEN.isActive());
        assertFalse(UserStatus.DELETED.isActive());
    }

    @Test
    void isFrozen_frozenStatus_returnsTrue() {
        assertFalse(UserStatus.ACTIVE.isFrozen());
        assertTrue(UserStatus.FROZEN.isFrozen());
        assertFalse(UserStatus.DELETED.isFrozen());
    }

    @Test
    void canModify_onlyActiveCanModify() {
        assertTrue(UserStatus.ACTIVE.canModify());
        assertFalse(UserStatus.FROZEN.canModify());
        assertFalse(UserStatus.DELETED.canModify());
    }
}
