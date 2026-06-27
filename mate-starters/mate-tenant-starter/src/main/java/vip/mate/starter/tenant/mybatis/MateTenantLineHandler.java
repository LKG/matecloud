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

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import vip.mate.starter.tenant.core.MultiTenantType;
import vip.mate.starter.tenant.core.TenantContext;
import vip.mate.starter.tenant.core.TenantHelper;
import vip.mate.starter.tenant.core.TenantId;
import vip.mate.starter.tenant.core.TenantProperties;

/**
 * Row-level tenant handler (COLUMN mode). Extracted from the auto-configuration
 * so the security-critical fail-closed logic is unit-testable.
 *
 * <p>Contract:
 * <ul>
 *   <li>{@link #ignoreTable(String)} returns true only for non-COLUMN mode,
 *       non-whitelisted tables, and the super tenant — it does NOT ignore a
 *       whitelisted table merely because the context is empty.</li>
 *   <li>{@link #getTenantId()} is reached only for tables that were not ignored,
 *       and <b>fails closed</b>: a blank or malformed context throws instead of
 *       emitting {@code tenant_id = NULL} or an injectable predicate.</li>
 * </ul>
 * Legitimate cross-tenant access must opt in via {@link TenantHelper#withoutTenant},
 * which makes MyBatis-Plus skip this handler entirely.
 *
 * @author mateaix
 */
@RequiredArgsConstructor
public class MateTenantLineHandler implements TenantLineHandler {

    private final TenantProperties properties;

    @Override
    public Expression getTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException(
                    "No tenant context for a tenant-scoped table; "
                            + "use TenantHelper.withoutTenant for cross-tenant access");
        }
        if (!TenantId.isValid(tenantId)) {
            throw new IllegalStateException("Invalid tenant id in context");
        }
        return new StringValue(tenantId);
    }

    @Override
    public String getTenantIdColumn() {
        return properties.getColumn();
    }

    @Override
    public boolean ignoreTable(String tableName) {
        if (properties.getType() != MultiTenantType.COLUMN) {
            return true;
        }
        // Non-whitelisted tables are global — never filtered.
        if (!properties.getIncludeTables().contains(tableName)) {
            return true;
        }
        // Super tenant sees every tenant's rows.
        String tenantId = TenantContext.getTenantId();
        String superTenantId = properties.getSuperTenantId();
        if (superTenantId != null && !superTenantId.isBlank()
                && superTenantId.equals(tenantId)) {
            return true;
        }
        // Whitelisted + not super → must filter. A blank/invalid context is
        // rejected (fail closed) in getTenantId; we deliberately do NOT ignore
        // here just because the context is empty.
        return false;
    }
}
