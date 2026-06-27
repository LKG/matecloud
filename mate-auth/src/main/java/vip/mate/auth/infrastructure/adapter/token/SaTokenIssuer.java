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
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import vip.mate.api.admin.service.IRpcPermissionService;
import vip.mate.api.rpc.RpcConstants;
import vip.mate.auth.domain.adapter.port.TokenIssuerPort;
import vip.mate.auth.domain.model.aggregate.AuthUser;
import vip.mate.auth.domain.model.valobj.LoginResult;
import vip.mate.base.result.Result;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Sa-Token implementation of the {@link TokenIssuerPort}. Also pushes the
 * user's role + permission set into Redis so the WebFlux gateway can read
 * them from its own {@code StpInterface}.
 *
 * @author mateaix
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mate.rpc.mode", havingValue = "dubbo", matchIfMissing = true)
public class SaTokenIssuer implements TokenIssuerPort {

    private static final String ROLE_KEY_PREFIX = "mate:user:role:";
    private static final String PERM_KEY_PREFIX = "mate:user:perm:";
    private static final long CACHE_TTL_SECONDS = 86400L;
    private static final String DEFAULT_TENANT_ID = "1";

    private final StringRedisTemplate stringRedisTemplate;

    @DubboReference(check = false, timeout = 5000, retries = 1,
            version = RpcConstants.VERSION, group = RpcConstants.GROUP_SYSTEM)
    private IRpcPermissionService rpcPermissionService;

    public SaTokenIssuer(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

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
            stringRedisTemplate.delete(ROLE_KEY_PREFIX + loginId);
            stringRedisTemplate.delete(PERM_KEY_PREFIX + loginId);
            StpUtil.logout();
            log.info("[auth] Session revoked: loginId={}", loginId);
        }
    }

    @Override
    public String currentUserId() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        return loginId == null ? null : loginId.toString();
    }

    /** 解析并缓存角色/权限; 返回解析出的角色集 (供 session 写入复用, 避免二次 RPC)。 */
    private List<String> cacheRolesAndPermissions(AuthUser user) {
        String userId = user.getUserId();

        List<String> roles = resolveRoles(user);
        if (!roles.isEmpty()) {
            String roleKey = ROLE_KEY_PREFIX + userId;
            stringRedisTemplate.delete(roleKey);
            stringRedisTemplate.opsForSet().add(roleKey, roles.toArray(new String[0]));
            stringRedisTemplate.expire(roleKey, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        }

        List<String> permissions = resolvePermissions(user);
        if (!permissions.isEmpty()) {
            String permKey = PERM_KEY_PREFIX + userId;
            stringRedisTemplate.delete(permKey);
            stringRedisTemplate.opsForSet().add(permKey, permissions.toArray(new String[0]));
            stringRedisTemplate.expire(permKey, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        }
        return roles;
    }

    /**
     * Resolve roles for the user. First checks if the user already carries roles
     * (from mate-system). If not, queries mate-admin via Dubbo RPC by username.
     *
     * <p><b>Fail-closed:</b> if the RPC call fails, we check Redis for a
     * previously cached value. Only if both RPC and cache miss do we return
     * empty — this prevents a transient mate-admin outage from silently
     * stripping all roles from a logged-in user.
     */
    private List<String> resolveRoles(AuthUser user) {
        List<String> roles = user.getRoleCodes();
        if (roles != null && !roles.isEmpty()) {
            return roles;
        }
        // Query mate-admin for role keys by username
        try {
            Result<List<String>> result = rpcPermissionService.getRoleKeysByUsername(user.getUsername());
            if (result != null && Boolean.TRUE.equals(result.getSuccess())
                    && result.getData() != null && !result.getData().isEmpty()) {
                log.debug("[auth] Fetched roles from mate-admin for username={}: {}", user.getUsername(), result.getData());
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("[auth] Failed to fetch roles from mate-admin for username={}: {}", user.getUsername(), e.getMessage());
            // Fail-closed: try to use previously cached roles from Redis
            return fallbackFromCache(ROLE_KEY_PREFIX + user.getUserId(),
                    "roles", user.getUsername());
        }
        return List.of();
    }

    /**
     * Resolve permissions for the user. First checks upstream, then queries
     * mate-admin via Dubbo RPC by username.
     *
     * <p><b>Fail-closed:</b> same strategy as {@link #resolveRoles} — on RPC
     * failure, fall back to the Redis cache from a previous successful login.
     */
    private List<String> resolvePermissions(AuthUser user) {
        List<String> upstream = user.getPermissions();
        if (upstream != null && !upstream.isEmpty()) {
            return upstream;
        }
        // Query mate-admin for permissions by username
        try {
            Result<List<String>> result = rpcPermissionService.getPermissionsByUsername(user.getUsername());
            if (result != null && Boolean.TRUE.equals(result.getSuccess())
                    && result.getData() != null && !result.getData().isEmpty()) {
                log.debug("[auth] Fetched permissions from mate-admin for username={}: {}", user.getUsername(), result.getData());
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("[auth] Failed to fetch permissions from mate-admin for username={}: {}", user.getUsername(), e.getMessage());
            // Fail-closed: try to use previously cached permissions from Redis
            return fallbackFromCache(PERM_KEY_PREFIX + user.getUserId(),
                    "permissions", user.getUsername());
        }
        return List.of();
    }

    /**
     * Attempt to read a previously-cached role/permission set from Redis.
     * If the cache is also empty, log an error (this is a degraded state)
     * and return an empty list rather than throwing — the user can still log
     * in but will have no authorised actions until mate-admin recovers.
     */
    private List<String> fallbackFromCache(String redisKey, String label, String username) {
        try {
            var cached = stringRedisTemplate.opsForSet().members(redisKey);
            if (cached != null && !cached.isEmpty()) {
                log.info("[auth] Using cached {} for username={} (mate-admin unavailable)", label, username);
                return List.copyOf(cached);
            }
        } catch (Exception ex) {
            log.error("[auth] Redis fallback also failed for {} of username={}: {}", label, username, ex.getMessage());
        }
        log.error("[auth] No {} available for username={} — mate-admin RPC failed and no Redis cache exists. "
                + "User will have zero {}.", label, username, label);
        return List.of();
    }
}
