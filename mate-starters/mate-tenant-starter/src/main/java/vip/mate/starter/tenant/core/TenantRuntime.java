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
 * Static accessor to the active {@link TenantProperties}, populated by
 * {@link TenantAutoConfiguration} when multi-tenancy is enabled.
 *
 * <p>Dubbo SPI filters ({@link TenantDubboProviderFilter}) are instantiated by
 * Dubbo, not Spring, so they cannot inject the properties bean directly. They
 * read it here instead. When tenancy is disabled the value stays {@code null}
 * and the filters behave as no-ops.
 *
 * @author mateaix
 */
public final class TenantRuntime {

    private static volatile TenantProperties properties;

    private TenantRuntime() {
    }

    /**
     * Install the active {@link TenantProperties}. Called once by
     * {@code TenantAutoConfiguration} (which lives in the parent package
     * {@code vip.mate.starter.tenant}, hence this method is public rather than
     * package-private). Application code should NOT call this.
     */
    public static void set(TenantProperties props) {
        properties = props;
    }

    public static TenantProperties get() {
        return properties;
    }

    /** True when datasource-level routing (SCHEMA / DATASOURCE) is the active mode. */
    public static boolean isDatasourceMode() {
        TenantProperties p = properties;
        return p != null
                && (p.getType() == MultiTenantType.DATASOURCE
                || p.getType() == MultiTenantType.SCHEMA);
    }
}
