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
package vip.mate.system.admin.domain.permission.model.valobj;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AdminStatus value object tests")
class AdminStatusTest {

    @Test
    @DisplayName("fromCode(0) should return ACTIVE")
    void fromCode_zero_returnsActive() {
        assertEquals(AdminStatus.ACTIVE, AdminStatus.fromCode(0));
    }

    @Test
    @DisplayName("fromCode(1) should return DISABLED")
    void fromCode_one_returnsDisabled() {
        assertEquals(AdminStatus.DISABLED, AdminStatus.fromCode(1));
    }

    @Test
    @DisplayName("fromCode(null) should return null")
    void fromCode_null_returnsNull() {
        assertNull(AdminStatus.fromCode(null));
    }

    @Test
    @DisplayName("fromCode with invalid code should throw IllegalArgumentException")
    void fromCode_invalidCode_throws() {
        assertThrows(IllegalArgumentException.class, () -> AdminStatus.fromCode(99));
        assertThrows(IllegalArgumentException.class, () -> AdminStatus.fromCode(-1));
    }

    @Test
    @DisplayName("ACTIVE code is 0, DISABLED code is 1")
    void codes_areCorrect() {
        assertEquals(0, AdminStatus.ACTIVE.getCode());
        assertEquals(1, AdminStatus.DISABLED.getCode());
    }

    @Test
    @DisplayName("Descriptions are not empty")
    void descriptions_areNotEmpty() {
        for (AdminStatus status : AdminStatus.values()) {
            assertNotNull(status.getDesc());
            assertFalse(status.getDesc().isEmpty());
        }
    }

    @Test
    @DisplayName("Enum has exactly 2 values")
    void enumSize() {
        assertEquals(2, AdminStatus.values().length);
    }
}
