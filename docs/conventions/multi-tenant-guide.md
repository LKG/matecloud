# 多租户：启用与验证手册

> 适用范围：RFC-012(隔离内核) + RFC-028(SaaS 生命周期/配额) 已落地的 **COLUMN 行级隔离** 路径。
> 最近更新：2026-06-03。

多租户默认 **关闭**（`mate.tenant.enabled=false`），单租户部署零感知。本文说明如何开启并端到端验证隔离、开通、到期与配额。

---

## 1. 核心概念速查

| 概念 | 说明 |
|------|------|
| 隔离模式 | 仅 `COLUMN`（行级 `WHERE tenant_id=?`）已接线；`SCHEMA`/`DATASOURCE` 预留 |
| 白名单表 | 只有 `mate.tenant.include-tables` 列出的表会被加租户条件，默认 `mate_user / mate_admin / mate_role` |
| Fail-closed | 白名单表在 **无租户上下文** 时查询会抛错，不会"裸读"全表；合法跨租户读必须 `TenantHelper.withoutTenant(...)` |
| 超级租户 | `super-tenant-id` 命中的租户绕过行过滤与配额，作为平台超管 |
| Resolver | `header`（X-Tenant-Id）/ `domain`（子域名）/ `token`（Sa-Token 会话，登录后由 `SaTokenIssuer` 写入） |

---

## 2. 启用方式

### 2.1 本地演示（最快）
用随仓库附带的 demo profile + 种子数据（迁移 `V1.3.1` 已造租户 `acme`/`globex`）：

```bash
cd mate-biz/mate-system && mvn spring-boot:run -Dspring-boot.run.profiles=tenant
# 或： java -jar mate-system.jar --spring.profiles.active=tenant
```

`application-tenant.yml` 内容等价于：

```yaml
mate:
  tenant:
    enabled: true
    type: COLUMN
    resolver: token          # 登录态决定租户，无 header 伪造面
    super-tenant-id: "1"     # 系统租户=平台超管，可见全部租户数据
    grace-days: 7            # 到期后只读宽限期
    include-tables: [mate_user, mate_admin, mate_role]
```

### 2.2 生产启用
把上面的 `mate.tenant.*` 放进 Nacos 的 `mate-infra-<profile>.yml`（共享）或 `mate-system-<profile>.yml`（单服务）。注意：
- 网关前置场景用 `resolver: header` 或 `domain`，由 `TenantGatewayFilter` 解析后下发 `X-Tenant-Id`。
- **不要**在公网可改 header 的情况下随意设 `super-tenant-id`——那是越权旁路，必须在网关鉴权 + 角色校验之后。

---

## 3. 端到端验证

### 3.1 隔离（不同租户互不可见）
demo 数据内置三类账号（密码：管理员 `admin123`，终端用户 `demo123`）：

| 账号 | 租户 | 预期可见 |
|------|------|----------|
| `admin` | system(1) — 超级租户 | **全部** 租户的用户/角色 |
| `acme_admin` | 1001 | 仅 Acme 的用户(`acme_u*`)/角色 |
| `globex_admin` | 1002 | 仅 Globex 的用户(`globex_u*`)/角色 |

验证步骤：分别登录三个账号，打开「系统管理 → 用户管理」，确认 `acme_admin` 看不到 `globex_u*`，反之亦然；`admin` 两者都能看到。前端会自动带上 `X-Tenant-Id`（`packages/core` 的 client 拦截器从 `mate_tenant_id` 注入），但因 `resolver=token`，后端实际以会话里的租户为准。

### 3.2 开通闭环（新建租户即可登录）
平台超管在「租户管理」新建一个租户（如 code=`demo`），后端会自动：
1. 建租户级角色 `DEMO_ADMIN`；2. 建管理员 `demo_admin`（默认密码 `admin123`）；3. 授默认菜单；4. 发 `TenantProvisionedEvent`。
随后用 `demo_admin / admin123` 即可登录——无需手工补任何数据。
（对应 `TenantProvisionService`，与 `TenantProvisioningIntegrationTest` 覆盖一致。）

