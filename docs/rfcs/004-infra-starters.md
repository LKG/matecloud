# RFC-004: Infrastructure Starters

| Field       | Value                        |
|-------------|------------------------------|
| **RFC**     | 004                          |
| **Title**   | Infrastructure Starters      |
| **Status**  | Draft                        |
| **Created** | 2026-04-11                   |

## Summary

This RFC defines four infrastructure-level Spring Boot starters for the matecloud DDD microservice scaffold:

1. **mate-cache-starter** -- Two-level cache (Caffeine L1 + Redis L2) with Redisson utilities
2. **mate-lock-starter** -- Distributed lock via Redisson (annotation + programmatic)
3. **mate-monitor-starter** -- Actuator + Prometheus metrics export
4. **mate-sa-token-starter** -- Sa-Token authentication integration

All starters follow the convention:
- GroupId: `vip.mate`
- Package base: `vip.mate.starter.<module>`
- Each provides `@AutoConfiguration` plus `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- **NO JetCache** -- caching uses Spring Cache + Redis + Caffeine exclusively

---

## 1. mate-cache-starter

### 1.1 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>mate-starters</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>mate-cache-starter</artifactId>
    <packaging>jar</packaging>
    <description>MateCloud Cache Starter - Caffeine L1 + Redis L2 with Redisson</description>

    <dependencies>
        <!-- Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Redisson -->
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
            <version>3.52.0</version>
        </dependency>

        <!-- Caffeine -->
        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
            <version>3.2.3</version>
        </dependency>

        <!-- AOP -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>

        <!-- Spring Cache -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>
    </dependencies>
</project>
```

### 1.2 Java Sources

#### CacheAutoConfiguration.java

```java
package vip.mate.starter.cache;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.redisson.api.RedissonClient;

import java.time.Duration;

/**
 * Cache auto-configuration.
 * <p>
 * Enables Spring Cache and provides a composite two-level cache manager
 * (Caffeine L1 + Redis L2). Uses standard {@code @Cacheable} / {@code @CacheEvict}
 * annotations -- no JetCache.
 */
@AutoConfiguration
@EnableCaching
@ConditionalOnClass({RedissonClient.class, Caffeine.class})
public class CacheAutoConfiguration {

    /**
     * Caffeine-backed L1 cache manager.
     */
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

    /**
     * Redis-backed L2 cache manager.
     */
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

    /**
     * Primary composite cache manager -- tries Caffeine first, falls back to Redis.
     */
    @Bean
    @Primary
    public CacheManager cacheManager(CaffeineCacheManager caffeineCacheManager,
                                     RedisCacheManager redisCacheManager) {
        return new CompositeCacheManager(caffeineCacheManager, redisCacheManager);
    }

    /**
     * Redisson utility service.
     */
    @Bean
    public RedissonService redissonService(RedissonClient redissonClient) {
        return new RedissonService(redissonClient);
    }
}
```

#### CompositeCacheManager.java

```java
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
 * Two-level composite cache manager.
 * <p>
 * Read path: L1 (Caffeine) -> L2 (Redis).
 * Write path: writes to both levels via {@link CompositeCache}.
 */
public class CompositeCacheManager implements CacheManager {

    private final CaffeineCacheManager caffeineCacheManager;
    private final RedisCacheManager redisCacheManager;

    public CompositeCacheManager(CaffeineCacheManager caffeineCacheManager,
                                 RedisCacheManager redisCacheManager) {
        this.caffeineCacheManager = caffeineCacheManager;
        this.redisCacheManager = redisCacheManager;
    }

    @Override
    @Nullable
    public Cache getCache(@NonNull String name) {
        Cache caffeineCache = caffeineCacheManager.getCache(name);
        Cache redisCache = redisCacheManager.getCache(name);
        if (caffeineCache == null && redisCache == null) {
            return null;
        }
        return new CompositeCache(name, caffeineCache, redisCache);
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
```

#### CompositeCache.java

