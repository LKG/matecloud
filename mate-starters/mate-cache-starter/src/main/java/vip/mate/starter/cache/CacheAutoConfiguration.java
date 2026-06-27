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

import com.github.benmanes.caffeine.cache.Caffeine;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Cache auto-configuration. Spring Cache + Caffeine L1 + Redis L2.
 *
 * <p>Redisson's own auto-config references Boot 3.x's RedisAutoConfiguration
 * which is removed in Boot 4.x, so we create RedissonClient manually here.</p>
 *
 * @author mateaix
 */
@AutoConfiguration
@EnableCaching
@ConditionalOnClass({RedissonClient.class, Caffeine.class})
public class CacheAutoConfiguration {

    /**
     * Create RedissonClient manually since Redisson's Boot 3.x auto-config
     * is excluded (incompatible with Boot 4.x).
     * Reads standard spring.data.redis.* properties.
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient(Environment env) {
        String host = env.getProperty("spring.data.redis.host", "127.0.0.1");
        int port = env.getProperty("spring.data.redis.port", Integer.class, 6379);
        String password = env.getProperty("spring.data.redis.password");
        int database = env.getProperty("spring.data.redis.database", Integer.class, 0);
        int timeout = env.getProperty("spring.data.redis.timeout", Integer.class, 5000);

        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database)
                .setConnectTimeout(timeout)
                // Command response timeout — previously the `timeout` property fed only
                // setConnectTimeout, so a stalled Redis could hang commands far longer
                // than the configured value. Also bound retries so calls fail fast.
                .setTimeout(timeout)
                .setRetryAttempts(3)
                .setConnectionMinimumIdleSize(2)
                .setConnectionPoolSize(16);
        if (password != null && !password.isBlank()) {
            config.useSingleServer().setPassword(password);
        }
        return Redisson.create(config);
    }

    @Bean("caffeineCacheManager")
    public CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(128)
                .maximumSize(1024)
                .expireAfterWrite(Duration.ofMinutes(5)));
        manager.setAllowNullValues(true);
        return manager;
    }

    @Bean("redisCacheManager")
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .transactionAware()
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(CacheSyncBroadcaster.class)
    public CacheSyncBroadcaster cacheSyncBroadcaster(RedissonClient redissonClient,
                                                     CaffeineCacheManager caffeineCacheManager) {
        return new CacheSyncBroadcaster(redissonClient, caffeineCacheManager);
    }

    @Bean
    @Primary
    public CacheManager cacheManager(CaffeineCacheManager caffeineCacheManager,
                                     RedisCacheManager redisCacheManager,
                                     CacheSyncBroadcaster cacheSyncBroadcaster) {
        return new CompositeCacheManager(caffeineCacheManager, redisCacheManager, cacheSyncBroadcaster);
    }

    @Bean
    public RedissonService redissonService(RedissonClient redissonClient) {
        return new RedissonService(redissonClient);
    }
}
