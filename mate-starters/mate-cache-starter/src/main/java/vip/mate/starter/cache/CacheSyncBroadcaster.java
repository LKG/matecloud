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
package vip.mate.starter.cache;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import jakarta.annotation.PostConstruct;
import java.util.UUID;

/**
 * Keeps the L1 (Caffeine) tier coherent across instances.
 * <p>
 * Spring's {@code @CacheEvict}/{@code @CachePut} only mutate the local JVM's L1
 * plus the shared L2. Without this broadcaster, a key evicted on instance A stays
 * stale in instance B's L1 until its {@code expireAfterWrite} window elapses — a
 * classic multi-node dirty read. Here every L1-affecting mutation is published on
 * a Redis topic; each instance evicts its own L1 on receipt and ignores its own
 * messages (so there is no broadcast loop, and L2 is never touched on receipt).
 *
 * @author mateaix
 */
@Slf4j
public class CacheSyncBroadcaster {

    private static final String TOPIC = "mate:cache:l1-invalidation";

    private final RedissonClient redissonClient;
    private final CaffeineCacheManager caffeineCacheManager;
    /** Identifies this JVM so we can drop echoes of our own publishes. */
    private final String instanceId = UUID.randomUUID().toString();

    public CacheSyncBroadcaster(RedissonClient redissonClient, CaffeineCacheManager caffeineCacheManager) {
        this.redissonClient = redissonClient;
        this.caffeineCacheManager = caffeineCacheManager;
    }

    @PostConstruct
    public void subscribe() {
        RTopic topic = redissonClient.getTopic(TOPIC);
        topic.addListener(CacheInvalidationMessage.class, (channel, msg) -> {
            if (msg == null || instanceId.equals(msg.getOrigin())) {
                return;
            }
            applyLocally(msg);
        });
        log.info("[mate-cache] L1 invalidation broadcaster subscribed (instance {})", instanceId);
    }

    /** Evict a single key from every instance's L1. */
    public void broadcastEvict(String cacheName, Object key) {
        publish(new CacheInvalidationMessage(instanceId, cacheName, key, false));
    }

    /** Clear an entire named cache's L1 on every instance. */
    public void broadcastClear(String cacheName) {
        publish(new CacheInvalidationMessage(instanceId, cacheName, null, true));
    }

    private void publish(CacheInvalidationMessage msg) {
        try {
            redissonClient.getTopic(TOPIC).publish(msg);
        } catch (Exception e) {
            // A failed broadcast must never break the caching write path; the local
            // tier is already consistent and the stale remote L1 expires on TTL.
            log.warn("[mate-cache] Failed to broadcast L1 invalidation for cache '{}': {}",
                    msg.getCacheName(), e.getMessage());
        }
    }

    private void applyLocally(CacheInvalidationMessage msg) {
        // Touch only L1 — L2 is shared and already consistent. getCache(name) on a
        // CaffeineCacheManager returns null only for an unknown name; nothing to do then.
        Cache l1 = caffeineCacheManager.getCache(msg.getCacheName());
        if (l1 == null) {
            return;
        }
        if (msg.isClear()) {
            l1.clear();
        } else {
            l1.evict(msg.getKey());
        }
    }
}