```java
package vip.mate.starter.cache;

import org.springframework.cache.Cache;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.concurrent.Callable;

/**
 * A composite {@link Cache} that reads from L1 (Caffeine) first and falls back
 * to L2 (Redis). Writes and evictions are propagated to both levels.
 */
public class CompositeCache implements Cache {

    private final String name;
    @Nullable
    private final Cache l1Cache;
    @Nullable
    private final Cache l2Cache;

    public CompositeCache(String name,
                          @Nullable Cache l1Cache,
                          @Nullable Cache l2Cache) {
        this.name = name;
        this.l1Cache = l1Cache;
        this.l2Cache = l2Cache;
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
        // Try L1
        if (l1Cache != null) {
            ValueWrapper wrapper = l1Cache.get(key);
            if (wrapper != null) {
                return wrapper;
            }
        }
        // Try L2
        if (l2Cache != null) {
            ValueWrapper wrapper = l2Cache.get(key);
            if (wrapper != null) {
                // Back-fill L1
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
        // Try L1
        if (l1Cache != null) {
            T value = l1Cache.get(key, type);
            if (value != null) {
                return value;
            }
        }
        // Try L2
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
        // Try L1
        if (l1Cache != null) {
            ValueWrapper wrapper = l1Cache.get(key);
            if (wrapper != null) {
                @SuppressWarnings("unchecked")
                T value = (T) wrapper.get();
                return value;
            }
        }
        // Try L2 with loader
        if (l2Cache != null) {
            T value = l2Cache.get(key, valueLoader);
            if (value != null && l1Cache != null) {
                l1Cache.put(key, value);
            }
            return value;
        }
        // Fallback: call loader directly
        try {
            return valueLoader.call();
        } catch (Exception e) {
            throw new Cache.ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(@NonNull Object key, @Nullable Object value) {
        if (l1Cache != null) {
            l1Cache.put(key, value);
        }
        if (l2Cache != null) {
            l2Cache.put(key, value);
        }
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
        if (l1Cache != null) {
            l1Cache.evict(key);
        }
        if (l2Cache != null) {
            l2Cache.evict(key);
        }
    }

    @Override
    public boolean evictIfPresent(@NonNull Object key) {
        boolean evicted = false;
        if (l1Cache != null) {
            evicted = l1Cache.evictIfPresent(key);
        }
        if (l2Cache != null) {
            evicted = l2Cache.evictIfPresent(key) || evicted;
        }
        return evicted;
    }

    @Override
    public void clear() {
        if (l1Cache != null) {
            l1Cache.clear();
        }
        if (l2Cache != null) {
            l2Cache.clear();
        }
    }

    @Override
    public boolean invalidate() {
        boolean invalidated = false;
        if (l1Cache != null) {
            invalidated = l1Cache.invalidate();
        }
        if (l2Cache != null) {
            invalidated = l2Cache.invalidate() || invalidated;
        }
        return invalidated;
    }
}
```

#### RedissonService.java

