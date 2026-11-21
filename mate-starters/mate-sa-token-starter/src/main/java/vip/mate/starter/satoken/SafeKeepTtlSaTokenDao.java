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

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoForRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Sa-Token DAO that neutralizes a Redisson / Spring-Data incompatibility which
 * otherwise breaks login.
 * <p>
 * Sa-Token persists a session on {@code StpUtil.login()} via
 * {@link SaTokenDaoForRedisTemplate#setStringAndKeepTTL(String, String)}, issuing
 * {@code SET key value KEEPTTL XX} using Spring-Data's {@code Expiration.keepTtl()}.
 * Lettuce forwards that as the native {@code KEEPTTL} flag, so updating a
 * not-yet-persisted key is a harmless no-op. Redisson does <em>not</em> implement
 * {@code KEEPTTL}: it emulates it by reading the key's remaining TTL and re-issuing
 * {@code SET ... PX <ttl>}. During {@code createLoginSession -> addTerminal -> update}
 * the freshly created session key may still resolve to PTTL = -2, which Redisson
 * turns into {@code SET ... PX -2000 XX}. Redis validates the expiry before the
 * {@code XX} guard and rejects it with {@code ERR invalid expire time}, surfacing as
 * login {@code SYSE001}.
 * <p>
 * We override the single KEEPTTL code path and reproduce the semantics explicitly
 * with ordinary read + write commands, so behaviour is identical whether the
 * underlying client is Lettuce or Redisson.
 *
 * @author mateaix
 */
public class SafeKeepTtlSaTokenDao extends SaTokenDaoForRedisTemplate {

    /**
     * Rewrite {@code value} under {@code key} while preserving its current expiry,
     * without relying on {@code Expiration.keepTtl()}:
     * <ul>
     *   <li>key absent ({@code ttl == -2}) &rarr; no-op, matching native {@code KEEPTTL XX}</li>
     *   <li>key persistent ({@code ttl == -1}) &rarr; rewrite with no expiry</li>
     *   <li>key expiring &rarr; rewrite keeping the remaining seconds</li>
     * </ul>
     */
    @Override
    public void setStringAndKeepTTL(String key, String value) {
        Long ttl = stringRedisTemplate.getExpire(key);
        if (ttl == null || ttl == SaTokenDao.NOT_VALUE_EXPIRE) {
            return;
        }
        if (ttl == SaTokenDao.NEVER_EXPIRE) {
            stringRedisTemplate.opsForValue().set(key, value);
            return;
        }
        stringRedisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
    }
}
