# 配置体系

MateCloud 采用 **5 层配置** 策略，服务的 `application.yml` 只需约 15 行。

## 优先级（从高到低）

```
1. 环境变量 / JVM -D 参数                         ← 最高优先级
2. Nacos: ${app-name}-${profile}.yml              ← 可选的服务级覆盖
3. Nacos: mate-infra-${profile}.yml               ← 共享基础设施配置
4. classpath: mate-defaults.yml                   ← 框架常量（打包在 mate-base.jar 中）
5. 服务 application.yml                           ← 入口文件（~15 行）
```

## 各层职责

| 层 | 内容 | 维护者 | 是否随环境变化 | 是否含密钥 |
|----|------|--------|---------------|-----------|
| `mate-defaults.yml` | 框架常量（Dubbo 协议、Jackson、Sa-Token、MyBatis Plus、Actuator、日志、Spring AI provider） | 框架作者 | 否 | 否（环境变量占位） |
| `mate-infra-${profile}.yml` | 环境相关端点（MySQL URL、Redis 地址、RabbitMQ 凭证、MinIO、Seata、XXL-Job、Druid 连接池） | 运维团队 | 是（dev/test/prod 各一份） | 推荐使用环境变量 |
| `${app-name}-${profile}.yml` | 服务特有配置 | 开发者 | 视情况 | 否 |

## 最小服务配置示例

一个服务的 `application.yml` 只需以下内容：

```yaml
spring:
  application:
    name: mate-system
  config:
    import:
      - classpath:mate-defaults.yml
      - optional:nacos:mate-infra-${spring.profiles.active:dev}.yml
      - optional:nacos:${spring.application.name}-${spring.profiles.active:dev}.yml

server:
  port: 9030

logging:
  level:
    vip.mate.system: debug
```

## 使用 CLI 管理配置

```bash
# 初始化共享配置到 Nacos
java -jar mate-cli.jar config init

# 使用 prod 模板
java -jar mate-cli.jar config init --profile prod

# 查看已发布的配置
java -jar mate-cli.jar config get mate-infra-dev.yml

# 推送自定义配置
java -jar mate-cli.jar config push mate-system-dev.yml --file ./my-config.yml
```

## Nacos 配置模板

模板文件位于 `docs/nacos-templates/`：

- `mate-infra-dev.yml` — 开发环境共享配置
- `mate-infra-prod.yml` — 生产环境配置
- `mate-ai-dev.yml` — AI 相关配置

详见 `docs/nacos-templates/README.md`。
