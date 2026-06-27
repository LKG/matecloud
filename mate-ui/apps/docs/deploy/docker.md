# Docker Compose 部署

MateCloud 使用 Docker Compose 进行容器化部署。

::: warning 注意
本环境仅支持 `docker-compose`（v1，带连字符），**不要**使用 `docker compose`（v2，空格分隔）。
:::

## 基础设施

```bash
# 启动 MySQL + Redis + RabbitMQ + Nacos + MinIO
make infra-up

# 停止基础设施
make infra-down
```

## 全栈启动

```bash
# 启动所有服务（基础设施 + 业务服务）
make up

# 停止
make down
```

## 单独构建和启动服务

```bash
# 构建指定服务镜像
docker-compose build mate-system mate-ui

# 启动指定服务
docker-compose up -d mate-system mate-ui

# 查看日志
docker-compose logs -f mate-system
```

## 升级流程

```bash
git pull
docker-compose build <service>
docker-compose up -d <service>
```

::: danger 注意
**永远不要**执行 `docker-compose down -v`，这会清除数据卷（数据库数据）。
:::

## Flyway 自动迁移

服务启动时，Flyway 自动执行 `src/main/resources/db/migration/` 下的新迁移脚本，无需手动操作。

## 环境变量

通过 `.env` 文件配置环境变量（从 `.env.example` 模板创建）：

```bash
cp .env.example .env
# 编辑 .env，填入实际的密码和密钥
```

::: warning
`.env` 文件包含敏感信息，已在 `.gitignore` 中排除，**不要**提交到版本库。
:::
