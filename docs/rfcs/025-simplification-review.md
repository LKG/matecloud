# RFC-025: Simplification Review — Focus = Saying No

- **Status**: Draft (design intent — actual codebase retains all 20 starters for max flexibility)
- **Created**: 2026-04-12
- **Author**: Steve Jobs (product review)
- **Implementation note**: This RFC documents the *product ideal*. The actual `mate-starters/pom.xml` retains all 20 starters as independent modules. In practice, starters like `mate-lock-starter` and `mate-distribute-starter` remain separate for fine-grained dependency control. Teams can choose to follow this RFC's merge recommendations or keep the granular structure based on their project needs.

> "People think focus means saying yes to the thing you've got to focus on.
> But that's not what it means at all.
> It means saying no to the hundred other good ideas that there are."
> — WWDC 1997

## 诊断: kaleido-ai 的过度设计

对 kaleido-ai 实际代码的审计揭示了一个严重问题:

| 模块 | Java 文件数 | 被引用次数 | 判定 |
|------|------------|-----------|------|
| kaleido-sentinel | **0** | 1/11 | 空壳 |
| kaleido-seata | **0** | 2/11 | 空壳 |
| kaleido-nacos | **0** | config-only | 空壳 |
| kaleido-sms | 2 | **0/11** | 死代码 |
| kaleido-dynamic-tp | 1 | **0/11** | 死代码 |
| kaleido-job | 2 | **0/11** | 死代码 |
| kaleido-monitor | 1 | **0/11** | 死代码 |

**21 个模块，只有 13 个在真正工作。** 这不是架构，这是囤积症。

## 手术: matecloud 的 27 个 starter 砍到多少？

### 第一刀: 砍掉不应该独立成 starter 的

| 砍掉 | 原因 | 合并到 |
|------|------|--------|
| ~~mate-devtools-starter~~ | 开发工具不是生产能力，塞到 mate-web-starter 的 dev profile | mate-web-starter |
| ~~mate-doc-starter~~ | Smart-Doc 只是一个 Maven 插件配置，不需要 starter | Root pom.xml plugin 配置 |
| ~~mate-dynamic-tp-starter~~ | kaleido-ai 里零引用。线程池用 Spring Boot 原生配置足够 | 删除 |
| ~~mate-trace-starter~~ | Micrometer Tracing 是 Spring Boot 3.x 内置的，不需要 wrapper | mate-monitor-starter 加一行依赖 |
| ~~mate-gray-starter~~ | 灰度发布 90% 的团队用不到，用到时再加，不要预制 | 删除 (按需添加) |

**砍掉 5 个。27 → 22。**

### 第二刀: 合并功能重叠的

| 合并 | 原因 | 合并后 |
|------|------|--------|
| mate-security-starter + mate-datascope-starter | 都是安全相关的 AOP 切面，拆两个是过度拆分 | **mate-security-starter** (含 DataScope) |
| mate-idempotent-starter → mate-security-starter | 幂等本质上也是请求安全策略 | **mate-security-starter** |
| mate-lock-starter → mate-cache-starter | 分布式锁基于 Redisson，和 cache 是同一个底层 | **mate-cache-starter** (含 lock) |
| mate-distribute-starter → mate-cache-starter | 雪花 ID 的 WorkerId 依赖 Redis，同属 cache 范畴 | **mate-cache-starter** (含 snowflake) |

**合并 5 个。22 → 17。**

### 第三刀: 按需引入，不预制

| 延后 | 原因 |
|------|------|
| mate-sharding-starter | 99% 的项目前期不需要分库分表，需要时再引 |
| mate-seata-starter | 分布式事务是最后才需要的，先用本地事务 + 最终一致性 |
| mate-sentinel-starter | 内网微服务初期流量小，不急 |
| mate-flow-starter | 工作流是高级功能，不是脚手架必需 |
| mate-rule-starter | 规则引擎同上 |
| mate-tenant-starter | 多租户是 SaaS 场景才需要 |
| mate-test-starter | 测试基类直接放在各模块的 test scope 里 |

**7 个延后到 Wave 后期。不是砍掉，是不在 MVP 里。**

---

## 最终结果: MateCloud MVP Starter 矩阵

就像 1997 年的 2×2，这是 matecloud 的产品矩阵:

```
                    基础能力              业务能力
                 ─────────────      ─────────────
数据层           mate-ds-starter     mate-mq-starter
                 mate-cache-starter  mate-job-starter
                 (含 lock + snowflake)

接入层           mate-web-starter    mate-sa-token-starter
                 mate-nacos-starter  mate-file-starter
                 mate-rpc-starter    mate-monitor-starter

安全层           mate-security-starter  mate-excel-starter
                 (含 datascope + idempotent + sign + rate-limit + desensitize + audit + encrypt)
```

**10 个 starter。** 不是 27 个，不是 21 个。十个。

每一个都:
- 有真实代码 (不是空壳)
- 被至少 2 个服务引用 (不是死代码)
- 一句话说清楚做什么 (不是模糊的 umbrella)

---

## 对比 kaleido-ai 的具体简化

### 简化 1: Cache + Lock + Distribute = 一个 starter

kaleido-ai 拆了 3 个模块 (kaleido-cache, kaleido-lock, kaleido-distribute)，但它们全部依赖 Redisson。

**matecloud 做法**: mate-cache-starter 统一包含:
- Spring Cache + Caffeine L1 + Redis L2
- `@DistributedLock` 注解 + AOP
- `SnowflakeUtil.newSnowflakeId()`
- `RedissonService` 工具类
- `DelayQueueService` 延迟队列

**一个依赖搞定。** 不需要开发者理解 3 个模块的边界。

