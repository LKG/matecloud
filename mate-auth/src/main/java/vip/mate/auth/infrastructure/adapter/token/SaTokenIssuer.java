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

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import vip.mate.auth.domain.adapter.port.RolePermissionResolverPort;
import vip.mate.auth.domain.adapter.port.TokenIssuerPort;
import vip.mate.auth.domain.model.aggregate.AuthUser;
import vip.mate.auth.domain.model.valobj.LoginResult;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Sa-Token implementation of {@link TokenIssuerPort}. Also pushes the user's
 * role + permission set into Redis so the WebFlux gateway can read them from its
 * own {@code StpInterface}.
 * <p>
 * Mode-agnostic: the only deployment-specific concern — resolving roles and
 * permissions — is delegated to {@link RolePermissionResolverPort} (Dubbo in
 * microservice mode, in-process in monolith mode), so this issuer is the single
 * implementation for both.
 *
 * @author mateaix
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SaTokenIssuer implements TokenIssuerPort {

    private static final String DEFAULT_TENANT_ID = "1";

    private final StringRedisTemplate stringRedisTemplate;
    private final RolePermissionResolverPort rolePermissionResolver;

    @Override
    public LoginResult issue(AuthUser user) {
        StpUtil.login(user.getUserId());
        StpUtil.getSession().set("userId",   user.getUserId());
        StpUtil.getSession().set("username", user.getUsername());
        StpUtil.getSession().set("realName", user.getRealName());

        // Multi-tenant: persist tenantId in the Sa-Token session so the
        // tenant-starter's TokenTenantResolver can read it on every request
        // without re-querying the user store. Falls back to the system tenant
        // when the user store has no value (legacy single-tenant rows).
        String tenantId = user.getTenantId() != null && !user.getTenantId().isBlank()
                ? user.getTenantId()
                : DEFAULT_TENANT_ID;
        StpUtil.getSession().set("tenantId", tenantId);

        List<String> roles = cacheRolesAndPermissions(user);
        // 网关 HeaderRelayFilter 读 session 注入 X-Roles 头, 下游 ACL 据此判定
        if (!roles.isEmpty()) {
            StpUtil.getSession().set("roles", roles);
        }

        return LoginResult.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .mobile(user.getMobile())
                .tokenName(StpUtil.getTokenName())
                .tokenValue(StpUtil.getTokenValue())
                .expiresInSeconds(StpUtil.getTokenTimeout())
                .roleCodes(user.getRoleCodes())
                .permissions(user.getPermissions())
                .build();
    }

    @Override
    public void revokeCurrentSession() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId != null) {
            stringRedisTemplate.delete(SessionCacheKeys.ROLE_KEY_PREFIX + loginId);
            stringRedisTemplate.delete(SessionCacheKeys.PERM_KEY_PREFIX + loginId);
            StpUtil.logout();
            log.info("[auth] Session revoked: loginId={}", loginId);
        }
    }

    @Override
    public String currentUserId() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        return loginId == null ? null : loginId.toString();
    }

    /** Resolve (via the port) and cache roles/permissions; returns the role set for session reuse. */
    private List<String> cacheRolesAndPermissions(AuthUser user) {
        String userId = user.getUserId();

        List<String> roles = rolePermissionResolver.resolveRoleKeys(user);
        if (roles != null && !roles.isEmpty()) {
            String roleKey = SessionCacheKeys.ROLE_KEY_PREFIX + userId;
            stringRedisTemplate.delete(roleKey);
            stringRedisTemplate.opsForSet().add(roleKey, roles.toArray(new String[0]));
            stringRedisTemplate.expire(roleKey, SessionCacheKeys.CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } else {
            roles = List.of();
        }

        List<String> permissions = rolePermissionResolver.resolvePermissions(user);
        if (permissions != null && !permissions.isEmpty()) {
            String permKey = SessionCacheKeys.PERM_KEY_PREFIX + userId;
            stringRedisTemplate.delete(permKey);
            stringRedisTemplate.opsForSet().add(permKey, permissions.toArray(new String[0]));
            stringRedisTemplate.expire(permKey, SessionCacheKeys.CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        }
        return roles;
    }
}
