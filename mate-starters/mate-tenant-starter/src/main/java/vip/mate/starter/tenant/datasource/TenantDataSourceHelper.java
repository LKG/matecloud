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

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import vip.mate.starter.tenant.core.TenantId;
import vip.mate.starter.tenant.core.TenantProperties;

import java.util.function.Supplier;

/**
 * Datasource-level tenant routing helper (SCHEMA / DATASOURCE modes).
 *
 * <p>Backed by baomidou dynamic-datasource's {@link DynamicDataSourceContextHolder}
 * — a thread-local stack of datasource names. Pushing a name routes every
 * subsequent SQL on this thread to that datasource until it is polled.
 *
 * <p>The super tenant and an absent tenant both route to the default (master)
 * datasource, where the tenant-management tables live.
 *
 * @author mateaix
 */
public final class TenantDataSourceHelper {

    private TenantDataSourceHelper() {
    }

    /**
     * Derive a tenant's datasource name; falls back to master for the super /
     * empty tenant. A malformed tenant id fails closed (throws) rather than
     * routing a spoofed id to an arbitrary {@code tenant_<x>} datasource.
     */
    public static String resolveDsName(TenantProperties props, String tenantId) {
        String superTenantId = props.getSuperTenantId();
        boolean isSuper = superTenantId != null && !superTenantId.isBlank()
                && superTenantId.equals(tenantId);
        if (tenantId == null || tenantId.isBlank() || isSuper) {
            return props.getDefaultDsName();
        }
        if (!TenantId.isValid(tenantId)) {
            throw new IllegalStateException("Invalid tenant id for datasource routing");
        }
        return props.getDsPrefix() + tenantId;
    }

    /** Push a datasource name onto the routing stack. */
    public static void push(String dsName) {
        DynamicDataSourceContextHolder.push(dsName);
    }

    /** Pop the top datasource name off the routing stack. */
    public static void poll() {
        DynamicDataSourceContextHolder.poll();
    }

    /** Execute on the given tenant's datasource (with return value). */
    public static <T> T executeWithDs(String dsName, Supplier<T> supplier) {
        try {
            DynamicDataSourceContextHolder.push(dsName);
            return supplier.get();
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
    }

    /** Execute on the given tenant's datasource (no return value). */
    public static void runWithDs(String dsName, Runnable runnable) {
        executeWithDs(dsName, () -> {
            runnable.run();
            return null;
        });
    }

    /** Execute on the master datasource — tenant-management / cross-tenant work. */
    public static <T> T executeWithMaster(TenantProperties props, Supplier<T> supplier) {
        return executeWithDs(props.getDefaultDsName(), supplier);
    }

    /** Execute on the master datasource (no return value). */
    public static void runWithMaster(TenantProperties props, Runnable runnable) {
        runWithDs(props.getDefaultDsName(), runnable);
    }
}
