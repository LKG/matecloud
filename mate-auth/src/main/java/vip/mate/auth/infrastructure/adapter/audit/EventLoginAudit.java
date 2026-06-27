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
package vip.mate.auth.infrastructure.adapter.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import vip.mate.auth.domain.adapter.port.LoginAuditPort;
import vip.mate.auth.domain.event.UserLoggedInEvent;
import vip.mate.auth.domain.event.UserLoginFailedEvent;

/**
 * Publishes login events through Spring's {@link ApplicationEventPublisher}
 * so any downstream listener (audit log persistence, MQ, Prometheus counter)
 * can react without the auth pipeline knowing about it.
 *
 * @author mateaix
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventLoginAudit implements LoginAuditPort {

    private final ApplicationEventPublisher publisher;

    @Override
    public void recordSuccess(UserLoggedInEvent event) {
        publisher.publishEvent(event);
        log.debug("[audit] UserLoggedIn: {}", event);
    }

    @Override
    public void recordFailure(UserLoginFailedEvent event) {
        publisher.publishEvent(event);
        log.debug("[audit] UserLoginFailed: {}", event);
    }
}
