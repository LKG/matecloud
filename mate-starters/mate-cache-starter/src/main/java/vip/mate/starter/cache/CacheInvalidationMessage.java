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

import java.io.Serializable;

/**
 * Broadcast over Redis to tell every instance to drop a stale entry (or the
 * whole cache) from its local L1 (Caffeine) tier. The L2 (Redis) tier is shared,
 * so only L1 needs cross-instance invalidation.
 *
 * @see CacheSyncBroadcaster
 *
 * @author mateaix
 */
public class CacheInvalidationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Instance id of the publisher, so receivers can ignore their own messages. */
    private String origin;
    private String cacheName;
    /** The cache key to evict; ignored when {@link #clear} is {@code true}. */
    private Object key;
    /** When {@code true}, clear the entire named cache rather than a single key. */
    private boolean clear;

    public CacheInvalidationMessage() {
    }

    public CacheInvalidationMessage(String origin, String cacheName, Object key, boolean clear) {
        this.origin = origin;
        this.cacheName = cacheName;
        this.key = key;
        this.clear = clear;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public Object getKey() {
        return key;
    }

    public void setKey(Object key) {
        this.key = key;
    }

    public boolean isClear() {
        return clear;
    }

    public void setClear(boolean clear) {
        this.clear = clear;
    }
}
