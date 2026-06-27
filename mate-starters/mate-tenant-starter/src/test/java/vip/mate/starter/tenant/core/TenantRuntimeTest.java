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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantRuntimeTest {

    @AfterEach
    void reset() {
        TenantRuntime.set(null);
    }

    @Test
    void datasourceModeFalseWhenUnset() {
        TenantRuntime.set(null);
        assertFalse(TenantRuntime.isDatasourceMode());
    }

    @Test
    void datasourceModeFalseForColumn() {
        TenantProperties p = new TenantProperties();
        p.setType(MultiTenantType.COLUMN);
        TenantRuntime.set(p);
        assertFalse(TenantRuntime.isDatasourceMode());
    }

    @Test
    void datasourceModeTrueForDatasourceAndSchema() {
        TenantProperties ds = new TenantProperties();
        ds.setType(MultiTenantType.DATASOURCE);
        TenantRuntime.set(ds);
        assertTrue(TenantRuntime.isDatasourceMode());

        TenantProperties schema = new TenantProperties();
        schema.setType(MultiTenantType.SCHEMA);
        TenantRuntime.set(schema);
        assertTrue(TenantRuntime.isDatasourceMode());
    }
}
