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

import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;

import java.util.function.Supplier;

/**
 * Programmatic control over tenant isolation.
 *
 * <p>Two distinct mechanisms:
 * <ul>
 *   <li><b>ignore</b> — temporarily disable the tenant-line interceptor for a
 *       block of code (cross-tenant admin queries, bootstrap seeding, tenant
 *       management itself). Backed by MyBatis-Plus {@link InterceptorIgnoreHelper}.</li>
 *   <li><b>impersonate</b> — run a block as if a specific tenant issued the
 *       request, by setting {@link TenantContext}.</li>
 * </ul>
 *
 * @author mateaix
 */
public final class TenantHelper {

    private TenantHelper() {
    }

    // ==================== ignore tenant filter ====================

    /** Run with the tenant-line filter disabled (with return value). */
    public static <T> T withoutTenant(Supplier<T> supplier) {
        try {
            InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
            return supplier.get();
        } finally {
            InterceptorIgnoreHelper.clearIgnoreStrategy();
        }
    }

    /** Run with the tenant-line filter disabled (no return value). */
    public static void runWithoutTenant(Runnable runnable) {
        withoutTenant(() -> {
            runnable.run();
            return null;
        });
    }

    // ==================== impersonate a tenant ====================

    /** Run as the given tenant (with return value), restoring the previous context afterwards. */
    public static <T> T callAsTenant(String tenantId, Supplier<T> supplier) {
        String previous = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenantId);
            return supplier.get();
        } finally {
            if (previous == null) {
                TenantContext.clear();
            } else {
                TenantContext.setTenantId(previous);
            }
        }
    }

    /** Run as the given tenant (no return value). */
    public static void runAsTenant(String tenantId, Runnable runnable) {
        callAsTenant(tenantId, () -> {
            runnable.run();
            return null;
        });
    }
}
