# RFC-029: Product Focus — 砍到骨头

- **Status**: Accepted
- **Created**: 2026-04-12
- **Author**: Product Review

> "Deciding what not to do is as important as deciding what to do."

## 问题

29 个 starter。6 个服务。28 个 RFC。

**太多了。**

一个新开发者 clone 这个项目，看到 29 个 starter 目录，第一反应不是「wow」，是「我该从哪里开始？」

iPod 成功是因为一个轮盘。不是因为它有 29 个按钮。

## 数据说话

4 个 starter 只有 1 个 Java 文件：
- `mate-dynamic-tp-starter` — 1 个文件，一个 `@EnableDynamicTp` 注解
- `mate-trace-starter` — 1 个文件，一个配置类
- `mate-seata-starter` — 1 个文件，一个配置类
- `mate-nacos-starter` — 1 个文件（这个可以接受，它本身就是 config-only）

这些 1 个文件的 starter 不值得独立存在。1 个 Java 文件 + 1 个 pom.xml + 1 个 auto-config imports 文件 = 3 个文件为了做 1 行配置。这是 overhead，不是 architecture。

## 决策：三级分类

### Tier 1: 核心盘（必须存在，否则脚手架不成立）

```
mate-common/
├── mate-base                    # 基础类型
└── mate-api                     # RPC 契约

mate-starters/
├── mate-ds-starter              # 数据库 (3 files)
├── mate-web-starter             # Web层 (3 files)
├── mate-cache-starter           # 缓存+锁+ID (4 files → 扩展到包含 lock+distribute)
├── mate-nacos-starter           # 注册配置 (1 file, OK)
├── mate-rpc-starter             # RPC (2 files)
├── mate-sa-token-starter        # 认证 (2 files)
└── mate-monitor-starter         # 监控 (2 files)

服务:
├── mate-gateway                 # 网关
├── mate-auth                    # 认证
└── mate-biz/mate-system         # 业务示例
```

**7 个 starter + 3 个服务。** 这是 MateCloud 的 iPod moment。

一个开发者引入 7 个 starter，就能跑起来一个完整的 DDD 微服务。数据库有了，缓存有了，RPC 有了，认证有了，监控有了。

**合并动作：**
- `mate-lock-starter` (5 files) → 并入 `mate-cache-starter`（都是 Redisson）
- `mate-distribute-starter` (3 files) → 并入 `mate-cache-starter`（SnowflakeId 依赖 Redis）

### Tier 2: 业务盘（做业务时按需引入）

```
mate-starters/
├── mate-mq-starter              # 消息队列 (9 files, 含延迟队列)
├── mate-job-starter             # 定时任务 (2 files)
├── mate-security-starter        # 安全全家桶 (15 files)
├── mate-file-starter            # 文件上传 (3 files)
├── mate-excel-starter           # 导入导出 (6 files)
└── mate-tenant-starter          # 多租户 (12 files)

服务:
├── mate-admin                   # 后台管理
└── mate-biz/mate-notice         # 通知服务
```

**6 个 starter + 2 个服务。** 做具体业务时才需要。不是每个项目都需要消息队列和多租户。

### Tier 3: 高级盘（特定场景才引入，不在默认目录里创建）

