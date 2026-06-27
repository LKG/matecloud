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
package vip.mate.system.infrastructure.adapter.port;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import vip.mate.system.domain.adapter.port.DomainEventPublisher;
import vip.mate.system.domain.event.BaseDomainEvent;

/**
 * Publishes domain events <b>synchronously on the caller's (transactional)
 * thread</b>. This is deliberate: when a command publishes inside an open
 * transaction, the event must be raised on the same thread so a downstream
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} can bind to that
 * transaction and only fire once the write actually commits.
 *
 * <p>Do NOT make publishing itself {@code @Async} — that would detach the event
 * from the transaction and let listeners run before (or despite a rolled-back)
 * commit, producing phantom side effects. Asynchrony belongs on the
 * <em>listener</em> ({@code @Async @TransactionalEventListener(AFTER_COMMIT)}),
 * not here.
 *
 * @author mateaix
 */
@Component
@RequiredArgsConstructor
public class DomainEventPublisherImpl implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(BaseDomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publishAsync(BaseDomainEvent event) {
        // Intentionally synchronous publish — see class javadoc. Off-thread
        // handling is the listener's responsibility, not the publisher's.
        applicationEventPublisher.publishEvent(event);
    }
}
