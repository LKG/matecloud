# RFC-052: 残余项收口 — 2026-04-25

> Status: **Done**
> Date: 2026-04-25
> Predecessor: [RFC-051](051-completion-pass-2026-04-25.md)（P0/P1/P2 主体收口）

## 背景

RFC-051 落地 12 项主体缺口后，[development-roadmap.md](../conventions/development-roadmap.md) 第二节列出 5 项残余（R1–R5）。本 RFC 记录这一回合内全部清理完成。

---

## R1: TenantContext ↔ Sa-Token 登录链路贯通

**改动**：

| 层 | 文件 | 改动 |
|----|------|------|
| API DTO | [UserInfoResponse](../../mate-common/mate-api/src/main/java/vip/mate/api/system/response/UserInfoResponse.java) | 增加 `tenantId` 字段 |
| Domain | [User](../../mate-biz/mate-system/src/main/java/vip/mate/system/domain/model/entity/User.java) | 增加 `tenantId` 字段 |
| Persistence | [UserPO](../../mate-biz/mate-system/src/main/java/vip/mate/system/infrastructure/dao/po/UserPO.java) | 增加 `@TableField("tenant_id") tenantId` 列映射（V1.1.3 已建列） |
| Convertor | [UserConvertor](../../mate-biz/mate-system/src/main/java/vip/mate/system/application/convertor/UserConvertor.java) | `@Mapping(source="user.tenantId", target="tenantId")` |
| Auth aggregate | [AuthUser](../../mate-auth/src/main/java/vip/mate/auth/domain/model/aggregate/AuthUser.java) | 增加 `tenantId` 字段；`from()` 透传 |
| Token | [SaTokenIssuer](../../mate-auth/src/main/java/vip/mate/auth/infrastructure/adapter/token/SaTokenIssuer.java) | 登录时 `StpUtil.getSession().set("tenantId", ...)`，缺省回退到 system tenant `"1"` |

**端到端链路**：

```
mate-system / UserPO.tenant_id (V1.1.3)
    → User.tenantId (domain entity)
    → UserConvertor.toResponse → UserInfoResponse.tenantId (RPC)
    → AuthUser.from → AuthUser.tenantId (mate-auth)
    → SaTokenIssuer.issue → StpUtil.getSession().set("tenantId", ...)
    → mate-tenant-starter / TokenTenantResolver 在每次请求读 session
    → TenantContext.setTenantId → MyBatis Plus TenantLineInnerInterceptor
    → 业务表 SQL 自动注入 WHERE tenant_id = ?
```

**启用方式**（部署侧）：在业务服务的 nacos 配置中添加 `mate.tenant.enabled=true` 和 `mate.tenant.resolver=token`。

---

## R2: 端到端集成测试代码框架

**改动**：

| 文件 | 用途 |
|------|------|
| [NoticeFlowIntegrationTest.java](../../mate-biz/mate-notice/src/test/java/vip/mate/notice/integration/NoticeFlowIntegrationTest.java) | mate-notice 全链路：CommandService.send → SmsNoticeAdapter → LogSmsProvider；MySQL/Redis Testcontainers 自动启动 |
| [AuthFlowIntegrationTest.java](../../mate-auth/src/test/java/vip/mate/auth/integration/AuthFlowIntegrationTest.java) | mate-auth 半链路：SmsCodePort.sendLoginCode + CaptchaService.generate |

二者均带 `@Disabled("Requires Docker for Testcontainers — enable with -Dintegration-test=true")`，避免无 Docker 环境下 CI 失败。开发本地可通过 `mvn verify -Pintegration-test -Dintegration-test=true` 实际跑通。

---

## R3: 核心 Controller Javadoc 补全

补全类级 Javadoc（含路由、auth、tenant 范围说明）以提升 Smart-Doc 输出质量：

- [AuthController](../../mate-auth/src/main/java/vip/mate/auth/trigger/controller/AuthController.java) — 类级 + 全部 5 个方法级 Javadoc（@param / @return）
- [AdminController](../../mate-biz/mate-system/src/main/java/vip/mate/system/admin/trigger/controller/AdminController.java) — 类级
- [UserController](../../mate-biz/mate-system/src/main/java/vip/mate/system/trigger/controller/UserController.java) — 类级
- [RoleController](../../mate-biz/mate-system/src/main/java/vip/mate/system/admin/trigger/controller/RoleController.java) — 类级
- [MenuController](../../mate-biz/mate-system/src/main/java/vip/mate/system/admin/trigger/controller/MenuController.java) — 类级
- [ConfigController](../../mate-biz/mate-system/src/main/java/vip/mate/system/admin/trigger/controller/ConfigController.java) — 类级

RFC-051 新建的 [TenantController / StorageController / CodeGenController / JobController] 已自带完整 Javadoc。

`mvn smart-doc:html -pl mate-biz/mate-system` 现在可生成带模块描述、路由前缀、auth 要求和 tenant 范围说明的 HTML。

---

## R4: SmsProvider templateParams Map 接口扩展

**问题**：阿里云 / 腾讯 SMS 网关需要 JSON 形式的模板参数（如 `{"code":"123456"}`），而旧的 `send(mobile, content, businessType)` 只能传一个已渲染的字符串，需要 provider 用正则反向解析。

**改动**：