**砍掉或标记为 contrib/**

| 砍什么 | 为什么 | 处理方式 |
|--------|--------|---------|
| mate-dynamic-tp-starter (1 file) | Spring Boot 原生线程池配置够了 | **删除**，README 里写一行配置 |
| mate-trace-starter (1 file) | Spring Boot 3.x 内置 Micrometer Tracing | **合并**到 mate-monitor-starter |
| mate-seata-starter (1 file) | 95% 场景用最终一致性，不需要 Seata | **移到 contrib/** |
| mate-doc-starter (2 files) | Smart-Doc 是 Maven 插件，不需要 runtime starter | **删除**，在 root pom plugin 里配 |
| mate-devtools-starter (2 files) | 开发工具塞到 mate-web-starter 的 dev profile | **合并**到 mate-web-starter |
| mate-datascope-starter (7 files) | 和 security-starter 功能重叠 | **合并**到 mate-security-starter |
| mate-idempotent-starter (6 files) | 本质是请求安全策略 | **合并**到 mate-security-starter |
| mate-gray-starter (4 files) | 大多数团队用不到灰度发布 | **移到 contrib/** |
| mate-sharding-starter (3 files) | 单表 1000 万行之前不需要 | **移到 contrib/** |
| mate-sentinel-starter (3 files) | 内网初期不需要熔断 | **移到 contrib/** |
| mate-flow-starter (5 files) | 工作流是高级功能 | **移到 contrib/** |
| mate-rule-starter (2 files) | 规则引擎是高级功能 | **移到 contrib/** |
| mate-test-starter (5 files) | 测试基类放 test scope 即可 | **移到 contrib/** |
| mate-ai-starter (2 files) | AI 按需引入 | **保留但标记 optional** |

## 最终目录结构

```
matecloud/
├── mate-common/                   # 不动
│   ├── mate-base/
│   └── mate-api/
│
├── mate-starters/                 # 13 个 (从 29 砍到 13)
│   │
│   │  ── Tier 1: 核心 (7个，新项目必选) ──
│   ├── mate-ds-starter/           # 数据库
│   ├── mate-web-starter/          # Web + DevTools(dev)
│   ├── mate-cache-starter/        # 缓存 + 锁 + 雪花ID + 延迟队列
│   ├── mate-nacos-starter/        # 注册配置
│   ├── mate-rpc-starter/          # Dubbo RPC
│   ├── mate-sa-token-starter/     # 认证
│   ├── mate-monitor-starter/      # 监控 + Tracing
│   │
│   │  ── Tier 2: 业务 (6个，按需选) ──
│   ├── mate-mq-starter/           # 消息 + 延迟队列 + 领域事件
│   ├── mate-job-starter/          # 定时任务
│   ├── mate-security-starter/     # 安全全家桶 (含 datascope + idempotent)
│   ├── mate-file-starter/         # 文件存储
│   ├── mate-excel-starter/        # 导入导出
│   └── mate-tenant-starter/       # 多租户
│
├── mate-starters-contrib/          # 高级 (按需独立引入)
│   ├── mate-seata-starter/         # 分布式事务
│   ├── mate-sharding-starter/      # 分库分表
│   ├── mate-sentinel-starter/      # 熔断限流
│   ├── mate-gray-starter/          # 灰度发布
│   ├── mate-flow-starter/          # 工作流
│   ├── mate-rule-starter/          # 规则引擎
│   ├── mate-ai-starter/            # AI 能力
│   └── mate-test-starter/          # 测试工具
│
├── mate-gateway/
├── mate-auth/
├── mate-admin/
├── mate-cli/
└── mate-biz/
    ├── mate-system/
    └── mate-notice/
```

## 合并清单 (实际代码需要执行的)

### 合并 1: mate-lock-starter → mate-cache-starter

```
操作: 
1. 把 mate-lock-starter/src 下 5 个 Java 文件移到 mate-cache-starter/src
2. 合并 pom.xml 依赖
3. 更新 auto-config imports
4. 删除 mate-lock-starter 目录
5. 全局替换 <artifactId>mate-lock-starter</artifactId> 引用
```

### 合并 2: mate-distribute-starter → mate-cache-starter

```
操作:
1. 把 mate-distribute-starter/src 下 3 个文件移到 mate-cache-starter/src
2. SnowflakeUtil + WorkerIdHolder 成为 cache-starter 的一部分
3. 删除 mate-distribute-starter 目录
```

### 合并 3: mate-datascope-starter + mate-idempotent-starter → mate-security-starter

```
操作:
1. 移动 datascope (7 files) 到 security-starter
2. 移动 idempotent (6 files) 到 security-starter  
3. security-starter 最终: 15 + 7 + 6 = 28 files
4. 删除两个源目录
```

### 合并 4: mate-trace-starter → mate-monitor-starter

```
操作:
1. 移动 trace (1 file) 到 monitor-starter
2. 合并 pom.xml 的 micrometer-tracing 依赖
3. 删除 mate-trace-starter
```

### 合并 5: mate-devtools-starter → mate-web-starter

```
操作:
1. 移动 devtools (2 files) 到 web-starter, 加 @Profile("dev")
2. 删除 mate-devtools-starter
```

### 删除: mate-dynamic-tp-starter, mate-doc-starter

```
操作:
1. 删除两个目录
2. Smart-Doc 配置留在 root pom pluginManagement
3. 线程池配置写一段 application.yml 示例到文档
```

## 开发者体验变化

**Before (29 starters):**
```
开发者: "我想用 MateCloud 做个 CRM"
*打开 mate-starters 目录*
*看到 29 个文件夹*
"mate-datascope-starter 和 mate-security-starter 什么区别？"
"mate-lock-starter 和 mate-cache-starter 都用 Redisson，为啥分开？"
"mate-trace-starter 和 mate-monitor-starter 都是监控，到底用哪个？"
*关闭 IDE，去用 RuoYi 了*
```

**After (13 starters):**
```
开发者: "我想用 MateCloud 做个 CRM"
*打开 mate-starters 目录*
*看到 7 个核心 + 6 个业务*
"核心全选，业务按需。清晰。"
*10 分钟跑通 Hello World*
*开始写业务代码*
```

## 验证: 一句话说清每个 starter

如果说不清，就该砍。

| Starter | 一句话 |
|---------|--------|
| mate-ds-starter | 引入就有数据库 |
| mate-web-starter | 引入就有 REST API |
| mate-cache-starter | 引入就有缓存+锁+ID |
| mate-nacos-starter | 引入就注册到 Nacos |
| mate-rpc-starter | 引入就能 Dubbo 调用 |
| mate-sa-token-starter | 引入就有认证 |
| mate-monitor-starter | 引入就有监控+追踪 |
| mate-mq-starter | 引入就能发消息 |
| mate-job-starter | 引入就能跑定时任务 |
| mate-security-starter | 引入就有接口防护 |
| mate-file-starter | 引入就能传文件 |
| mate-excel-starter | 引入就能导入导出 |
| mate-tenant-starter | 引入就有多租户 |

每一个，一句话。13 个。不多不少。

contrib/ 里的，需要时去拿。不在你的默认视野里。
