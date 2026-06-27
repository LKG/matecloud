# mate-web-starter

Web Starter，封装 REST 支持、全局异常处理、Jackson 配置。

## 提供的能力

- **GlobalExceptionHandler** — 统一异常处理，所有异常转为 `Result<T>` 格式响应
- **Jackson 配置** — Long 序列化为 String（防止前端精度丢失）、日期格式化
- **CORS 配置** — 开发环境允许跨域
- **DevTools** — 热重载支持

## 统一响应格式

所有 API 返回 `Result<T>` 包装：

```json
{
  "success": true,
  "code": "200",
  "message": "操作成功",
  "data": { ... }
}
```

异常响应：

```json
{
  "success": false,
  "code": "USRB001",
  "message": "用户不存在",
  "data": null
}
```

## Controller 编写

Controller 放在 `trigger/controller/` 包下：

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserCommandService commandService;
    private final UserQueryService queryService;

    @GetMapping
    public Result<PageResult<UserResponse>> list(UserQuery query) {
        return Result.ok(queryService.listUsers(query));
    }

    @PostMapping
    public Result<Void> create(@RequestBody @Valid CreateUserCommand cmd) {
        commandService.createUser(cmd);
        return Result.ok();
    }
}
```
