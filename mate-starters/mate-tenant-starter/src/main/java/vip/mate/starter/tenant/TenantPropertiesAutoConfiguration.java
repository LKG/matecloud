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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import vip.mate.starter.tenant.core.TenantProperties;

/**
 * Always-on registration of {@link TenantProperties}.
 *
 * <p>{@link TenantProperties} is a passive config holder (defaults to
 * {@code enabled=false}) that application code may legitimately read regardless
 * of whether the tenant <em>behaviour</em> (interceptors / filters) is active —
 * e.g. {@code TenantQuotaService} short-circuits on {@code enabled=false}. It is
 * therefore registered unconditionally here, separate from
 * {@link TenantAutoConfiguration} (which is gated by {@code mate.tenant.enabled}
 * and wires the behavioural beans). Without this split, any non-conditional bean
 * that injects {@code TenantProperties} would fail to start whenever multi-tenancy
 * is disabled.
 *
 * @author mateaix
 */
@AutoConfiguration
@EnableConfigurationProperties(TenantProperties.class)
public class TenantPropertiesAutoConfiguration {
}
