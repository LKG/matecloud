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
package vip.mate.gateway.auth;

import cn.dev33.satoken.stp.StpInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Sa-Token permission / role data provider, backed by Redis.
 *
 * @author mateaix
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StpUserInterfaceImpl implements StpInterface {

    private static final String ROLE_KEY_PREFIX = "mate:user:role:";
    private static final String PERM_KEY_PREFIX = "mate:user:perm:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        String key = PERM_KEY_PREFIX + loginId;
        Set<String> perms = stringRedisTemplate.opsForSet().members(key);
        if (perms == null || perms.isEmpty()) {
            log.debug("[Gateway] No permissions found for loginId={}", loginId);
            return Collections.emptyList();
        }
        return new ArrayList<>(perms);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        String key = ROLE_KEY_PREFIX + loginId;
        Set<String> roles = stringRedisTemplate.opsForSet().members(key);
        if (roles == null || roles.isEmpty()) {
            log.debug("[Gateway] No roles found for loginId={}", loginId);
            return Collections.emptyList();
        }
        return new ArrayList<>(roles);
    }
}
