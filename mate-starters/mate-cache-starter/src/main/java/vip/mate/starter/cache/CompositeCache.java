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
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.concurrent.Callable;

/**
 * Composite Cache: L1 (Caffeine) + L2 (Redis).
 *
 * @author mateaix
 */
public class CompositeCache implements Cache {

    private final String name;
    @Nullable
    private final Cache l1Cache;
    @Nullable
    private final Cache l2Cache;
    /** Broadcasts L1 invalidations to other instances; null disables cross-instance sync. */
    @Nullable
    private final CacheSyncBroadcaster broadcaster;

    public CompositeCache(String name,
                          @Nullable Cache l1Cache,
                          @Nullable Cache l2Cache,
                          @Nullable CacheSyncBroadcaster broadcaster) {
        this.name = name;
        this.l1Cache = l1Cache;
        this.l2Cache = l2Cache;
        this.broadcaster = broadcaster;
    }

    /** Tell other instances to drop this key from their L1 (no-op without an L1 to mirror). */
    private void broadcastEvict(Object key) {
        if (broadcaster != null && l1Cache != null) {
            broadcaster.broadcastEvict(name, key);
        }
    }

    private void broadcastClear() {
        if (broadcaster != null && l1Cache != null) {
            broadcaster.broadcastClear(name);
        }
    }

    @Override
    @NonNull
    public String getName() {
        return name;
    }

    @Override
    @NonNull
    public Object getNativeCache() {
        return this;
    }

    @Override
    @Nullable
    public ValueWrapper get(@NonNull Object key) {
        if (l1Cache != null) {
            ValueWrapper wrapper = l1Cache.get(key);
            if (wrapper != null) {
                return wrapper;
            }
        }
        if (l2Cache != null) {
            ValueWrapper wrapper = l2Cache.get(key);
            if (wrapper != null) {
                if (l1Cache != null) {
                    l1Cache.put(key, wrapper.get());
                }
                return wrapper;
            }
        }
        return null;
    }

    @Override
    @Nullable
    public <T> T get(@NonNull Object key, @Nullable Class<T> type) {
        if (l1Cache != null) {
            T value = l1Cache.get(key, type);
            if (value != null) {
                return value;
            }
        }
        if (l2Cache != null) {
            T value = l2Cache.get(key, type);
            if (value != null) {
                if (l1Cache != null) {
                    l1Cache.put(key, value);
                }
                return value;
            }
        }
        return null;
    }

    @Override
    @Nullable
    public <T> T get(@NonNull Object key, @NonNull Callable<T> valueLoader) {
        if (l1Cache != null) {
            ValueWrapper wrapper = l1Cache.get(key);
            if (wrapper != null) {
                @SuppressWarnings("unchecked")
                T value = (T) wrapper.get();
                return value;
            }
        }
        if (l2Cache != null) {
            T value = l2Cache.get(key, valueLoader);
            if (value != null && l1Cache != null) {
                l1Cache.put(key, value);
            }
            return value;
        }
        try {
            return valueLoader.call();
        } catch (Exception e) {
            throw new Cache.ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(@NonNull Object key, @Nullable Object value) {
        // Write the shared L2 first, then the local L1, so a concurrent reader on
        // this instance never promotes a value newer than what L2 holds.
        if (l2Cache != null) {
            l2Cache.put(key, value);
        }
        if (l1Cache != null) {
            l1Cache.put(key, value);
        }
        // Other instances drop their stale L1 entry and lazily reload from L2.
        broadcastEvict(key);
    }

    @Override
    @Nullable
    public ValueWrapper putIfAbsent(@NonNull Object key, @Nullable Object value) {
        ValueWrapper existing = get(key);
        if (existing != null) {
            return existing;
        }
        put(key, value);
        return null;
    }

    @Override
    public void evict(@NonNull Object key) {
        // Evict shared L2 first, then local L1, then tell other instances.
        if (l2Cache != null) {
            l2Cache.evict(key);
        }
        if (l1Cache != null) {
            l1Cache.evict(key);
        }
        broadcastEvict(key);
    }

    @Override
    public boolean evictIfPresent(@NonNull Object key) {
        boolean evicted = false;
        if (l2Cache != null) {
            evicted = l2Cache.evictIfPresent(key);
        }
        if (l1Cache != null) {
            evicted = l1Cache.evictIfPresent(key) || evicted;
        }
        broadcastEvict(key);
        return evicted;
    }

    @Override
    public void clear() {
        if (l2Cache != null) {
            l2Cache.clear();
        }
        if (l1Cache != null) {
            l1Cache.clear();
        }
        broadcastClear();
    }

    @Override
    public boolean invalidate() {
        boolean invalidated = false;
        if (l2Cache != null) {
            invalidated = l2Cache.invalidate();
        }
        if (l1Cache != null) {
            invalidated = l1Cache.invalidate() || invalidated;
        }
        broadcastClear();
        return invalidated;
    }
}
