# mate-security-starter

安全增强 Starter，提供一组声明式安全注解。

## 注解一览

| 注解 | 用途 | 说明 |
|------|------|------|
| `@ApiSign` | 接口签名验证 | 防篡改，支持时间戳 + nonce + HMAC |
| `@RateLimit` | 接口限流 | 基于 Redisson 令牌桶，支持 IP/用户/路径维度 |
| `@AuditLog` | 审计日志 | 自动记录操作人、请求参数、响应结果、耗时 |
| `@DataPermission` | 数据权限 | 按角色数据范围过滤 SQL 查询结果 |
| `@Idempotent` | 幂等控制 | 基于 Token 机制防止重复提交 |

## @RateLimit 限流

```java
@RateLimit(key = "user:create", rate = 10, interval = 60)
@PostMapping("/api/v1/users")
public Result<Void> create(@RequestBody CreateUserCommand cmd) { ... }
```

每 60 秒最多允许 10 次请求。超限返回 429 状态码。

## @DataPermission 数据权限

支持的数据范围：

| 范围 | 说明 |
|------|------|
| ALL | 全部数据 |
| DEPT | 本部门数据 |
| DEPT_AND_CHILD | 本部门及下级数据 |
| SELF | 仅本人数据 |
| CUSTOM | 自定义部门列表 |

```java
@DataPermission
@GetMapping("/api/v1/users")
public Result<PageResult<UserResponse>> list(UserQuery query) {
    // SQL 自动追加数据范围过滤条件
}
```

## @Idempotent 幂等

```java
@Idempotent(expireTime = 10)
@PostMapping("/api/v1/orders")
public Result<Void> createOrder(@RequestBody CreateOrderCommand cmd) {
    // 10 秒内相同请求自动拦截
}
```
