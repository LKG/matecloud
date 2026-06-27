# mate-sa-token-starter

认证鉴权 Starter，基于 Sa-Token 框架，同时支持 Servlet 和 Reactor（WebFlux）模式。

## 提供的能力

- **登录认证** — 账号密码 + 短信验证码 + 验证码登录
- **权限校验** — 基于角色和权限码的访问控制
- **Token 管理** — JWT 模式，支持自定义过期时间
- **多端登录** — 支持 PC / Mobile / API 多端互踢或共存策略
- **网关集成** — Reactor 模式下的 WebFlux 过滤器

## 权限校验

```java
// 注解方式
@SaCheckPermission("user:list")
@GetMapping("/api/v1/users")
public Result<List<UserResponse>> list() { ... }

@SaCheckRole("admin")
@DeleteMapping("/api/v1/users/{id}")
public Result<Void> delete(@PathVariable Long id) { ... }

// 编程方式
StpUtil.checkPermission("user:edit");
StpUtil.checkRole("admin");
```

## 网关鉴权

`mate-gateway` 使用 Sa-Token 的 Reactor 模式，在网关层统一拦截认证：

- 白名单路径（登录、注册、验证码）直接放行
- 其他请求验证 Token 有效性
- Token 信息通过 Header 透传到下游服务
