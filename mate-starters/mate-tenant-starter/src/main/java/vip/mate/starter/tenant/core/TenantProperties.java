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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import vip.mate.base.constant.AuthHeaders;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for multi-tenant support.
 *
 * @author mateaix
 */
@Data
@ConfigurationProperties(prefix = "mate.tenant")
public class TenantProperties {

    /** Master switch. When false the whole tenant chain is inert. */
    private boolean enabled = false;

    /** Isolation strategy. Only COLUMN is implemented today. */
    private MultiTenantType type = MultiTenantType.COLUMN;

    /** Tenant column name used by the row-level interceptor. */
    private String column = "tenant_id";

    /**
     * Whitelist of tables that carry the tenant column. ONLY these tables get
     * {@code WHERE tenant_id = ?} appended; every other table is untouched.
     * A whitelist (vs. a blacklist) is the safe default — a table that lacks
     * the column can never be filtered by accident.
     */
    private List<String> includeTables = new ArrayList<>();

    /**
     * Super-tenant id. Requests carrying this tenant see all tenants' data
     * (the row-level filter is skipped). Empty (the default) DISABLES the super
     * tenant — recommended, since the default header resolver makes a non-empty
     * value a cross-tenant bypass for anyone who can set {@code X-Tenant-Id}.
     * If you enable it, authorize the super tenant from the authenticated
     * principal/role, not from the raw tenant id alone.
     */
    private String superTenantId = "";

    /** Request resolver: header | token | domain. */
    private String resolver = "header";

    /** Header carrying the tenant id (header/domain resolvers). */
    private String headerName = AuthHeaders.TENANT_ID;

    /** URL patterns (Ant-style) that bypass tenant resolution entirely. */
    private List<String> ignoreUrls = new ArrayList<>();

    /**
     * Read-only grace window (in days) after a tenant's expireAt during which
     * the tenant is still allowed in (RFC-028 宽限期). After the window closes
     * the status gate rejects the tenant until it renews.
     */
    private int graceDays = 7;

    // ==================== SCHEMA / DATASOURCE mode ====================

    /**
     * Default (master) datasource name — must match a key under
     * {@code spring.datasource.dynamic.datasource.*} and
     * {@code spring.datasource.dynamic.primary}. Tenant-management tables and
     * the super tenant run here.
     */
    private String defaultDsName = "master";

    /**
     * Prefix used to derive a tenant's datasource name from its id:
     * {@code dsName = dsPrefix + tenantId}. The derived name must match a
     * configured {@code spring.datasource.dynamic.datasource.*} key.
     */
    private String dsPrefix = "tenant_";
}
