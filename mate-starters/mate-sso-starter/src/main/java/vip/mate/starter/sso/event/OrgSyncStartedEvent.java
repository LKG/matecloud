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
import vip.mate.starter.sso.core.sync.SyncMode;

/**
 * Published when an organization sync starts. First cut uses Spring's in-process
 * {@code ApplicationEvent}; a consumer may bridge it to mate-mq-starter for a
 * cross-service domain event.
 *
 * @author mateaix
 */
public class OrgSyncStartedEvent extends ApplicationEvent {

    private final String provider;
    private final SyncMode mode;

    public OrgSyncStartedEvent(Object source, String provider, SyncMode mode) {
        super(source);
        this.provider = provider;
        this.mode = mode;
    }

    public String getProvider() {
        return provider;
    }

    public SyncMode getMode() {
        return mode;
    }
}
