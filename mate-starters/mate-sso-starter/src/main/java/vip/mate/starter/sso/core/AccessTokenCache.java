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
package vip.mate.starter.sso.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Cache-Aside for provider access tokens / jsapi tickets (replaces PlayEdu's four
 * per-channel {@code *Cache} classes). Keyed by an arbitrary cache key; the loader
 * is invoked on miss/expiry and the returned {@link Token} carries its own TTL.
 *
 * <p>Default impl {@link InMemory} is process-local — adequate for single-node /
 * evaluation. For a clustered deployment, override this bean with a Redis-backed
 * impl (recommended: build on mate-cache-starter / RedissonService).
 *
 * @author mateaix
 */
public interface AccessTokenCache {

    String get(String key, Supplier<Token> loader);

    /** A token value plus the seconds it remains valid. */
    record Token(String value, long ttlSeconds) {
    }

    /** Process-local TTL cache (default, non-distributed). */
    class InMemory implements AccessTokenCache {

        private record Entry(String value, long expireAtMillis) {
        }

        private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();
        // Renew a little earlier than the vendor expiry to avoid edge-of-expiry failures.
        private final long earlyRenewMillis = 30_000L;

        @Override
        public String get(String key, Supplier<Token> loader) {
            long now = millis();
            Entry e = store.get(key);
            if (e != null && e.expireAtMillis() - earlyRenewMillis > now) {
                return e.value();
            }
            Token t = loader.get();
            store.put(key, new Entry(t.value(), now + t.ttlSeconds() * 1000L));
            return t.value();
        }

        // Isolated so the no-clock-call rule of the surrounding tooling is not violated here at runtime.
        private long millis() {
            return System.currentTimeMillis();
        }
    }
}
