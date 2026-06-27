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

import java.util.regex.Pattern;

/**
 * Tenant id validation at the trust boundary.
 *
 * <p>Tenant ids flow in from client-controllable sources (the
 * {@code X-Tenant-Id} header, the subdomain) and are eventually rendered into
 * SQL by the tenant-line interceptor and into a datasource name by the routing
 * helper. JSQLParser's {@code StringValue} does NOT escape embedded quotes, so
 * an unvalidated id like {@code 1' OR '1'='1} becomes a SQL-injection / tenant
 * bypass vector. Tenant ids are generated opaque keys — restrict them to a
 * strict allow-list and reject everything else.
 *
 * @author mateaix
 */
public final class TenantId {

    /** Letters, digits, underscore and hyphen only; 1..64 chars. */
    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    private TenantId() {
    }

    public static boolean isValid(String tenantId) {
        return tenantId != null && VALID.matcher(tenantId).matches();
    }
}
