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
package vip.mate.starter.tenant.mybatis;

import net.sf.jsqlparser.expression.StringValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vip.mate.starter.tenant.core.MultiTenantType;
import vip.mate.starter.tenant.core.TenantContext;
import vip.mate.starter.tenant.core.TenantProperties;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MateTenantLineHandlerTest {

    private TenantProperties props() {
        TenantProperties p = new TenantProperties();
        p.setType(MultiTenantType.COLUMN);
        p.setIncludeTables(List.of("mate_user", "mate_admin"));
        p.setSuperTenantId("");
        return p;
    }

    private MateTenantLineHandler handler(TenantProperties p) {
        return new MateTenantLineHandler(p);
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void ignoresNonWhitelistedTables() {
        TenantContext.setTenantId("1");
        assertTrue(handler(props()).ignoreTable("mate_config"));
    }

    @Test
    void filtersWhitelistedTableWithContext() {
        TenantContext.setTenantId("1");
        MateTenantLineHandler h = handler(props());
        assertFalse(h.ignoreTable("mate_user"));
        assertEquals("'1'", h.getTenantId().toString());
        assertEquals("tenant_id", h.getTenantIdColumn());
    }

    @Test
    void failsClosedWhenContextMissing() {
        // ignoreTable must NOT skip a whitelisted table just because context is blank
        assertFalse(handler(props()).ignoreTable("mate_user"));
        // and rendering the predicate must throw rather than read all tenants
        assertThrows(IllegalStateException.class, () -> handler(props()).getTenantId());
    }

    @Test
    void failsClosedOnMalformedContext() {
        TenantContext.setTenantId("1' OR '1'='1");
        assertThrows(IllegalStateException.class, () -> handler(props()).getTenantId());
    }

    @Test
    void superTenantSeesAllRows() {
        TenantProperties p = props();
        p.setSuperTenantId("0");
        TenantContext.setTenantId("0");
        assertTrue(handler(p).ignoreTable("mate_user"));
    }

    @Test
    void nonColumnModeIgnoresEverything() {
        TenantProperties p = props();
        p.setType(MultiTenantType.DATASOURCE);
        TenantContext.setTenantId("1");
        assertTrue(handler(p).ignoreTable("mate_user"));
    }

    @Test
    void rendersValidStringValue() {
        TenantContext.setTenantId("tenant_42");
        assertEquals(new StringValue("tenant_42").toString(),
                handler(props()).getTenantId().toString());
    }
}
