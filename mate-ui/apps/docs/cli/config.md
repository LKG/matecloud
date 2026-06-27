# Nacos 配置管理

## 初始化共享配置

```bash
# 发布 dev 模板到 Nacos
java -jar mate-cli.jar config init

# 使用 prod 模板
java -jar mate-cli.jar config init --profile prod

# 覆盖已有配置
java -jar mate-cli.jar config init --overwrite
```

## 查看配置

```bash
java -jar mate-cli.jar config get mate-infra-dev.yml
```

## 推送配置

```bash
java -jar mate-cli.jar config push mate-system-dev.yml --file ./my-config.yml
```

## 删除配置

```bash
java -jar mate-cli.jar config delete mate-infra-dev.yml
```

## 配置模板

模板文件位于 `docs/nacos-templates/`：

| 模板 | 说明 |
|------|------|
| `mate-infra-dev.yml` | 开发环境共享基础设施配置 |
| `mate-infra-prod.yml` | 生产环境配置 |
| `mate-ai-dev.yml` | AI 相关配置 |
