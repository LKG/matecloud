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

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Two-level composite cache manager (L1 Caffeine + L2 Redis).
 *
 * @author mateaix
 */
public class CompositeCacheManager implements CacheManager {

    private final CaffeineCacheManager caffeineCacheManager;
    private final RedisCacheManager redisCacheManager;
    /** Cross-instance L1 invalidation; null disables it (single-node / no Redis topic). */
    @Nullable
    private final CacheSyncBroadcaster broadcaster;

    public CompositeCacheManager(CaffeineCacheManager caffeineCacheManager,
                                 RedisCacheManager redisCacheManager,
                                 @Nullable CacheSyncBroadcaster broadcaster) {
        this.caffeineCacheManager = caffeineCacheManager;
        this.redisCacheManager = redisCacheManager;
        this.broadcaster = broadcaster;
    }

    @Override
    @Nullable
    public Cache getCache(@NonNull String name) {
        Cache caffeineCache = caffeineCacheManager.getCache(name);
        Cache redisCache = redisCacheManager.getCache(name);
        if (caffeineCache == null && redisCache == null) {
            return null;
        }
        return new CompositeCache(name, caffeineCache, redisCache, broadcaster);
    }

    @Override
    @NonNull
    public Collection<String> getCacheNames() {
        Set<String> names = new LinkedHashSet<>();
        names.addAll(caffeineCacheManager.getCacheNames());
        names.addAll(redisCacheManager.getCacheNames());
        return names;
    }
}
