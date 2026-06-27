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
package vip.mate.starter.satoken;

import cn.dev33.satoken.stp.StpInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Default {@link StpInterface} implementation backed by Redis.
 *
 * <p>Looks up role / permission lists for a given login id under the keys
 * <ul>
 *   <li>{@code mate:user:role:{loginId}} — set of role codes</li>
 *   <li>{@code mate:user:perm:{loginId}} — set of permission codes</li>
 * </ul>
 *
 * Both keys are populated by the auth flow (see {@code SaTokenIssuer#issue}
 * in mate-auth) at login. If they are empty the user resolves to "no
 * permissions" — every {@code @SaCheckPermission} call then fails with 403.
 *
 * <p>Wired up by {@link SaTokenAutoConfiguration} as the default; bespoke
 * services (e.g. {@code mate-gateway}'s reactive variant) can override by
 * declaring their own {@link StpInterface} bean — Spring's
 * {@code @ConditionalOnMissingBean} on the factory method respects that.
 *
 * @author mateaix
 */
@Slf4j
@RequiredArgsConstructor
public class RedisStpInterface implements StpInterface {

    private static final String ROLE_KEY_PREFIX = "mate:user:role:";
    private static final String PERM_KEY_PREFIX = "mate:user:perm:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Set<String> perms = stringRedisTemplate.opsForSet().members(PERM_KEY_PREFIX + loginId);
        if (perms == null || perms.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("[Sa-Token] No permissions cached for loginId={}", loginId);
            }
            return Collections.emptyList();
        }
        return new ArrayList<>(perms);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Set<String> roles = stringRedisTemplate.opsForSet().members(ROLE_KEY_PREFIX + loginId);
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(roles);
    }
}
