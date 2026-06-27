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
package vip.mate.auth.domain.event;

import lombok.Builder;
import vip.mate.auth.domain.model.valobj.LoginType;

import java.time.Instant;

/**
 * Spring {@link org.springframework.context.ApplicationEvent} payload emitted
 * every time a login succeeds. Consumed by {@code LoginAuditPort} adapters.
 *
 * @author mateaix
 */
@Builder
public record UserLoggedInEvent(
        String userId,
        String username,
        LoginType loginType,
        String clientIp,
        String userAgent,
        Instant at
) {
}
