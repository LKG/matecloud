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

import java.util.List;

/**
 * Outbound port: resolve a user's RBAC role keys and permission codes.
 * <p>
 * This is the ONLY part of token issuance that differs between deployment modes:
 * in microservice mode it goes to mate-system over Dubbo
 * ({@code DubboRolePermissionResolver}); in monolith mode it calls
 * mate-system's permission domain service in-process
 * ({@code LocalRolePermissionResolver}). {@code SaTokenIssuer} stays mode-agnostic
 * and depends only on this port.
 *
 * @author mateaix
 */
public interface RolePermissionResolverPort {

    /**
     * Role keys for the user. Implementations should prefer roles already carried
     * on the {@link AuthUser} and only look them up when absent. Never returns
     * {@code null} — an empty list means "no roles resolved".
     */
    List<String> resolveRoleKeys(AuthUser user);

    /**
     * Permission codes for the user, with the same contract as
     * {@link #resolveRoleKeys(AuthUser)}.
     */
    List<String> resolvePermissions(AuthUser user);
}