```java
package vip.mate.starter.cache;

import org.redisson.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Utility class wrapping common Redisson / Redis operations.
 */
public class RedissonService {

    private static final Logger log = LoggerFactory.getLogger(RedissonService.class);

    private final RedissonClient redissonClient;

    public RedissonService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public RedissonClient getClient() {
        return redissonClient;
    }

    // ======================== String / Bucket ========================

    /**
     * Set a value with optional TTL.
     */
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

    // ======================== Hash / Map ========================

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
        long removed = 0;
        RMap<String, Object> map = redissonClient.getMap(key);
        for (String field : fields) {
            if (map.remove(field) != null) {
                removed++;
            }
        }
        return removed > 0;
    }

    // ======================== List ========================

    public <V> RList<V> getList(String key) {
        return redissonClient.getList(key);
    }

    public <V> void listAdd(String key, V value) {
        redissonClient.<V>getList(key).add(value);
    }

    public <V> List<V> listAll(String key) {
        return redissonClient.<V>getList(key).readAll();
    }

    // ======================== Set ========================

    public <V> RSet<V> getSet(String key) {
        return redissonClient.getSet(key);
    }

    public <V> boolean setAdd(String key, V value) {
        return redissonClient.<V>getSet(key).add(value);
    }

    public <V> Collection<V> setMembers(String key) {
        return redissonClient.<V>getSet(key).readAll();
    }

    // ======================== Atomic ========================

    public long increment(String key) {
        return redissonClient.getAtomicLong(key).incrementAndGet();
    }

    public long incrementBy(String key, long delta) {
        return redissonClient.getAtomicLong(key).addAndGet(delta);
    }

    public long decrement(String key) {
        return redissonClient.getAtomicLong(key).decrementAndGet();
    }

    // ======================== Lock (convenience) ========================

    public RLock getLock(String key) {
        return redissonClient.getLock(key);
    }

    // ======================== Topic / Pub-Sub ========================

    public <M> long publish(String topic, M message) {
        return redissonClient.<M>getTopic(topic).publish(message);
    }

    public <M> int subscribe(String topic, Class<M> type, MessageListener<M> listener) {
        return redissonClient.<M>getTopic(topic).addListener(type, listener);
    }
}
```

### 1.3 Auto-Configuration Imports

**File:** `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
vip.mate.starter.cache.CacheAutoConfiguration
```

---

## 2. mate-lock-starter

### 2.1 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>mate-starters</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>mate-lock-starter</artifactId>
    <packaging>jar</packaging>
    <description>MateCloud Distributed Lock Starter - Redisson-based annotation and programmatic locking</description>

    <dependencies>
        <!-- Provides RedissonClient -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-cache-starter</artifactId>
        </dependency>

        <!-- AOP -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
    </dependencies>
