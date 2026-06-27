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
package vip.mate.starter.sso.core.redis;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import vip.mate.starter.sso.core.AccessTokenCache;

/**
 * Clustered {@link AccessTokenCache} on Redis (Redisson) — replaces the in-memory
 * default so multiple service instances share one provider token / jsapi ticket.
 * Auto-selected when a {@code RedissonClient} bean is present.
 *
 * <p>Refresh is guarded by a per-key distributed lock + double-check: WeChat
 * Work's {@code access_token} is a <em>per-corp singleton</em>, so concurrent
 * {@code gettoken} calls across instances/threads would each mint a new token and
 * invalidate the previously fetched one — causing intermittent login/sync
 * failures (the classic "cache stampede" against a single-flight vendor API).
 * Only one caller hits the vendor; the rest wait and read the freshly cached value.
 *
 * @author mateaix
 */
public class RedisAccessTokenCache implements AccessTokenCache {

    private static final long EARLY_RENEW_SECONDS = 30L;
    /** Max wait for the refresh lock — long enough for one vendor round-trip (HTTP timeout is 10s). */
    private static final long LOCK_WAIT_SECONDS = 12L;
    /** Lease for the refresh critical section; watchdog-free upper bound so the lock self-heals on crash. */
    private static final long LOCK_LEASE_SECONDS = 15L;

    private final RedissonClient redisson;

    public RedisAccessTokenCache(RedissonClient redisson) {
        this.redisson = redisson;
    }

    @Override
    public String get(String key, Supplier<Token> loader) {
        RBucket<String> bucket = redisson.getBucket(key);
        String cached = bucket.get();
        if (cached != null) {
            return cached;
        }
        RLock lock = redisson.getLock(key + ":refresh");
        boolean acquired = false;
        try {
            acquired = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (acquired) {
                // double-check: a peer may have refreshed while we waited for the lock
                String fresh = bucket.get();
                if (fresh != null) {
                    return fresh;
                }
                Token t = loader.get();
                long ttl = Math.max(1L, t.ttlSeconds() - EARLY_RENEW_SECONDS);
                bucket.set(t.value(), Duration.ofSeconds(ttl));
                return t.value();
            }
            // lock not acquired in time — the holder should have populated the cache by now
            String fresh = bucket.get();
            if (fresh != null) {
                return fresh;
            }
            // last resort: load without the lock rather than fail the request
            return loader.get().value();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return loader.get().value();
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