### 3.3 到期生命周期
- 把某租户 `expire_at` 改到过去（或用「续费」对话框设一个过去时间），等下一次整点 `TenantLifecycleJob` 扫描，状态会 `ACTIVE → EXPIRED`。
- 到期后在宽限期（默认 7 天）内仍可读；超出宽限期，`TenantStatusInterceptor` 会拦截该租户的业务请求（"租户已到期，请续费"）。
- 手动触发可直接调用 `TenantLifecycleService.expireOverdueTenants()`（见 `TenantLifecycleIntegrationTest`）。

### 3.4 配额（RFC-028 切片）
- **用户数上限**：套餐 `max_users` 满后，新建用户被 `TenantQuotaService.assertCanAddUser` 拒绝（"租户用户数已达套餐上限"）。`0=不限`。
- **功能开关**：`assertFeature(tenantId, "ai")` 按套餐 `features` 字段放行。
- **每日 AI 次数**：`recordAndCheckAiCall` 用 Redisson 计数键 `tenant:quota:ai:{tenantId}:{yyyy-MM-dd}`，按套餐 `ai_quota_daily` 限流（`0=不限`）。
  - ⚠️ 该方法已就绪但**尚未**接入 mate-ai 的调用链——接入点见 RFC-028 status 表。
- **冻结**：`status=SUSPENDED` 的租户被 `TenantStatusInterceptor` 直接拦截。

套餐配额列（`V1.3.2` 迁移）：`ai_enabled / ai_quota_daily / max_apps / price`，三档种子套餐已回填。

---

## 4. 给业务模块接入多租户的清单

1. `pom.xml` 加 `mate-tenant-starter` 依赖。
2. 给需要隔离的表加 `tenant_id VARCHAR(32) NOT NULL DEFAULT '1'` 列 + 索引（参考 `V1.1.3` 的 `_add_col_if_missing` 守卫式存储过程，H2/MySQL 双跑）。
3. 把这些表加入 `mate.tenant.include-tables`。
4. 对应 PO 加 `@TableField("tenant_id") private String tenantId;`，写入路径显式赋值或让拦截器在 INSERT 时注入。
5. 鉴权/身份类 RPC（登录前、跨租户）标 `@CrossTenantRpc`，或用 `TenantHelper.withoutTenant(...)`。
6. `@Async`/线程池任务的租户上下文已由 `TenantContextTaskDecorator` 自动透传，无需手动搬运。

---

## 5. 已知约束与后续

- **用户名/手机号/角色 key 仍是全局唯一**（非租户内唯一）。原因：按用户名登录在租户确立 *之前* 跨租户查找，全局唯一才不歧义。要支持"不同租户同名用户名"，需先按域名/header 解析租户再鉴权 + 复合唯一键——属后续工作。
- **配额强制目前是 mate-system 本地**（拦截器 + service）。跨服务（网关全局闸门、mate-ai 配额）需要新增 `IRpcTenantService` 暴露租户/套餐快照并加缓存——见 RFC-028 status 表的 todo 项。
- `SCHEMA`/`DATASOURCE` 物理隔离：helper 就绪，按租户开通数据源/库的运维编排未做。

---

## 6. 关键代码索引

| 能力 | 位置 |
|------|------|
| 隔离内核 | `mate-starters/mate-tenant-starter`（`MateTenantLineHandler` / 各 resolver / filter / `TenantHelper`） |
| 异步透传 | `mate-tenant-starter/.../async/TenantContextTaskDecorator.java` |
| 登录注入租户 | `mate-auth/.../token/SaTokenIssuer.java` |
| 租户域 / CRUD | `mate-biz/mate-system/.../tenant/`（aggregate / command / query / controller） |
| 开通 | `.../tenant/application/provision/TenantProvisionService.java` |
| 生命周期 | `.../tenant/application/lifecycle/TenantLifecycleService.java` + `.../trigger/job/TenantLifecycleJob.java` |
| 配额 | `.../tenant/application/quota/TenantQuotaService.java` + `.../trigger/interceptor/TenantStatusInterceptor.java` |
| 迁移 | `V1.1.3`(列) / `V1.1.6`·`V1.1.10`(菜单) / `V1.3.1`(demo) / `V1.3.2`(配额列) |
| 测试 | starter: `TenantContextTaskDecoratorTest` 等；mate-system: `TenantAggregateTest` / `TenantQuotaServiceTest` / `*IntegrationTest`(@Disabled，需 Docker) |
