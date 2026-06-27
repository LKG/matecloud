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
package vip.mate.system.tenant.trigger.job;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring task scheduling for the tenant lifecycle job. Opt-in: dormant
 * unless {@code mate.tenant.enabled=true}, so single-tenant deployments pay no
 * scheduler cost. The app does not enable scheduling globally (only @EnableAsync),
 * hence this dedicated guarded configuration.
 *
 * @author mateaix
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "mate.tenant.enabled", havingValue = "true")
public class TenantSchedulingConfig {
}
