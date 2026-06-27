# RFC-010: CLI Support & MateClaw Integration

- **Status**: Draft
- **Created**: 2026-04-12
- **Author**: MateCloud Team
- **Wave**: 5 (after admin backend)
- **Dependencies**: RFC-008 (mate-system), RFC-009 (mate-admin)

## 背景

MateCloud 需要一个命令行工具 (CLI) 来加速开发流程，并与 MateClaw AI 助手深度集成。CLI 既是开发者的效率工具，也是 MateClaw Agent 的 Tool，实现「AI 驱动的微服务开发」。

## 设计方案

### Part 1: mate-cli (命令行工具)

基于 Spring Shell 或 Picocli 构建独立 CLI JAR。

#### 核心命令

##### 1. 项目脚手架命令

```bash
# 创建新业务模块 (自动生成 DDD 四层骨架)
mate new module <name> [--port 9050]
# 例: mate new module mate-order --port 9050
# 自动生成:
#   mate-biz/mate-order/pom.xml
#   mate-biz/mate-order/src/.../MateOrderApplication.java
#   mate-biz/mate-order/src/.../trigger/controller/
#   mate-biz/mate-order/src/.../application/command/
#   mate-biz/mate-order/src/.../domain/model/aggregate/
#   mate-biz/mate-order/src/.../infrastructure/dao/
#   mate-biz/mate-order/src/.../types/exception/
#   mate-biz/mate-order/src/main/resources/bootstrap.yml
#   更新 root pom.xml 和 mate-biz/pom.xml 的 modules

# 在当前模块创建新的子域聚合
mate new aggregate <name> [--module mate-system]
# 例: mate new aggregate Order --module mate-order
# 自动生成:
#   domain/model/aggregate/OrderAggregate.java
#   domain/model/entity/Order.java
#   domain/adapter/repository/OrderRepository.java
#   infrastructure/adapter/repository/OrderRepositoryImpl.java
#   infrastructure/dao/OrderDao.java
#   infrastructure/dao/po/OrderPO.java
#   application/command/OrderCommandService.java
#   application/query/IOrderQueryService.java
#   trigger/controller/OrderController.java
#   types/exception/OrderErrorCode.java
#   types/exception/OrderException.java

# 根据数据库表反向生成 DDD 代码
mate gen code --table mate_order --module mate-order
```

##### 2. 服务管理命令

```bash
# 查看所有已注册服务
mate service list

# 查看服务详情 (从 Nacos 拉取)
mate service info mate-system

# 启动本地服务
mate service start mate-system

# 查看服务健康状态
mate service health

# 查看服务日志 (tail -f)
mate service logs mate-system
```

##### 3. Dubbo RPC 调试命令

```bash
# 列出所有 RPC 接口
mate rpc list

# 调用 RPC 接口 (类似 dubbo telnet)
mate rpc invoke IRpcUserService.getById --args '{"userId":"123"}'

# 查看 RPC 接口元数据
mate rpc describe IRpcUserService
```

##### 4. 数据库操作命令

```bash
# 执行 SQL
mate db query "SELECT * FROM mate_user LIMIT 10" --service mate-system

# 查看表结构
mate db describe mate_user

# 数据库迁移
mate db migrate --module mate-system

# 生成建表 SQL (从 PO 类)
mate db gen-ddl --class vip.mate.system.infrastructure.dao.po.UserPO
```

##### 5. 缓存操作命令

```bash
# 查看缓存 key
mate cache keys "user:*"

# 获取缓存值
mate cache get "user:123"

# 清除缓存
mate cache evict "user:*"
```

##### 6. 配置管理命令

```bash
# 查看 Nacos 配置
mate config list

# 获取配置内容
mate config get mate-system-dev.yml

# 推送配置
mate config push mate-system-dev.yml --file ./config.yml
```

---

### Part 2: MateClaw Integration

#### 2.1 MateCloud 作为 MateClaw 的 MCP Tool Server

将 MateCloud CLI 能力暴露为 MCP (Model Context Protocol) Tools，让 MateClaw 的 AI Agent 直接操作 MateCloud。

```yaml
# MateClaw MCP Server 配置
mateclaw:
  mcp:
    servers:
      matecloud:
        transport: stdio
        command: java
        args: ["-jar", "mate-cli.jar", "--mcp"]
        tools:
          - name: matecloud_create_module
            description: "Create a new DDD business module in MateCloud"
          - name: matecloud_query_db
            description: "Query MateCloud database"
          - name: matecloud_rpc_invoke
            description: "Invoke a Dubbo RPC service"
          - name: matecloud_service_status
            description: "Check service health status"
          - name: matecloud_gen_code
            description: "Generate DDD code from database table"
          - name: matecloud_cache_ops
            description: "Operate on Redis cache"
```

#### 2.2 MateClaw Agent 调用 MateCloud API

MateClaw 内置 MateCloud API 工具:

```java
// MateClaw 侧: MateCloud API Tool
@Tool("Query MateCloud users")
public String queryUsers(@Param("query") String query) {
    // 调用 MateCloud REST API: GET /api/v1/users?q={query}
    return httpClient.get(mateCloudBaseUrl + "/api/v1/users?q=" + query);
}

@Tool("Create user in MateCloud")
public String createUser(@Param("mobile") String mobile, @Param("nickName") String nickName) {
    // 调用 MateCloud REST API: POST /api/v1/users
    return httpClient.post(mateCloudBaseUrl + "/api/v1/users", Map.of(
        "mobile", mobile, "nickName", nickName));
}
```

