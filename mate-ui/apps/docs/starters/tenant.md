# mate-tenant-starter

多租户 Starter，详见 [多租户架构](/architecture/multi-tenant)。

## 快速启用

1. 引入依赖：

```xml
<dependency>
    <groupId>vip.mate</groupId>
    <artifactId>mate-tenant-starter</artifactId>
</dependency>
```

2. 配置：

```yaml
mate:
  tenant:
    enabled: true
    column: tenant_id
    ignore-tables:
      - mate_dict_type
      - mate_config
```

3. 数据库表添加 `tenant_id` 列。

即可自动在所有 SQL 中追加租户过滤条件。

## 三种隔离模式

详见 [多租户架构文档](/architecture/multi-tenant)。
