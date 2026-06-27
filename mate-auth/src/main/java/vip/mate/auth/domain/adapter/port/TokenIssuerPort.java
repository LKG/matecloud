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

import vip.mate.auth.domain.model.aggregate.AuthUser;
import vip.mate.auth.domain.model.valobj.LoginResult;

/**
 * Outbound port: issue / revoke authentication tokens.
 * <p>
 * Default implementation is Sa-Token; swappable for JWT or any other
 * token technology without touching domain code.
 *
 * @author mateaix
 */
public interface TokenIssuerPort {

    /**
     * Create a new session for the given user and return the full
     * {@link LoginResult} (token + user info + roles + permissions).
     */
    LoginResult issue(AuthUser user);

    /**
     * Invalidate the current session.
     */
    void revokeCurrentSession();

    /**
     * Return the currently logged-in user id, or {@code null} if no session.
     */
    String currentUserId();
}
