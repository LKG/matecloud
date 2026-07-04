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
package vip.mate.auth.domain.adapter.port;

/**
 * Outbound port: issue / consume long-lived refresh tokens.
 * <p>
 * A refresh token is an opaque, single-use credential bound to a user id. It
 * lets the client obtain a fresh short-lived access token without re-entering
 * credentials, and outlives the access token (default 7 days vs. the access
 * token's {@code active-timeout}). The default implementation stores tokens in
 * Redis; it is swappable (e.g. for a DB-backed store) without touching the
 * domain or application layers.
 *
 * @author mateaix
 */
public interface RefreshTokenPort {

    /**
     * Mint a fresh refresh token for the user, persist it (with the configured
     * TTL) and return the opaque token value.
     */
    String issue(String userId);

    /**
     * Atomically validate and invalidate a refresh token (single-use rotation):
     * the token is deleted as it is read, so it can never be replayed.
     *
     * @return the bound user id, or {@code null} if the token is unknown,
     *         already used, or expired.
     */
    String consume(String refreshToken);

    /**
     * Revoke every refresh token currently held by the user — called on logout
     * so a stolen or lingering refresh token can no longer mint access tokens.
     */
    void revokeByUser(String userId);
}
