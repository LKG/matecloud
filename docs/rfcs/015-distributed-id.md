# RFC-015: Distributed ID Generation

| Field       | Value                              |
|-------------|------------------------------------|
| **RFC**     | 015                                |
| **Title**   | Distributed ID Generation          |
| **Status**  | Draft                              |
| **Created** | 2026-04-11                         |

## Summary

This RFC defines **mate-distribute-starter**, a Spring Boot starter providing Snowflake-based distributed ID generation for the matecloud microservice scaffold. Worker IDs are automatically assigned per JVM instance via Redis atomic increment, ensuring uniqueness across horizontally scaled deployments.

Key components:
- `SnowflakeUtil` -- static utility returning String IDs via Hutool Snowflake
- `WorkerIdHolder` -- Redis-based worker ID assignment on startup (mod 32)
- `DistributeAutoConfiguration` -- auto-configuration wiring

Reference: kaleido-ai/kaleido-common/kaleido-distribute

---

## 1. pom.xml

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

    <artifactId>mate-distribute-starter</artifactId>
    <packaging>jar</packaging>
    <description>MateCloud Distribute Starter - Snowflake ID generation with Redis-based worker ID</description>

    <dependencies>
        <!-- mate-cache-starter provides Redisson -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-cache-starter</artifactId>
        </dependency>

        <!-- Hutool for Snowflake -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-core</artifactId>
        </dependency>

        <!-- mate-base for common types -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-base</artifactId>
        </dependency>
    </dependencies>
</project>
```

---

## 2. Java Source Code

### 2.1 SnowflakeUtil

```java
package vip.mate.starter.distribute.util;

import cn.hutool.core.util.IdUtil;
import vip.mate.starter.distribute.worker.WorkerIdHolder;

/**
 * Snowflake ID generation utility.
 * <p>
 * Produces globally unique, time-ordered String IDs using Hutool's Snowflake
 * implementation. The worker ID is auto-assigned from Redis on JVM startup
 * via {@link WorkerIdHolder}.
 * </p>
 *
 * <p>Usage:</p>
 * <pre>
 *   String id = SnowflakeUtil.newSnowflakeId();
 * </pre>
 */
public class SnowflakeUtil {

    private SnowflakeUtil() {
        // utility class
    }

    /**
     * Generate a new Snowflake ID as a String.
     *
     * @return unique Snowflake ID string
     */
    public static String newSnowflakeId() {
        return IdUtil.getSnowflake(WorkerIdHolder.WORKER_ID).nextIdStr();
    }
}
```

### 2.2 WorkerIdHolder

```java
package vip.mate.starter.distribute.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;

/**
 * Manages the Snowflake worker ID for the current JVM instance.
 * <p>
 * On application startup, atomically increments a Redis counter keyed by
 * the application name, then takes modulo 32 to derive a unique worker ID
 * within the Snowflake 5-bit worker ID space.
 * </p>
 * <p>
 * This guarantees that multiple instances of the same service will receive
 * different worker IDs (up to 32 concurrent instances).
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class WorkerIdHolder implements CommandLineRunner {

    private static final String WORKER_ID_PREFIX = "mate:distribute:worker:";
    private static final int MAX_WORKER_ID = 32;

    private final RedissonClient redissonClient;

    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * The Snowflake worker ID for this JVM instance.
     * Set once during startup and read by {@link vip.mate.starter.distribute.util.SnowflakeUtil}.
     */
    public static long WORKER_ID;

    @Override
    public void run(String... args) {
        String key = WORKER_ID_PREFIX + applicationName;
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        WORKER_ID = atomicLong.incrementAndGet() % MAX_WORKER_ID;
        log.info("[mate-distribute] Worker ID assigned: {} for application: {}", WORKER_ID, applicationName);
    }
}
```

### 2.3 DistributeAutoConfiguration

```java
package vip.mate.starter.distribute.config;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import vip.mate.starter.distribute.worker.WorkerIdHolder;

/**
 * Auto-configuration for mate-distribute-starter.
 * <p>
 * Registers {@link WorkerIdHolder} which auto-assigns a Snowflake worker ID
 * from Redis on startup. Requires a {@link RedissonClient} bean (provided by
 * mate-cache-starter).
 * </p>
 */
@AutoConfiguration
@ConditionalOnBean(RedissonClient.class)
public class DistributeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public WorkerIdHolder workerIdHolder(RedissonClient redissonClient) {
        return new WorkerIdHolder(redissonClient);
    }
}
```

### 2.4 spring.factories / AutoConfiguration.imports

**`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`**

```text
vip.mate.starter.distribute.config.DistributeAutoConfiguration
```

---

## 3. Configuration

No additional configuration is required. The starter uses:
- `spring.application.name` -- to namespace the Redis worker ID counter
- Redis connection from mate-cache-starter (Redisson)

Optional properties:

```yaml
# No special config needed; relies on mate-cache-starter Redis connection
spring:
  application:
    name: mate-system  # used as Redis key for worker ID
```

---

## 4. Usage Examples

### 4.1 Basic ID Generation

```java
import vip.mate.starter.distribute.util.SnowflakeUtil;

@Service
public class OrderService {

    public Order createOrder(CreateOrderCommand cmd) {
        String orderId = SnowflakeUtil.newSnowflakeId();
        // orderId example: "1867234567890123456"
        Order order = Order.builder()
                .id(orderId)
                .userId(cmd.getUserId())
                .build();
        return orderRepository.save(order);
    }
}
```

### 4.2 In DDD Aggregate Root

```java
import vip.mate.starter.distribute.util.SnowflakeUtil;

@Data
@SuperBuilder
public class NoticeAggregate extends BaseEntity {

    public static NoticeAggregate create(NoticeChannel channel, String target, String content) {
        String id = SnowflakeUtil.newSnowflakeId();
        return NoticeAggregate.builder()
                .id(id)
                .channel(channel)
                .target(target)
                .content(content)
                .status(NoticeStatus.PENDING)
                .build();
    }
}
```

### 4.3 In MyBatis Plus Entity

```java
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;

@Data
@TableName("t_order")
public class OrderPO {

    // Use Snowflake-generated ID as primary key (assigned externally)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String userId;
    private BigDecimal amount;
}
```

---

## 5. Module Structure

```
mate-starters/
  mate-distribute-starter/
    pom.xml
    src/main/java/
      vip/mate/starter/distribute/
        config/
          DistributeAutoConfiguration.java
        util/
          SnowflakeUtil.java
        worker/
          WorkerIdHolder.java
    src/main/resources/
      META-INF/spring/
        org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

---

## 6. Design Notes

1. **Worker ID space**: Snowflake supports 5-bit worker ID (0-31). Using `incr % 32` means the 33rd instance of the same service wraps to worker ID 0. For most microservice deployments, 32 instances per service is more than sufficient.

2. **Redis dependency**: The starter depends on mate-cache-starter for Redisson. This is acceptable because distributed ID generation inherently requires coordination, and Redis is already part of the infrastructure stack.

3. **Static access pattern**: `SnowflakeUtil.newSnowflakeId()` is static for maximum convenience. The worker ID is set once at startup via `CommandLineRunner` and never changes during the JVM lifecycle.

4. **Thread safety**: Hutool's `IdUtil.getSnowflake()` internally caches and synchronizes Snowflake instances per worker ID, so `SnowflakeUtil` is fully thread-safe.
