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
package vip.mate.starter.sso.spi.model;

/**
 * Unified authentication input. OAuth providers consume {@link #code()} /
 * {@link #state()}; directory-bind providers (LDAP) consume {@link #username()} /
 * {@link #password()}.
 *
 * @author mateaix
 */
public record AuthRequest(
        String code,
        String state,
        String redirectUri,
        String username,
        String password) {

    public static AuthRequest ofCode(String code, String state, String redirectUri) {
        return new AuthRequest(code, state, redirectUri, null, null);
    }

    public static AuthRequest ofCredentials(String username, String password) {
        return new AuthRequest(null, null, null, username, password);
    }
}
