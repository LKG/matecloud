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
package vip.mate.auth.types.constant;

/**
 * Redis keys for the per-user role/permission cache — shared across layers.
 * <p>
 * {@code SaTokenIssuer} (infrastructure) writes them on login so the WebFlux
 * gateway's {@code StpInterface} can read roles without an RPC;
 * {@code DubboRolePermissionResolver} (infrastructure) reads them as a
 * fail-closed fallback when mate-system is briefly unreachable;
 * {@code AuthAppService} (application) reads them to enrich the current-user
 * response.
 * <p>
 * Lives in the {@code types} layer (public) so all three reference ONE
 * definition — a drifting copy would mean a role/permission cache-key mismatch,
 * i.e. a live authorization bug.
 *
 * @author mateaix
 */
public final class SessionCacheKeys {

    public static final String ROLE_KEY_PREFIX = "mate:user:role:";
    public static final String PERM_KEY_PREFIX = "mate:user:perm:";
    public static final long CACHE_TTL_SECONDS = 86400L;

    private SessionCacheKeys() {
    }
}
