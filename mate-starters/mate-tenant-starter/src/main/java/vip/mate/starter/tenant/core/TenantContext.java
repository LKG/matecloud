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

/**
 * Tenant context holder. Uses ThreadLocal to propagate the current tenant ID
 * within a thread of execution.
 * <p>
 * Note: we intentionally use ThreadLocal rather than ScopedValue, since
 * ScopedValue is still a preview API in JDK 21 and we do not want to require
 * --enable-preview at compile time.
 *
 * @author mateaix
 */
public final class TenantContext {

    private static final ThreadLocal<String> TENANT_LOCAL = new ThreadLocal<>();

    private TenantContext() {
    }

    public static String getTenantId() {
        return TENANT_LOCAL.get();
    }

    public static void setTenantId(String tenantId) {
        TENANT_LOCAL.set(tenantId);
    }

    public static void clear() {
        TENANT_LOCAL.remove();
    }

    /**
     * Execute a runnable within a tenant context scope.
     */
    public static void runWithTenant(String tenantId, Runnable task) {
        String previous = getTenantId();
        try {
            setTenantId(tenantId);
            task.run();
        } finally {
            if (previous == null) {
                clear();
            } else {
                setTenantId(previous);
            }
        }
    }
}
