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
package vip.mate.gateway.route;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * P2 — periodically rebuilds the gateway route table so newly-registered services (see
 * {@link DiscoveryMetadataRouteLocator}) become routable without a gateway restart.
 *
 * <p>Nacos discovery already triggers a heartbeat-driven refresh; this scheduler is a belt-and-braces
 * fallback that guarantees a freshly-registered service is routable within ~30s. One refresh is also
 * fired once the context is fully ready so dynamic routes are present from the first request.
 *
 * @author mateaix
 */
@Slf4j
@Configuration
@EnableScheduling
public class RouteRefreshScheduler {

    private final ApplicationEventPublisher publisher;

    public RouteRefreshScheduler(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    /** Build dynamic routes immediately after startup. */
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("[gateway] application ready — publishing initial RefreshRoutesEvent");
        publisher.publishEvent(new RefreshRoutesEvent(this));
    }

    /** Periodic fallback refresh so new services come online within ~30s. */
    @Scheduled(fixedDelay = 30_000, initialDelay = 15_000)
    public void refresh() {
        publisher.publishEvent(new RefreshRoutesEvent(this));
    }
}