</project>
```

### 2.2 Java Sources

#### DistributedLock.java (Annotation)

```java
package vip.mate.starter.lock;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Annotation-driven distributed lock.
 * <p>
 * Place on any Spring-managed bean method. The AOP aspect will acquire a
 * Redisson lock before method execution and release it afterwards.
 *
 * <pre>{@code
 * @DistributedLock(name = "'order:' + #orderId", waitTime = 5, leaseTime = 10)
 * public void processOrder(Long orderId) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * Lock key name. Supports SpEL expressions referencing method parameters.
     */
    String name();

    /**
     * Maximum time to wait for the lock (seconds). Default 3s.
     */
    long waitTime() default 3;

    /**
     * Auto-release time after acquisition (seconds). Default 10s.
     * Set to -1 for Redisson watchdog (auto-extend).
     */
    long leaseTime() default 10;

    /**
     * Time unit for waitTime and leaseTime. Default SECONDS.
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
```

#### DistributedLockException.java

```java
package vip.mate.starter.lock;

/**
 * Thrown when a distributed lock cannot be acquired within the specified wait time.
 */
public class DistributedLockException extends RuntimeException {

    public DistributedLockException(String message) {
        super(message);
    }

    public DistributedLockException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

#### DistributedLockAspect.java

```java
package vip.mate.starter.lock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * AOP aspect that intercepts methods annotated with {@link DistributedLock}
 * and wraps them with a Redisson-based distributed lock.
 */
@Aspect
public class DistributedLockAspect {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockAspect.class);

    private static final String LOCK_KEY_PREFIX = "mate:lock:";

    private final RedissonClient redissonClient;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    public DistributedLockAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String lockKey = LOCK_KEY_PREFIX + resolveLockName(joinPoint, distributedLock.name());
        long waitTime = distributedLock.waitTime();
        long leaseTime = distributedLock.leaseTime();

        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;

        try {
            acquired = lock.tryLock(waitTime, leaseTime, distributedLock.timeUnit());
            if (!acquired) {
                throw new DistributedLockException(
                        "Failed to acquire distributed lock [" + lockKey
                                + "] within " + waitTime + " " + distributedLock.timeUnit());
            }
            log.debug("Acquired distributed lock [{}]", lockKey);
            return joinPoint.proceed();
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Released distributed lock [{}]", lockKey);
            }
        }
    }

    /**
     * Resolve SpEL expression in the lock name against method parameters.
     */
    private String resolveLockName(ProceedingJoinPoint joinPoint, String expression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = nameDiscoverer.getParameterNames(method);
        Object[] args = joinPoint.getArgs();

        if (paramNames == null || paramNames.length == 0) {
            return expression;
        }

        try {
            EvaluationContext context = new StandardEvaluationContext();
            for (int i = 0; i < paramNames.length; i++) {
                ((StandardEvaluationContext) context).setVariable(paramNames[i], args[i]);
            }
            Object value = parser.parseExpression(expression).getValue(context);
            return value != null ? value.toString() : expression;
        } catch (Exception e) {
            log.warn("Failed to parse SpEL expression [{}], using raw value", expression, e);
            return expression;
        }
    }
}
```

#### DistributedLockService.java

```java
package vip.mate.starter.lock;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Programmatic distributed lock API.
 * <p>
 * Use this when the annotation approach is not flexible enough (e.g. dynamic
 * lock names, conditional locking, nested lock scopes).
 *
 * <pre>{@code
 * String result = distributedLockService.executeWithLock(
 *     "order:" + orderId, 3, 10, TimeUnit.SECONDS,
 *     () -> orderService.process(orderId));
 * }</pre>
 */
public class DistributedLockService {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockService.class);

    private static final String LOCK_KEY_PREFIX = "mate:lock:";

    private final RedissonClient redissonClient;

    public DistributedLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * Execute a supplier within a distributed lock scope.
     *
     * @param lockName  lock key (will be prefixed with {@code mate:lock:})
     * @param waitTime  maximum wait time to acquire the lock
     * @param leaseTime auto-release time (-1 for watchdog)
     * @param timeUnit  time unit
     * @param supplier  business logic
     * @param <T>       return type
     * @return supplier result
     */
    public <T> T executeWithLock(String lockName, long waitTime, long leaseTime,
                                 TimeUnit timeUnit, Supplier<T> supplier) {
        String key = LOCK_KEY_PREFIX + lockName;
        RLock lock = redissonClient.getLock(key);
        boolean acquired = false;

        try {
            acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (!acquired) {
                throw new DistributedLockException(
                        "Failed to acquire distributed lock [" + key
                                + "] within " + waitTime + " " + timeUnit);
            }
            log.debug("Acquired distributed lock [{}]", key);
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DistributedLockException("Lock acquisition interrupted for [" + key + "]", e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Released distributed lock [{}]", key);
            }
        }
    }

    /**
     * Execute a runnable within a distributed lock scope.
     */
    public void executeWithLock(String lockName, long waitTime, long leaseTime,
                                TimeUnit timeUnit, Runnable runnable) {
        executeWithLock(lockName, waitTime, leaseTime, timeUnit, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Execute with default timing: 3s wait, 10s lease.
     */
    public <T> T executeWithLock(String lockName, Supplier<T> supplier) {
        return executeWithLock(lockName, 3, 10, TimeUnit.SECONDS, supplier);
    }

    /**
     * Execute with default timing: 3s wait, 10s lease.
     */
    public void executeWithLock(String lockName, Runnable runnable) {
        executeWithLock(lockName, 3, 10, TimeUnit.SECONDS, runnable);
    }

    /**
     * Try to acquire a lock. Caller is responsible for unlocking.
     */
    public RLock tryLock(String lockName, long waitTime, long leaseTime, TimeUnit timeUnit) {
        String key = LOCK_KEY_PREFIX + lockName;
        RLock lock = redissonClient.getLock(key);
        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (!acquired) {
                return null;
            }
            return lock;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DistributedLockException("Lock acquisition interrupted for [" + key + "]", e);
        }
    }
}
```

#### DistributedLockAutoConfiguration.java

```java
package vip.mate.starter.lock;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import vip.mate.starter.cache.CacheAutoConfiguration;

/**
 * Auto-configuration for distributed locking.
 * <p>
 * Depends on {@link CacheAutoConfiguration} which provides the {@link RedissonClient}.
 */
@AutoConfiguration(after = CacheAutoConfiguration.class)
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnBean(RedissonClient.class)
public class DistributedLockAutoConfiguration {

    @Bean
    public DistributedLockAspect distributedLockAspect(RedissonClient redissonClient) {
        return new DistributedLockAspect(redissonClient);
    }

    @Bean
    public DistributedLockService distributedLockService(RedissonClient redissonClient) {
        return new DistributedLockService(redissonClient);
    }
}
```

### 2.3 Auto-Configuration Imports

**File:** `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
vip.mate.starter.lock.DistributedLockAutoConfiguration
```

---

## 3. mate-monitor-starter

### 3.1 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>mate-starters</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>mate-monitor-starter</artifactId>
    <packaging>jar</packaging>
    <description>MateCloud Monitor Starter - Actuator + Prometheus metrics</description>

    <dependencies>
        <!-- Actuator -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Prometheus -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>
    </dependencies>
</project>
```

### 3.2 Java Sources

#### MonitorAutoConfiguration.java

```java
package vip.mate.starter.monitor;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for monitoring.
 * <p>
 * Simply ensures that Actuator and Prometheus are wired. The majority of
 * configuration is driven by {@code application.yml} properties:
 * <pre>
 * management:
 *   endpoints:
 *     web:
 *       exposure:
 *         include: health,info,prometheus,metrics
 *   metrics:
 *     export:
 *       prometheus:
 *         enabled: true
 * </pre>
 */
@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
public class MonitorAutoConfiguration {

    @Bean
    public MonitorInfoContributor monitorInfoContributor() {
        return new MonitorInfoContributor();
    }
}
```

#### MonitorInfoContributor.java

```java
package vip.mate.starter.monitor;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.Map;

/**
 * Contributes basic MateCloud framework info to the {@code /actuator/info} endpoint.
 */
public class MonitorInfoContributor implements InfoContributor {

    private final Instant startupTime = Instant.now();

    @Override
    public void contribute(@NonNull Info.Builder builder) {
        builder.withDetail("matecloud", Map.of(
                "framework", "MateCloud DDD Scaffold",
                "startupTime", startupTime.toString()
        ));
    }
}
```

### 3.3 Auto-Configuration Imports

**File:** `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
vip.mate.starter.monitor.MonitorAutoConfiguration
```

---

## 4. mate-sa-token-starter

### 4.1 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>mate-starters</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>mate-sa-token-starter</artifactId>
    <packaging>jar</packaging>
    <description>MateCloud Sa-Token Starter - Authentication and authorization</description>

    <dependencies>
        <!-- Sa-Token for Spring Boot 3 -->
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-spring-boot3-starter</artifactId>
            <version>1.44.0</version>
        </dependency>

        <!-- Sa-Token Reactor (for WebFlux gateway) -->
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-reactor-spring-boot3-starter</artifactId>
            <version>1.44.0</version>
            <optional>true</optional>
        </dependency>

        <!-- Sa-Token Redis integration (Jackson serializer) -->
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-redis-jackson</artifactId>
            <version>1.44.0</version>
        </dependency>

        <!-- Redis connection pool required by sa-token-redis -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-pool2</artifactId>
        </dependency>
    </dependencies>
</project>
```

### 4.2 Java Sources

#### SaTokenAutoConfiguration.java

```java
package vip.mate.starter.satoken;

import cn.dev33.satoken.SaManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Sa-Token.
 * <p>
 * Sa-Token's own auto-configuration handles most of the wiring. This class
 * provides MateCloud-specific utilities on top.
 */
@AutoConfiguration
@ConditionalOnClass(SaManager.class)
public class SaTokenAutoConfiguration {

    @Bean
    public StpUserUtil stpUserUtil() {
        return new StpUserUtil();
    }
}
```

#### StpUserUtil.java

```java
package vip.mate.starter.satoken;

import cn.dev33.satoken.stp.StpUtil;

/**
 * Utility class for retrieving current authenticated user information
 * from the Sa-Token session.
 * <p>
 * This is a convenience wrapper around {@link StpUtil} that provides
 * strongly-typed accessors for common user attributes stored in the
 * Sa-Token session.
 *
 * <p>Convention: Business services store the following keys in the session
 * during login:
 * <ul>
 *   <li>{@code userId} -- Long</li>
 *   <li>{@code username} -- String</li>
 *   <li>{@code realName} -- String</li>
 *   <li>{@code tenantId} -- Long (multi-tenant scenarios)</li>
 *   <li>{@code roleIds} -- String (comma-separated)</li>
 * </ul>
 */
public class StpUserUtil {

    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_REAL_NAME = "realName";
    private static final String KEY_TENANT_ID = "tenantId";
    private static final String KEY_ROLE_IDS = "roleIds";

    /**
     * Get the current login ID (as Long).
     */
    public Long getLoginId() {
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * Get the current login ID as String.
     */
    public String getLoginIdAsString() {
        return StpUtil.getLoginIdAsString();
    }

    /**
     * Get the token value of current session.
     */
    public String getTokenValue() {
        return StpUtil.getTokenValue();
    }

    /**
     * Check whether the current request is logged in.
     */
    public boolean isLogin() {
        return StpUtil.isLogin();
    }

    /**
     * Get userId from session.
     */
    public Long getUserId() {
        return (Long) StpUtil.getSession().get(KEY_USER_ID);
    }

    /**
     * Get username from session.
     */
    public String getUsername() {
        return (String) StpUtil.getSession().get(KEY_USERNAME);
    }

    /**
     * Get real name from session.
     */
    public String getRealName() {
        return (String) StpUtil.getSession().get(KEY_REAL_NAME);
    }

    /**
     * Get tenant ID from session (multi-tenant).
     */
    public Long getTenantId() {
        Object val = StpUtil.getSession().get(KEY_TENANT_ID);
        return val != null ? (Long) val : null;
    }

    /**
     * Get comma-separated role IDs from session.
     */
    public String getRoleIds() {
        return (String) StpUtil.getSession().get(KEY_ROLE_IDS);
    }

    /**
     * Store user attributes in session during login.
     */
    public void setUserSession(Long userId, String username, String realName,
                               Long tenantId, String roleIds) {
        StpUtil.getSession().set(KEY_USER_ID, userId);
        StpUtil.getSession().set(KEY_USERNAME, username);
        StpUtil.getSession().set(KEY_REAL_NAME, realName);
        StpUtil.getSession().set(KEY_TENANT_ID, tenantId);
        StpUtil.getSession().set(KEY_ROLE_IDS, roleIds);
    }

    /**
     * Logout current user.
     */
    public void logout() {
        StpUtil.logout();
    }

    /**
     * Check if the current user has a specific role.
     */
    public boolean hasRole(String role) {
        return StpUtil.hasRole(role);
    }

    /**
     * Check if the current user has a specific permission.
     */
    public boolean hasPermission(String permission) {
        return StpUtil.hasPermission(permission);
    }
}
```

### 4.3 Auto-Configuration Imports

**File:** `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
vip.mate.starter.satoken.SaTokenAutoConfiguration
```

---

## Appendix: Recommended application.yml Snippets

### Cache (mate-cache-starter)

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      database: 0
  cache:
    type: redis
```

### Monitor (mate-monitor-starter)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
  endpoint:
    health:
      show-details: always
```

### Sa-Token (mate-sa-token-starter)

```yaml
sa-token:
  token-name: Authorization
  timeout: 86400
  active-timeout: 3600
  is-concurrent: true
  is-share: false
  token-style: uuid
  is-log: true
```
