# mate-sso-starter

平台级可插拔**身份接入**能力:企业微信 / 钉钉 / 飞书 / LDAP 的 **SSO 登录 + 组织(部门/成员)同步**。设计见 [RFC-056](../../docs/rfcs/056-mate-sso-starter.md)。

> 借鉴 PlayEdu 的渠道逻辑,以"模式化"重构,**零侵入** mate-auth,**不碰业务表**。

## 模式化设计

| 模式 | 落点 |
|---|---|
| Strategy + Plugin SPI | `@IdentityProvider("wechat_work")` + `IdentityProvider` + `ProviderRegistry`(对标 `@FileProvider`)|
| Template Method | `AbstractOAuthIdentityProvider`:换 token → 拉用户 → 拉组织(三家 OAuth 共用骨架)|
| Facade | `SsoTemplate`:`authenticate(provider, req)` / `sync(provider, mode)` |
| Ports & Adapters(六边形)| `OrgProvisionPort`(消费方实现,写自己的 dept/user)、`IdentityMappingPort`(starter 自带 JDBC 默认实现)|
| Pipeline | `SyncPipeline`:fetch → 建部门树 → provision → (prune) → publish |
| Cache-Aside | `AccessTokenCache`(默认进程内,可换 Redis)|
| ACL | provider 把 vendor JSON 翻译为中性 `ExternalUser` / `ExternalDept` |
| Observer | `OrgSyncStartedEvent` / `OrgSyncCompletedEvent`(Spring 事件,可桥接 mate-mq)|

## 启用

```yaml
mate:
  feature:
    sso:
      enabled: true          # 主开关(遵循 coding-standards §12.3)
  sso:
    default-provider: wechat_work
    sync:
      cron: "0 0 2 * * ?"
      mode: FULL             # FULL | INCREMENTAL
      leave-strategy: DISABLE # DISABLE | UNLINK
```

凭证不放 yml,放 `mate_channel_config`(channel=`identity`,AES 加密,后台「身份接入」配置页维护)。

## 消费方接入(两步)

1. 依赖:`<artifactId>mate-sso-starter</artifactId>`
2. 实现 `OrgProvisionPort`(把外部组织写入你的 dept/user 表)——**没有默认实现**,组织同步必须由消费方提供落点。
3. 把 `sso/identity-schema-reference.sql` 的建表语句拷入**你自己服务**的 `db/migration/V{next}__sso_identity.sql`(starter 不自带 Flyway 迁移,避免版本冲突;参照 mate-channel-starter)。

登录:`SsoTemplate.authenticateAndResolve(provider, AuthRequest)` 返回本地 principalId(由 mate-auth 的 `SsoLoginStrategy` 调用并签发 token)。

> **平台侧已内置实现(mate-system)**:`SystemOrgProvisionAdapter`(写 `mate_dept`/`mate_admin`,同步的管理员**默认零角色**,需手动授权)、迁移 `V1.3.0__sso_identity.sql`、手动同步端点 `POST /api/v1/admin/sso/sync/{provider}`。启用只需 `mate.feature.sso.enabled=true` + 在 `mate_channel_config(channel=identity)` 配置渠道。
> 学员场景由 mate-lms 实现自己的 `OrgProvisionPort`(写 `mate_lms_member`)。
> 本 starter 已依赖 `mate-channel-starter`(凭证读取),消费方无需再单独引入。

## 状态

| 项 | 状态 |
|---|---|
| SPI / 模板方法 / 注册表 / 门面 / 端口 / 同步管道 / 配置 / 自动装配 | ✅ 已实现,编译通过 |
| **企业微信 provider** | ✅ 对齐 PlayEdu:OAuth 登录、通讯录同步、**H5 JS-SDK 签名(wx.config/wx.agentConfig)**、跳过非激活成员(status≠1)、过滤"其他（待设置部门）" |
| **H5 JS-SDK 签名** | ✅ `JsSdkProvider` SPI + `SsoTemplate.jsConfig` + `SsoJsConfigController`(`POST /api/v1/sso/jsconfig`)|
| **同步 prune 差集** | ✅ FULL 同步时按 `leave-strategy`(DISABLE/UNLINK)清理离职成员;删除上游已删部门 |
| **Redis token 缓存 + 同步互斥锁** | ✅ `RedisAccessTokenCache` + `RedissonSyncLock`(检测到 RedissonClient 即启用,否则进程内/NoOp 兜底)|
| LDAP provider | ✅ 登录(目录绑定)已实现;通讯录同步(OU 链 / 分页 / 禁用判断)为 TODO |
| 钉钉 / 飞书 provider | 🟡 骨架(token 已通,login/sync 步骤标注 TODO,按官方 OpenAPI 收尾)|
| 登录策略(`SsoLoginStrategy`/`LdapLoginStrategy`)| ✅ 已在 **mate-auth** 实现并打通(`POST /api/v1/auth/sso/login`、`/ldap/login`);mate-auth 常规依赖本 starter,`@ConditionalOnProperty(mate.feature.sso.enabled)` 激活。**无需 mate-auth-spi**(详见 RFC-056 §6.1 修正)|
| Webhook 回调控制器 | ⏳ 下一步 |

> 登录端到端:前端取 code → `POST /api/v1/auth/sso/login {providerCode, code}` → `SsoLoginStrategy` 经 `SsoTemplate` 换取外部身份 → `IdentityMappingPort` 解析本地用户 → 签发 Sa-Token。学员须已由组织同步绑定(未绑定则拒绝,对齐 PlayEdu)。本地 id 采用 `String`(matecloud VARCHAR(32) 约定)。

**与 PlayEdu 的有意差异(非缺陷)**:① 消息推送(`message/send`)走 `mate-notice`,不在本 starter;② 切换渠道清旧数据(`clearOtherChannelData`)—— 本 starter 支持多渠道并存,故不适用;③ 失败重试 —— 依赖幂等的定时重跑 + `OrgSyncCompletedEvent(fail)` 告警,不建独立重试表(避免与 mate-mq 重试重复)。

## 包结构

```
vip.mate.starter.sso
├── spi/            IdentityProvider(SPI)· AbstractOAuthIdentityProvider(模板)· AuthKind · annotation/ · model/
├── provider/       WechatWork / DingTalk / Feishu / Ldap
├── core/           ProviderRegistry · SsoTemplate · SsoConfigStore · AccessTokenCache · http/ · sync/
├── port/           OrgProvisionPort · IdentityMappingPort(+impl/JdbcIdentityMappingAdapter)
├── event/          OrgSyncStarted/CompletedEvent
├── config/         SsoProperties · SsoAutoConfiguration
└── types/          SsoErrorCode
```
