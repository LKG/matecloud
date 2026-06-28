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
package vip.mate.auth.infrastructure.adapter.token;

/**
 * Redis keys for the per-user role/permission cache. {@code SaTokenIssuer} writes
 * them on login (so the WebFlux gateway's {@code StpInterface} can read roles
 * without an RPC); {@code DubboRolePermissionResolver} reads them as a fail-closed
 * fallback when mate-system is briefly unreachable. Shared here so the two never
 * drift apart.
 *
 * @author mateaix
 */
final class SessionCacheKeys {

    static final String ROLE_KEY_PREFIX = "mate:user:role:";
    static final String PERM_KEY_PREFIX = "mate:user:perm:";
    static final long CACHE_TTL_SECONDS = 86400L;

    private SessionCacheKeys() {
    }
}
