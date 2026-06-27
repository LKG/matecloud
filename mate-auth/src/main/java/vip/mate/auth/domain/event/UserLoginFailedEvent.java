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
 * Emitted when a login attempt fails (bad password, frozen account, ...).
 * Used by LoginAttemptPort for rate-limiting and by audit logging.
 *
 * @author mateaix
 */
@Builder
public record UserLoginFailedEvent(
        String account,
        LoginType loginType,
        String reason,
        String clientIp,
        Instant at
) {
}