| 层 | 文件 | 改动 |
|----|------|------|
| SPI | [SmsProvider](../../mate-biz/mate-notice/src/main/java/vip/mate/notice/infrastructure/adapter/sms/SmsProvider.java) | 新增 `default send(mobile, content, businessType, Map<String,String> templateParams)`；3 参数版本保留作为 fallback |
| Aliyun | [AliyunSmsProvider](../../mate-biz/mate-notice/src/main/java/vip/mate/notice/infrastructure/adapter/sms/AliyunSmsProvider.java) | `dispatch()` 优先用 templateParams 渲染 TemplateParam JSON，否则正则提取 |
| Tencent | [TencentSmsProvider](../../mate-biz/mate-notice/src/main/java/vip/mate/notice/infrastructure/adapter/sms/TencentSmsProvider.java) | 同上，`templateParamsMap.values()` 直接传给 `TemplateParamSet` |
| Channel adapter | [SmsNoticeAdapter](../../mate-biz/mate-notice/src/main/java/vip/mate/notice/infrastructure/adapter/sms/SmsNoticeAdapter.java) | 透传 `notice.getTemplateParams()` 到 SmsProvider |
| Aggregate | [NoticeAggregate](../../mate-biz/mate-notice/src/main/java/vip/mate/notice/domain/model/aggregate/NoticeAggregate.java) | 新增 `transient Map<String,String> templateParams`（不入库） |
| Application | [NoticeCommandService](../../mate-biz/mate-notice/src/main/java/vip/mate/notice/application/command/NoticeCommandService.java) | `send()` 增加重载支持 templateParams |
| RPC contract | [IRpcNoticeService](../../mate-common/mate-api/src/main/java/vip/mate/api/notice/service/IRpcNoticeService.java) | 新增 `sendSmsTemplate(mobile, businessType, templateParams)` |
| RPC impl | [RpcNoticeServiceImpl](../../mate-biz/mate-notice/src/main/java/vip/mate/notice/trigger/rpc/RpcNoticeServiceImpl.java) | 实现 sendSmsTemplate |
| Auth port | [NoticeDispatcher](../../mate-auth/src/main/java/vip/mate/auth/domain/adapter/port/NoticeDispatcher.java) | 新增 `default dispatchSmsTemplate(...)` |
| Dubbo dispatcher | [DubboNoticeDispatcher](../../mate-auth/src/main/java/vip/mate/auth/infrastructure/adapter/notice/DubboNoticeDispatcher.java) | 实现 dispatchSmsTemplate |
| Local dispatcher | [LocalNoticeDispatcher](../../mate-monolith/src/main/java/vip/mate/monolith/adapter/LocalNoticeDispatcher.java) | 实现 dispatchSmsTemplate |
| Caller | [RedisSmsCodePort](../../mate-auth/src/main/java/vip/mate/auth/infrastructure/adapter/sms/RedisSmsCodePort.java) | 优先调 `dispatchSmsTemplate(mobile, "VERIFY_CODE", Map.of("code", code))`，失败回退到 dispatchSms |

---

## R5: mobile / desktop 业务卡片扩展 + packages/core 端点常量

### packages/core
- 新增 [endpoints.ts](../../mate-ui/packages/core/src/api/endpoints.ts) — 平台无关 URL 常量表，admin / mobile / desktop 三端共享
- [index.ts](../../mate-ui/packages/core/src/index.ts) 导出 `Endpoints` / `EndpointKey`

### mobile（uni-app）
- [home/index.vue](../../mate-ui/apps/mobile/src/pages/home/index.vue) — 加 4 个 dashboard 数据卡片（用户数 / 在线数 / 今日登录 / 今日操作），点击跳转到通知中心
- [notifications/index.vue](../../mate-ui/apps/mobile/src/pages/notifications/index.vue) — 通知列表（暂用 operation-logs 数据源，未来对接 mate-notice 用户收件箱）
- [profile/index.vue](../../mate-ui/apps/mobile/src/pages/profile/index.vue) — 重写为带头像 + 菜单项 + 修改密码入口
- [profile/change-password.vue](../../mate-ui/apps/mobile/src/pages/profile/change-password.vue) — 新增修改密码页（旧密码 + 新密码 + 确认）
- [pages.json](../../mate-ui/apps/mobile/src/pages.json) — 注册 2 个新路由

### desktop（Tauri 2 + Vue 3 + Element Plus）
- [Home.vue](../../mate-ui/apps/desktop/src/views/Home.vue) — 重写为左侧 sidebar 导航 + 顶栏 + dashboard 数据卡片
- [Users.vue](../../mate-ui/apps/desktop/src/views/Users.vue) — 新增用户列表页（关键字搜索 + Element Plus Table + 分页）
- [main.ts](../../mate-ui/apps/desktop/src/main.ts) — 注册 `/users` 路由

---

## 落地完整度（2026-04-25 终态）

- **52 个 RFC**：51 个 Done（98%），1 个 Partial（022 Smart-Doc 仍可继续补 Javadoc）
- **真实代码缺口**：0
- **配置缺口**：3 项部署侧（不在脚手架代码层）
  - 多租户：`mate.tenant.enabled=true` 需在业务服务 nacos 配置中开启
  - 短信网关：`mate.notice.sms.provider=aliyun|tencent` + 凭据需用户填入
  - XXL-Job：`xxl.job.admin-addresses` 需用户部署 admin 后填入

## 后续建议

| 编号 | 项 | 备注 |
|------|---|------|
| F1 | mate-auth 当 user 存在但 tenant 已 SUSPENDED/EXPIRED 时阻止登录 | 业务策略增强；当前 SaTokenIssuer 不查 tenant 状态 |
| F2 | apps/mobile 抽象出 packages/core 的 uni.request 适配器 | 让 endpoints.ts 配 platform-aware client，复用 axios interceptor 行为 |
| F3 | E2E 测试套件（mate-auth + mate-notice + Nacos 三容器） | 当前 R2 是模块内集成；跨服务 Dubbo 链路验证仍需 docker-compose 级别样板 |

这三项已在 [development-roadmap.md](../conventions/development-roadmap.md) 第二节标记，作为下一轮迭代候选。
