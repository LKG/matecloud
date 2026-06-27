# MCP 协议

[Model Context Protocol (MCP)](https://modelcontextprotocol.io/) 是 Anthropic 提出的开放协议，让 AI 助手能够与外部工具和数据源交互。

MateCloud 通过两种方式支持 MCP：

## 方式一：CLI 聚合模式

`mate-cli --mcp` 作为 MCP stdio 服务器，聚合集群中所有服务的 `@Tool` 方法。

```bash
java -jar mate-cli/target/mate-cli.jar --mcp
```

**优点**：一个入口访问所有服务的工具，无需单独配置每个服务。

**配置 Claude Code**：

```json
{
  "mcpServers": {
    "matecloud": {
      "command": "java",
      "args": ["-jar", "./mate-cli/target/mate-cli.jar", "--mcp"],
      "env": {
        "NACOS_SERVER_ADDR": "127.0.0.1:8848",
        "NACOS_NAMESPACE": "dev"
      }
    }
  }
}
```

## 方式二：服务直连模式

在服务中启用 MCP 传输，Claude 直接连接该服务：

```yaml
spring:
  ai:
    mcp:
      server:
        enabled: true
        name: ${spring.application.name}
```

服务会在 `POST /mcp` 暴露 SSE 传输端点。

**优点**：无需 CLI 中转，延迟更低。
**缺点**：每个服务需要单独配置。

## 使用场景

配置完成后，在 Claude Code 中可以直接用自然语言操作集群：

- *"列出所有用户状态字典"*
- *"查询今天新增了多少用户"*
- *"mate-system 的健康状态如何"*

Claude 会自动匹配并调用对应的 `@Tool` 方法。
