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
package vip.mate.starter.sso.event;

import org.springframework.context.ApplicationEvent;
import vip.mate.starter.sso.core.sync.SyncResult;

/**
 * Published when an organization sync finishes (success or failure). Carries the
 * {@link SyncResult} so listeners can persist the audit log / fire notifications.
 *
 * @author mateaix
 */
public class OrgSyncCompletedEvent extends ApplicationEvent {

    private final SyncResult result;

    public OrgSyncCompletedEvent(Object source, SyncResult result) {
        super(source);
        this.result = result;
    }

    public SyncResult getResult() {
        return result;
    }
}
