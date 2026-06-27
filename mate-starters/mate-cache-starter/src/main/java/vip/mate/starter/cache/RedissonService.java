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

import org.redisson.api.RKeys;
import org.redisson.api.RList;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Utility wrapping common Redisson / Redis operations.
 *
 * @author mateaix
 */
public class RedissonService {

    private final RedissonClient redissonClient;

    public RedissonService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public RedissonClient getClient() {
        return redissonClient;
    }

    public <T> void set(String key, T value) {
        redissonClient.<T>getBucket(key).set(value);
    }

    public <T> void set(String key, T value, Duration ttl) {
        redissonClient.<T>getBucket(key).set(value, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redissonClient.getBucket(key).get();
    }

    public boolean delete(String key) {
        return redissonClient.getBucket(key).delete();
    }

    public long delete(String... keys) {
        RKeys rKeys = redissonClient.getKeys();
        return rKeys.delete(keys);
    }

    public boolean exists(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    public boolean expire(String key, Duration ttl) {
        return redissonClient.getBucket(key).expire(ttl);
    }

    public long ttl(String key) {
        return redissonClient.getBucket(key).remainTimeToLive();
    }

    public <K, V> RMap<K, V> getMap(String key) {
        return redissonClient.getMap(key);
    }

    public <V> void hSet(String key, String field, V value) {
        redissonClient.<String, V>getMap(key).put(field, value);
    }

    @SuppressWarnings("unchecked")
    public <V> V hGet(String key, String field) {
        return (V) redissonClient.getMap(key).get(field);
    }

    public <K, V> Map<K, V> hGetAll(String key) {
        return redissonClient.<K, V>getMap(key).readAllMap();
    }

    public boolean hDelete(String key, String... fields) {
        // fastRemove deletes all fields in one atomic round trip and returns the
        // count removed, instead of a per-field remove() loop (N round trips, and
        // each remove() needlessly fetches+returns the old value).
        RMap<String, Object> map = redissonClient.getMap(key);
        return map.fastRemove(fields) > 0;
    }

    public <V> RList<V> getList(String key) {
        return redissonClient.getList(key);
    }

    public <V> void listAdd(String key, V value) {
        redissonClient.<V>getList(key).add(value);
    }

    public <V> List<V> listAll(String key) {
        return redissonClient.<V>getList(key).readAll();
    }

    public <V> RSet<V> getSet(String key) {
        return redissonClient.getSet(key);
    }

    public <V> boolean setAdd(String key, V value) {
        return redissonClient.<V>getSet(key).add(value);
    }

    public <V> Collection<V> setMembers(String key) {
        return redissonClient.<V>getSet(key).readAll();
    }

    public long increment(String key) {
        return redissonClient.getAtomicLong(key).incrementAndGet();
    }

    public long incrementBy(String key, long delta) {
        return redissonClient.getAtomicLong(key).addAndGet(delta);
    }

    public long decrement(String key) {
        return redissonClient.getAtomicLong(key).decrementAndGet();
    }

    public RLock getLock(String key) {
        return redissonClient.getLock(key);
    }

    public <M> long publish(String topic, M message) {
        return redissonClient.<M>getTopic(topic).publish(message);
    }

    public <M> int subscribe(String topic, Class<M> type, MessageListener<M> listener) {
        return redissonClient.<M>getTopic(topic).addListener(type, listener);
    }
}
