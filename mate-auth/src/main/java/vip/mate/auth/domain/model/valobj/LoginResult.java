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
package vip.mate.auth.domain.model.valobj;

import lombok.Builder;

import java.util.List;

/**
 * Immutable value object returned from a successful login.
 *
 * @author mateaix
 */
@Builder
public record LoginResult(
        String userId,
        String username,
        String realName,
        String mobile,
        String tokenName,
        String tokenValue,
        long expiresInSeconds,
        /**
         * Long-lived, single-use refresh token. The access token ({@link #tokenValue})
         * is short-lived — frozen by Sa-Token's {@code active-timeout} after 30 min of
         * inactivity. When a request then hits 401 the client silently exchanges this
         * refresh token for a fresh access token via {@code POST /api/v1/auth/refresh},
         * so an active user never has to re-enter credentials until the refresh token
         * itself expires.
         */
        String refreshToken,
        List<String> roleCodes,
        List<String> permissions
) {
}
