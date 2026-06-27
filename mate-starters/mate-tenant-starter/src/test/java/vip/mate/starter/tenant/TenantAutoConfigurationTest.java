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
package vip.mate.starter.tenant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import vip.mate.starter.tenant.core.TenantProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wiring regression test: {@link TenantProperties} must be available even when
 * multi-tenancy is DISABLED, because non-conditional consumers (e.g.
 * mate-system's {@code TenantQuotaService}) inject it unconditionally. The
 * behavioural beans, by contrast, must stay gated by {@code mate.tenant.enabled}.
 *
 * <p>Guards against the boot failure: "No qualifying bean of type
 * 'TenantProperties' available" when the tenant feature is off.
 *
 * @author mateaix
 */
class TenantAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    TenantPropertiesAutoConfiguration.class,
                    TenantAutoConfiguration.class));

    @Test
    void tenantPropertiesAvailableEvenWhenDisabled() {
        // Default: mate.tenant.enabled is unset (false).
        runner.run(ctx -> {
            assertNull(ctx.getStartupFailure(), "context must start when tenancy is disabled");
            assertEquals(1, ctx.getBeanNamesForType(TenantProperties.class).length,
                    "TenantProperties must be registered unconditionally");
            assertFalse(ctx.getBean(TenantProperties.class).isEnabled());
            // Behavioural beans stay off when disabled.
            assertFalse(ctx.containsBean("tenantCacheKeyGenerator"));
        });
    }

    @Test
    void behaviouralBeansWireUpWhenEnabled() {
        runner.withPropertyValues("mate.tenant.enabled=true").run(ctx -> {
            assertNull(ctx.getStartupFailure(), "context must start when tenancy is enabled");
            assertEquals(1, ctx.getBeanNamesForType(TenantProperties.class).length);
            assertTrue(ctx.getBean(TenantProperties.class).isEnabled());
            assertTrue(ctx.containsBean("tenantCacheKeyGenerator"));
        });
    }
}