#### 2.3 MateCloud 调用 MateClaw AI 能力

MateCloud 服务可通过 MateClaw API 获取 AI 能力:

```java
// MateCloud 侧: 调用 MateClaw AI
@Service
public class AiAssistService {

    @Value("${mateclaw.api.url:http://localhost:18088}")
    private String mateClawUrl;

    // AI 辅助代码审查
    public String reviewCode(String code) {
        return restTemplate.postForObject(
            mateClawUrl + "/api/v1/chat/{agentId}/message",
            Map.of("content", "Review this code:\n" + code),
            String.class, codeReviewAgentId);
    }

    // AI 辅助 SQL 生成
    public String generateSql(String naturalLanguage) {
        return restTemplate.postForObject(
            mateClawUrl + "/api/v1/chat/{agentId}/message",
            Map.of("content", "Generate SQL for: " + naturalLanguage),
            String.class, sqlAgentId);
    }
}
```

---

### Part 3: Claude Code 开发流工具

#### 3.1 mate-cli 作为 Claude Code 的 MCP Server

在 Claude Code 的 `.claude/settings.json` 中配置:

```json
{
  "mcpServers": {
    "matecloud": {
      "command": "java",
      "args": ["-jar", "D:/codes/matecloud/mate-cli/target/mate-cli.jar", "--mcp"],
      "env": {
        "NACOS_HOST": "localhost",
        "NACOS_PORT": "8848"
      }
    }
  }
}
```

这样 Claude Code 在开发 MateCloud 时可以:
- 直接查询数据库验证数据
- 调用 RPC 接口测试
- 创建新模块和聚合
- 查看服务状态
- 管理缓存和配置

#### 3.2 Claude Code Hook 集成

```json
// .claude/settings.json hooks
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit",
        "command": "mvn compile -pl $(echo $CLAUDE_FILE_PATH | grep -oP 'mate-\\w+') -q 2>&1 | tail -3"
      }
    ]
  }
}
```

---

## 技术实现

### mate-cli 模块

```
mate-cli/
├── pom.xml
└── src/main/java/vip/mate/cli/
    ├── MateCliApplication.java          # Picocli main
    ├── command/
    │   ├── NewCommand.java              # mate new module/aggregate
    │   ├── ServiceCommand.java          # mate service list/start/health
    │   ├── RpcCommand.java              # mate rpc list/invoke
    │   ├── DbCommand.java              # mate db query/describe
    │   ├── CacheCommand.java            # mate cache keys/get/evict
    │   ├── ConfigCommand.java           # mate config list/get/push
    │   └── GenCommand.java              # mate gen code
    ├── mcp/
    │   ├── McpServerMode.java           # MCP stdio server adapter
    │   └── McpToolRegistry.java         # Register CLI commands as MCP tools
    ├── template/
    │   ├── ModuleTemplate.java          # DDD module scaffolding templates
    │   └── AggregateTemplate.java       # Aggregate scaffolding templates
    └── config/
        └── CliConfig.java               # Nacos/DB connection config
```

### 依赖

```xml
<dependencies>
    <dependency>
        <groupId>info.picocli</groupId>
        <artifactId>picocli-spring-boot-starter</artifactId>
        <version>4.7.6</version>
    </dependency>
    <dependency>
        <groupId>vip.mate</groupId>
        <artifactId>mate-base</artifactId>
    </dependency>
    <!-- Nacos client for service discovery -->
    <!-- Dubbo generic invoke for RPC testing -->
    <!-- JDBC for DB operations -->
    <!-- Redisson for cache operations -->
</dependencies>
```

---

## 开发计划 (可拆分为子任务)

| 子任务 | 内容 | 独立开发 |
|--------|------|---------|
| CLI-01 | Picocli 基础框架 + `mate new module` | Yes |
| CLI-02 | `mate new aggregate` + DDD 模板引擎 | Yes |
| CLI-03 | `mate service` 命令 (Nacos API) | Yes |
| CLI-04 | `mate rpc` 命令 (Dubbo Generic) | Yes |
| CLI-05 | `mate db` 命令 (JDBC) | Yes |
| CLI-06 | `mate cache` 命令 (Redisson) | Yes |
| CLI-07 | `mate config` 命令 (Nacos Config API) | Yes |
| CLI-08 | MCP Server 模式 (`--mcp` flag) | Depends on CLI-01~07 |
| CLI-09 | `mate gen code` 反向工程 | Yes |
| INT-01 | MateClaw MCP Tool 注册 | Depends on CLI-08 |
| INT-02 | Claude Code MCP 配置 | Depends on CLI-08 |

## 验证方案

1. `mate new module mate-order --port 9050` -> 生成完整 DDD 模块骨架
2. `mate new aggregate Order --module mate-order` -> 生成聚合根全套代码
3. `mate service list` -> 显示 Nacos 注册的所有服务
4. `mate rpc invoke IRpcUserService.getById --args '{"userId":"1"}'` -> 返回用户信息
5. `mate db query "SELECT COUNT(*) FROM mate_user"` -> 返回用户数量
6. 在 Claude Code 中通过 MCP 调用 `matecloud_create_module` -> 自动创建模块
7. MateClaw Agent 执行 "查询所有活跃用户" -> 通过 MCP 调用 MateCloud API 返回结果
