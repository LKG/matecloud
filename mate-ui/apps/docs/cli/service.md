# 服务管理

## 列出服务

```bash
java -jar mate-cli.jar service list
```

查询 Nacos 注册的所有服务，显示名称、实例数、健康状态。

## 服务详情

```bash
java -jar mate-cli.jar service info mate-system
```

显示服务的所有实例信息：IP、端口、版本、元数据。

## 健康检查

```bash
java -jar mate-cli.jar service health
```

对每个实例调用 `/actuator/health`，汇总显示健康状态。
