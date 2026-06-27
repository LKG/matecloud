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
package vip.mate.starter.tenant.datasource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vip.mate.starter.tenant.core.MultiTenantType;
import vip.mate.starter.tenant.core.TenantContext;
import vip.mate.starter.tenant.core.TenantProperties;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantDataSourceWebFilterTest {

    private TenantProperties props() {
        TenantProperties p = new TenantProperties();
        p.setType(MultiTenantType.DATASOURCE);
        p.setDefaultDsName("master");
        p.setDsPrefix("tenant_");
        p.setSuperTenantId("0");
        p.setIgnoreUrls(List.of("/actuator/**", "/api/v1/auth/**"));
        return p;
    }

    private TenantDataSourceWebFilter filter() {
        return new TenantDataSourceWebFilter(props());
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void rejectsProtectedPathWithoutTenant() {
        // null = reject (→ 400 in the filter)
        assertNull(filter().resolveRoute("/api/v1/users"));
    }

    @Test
    void ignoreUrlWithoutTenantUsesMaster() {
        assertEquals("master", filter().resolveRoute("/actuator/health"));
        assertEquals("master", filter().resolveRoute("/api/v1/auth/login"));
    }

    @Test
    void validTenantRoutesToTenantDatasource() {
        TenantContext.setTenantId("1001");
        assertEquals("tenant_1001", filter().resolveRoute("/api/v1/users"));
    }

    @Test
    void superTenantRoutesToMaster() {
        TenantContext.setTenantId("0");
        assertEquals("master", filter().resolveRoute("/api/v1/users"));
    }

    @Test
    void malformedTenantFailsClosed() {
        TenantContext.setTenantId("1' OR '1'='1");
        assertThrows(IllegalStateException.class,
                () -> filter().resolveRoute("/api/v1/users"));
    }
}
