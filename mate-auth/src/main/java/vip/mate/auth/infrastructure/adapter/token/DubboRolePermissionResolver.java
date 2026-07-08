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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.apache.dubbo.config.annotation.DubboReference;
import vip.mate.api.admin.service.IRpcPermissionService;
import vip.mate.api.rpc.RpcConstants;
import vip.mate.auth.domain.adapter.port.RolePermissionResolverPort;
import vip.mate.auth.domain.model.aggregate.AuthUser;
import vip.mate.auth.types.constant.SessionCacheKeys;
import vip.mate.base.result.Result;

import java.util.List;

/**
 * Microservice-mode {@link RolePermissionResolverPort}: fetches roles/permissions
 * from mate-system over Dubbo.
 *
 * <p><b>Fail-closed:</b> on RPC failure it falls back to the Redis set that
 * {@code SaTokenIssuer} cached on the user's previous successful login, so a
 * transient mate-system outage does not silently strip a user's authority.
 *
 * @author mateaix
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mate.rpc.mode", havingValue = "dubbo", matchIfMissing = true)
public class DubboRolePermissionResolver implements RolePermissionResolverPort {

    private final StringRedisTemplate stringRedisTemplate;

    @DubboReference(check = false, timeout = 5000, retries = 1,
            version = RpcConstants.VERSION, group = RpcConstants.GROUP_SYSTEM)
    private IRpcPermissionService rpcPermissionService;

    public DubboRolePermissionResolver(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public List<String> resolveRoleKeys(AuthUser user) {
        List<String> roles = user.getRoleCodes();
        if (roles != null && !roles.isEmpty()) {
            return roles;
        }
        try {
            Result<List<String>> result = rpcPermissionService.getRoleKeysByUsername(user.getUsername());
            if (result != null && Boolean.TRUE.equals(result.getSuccess())
                    && result.getData() != null && !result.getData().isEmpty()) {
                log.debug("[auth] Fetched roles from mate-system for username={}: {}", user.getUsername(), result.getData());
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("[auth] Failed to fetch roles from mate-system for username={}: {}", user.getUsername(), e.getMessage());
            return fallbackFromCache(SessionCacheKeys.ROLE_KEY_PREFIX + user.getUserId(),
                    "roles", user.getUsername());
        }
        return List.of();
    }

    @Override
    public List<String> resolvePermissions(AuthUser user) {
        List<String> upstream = user.getPermissions();
        if (upstream != null && !upstream.isEmpty()) {
            return upstream;
        }
        try {
            Result<List<String>> result = rpcPermissionService.getPermissionsByUsername(user.getUsername());
            if (result != null && Boolean.TRUE.equals(result.getSuccess())
                    && result.getData() != null && !result.getData().isEmpty()) {
                log.debug("[auth] Fetched permissions from mate-system for username={}: {}", user.getUsername(), result.getData());
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("[auth] Failed to fetch permissions from mate-system for username={}: {}", user.getUsername(), e.getMessage());
            return fallbackFromCache(SessionCacheKeys.PERM_KEY_PREFIX + user.getUserId(),
                    "permissions", user.getUsername());
        }
        return List.of();
    }

    /**
     * Attempt to read a previously-cached role/permission set from Redis. If the
     * cache is also empty, log the degraded state and return empty rather than
     * throwing — the user can still log in but will have no authorised actions
     * until mate-system recovers.
     */
    private List<String> fallbackFromCache(String redisKey, String label, String username) {
        try {
            var cached = stringRedisTemplate.opsForSet().members(redisKey);
            if (cached != null && !cached.isEmpty()) {
                log.info("[auth] Using cached {} for username={} (mate-system unavailable)", label, username);
                return List.copyOf(cached);
            }
        } catch (Exception ex) {
            log.error("[auth] Redis fallback also failed for {} of username={}: {}", label, username, ex.getMessage());
        }
        log.error("[auth] No {} available for username={} — mate-system RPC failed and no Redis cache exists. "
                + "User will have zero {}.", label, username, label);
        return List.of();
    }
}
