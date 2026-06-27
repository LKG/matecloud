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
package vip.mate.system.tenant.application.lifecycle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.system.domain.adapter.port.DomainEventPublisher;
import vip.mate.system.tenant.domain.adapter.repository.TenantRepository;
import vip.mate.system.tenant.domain.event.TenantExpiredEvent;
import vip.mate.system.tenant.domain.model.aggregate.TenantAggregate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Drives the time-based portion of the tenant lifecycle (RFC-028): the
 * ACTIVE → EXPIRED sweep and the upcoming-renewal reminders. Invoked by the
 * scheduled {@code TenantLifecycleJob}; safe to call manually for ops/testing.
 *
 * @author mateaix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantLifecycleService {

    /** Max tenants processed per sweep run to keep transactions bounded. */
    private static final int EXPIRE_BATCH_SIZE = 500;

    /** Lead time for renewal reminders (RFC-028: 到期提醒 7/3/1 天). */
    private static final int REMIND_AHEAD_DAYS = 7;

    private final TenantRepository tenantRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * Mark every overdue ACTIVE tenant as EXPIRED.
     *
     * @return the number of tenants transitioned to EXPIRED
     */
    @Transactional(rollbackFor = Exception.class)
    public int expireOverdueTenants() {
        LocalDateTime now = LocalDateTime.now();
        List<TenantAggregate> due = tenantRepository.findActiveExpiredBefore(now, EXPIRE_BATCH_SIZE);
        int expired = 0;
        for (TenantAggregate agg : due) {
            if (agg.markExpiredIfDue(now)) {
                tenantRepository.update(agg);
                eventPublisher.publish(new TenantExpiredEvent(
                        agg.getId(), agg.getTenant().getTenantCode(), agg.getTenant().getExpireAt()));
                expired++;
            }
        }
        log.info("[tenant] expiration sweep done, scanned={} expired={}", due.size(), expired);
        return expired;
    }

    /**
     * Log a reminder for every ACTIVE tenant expiring within the next
     * {@link #REMIND_AHEAD_DAYS} days. There is no real notification channel yet,
     * so this only logs at WARN.
     *
     * @return the number of tenants reminded
     */
    public int remindExpiring() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime horizon = now.plusDays(REMIND_AHEAD_DAYS);
        List<TenantAggregate> expiring = tenantRepository.findExpiringBetween(now, horizon);
        for (TenantAggregate agg : expiring) {
            // TODO: route through mate-notice (email/SMS) once a tenant notification
            //       channel exists; emit reminders at the RFC-028 7/3/1-day marks.
            log.warn("[tenant] expiring soon tenant={} code={} expireAt={}",
                    agg.getId(), agg.getTenant().getTenantCode(), agg.getTenant().getExpireAt());
        }
        return expiring.size();
    }
}
