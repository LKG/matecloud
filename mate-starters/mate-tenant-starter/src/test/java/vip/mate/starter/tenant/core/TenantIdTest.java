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
package vip.mate.starter.tenant.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantIdTest {

    @ParameterizedTest
    @ValueSource(strings = {"1", "0", "tenant_1001", "A-b_9", "system"})
    void acceptsWellFormedIds(String id) {
        assertTrue(TenantId.isValid(id));
    }

    @Test
    void rejectsNull() {
        assertFalse(TenantId.isValid(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "1' OR '1'='1",
            "1; DROP TABLE mate_user;--",
            "a b",
            "tenant'"
    })
    void rejectsMalformedOrInjectionIds(String id) {
        assertFalse(TenantId.isValid(id));
    }

    @Test
    void rejectsOverlongId() {
        assertFalse(TenantId.isValid("x".repeat(65)));
    }
}
