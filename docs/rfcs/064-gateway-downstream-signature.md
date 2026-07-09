# RFC 064 — 网关↔下游请求签名（修复身份头伪造高危漏洞）

状态: 实施中 · 作者: mateaix · 日期: 2026-07-05

## 1. 漏洞（高危）

网关鉴权通过后，`HeaderRelayFilter` 把身份上下文以明文头透传给下游：`X-User-Id / X-User-Name / X-Tenant-Id / X-Dept-Id / X-Workspace-Id / X-Roles / X-Data-Scope`。下游服务据此判定身份与权限。

- 网关入口 `SecurityHeaderFilter`（order -200）会**剥离**外部携带的这些头再重注，防止「经过网关」的请求伪造身份 —— 这部分是对的。
- 但下游拦截器 `NoticeAuthInterceptor` / `AiAuthInterceptor` **只校验 `X-User-Id` 是否存在**，不校验来源。`AiAuthInterceptor` 的 Javadoc 甚至断言「确保必须经过网关」——**该断言不成立**。
- **攻击面**：任何能直连下游服务端口（9050 / AI 端口 / …）的人，只要发 `X-User-Id: 1`（超管），即可冒充任意用户执行操作。唯一防线是网络隔离，**无应用层兜底**。

## 2. 方案：HMAC 请求签名

网关持有共享密钥，对整套身份头 + 时间戳做 **HMAC-SHA256** 签名并注入；下游用同一密钥重算校验。攻击者没有密钥，无法为任何伪造的身份头组合生成有效签名。

**为什么签「整套身份头」而非仅 userId**：若只签 userId，攻击者可复用自己合法会话的签名、篡改 `X-Roles`/`X-Tenant-Id` 提权。签名覆盖 `AuthHeaders.ALL` 全部值后，任一头被篡改签名即失效。

**为什么带时间戳**：下游校验 `ts` 在 ±N 分钟窗口内，限制签名泄露后的重放窗口（攻击者重放自己会话的签名无提权意义，篡改则签名失效，故不引入 nonce/缓存，避免过度设计）。

### 2.1 签名算法（canonical）
```
payload = join("\n", [值(USER_ID), 值(USER_NAME), 值(TENANT_ID), 值(DEPT_ID),
                      值(WORKSPACE_ID), 值(ROLES), 值(DATA_SCOPE)])  // 缺失→""
                + "\n" + ts
sign = Base64Url( HMAC_SHA256(secret, payload) )
```
顺序严格按 `AuthHeaders.ALL`。网关与下游共用同一份 `AuthHeaders.ALL` 与同一份签名工具，保证一致。校验用**常量时间比较**防时序攻击。

## 3. 改动清单

### mate-base（纯 JDK，网关 reactive 与下游 servlet 共用）
1. `AuthHeaders` 新增 `GATEWAY_SIGN = "X-Gateway-Sign"`、`GATEWAY_TS = "X-Gateway-Ts"`（**不进 `ALL`**，避免 canonicalize 自引用；单独纳入入口剥离）。
2. 新增 `vip.mate.base.security.GatewaySignature`：`canonicalize(headerLookup, ts)` / `sign(secret, headerLookup, ts)` / `verify(secret, headerLookup, ts, sign, skewMs)`。零框架依赖，HmacSHA256 + Base64Url + `MessageDigest.isEqual` 常量时间比较。

### mate-gateway
3. `HeaderRelayFilter`：注入身份头后，用 `secret + now` 计算并注入 `X-Gateway-Ts` + `X-Gateway-Sign`（仅当 loginId 有效）。secret 由配置注入。
4. `SecurityHeaderFilter`：入口额外剥离 `GATEWAY_SIGN` / `GATEWAY_TS`（防外部伪造签名头）。
5. secret 缺失（空）→ 启动 fail-fast；仍为 dev 默认值 → WARN。

### 下游（mate-notice + mate-ai-starter）
6. `NoticeAuthInterceptor` / `AiAuthInterceptor` 改为**验签**：`signature-required=true`（默认）时，缺签名头或验签失败 → 401；`required=false`（本地直连调试）时回退为「仅判 X-User-Id 存在」+ WARN。修正 `AiAuthInterceptor` 误导性 Javadoc。
7. 注册处 `NoticeWebConfig` / `AiAutoConfiguration` 把 secret / skew / required 传入拦截器构造。
8. 拦截器壳保留在各自模块，仅复用 `GatewaySignature` 算法（`HandlerInterceptor` 依赖 spring-web，不能下沉 mate-base）。未来可统一上移 mate-web-starter。

### 配置（mate-defaults.yml）
9.
```yaml
mate:
  gateway:
    internal:
      secret: ${MATE_GATEWAY_INTERNAL_SECRET:dev-only-gateway-internal-secret-change-in-prod-min-32}
      signature-required: ${MATE_GATEWAY_SIGN_REQUIRED:true}
      timestamp-skew-ms: 300000   # 5 分钟重放窗口
```

## 4. 兼容与灰度

- 网关**先于**下游升级：下游旧版不验签，兼容。
- 下游**先于**网关升级：下游 `signature-required=true` 会拒绝无签名请求 → 升级期需先升网关，或临时 `MATE_GATEWAY_SIGN_REQUIRED=false` 灰度。
- dev（网关+下游同起）无影响；本地直连下游调试可设 `signature-required=false`。

## 5. 范围外（记录，后续批次）
- mate-system 走 `@SaCheckLogin`（sa-token 二次校验），不在本漏洞面；其对 `X-Tenant-Id` 的数据权限信任可后续纳入同一签名校验。
- 「prod profile + dev 默认密钥即 fail-fast」归批次 B 密钥项，本次仅对空 secret fail-fast + dev 默认 WARN。

## 6. 验收
- 经网关正常请求 notice/ai：带合法签名 → 200。
- 直连下游端口发 `X-User-Id:1`（无签名）：→ 401。
- 带过期 `ts`（超窗口）或篡改任一身份头：→ 401。
- `signature-required=false` 时直连仅判 X-User-Id 存在（回退）。
- mate-gateway / mate-notice / mate-ai(monolith) 编译通过。

## 7. 任务（loop）
1. mate-base：`AuthHeaders` 加签名头常量 + `GatewaySignature` 工具。
2. 网关 `HeaderRelayFilter` 注入签名 + `SecurityHeaderFilter` 剥离 + secret 配置/校验。
3. 下游 `NoticeAuthInterceptor` / `AiAuthInterceptor` 验签 + 注册处传参。
4. `mate-defaults.yml` 配置。
5. 编译验证（gateway/notice/ai/monolith）。
