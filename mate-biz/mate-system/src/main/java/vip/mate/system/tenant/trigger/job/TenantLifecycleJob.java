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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vip.mate.system.tenant.application.lifecycle.TenantLifecycleService;

/**
 * Periodic sweep that transitions overdue tenants to EXPIRED and logs renewal
 * reminders (RFC-028). Opt-in via {@code mate.tenant.enabled=true}; the cron is
 * overridable with {@code mate.tenant.lifecycle-cron} (hourly by default).
 * Failures are swallowed so one bad run never kills the scheduler thread.
 *
 * @author mateaix
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mate.tenant.enabled", havingValue = "true")
public class TenantLifecycleJob {

    private final TenantLifecycleService tenantLifecycleService;

    @Scheduled(cron = "${mate.tenant.lifecycle-cron:0 0 * * * *}")
    public void run() {
        try {
            int expired = tenantLifecycleService.expireOverdueTenants();
            int reminded = tenantLifecycleService.remindExpiring();
            log.debug("[tenant] lifecycle job finished, expired={} reminded={}", expired, reminded);
        } catch (Exception e) {
            log.error("[tenant] lifecycle job failed", e);
        }
    }
}
