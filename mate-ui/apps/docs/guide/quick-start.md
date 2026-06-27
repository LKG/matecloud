# 快速开始

## 环境要求

| 工具 | 版本 | 说明 |
|------|------|------|
| JDK | 21+ | 推荐 GraalVM CE 或 Temurin |
| Maven | 3.9+ | 构建工具 |
| Docker | 20+ | 基础设施容器化 |
| Node.js | 20+ | 前端开发（可选） |
| pnpm | 9+ | 前端包管理（可选） |

## 1. 克隆仓库

```bash
git clone https://github.com/matevip/matecloud.git
cd matecloud
```

## 2. 启动基础设施

```bash
# MySQL + Redis + RabbitMQ + Nacos + MinIO
make infra-up
```

或者手动启动上述服务，通过环境变量指向它们的地址。

## 3. 构建后端

```bash
mvn clean install -DskipTests
```

全部 42 个模块构建成功后，每个服务目录下都有可运行的 fat jar。

## 4. 初始化 Nacos 配置

```bash
java -jar mate-cli/target/mate-cli.jar config init
```

此命令将 `mate-infra-dev.yml` 发布到 Nacos（namespace=dev），整个集群的数据库、Redis、MQ、MinIO 连接信息统一从此文件读取。

## 5. 启动服务

```bash
# 方式一：Docker Compose（推荐）
make up

# 方式二：Maven 直接启动（无需 Docker）
java -jar mate-cli/target/mate-cli.jar up --mvn
```

::: tip 数据库自动初始化
无需手动建表。`mate-system` 首次启动时 Flyway 自动执行 `db/migration/` 下的迁移：
`V1.0.0` 建表 → `V1.0.1` 写入内置账号 `admin/admin123`、角色、菜单、字典 → `V1.0.2` 迁移示例。
每个服务有独立的版本历史表 `flyway_history_<module>`。详见 [数据库迁移](/deploy/database) 与 [菜单配置](/guide/menu-config)。
:::

## 6. 验证集群

```bash
java -jar mate-cli/target/mate-cli.jar status
```

输出示例：

```
MateCloud cluster status
Nacos: http://127.0.0.1:8848  namespace=dev

SERVICE            ENDPOINT               HEALTH   INFO
────────────────────────────────────────────────────────
mate-gateway       127.0.0.1:9010         UP       up-since=...
mate-auth          127.0.0.1:9020         UP       up-since=...
mate-system        127.0.0.1:9030         UP       up-since=...
mate-notice        127.0.0.1:9050         UP       up-since=...
```

## 7. 启动前端

```bash
cd mate-ui
pnpm install
pnpm dev          # → http://localhost:3000
```

默认开发账号：`admin` / `admin123`

## 8. 停止服务

```bash
make down
```

## 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| mate-gateway | 9010 | API 网关（WebFlux + Sa-Token） |
| mate-auth | 9020 | 认证服务（Sa-Token + 短信登录 + 验证码） |
| mate-system | 9030 | 系统管理（DDD 示范 + RBAC + 字典 + AI） |
| mate-notice | 9050 | 通知服务（短信/邮件适配器） |
| mate-ui (dev) | 3000 | Vue 3 前端（Vite 开发服务器） |
