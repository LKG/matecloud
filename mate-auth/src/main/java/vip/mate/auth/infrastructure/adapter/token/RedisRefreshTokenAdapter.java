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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import vip.mate.auth.domain.adapter.port.RefreshTokenPort;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;

/**
 * Redis-backed {@link RefreshTokenPort}.
 *
 * <p><b>Storage model</b> — two keys per token so both directions are O(1):
 * <ul>
 *   <li>forward {@code mate:auth:refresh:{token} -> userId} — resolves a token
 *       to its owner; consumed with an atomic GETDEL so a token is single-use;</li>
 *   <li>reverse {@code mate:auth:refresh:uid:{userId} -> SET<token>} — lets
 *       {@link #revokeByUser} drop every outstanding token on logout.</li>
 * </ul>
 * Both carry the same TTL, refreshed on every {@link #issue}. Tokens are opaque
 * 256-bit values from {@link SecureRandom} (URL-safe Base64, no padding) — they
 * carry no claims, so nothing can be forged or read off them.
 *
 * @author mateaix
 */
@Slf4j
@Component
public class RedisRefreshTokenAdapter implements RefreshTokenPort {

    /** {@code mate:auth:refresh:{token}} → userId. */
    private static final String TOKEN_KEY_PREFIX = "mate:auth:refresh:";
    /** {@code mate:auth:refresh:uid:{userId}} → SET of the user's live tokens. */
    private static final String USER_INDEX_PREFIX = "mate:auth:refresh:uid:";
    private static final int TOKEN_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final StringRedisTemplate redis;
    private final Duration ttl;

    public RedisRefreshTokenAdapter(StringRedisTemplate redis,
                                    @Value("${mate.auth.refresh-token.timeout:604800}") long ttlSeconds) {
        this.redis = redis;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    @Override
    public String issue(String userId) {
        String token = generateToken();
        redis.opsForValue().set(TOKEN_KEY_PREFIX + token, userId, ttl);

        String indexKey = USER_INDEX_PREFIX + userId;
        redis.opsForSet().add(indexKey, token);
        redis.expire(indexKey, ttl);
        return token;
    }

    @Override
    public String consume(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        // GETDEL: reading and deleting in one round-trip guarantees single-use —
        // two concurrent refreshes with the same token can never both succeed.
        String userId = redis.opsForValue().getAndDelete(TOKEN_KEY_PREFIX + refreshToken);
        if (userId == null) {
            return null;
        }
        redis.opsForSet().remove(USER_INDEX_PREFIX + userId, refreshToken);
        return userId;
    }

    @Override
    public void revokeByUser(String userId) {
        if (userId == null) {
            return;
        }
        String indexKey = USER_INDEX_PREFIX + userId;
        Set<String> tokens = redis.opsForSet().members(indexKey);
        if (tokens != null && !tokens.isEmpty()) {
            redis.delete(tokens.stream().map(t -> TOKEN_KEY_PREFIX + t).toList());
        }
        redis.delete(indexKey);
    }

    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }
}
