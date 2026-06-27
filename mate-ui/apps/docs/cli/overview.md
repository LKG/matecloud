# CLI 工具

`mate-cli` 是 MateCloud 的命令行工具，基于 Picocli 构建，提供全栈管控能力。

## 安装

构建后即可使用：

```bash
mvn clean install -DskipTests
java -jar mate-cli/target/mate-cli.jar --help
```

## 命令一览

```
Usage: mate [-hV] [--mcp] [COMMAND]

Commands:
  new      创建新模块或聚合脚手架
  up       启动 MateCloud 集群
  down     停止 MateCloud 集群
  status   集群状态总览
  logs     查看服务日志
  service  列出和检查 Nacos 注册的服务
  config   管理 Nacos 配置中心
  ai       AI 工具检查和对话
  rpc      Dubbo RPC 调试
  db       数据库操作
  cache    缓存操作
  gen      代码生成
```

## 常用操作

### 集群管理

```bash
mate up                     # 启动集群（Docker Compose）
mate up --mvn               # 通过 Maven 直接启动
mate down                   # 停止集群
mate status                 # 查看集群状态
mate logs mate-system       # 查看服务日志
```

### 服务发现

```bash
mate service list           # 列出 Nacos 注册的服务
mate service info mate-system    # 查看服务实例详情
mate service health         # 所有实例健康检查
```

### 配置管理

```bash
mate config init            # 初始化 Nacos 共享配置
mate config init --profile prod  # 使用 prod 模板
mate config get mate-infra-dev.yml   # 查看配置
mate config push xxx.yml --file ./xxx.yml  # 推送配置
```

详见各子命令文档：[脚手架](/cli/scaffold) · [服务管理](/cli/service) · [配置](/cli/config) · [AI](/cli/ai) · [MCP](/cli/mcp)
