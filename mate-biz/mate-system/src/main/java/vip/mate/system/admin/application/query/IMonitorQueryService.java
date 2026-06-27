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
package vip.mate.system.admin.application.query;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Aggregated read-side queries that power the Dashboard view.
 *
 * <p>One method, one VO — keeping the surface small lets the implementation
 * fan out to several data sources (DAO + Sa-Token Redis + RPC + HTTP probe)
 * without leaking those concerns into the controller.
 *
 * @author mateaix
 */
public interface IMonitorQueryService {

    DashboardVO dashboard();

    /**
     * Wire shape consumed by the frontend Dashboard.vue.
     *
     * <p>{@code last7Days} is in chronological order (oldest first) so the
     * chart renders left-to-right naturally; {@code services} is in the
     * order the operator usually thinks about: gateway → auth → system → admin
     * → notice. Ordering is part of the contract — the frontend doesn't sort.
     */
    record DashboardVO(
            long userCount,
            long todayLoginCount,
            long todayOpCount,
            long onlineCount,
            List<DailyStat> last7Days,
            List<ServiceStatus> services
    ) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    /** One row per day for the last-7-days trend chart. */
    record DailyStat(String date, long apiCalls, long logins) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    /**
     * Result of pinging a backend service's actuator health endpoint.
     * {@code state} is normalized to {@code "UP"} / {@code "DOWN"} /
     * {@code "UNKNOWN"} so the frontend doesn't have to interpret HTTP codes.
     */
    record ServiceStatus(String name, String url, String state, Long latencyMs) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }
}
