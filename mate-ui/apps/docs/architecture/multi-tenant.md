# 多租户

MateCloud 通过 `mate-tenant-starter` 提供开箱即用的多租户支持，支持三种隔离模式。

## 隔离模式

| 模式 | 说明 | 适用场景 |
|------|------|----------|
| 行级隔离 | 所有租户共享数据库和表，通过 `tenant_id` 列过滤 | 中小租户、成本敏感 |
| Schema 隔离 | 共享数据库实例，每个租户独立 Schema | 需要一定隔离但不想独立实例 |
| 独立数据源 | 每个租户独立数据库连接 | 大客户、强隔离要求 |

## 行级隔离

基于 MyBatis Plus 的 `TenantLineInnerInterceptor`，自动在 SQL 中追加 `WHERE tenant_id = ?`。

### 启用

引入 `mate-tenant-starter` 依赖即可。配置项：

```yaml
mate:
  tenant:
    enabled: true
    column: tenant_id
    ignore-tables:
      - mate_dict_type
      - mate_config
```

### 安全策略

- **Fail-closed**：缺失或无效的租户上下文时，拒绝请求
- **白名单表**：通过 `ignore-tables` 配置不需要租户隔离的表
- **租户 ID 校验**：在信任边界（Web/Gateway/RPC）验证租户 ID，防止 SQL 注入

## 租户上下文

租户 ID 从请求中提取（Header / Token），通过 `TenantContext` 在线程间传递：

```java
// 获取当前租户
Long tenantId = TenantContext.getTenantId();

// 忽略租户隔离执行（超级管理员场景）
TenantContext.executeWithoutTenant(() -> {
    // 此代码块内不追加 tenant_id 条件
    return userMapper.selectList(null);
});
```

## 租户套餐

MateCloud 支持租户套餐管理，通过套餐控制每个租户可使用的菜单和功能模块。
