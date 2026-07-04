# RFC 063 — 无感刷新（双令牌）与会话续期审计

状态: 已实现（待端到端验证）· 作者: mateaix · 日期: 2026-07-04

## 1. 背景与问题

登录成功后 access token 由 `sa-token.active-timeout`（默认 1800s / 30min）冻结：活跃用户靠**滑动续期**永不掉线，但**空闲 > 30min 返回**或**到 24h 绝对上限**时，下一个请求会 401，用户被迫重新登录。

目标：在有操作请求时，对已过期/冻结的 token 做**无感刷新**；并让运维能在「日志审计 → 登录日志」里查看**续期发生的时间与状态**。

## 2. 方案总览

分两部分：

- **A. 双令牌无感刷新**（本会话已实现）：登录返回 `accessToken`(短) + `refreshToken`(长, 默认 7 天)。前端命中 401 时用 refresh token 静默换新并重放原请求。
- **B. 续期审计可视**（本 RFC 重点）：续期成功作为一种 `loginType=REFRESH` 的认证事件，复用现有登录日志管道落库，并在登录日志页新增"登录类型"筛选与友好展示。

设计原则：**复用胜于新建**。续期是会话生命周期事件，归属登录日志，不建新表、不加新菜单、不加新页面。

## 3. Part A — 双令牌无感刷新（已实现，存档）

- `LoginResult` 增 `refreshToken`。
- 新出站端口 `RefreshTokenPort`：`issue / consume / revokeByUser`；实现 `RedisRefreshTokenAdapter`——256-bit 不透明 token、GETDEL 单次使用轮换、按用户反向索引（登出即吊销）。TTL 由 `mate.auth.refresh-token.timeout`（默认 604800s）控制。
- `SaTokenIssuer.issue()` 签发时带 refresh；`revokeCurrentSession()` 登出时吊销。
- `AuthAppService.refresh()`：consume 旧 token → `loadById` + `ensureActive` → 重新签发（角色/权限一并刷新）。
- `POST /api/v1/auth/refresh`，网关三处放行（AuthStrategyConfig / gateway yml / MonolithSecurityConfig）。
- 前端 `client.ts`：401 与 SECB001 走 **single-flight 静默刷新 + 重放**，`_retry` 防死循环。

## 4. Part B — 续期审计（本次实施）

### 4.1 数据模型

不改表。续期事件写入 `mate_login_log`：

| 列 | 续期事件取值 |
|---|---|
| `login_type` | `REFRESH`（枚举名，大写） |
| `status` | `0` = 续期成功 |
| `username` | 续期用户 |
| `client_ip` / `user_agent` | 由 `LoginLogPersistenceListener` 从当前请求捕获 |
| `created_at` | 续期时间 |

失败续期（refresh token 失效）暂不落库：此时身份未知，且随后必然产生一条正常登录行，审计链路已自洽。后续如需，可加 `status=1` 匿名行（开关式）。

### 4.2 后端改动

1. `LoginType` 枚举增 `REFRESH("refresh", "令牌续期")`。
2. `AuthAppService` 注入 `LoginAuditPort`，`refresh()` 成功后发
   `UserLoggedInEvent(userId, username, loginType=REFRESH, at=now)`。
   —— 现有 `EventLoginAudit → LoginLogPersistenceListener` 零改动即落库。
3. 登录日志查询增 `loginType` 过滤：
   - `ILoginLogQueryService.page(...)` 增参数 `loginType`；
   - `LoginLogQueryServiceImpl` `LambdaQueryWrapper.eq(login_type)`（非空时）；
   - `LoginLogController` 增 `@RequestParam(required=false) String loginType`。

### 4.3 前端改动（apps/admin + packages/core）

1. `LoginLogQuery` 增 `loginType?: string`。
2. `LoginLog.vue`：
   - 搜索栏加"登录类型"下拉（全部 / 账号密码 / 短信 / SSO / LDAP / 令牌续期），值为大写枚举名。
   - `loginType` 列用 formatter 映射为中文标签；`REFRESH` 用醒目 tag（如 info/蓝）与登录行区分。
   - 详情弹窗登录类型同样走标签映射。
3. i18n `zh-CN`（及 en 若有）：补登录类型标签与筛选占位符。

### 4.4 兼容与影响

- 老数据 `login_type` 为 4 种登录名，新增 `REFRESH` 不影响既有行。
- 权限沿用 `sys:log:list`，无需新菜单/权限种子。
- 续期在滑动窗口下低频（仅空闲返回触发），日志噪声可控、信号价值高。

## 5. 验收

- 登录 → 空闲至 access 冻结 → 发一个业务请求 → 前端自动刷新且原请求成功（无跳登录页）。
- 「日志审计 → 登录日志」出现一条 `令牌续期 / 成功` 记录，时间 = 续期时刻，IP/UA 正确。
- "登录类型"筛选选"令牌续期"只返回续期行。
- 登出后旧 refresh token 再用 → 401。

## 6. 任务清单（loop 执行）

1. `LoginType` 加 `REFRESH`。
2. `AuthAppService` 注入 `LoginAuditPort` + `refresh()` 发事件。
3. 登录日志查询三件套加 `loginType` 过滤（interface / impl / controller）。
4. 前端 `LoginLogQuery` + `LoginLog.vue` 筛选与展示。
5. i18n 标签。
6. 编译（mate-auth / mate-system / monolith）+ 前端类型检查。
