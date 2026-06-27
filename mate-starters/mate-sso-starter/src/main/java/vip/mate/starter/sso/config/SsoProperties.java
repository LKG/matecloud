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
package vip.mate.starter.sso.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import vip.mate.starter.sso.core.sync.LeaveStrategy;
import vip.mate.starter.sso.core.sync.SyncMode;

/**
 * SSO starter settings. The master on/off switch follows the platform feature-flag
 * convention ({@code mate.feature.sso.enabled}, see coding-standards §12.3); this
 * class holds behavioural settings under {@code mate.sso.*}.
 *
 * @author mateaix
 */
@ConfigurationProperties(prefix = "mate.sso")
public class SsoProperties {

    /** Default provider for single-sign-on direct redirect (optional). */
    private String defaultProvider;

    /** Organization sync settings. */
    private Sync sync = new Sync();

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public Sync getSync() {
        return sync;
    }

    public void setSync(Sync sync) {
        this.sync = sync;
    }

    public static class Sync {

        /** Scheduled sync cron (consumed by the consumer's @MateJobHandler/scheduler). */
        private String cron = "0 0 2 * * ?";

        /** FULL (pull + diff + prune) or INCREMENTAL. */
        private SyncMode mode = SyncMode.FULL;

        /** How to treat members that have left the org. */
        private LeaveStrategy leaveStrategy = LeaveStrategy.DISABLE;

        /** Whether webhook-driven sync is accepted. */
        private boolean webhookEnabled = false;

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public SyncMode getMode() {
            return mode;
        }

        public void setMode(SyncMode mode) {
            this.mode = mode;
        }

        public LeaveStrategy getLeaveStrategy() {
            return leaveStrategy;
        }

        public void setLeaveStrategy(LeaveStrategy leaveStrategy) {
            this.leaveStrategy = leaveStrategy;
        }

        public boolean isWebhookEnabled() {
            return webhookEnabled;
        }

        public void setWebhookEnabled(boolean webhookEnabled) {
            this.webhookEnabled = webhookEnabled;
        }
    }
}
