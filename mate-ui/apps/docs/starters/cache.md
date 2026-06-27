# mate-cache-starter

缓存 Starter，封装 Caffeine L1 + Redis L2 二级缓存、分布式锁、雪花 ID。

## 提供的能力

- **二级缓存** — Caffeine 本地缓存（L1）+ Redis 远程缓存（L2），Spring Cache 抽象
- **`@DistributedLock`** — 基于 Redisson 的分布式锁注解
- **SnowflakeUtil** — 雪花 ID 生成器

## 二级缓存

使用标准 Spring Cache 注解：

```java
@Cacheable(value = "user", key = "#id")
public UserResponse getById(Long id) { ... }

@CacheEvict(value = "user", key = "#id")
public void updateUser(Long id, UpdateUserCommand cmd) { ... }
```

缓存策略：先查 Caffeine（毫秒级），未命中再查 Redis（毫秒~个位数毫秒），都未命中才查 DB。

## 分布式锁

```java
@DistributedLock(key = "'order:' + #orderId", waitTime = 5, leaseTime = 30)
public void processOrder(Long orderId) {
    // 自动获取锁，方法结束自动释放
}
```

| 参数 | 说明 | 默认值 |
|------|------|--------|
| key | 锁的 key（支持 SpEL） | 必填 |
| waitTime | 等待获取锁的时间（秒） | 5 |
| leaseTime | 持有锁的最大时间（秒） | 30 |

## 雪花 ID

```java
Long id = SnowflakeUtil.nextId();
```

Worker ID 通过 Redis 自动分配，无需手动配置。