```xml
<!-- 以前 (kaleido-ai 风格): 3 个依赖 -->
<dependency><artifactId>kaleido-cache</artifactId></dependency>
<dependency><artifactId>kaleido-lock</artifactId></dependency>
<dependency><artifactId>kaleido-distribute</artifactId></dependency>

<!-- 现在 (matecloud): 1 个依赖 -->
<dependency><artifactId>mate-cache-starter</artifactId></dependency>
```

### 简化 2: Security 大一统

kaleido-ai 的 kaleido-limiter (限流) 是独立模块。加上 matecloud 原本计划的 datascope、idempotent，安全相关散落在 4 个 starter 里。

**matecloud 做法**: mate-security-starter 一个搞定全部安全切面:
```java
@ApiSign           // 接口签名
@RepeatSubmit      // 防重提交
@RateLimit         // 限流
@Desensitize       // 脱敏
@AuditLog          // 审计
@DataPermission    // 数据权限
@Idempotent        // 幂等
// + EncryptTypeHandler 字段加密
```

**一句话定义: mate-security-starter = 接口防护的所有事。**

### 简化 3: 砍掉 Smart-Doc starter

kaleido-ai 有 kaleido-doc 模块——1 个 Java 文件。这是对 starter 概念的亵渎。

Smart-Doc 是一个 Maven 插件。它不需要 runtime auto-configuration。把它放在 root pom.xml 的 `<pluginManagement>` 里:

```xml
<!-- root pom.xml -->
<plugin>
    <groupId>com.github.shalousun</groupId>
    <artifactId>smart-doc-maven-plugin</artifactId>
    <version>${smart-doc.version}</version>
</plugin>
```

Done. 不需要 starter。

### 简化 4: Nacos starter 只做一件事

kaleido-ai 的 kaleido-nacos 模块有 0 个 Java 文件——它只是 3 个 Maven 依赖 + 一个 nacos.yml。

**matecloud 做法**: mate-nacos-starter 同样极简，但把 nacos.yml 做成 auto-import:
```
mate-nacos-starter/
├── pom.xml                    # 3 dependencies
└── src/main/resources/
    └── nacos.yml              # 自动被 Spring Cloud bootstrap 发现
```

0 个 Java 文件。这正好说明它做对了——最好的代码就是不需要写的代码。

### 简化 5: 不预制 Sentinel / Seata / Sharding

kaleido-ai 的经验已经证明了——这三个模块在它自己的项目里都是**零代码空壳**。

matecloud 的正确做法: 在 docs/rfcs 里有设计文档 (RFC-016, 017, 020)，但 **不在 MVP 的 mate-starters/ 目录里创建它们**。当某个业务模块真正需要分库分表时，再创建 mate-sharding-starter。

---

## 更新后的 CLAUDE.md Module Structure

```
matecloud/
├── mate-common/                  # 纯类型 (ZERO auto-config)
│   ├── mate-base/                # BaseEntity, Result, BizException, ErrorCode
│   └── mate-api/                 # Shared DTOs, RPC interfaces, enums
│
├── mate-starters/                # 10 starters (each one earns its place)
│   ├── mate-ds-starter/          # MyBatis Plus + Druid + MetaObjectHandler
│   ├── mate-web-starter/         # WebMvc + GlobalExceptionHandler + Jackson + DevTools(dev)
│   ├── mate-cache-starter/       # Redis + Caffeine + Lock + Snowflake + DelayQueue
│   ├── mate-nacos-starter/       # Nacos discovery + config (0 Java files)
│   ├── mate-rpc-starter/         # Dubbo + Nacos registry + ExceptionFilter
│   ├── mate-mq-starter/          # RabbitMQ + EventPublisher + DomainEventHandler
│   ├── mate-job-starter/         # XXL-Job auto-config
│   ├── mate-sa-token-starter/    # Sa-Token (servlet + reactor + Redis)
│   ├── mate-security-starter/    # ALL security: Sign/Repeat/Rate/Desensitize/Audit/DataScope/Idempotent/Encrypt
│   ├── mate-file-starter/        # MinIO upload/download/presigned
│   ├── mate-monitor-starter/     # Actuator + Prometheus + Tracing
│   └── mate-excel-starter/       # @ExcelExport + @ExcelImport (EasyExcel)
│
├── mate-gateway/                 # port 9010
├── mate-auth/                    # port 9020
├── mate-admin/                   # port 9040
├── mate-cli/                     # Picocli + MCP server
└── mate-biz/
    ├── mate-system/              # port 9030 (DDD example)
    └── mate-notice/              # port 9050
```

## 延后到需要时再创建的 (不在 MVP 目录里)

| 模块 | 触发条件 |
|------|---------|
| mate-sharding-starter | 单表超过 1000 万行 |
| mate-seata-starter | 跨服务写操作需要强一致性 |
| mate-sentinel-starter | QPS > 1000 或需要熔断 |
| mate-tenant-starter | 项目转 SaaS 模式 |
| mate-flow-starter | 业务需要审批流 |
| mate-rule-starter | 业务规则频繁变更 |
| mate-test-starter | 团队超过 5 人需要统一测试基类 |

**RFC 文档保留** (016, 017, 012, 013, 014) 作为设计储备。但代码目录里不创建空壳。

---

## 验证规则 (The Acid Test)

每个 starter 必须通过三个问题:

1. **一句话能说清吗?** — 如果需要两句话解释边界，就该合并
2. **有 ≥2 个服务在用吗?** — 只有 1 个服务用的，塞到那个服务里
3. **去掉它项目还能跑吗?** — 如果不影响，就不该在 MVP 里

kaleido-ai 的 kaleido-sms、kaleido-dynamic-tp、kaleido-job 全部无法通过第 2 条。
matecloud 不会重复这个错误。
