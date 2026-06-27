# MCP Server

`mate-cli --mcp` 将整个 MateCloud 集群暴露为 [Model Context Protocol](https://modelcontextprotocol.io/) 服务器，供 Claude Code / Claude Desktop 调用。

## 启动

```bash
java -jar mate-cli/target/mate-cli.jar --mcp
```

## 配置 Claude Code

在 `.claude/settings.json` 中添加：

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

## 工作原理

1. CLI 通过 Nacos 发现所有 AI-enabled 服务
2. 聚合各服务的 `@Tool` 方法为 MCP 工具列表
3. Claude Code 发起 `tools/call` 时，CLI 将请求路由到对应服务的 HTTP 端点
4. 结果返回给 Claude Code

## 直连模式

也可以让 Claude Code 直接连接服务（不通过 CLI）：

在服务中启用 MCP 传输：

```yaml
spring:
  ai:
    mcp:
      server:
        enabled: true
        name: ${spring.application.name}
```

服务将在 `POST /mcp` 暴露 MCP SSE 传输。
