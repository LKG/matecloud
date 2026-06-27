# RFC-056: mate-sso-starter — 平台级身份接入(SSO 登录 + 组织同步)可插拔能力

- **Status**: Proposed
- **Created**: 2026-06-02
- **Type**: Platform Capability (Starter) Architecture
- **Owner**: 平台 / 认证与身份
- **Related**: RFC-055(LMS 产品,首个消费方)、RFC-045(双模)、RFC-028(多租户)、RFC-041(Flyway)、`mate-auth` LoginStrategy SPI、`mate-channel-starter` 配置存储、`mate-file-starter` `@FileProvider` SPI 范式
- **Borrowed from**: PlayEdu(`C:\codes\playedu-full` 的 LoginController / WechatWorkController / SynchController / listener/v2/synch/*Listener / bus/LDAPBus / util/ldap/LdapUtil / caches/*Cache / constant/ConfigConstant —— 仅借鉴业务逻辑)

## TL;DR

**把"企业微信 / 钉钉 / 飞书 / LDAP 的 SSO 登录 + 组织(部门/成员)同步"抽象为一个平台级可插拔 starter。** 用一组**设计模式**收敛 PlayEdu 里散落的四套渠道实现:`IdentityProvider` 插件 SPI(对标 `@FileProvider`)+ `AbstractOAuthIdentityProvider` 模板方法(统一 OAuth 骨架)+ `SyncPipeline` 同步管道 + Redis 分布式锁 + 域事件 + **六边形端口**(`OrgProvisionPort` 由消费方实现,starter 不碰业务表)。登录侧**零侵入**复用 `mate-auth` 的 `LoginStrategyFactory`(仅把 `LoginStrategy` SPI 提取到 `mate-auth-spi` 共享,新增 `LoginType.SSO/LDAP` 两枚举);凭证复用 `mate_channel_config`(AES 加密);同步触发复用 `@MateJobHandler` + `mate-mq-starter`。**加一个渠道 = 加一个 `@IdentityProvider` 类,其余零改动。** LMS(RFC-055)与 mate-system 都是它的消费方。

---

## 1. Context

### 1.1 PlayEdu 现状(借鉴对象)
PlayEdu 已完整实现四渠道(`LOGIN_CHANNEL`:本地=0、企业微信=1、飞书=2、钉钉=3、LDAP=4),但实现方式**重复且耦合**:
- 登录分散在 `LoginController.doGetLocalUserId()` 的 `if(fromScene==1)…else if(2)…` 巨型分支;
- 同步是四个近乎复制的 `WechatWorkListener/FeiShuListener/DingtalkListener/LDAPSyncListener`(各自 Kafka topic);
- 四个 `*Cache`(access_token / jsapi_ticket / 同步互斥)、四个 `*HttpUtil`;
- 中间映射表 `user_synch` / `department_synch` / `ldap_user` / `ldap_department`;
- 配置硬编码 `synch.*` / `ldap.*` key。

**问题**:新增渠道要改登录分支 + 新建一套 Listener/Cache/HttpUtil,违反开闭原则。

### 1.2 MateCloud 现成接入点(复用,不重造)
| 能力 | 现成件 | 复用方式 |
|---|---|---|
| 登录策略 | `mate-auth`:`LoginStrategy<T>` + `LoginStrategyFactory`(注入 `List<LoginStrategy>` 按 `LoginType` 分发)+ `LoginType` 枚举(PASSWORD/SMS) | 新增 `SsoLoginStrategy`/`LdapLoginStrategy` 由工厂自动发现 |
| 令牌签发 | `TokenIssuerPort` / `SaTokenIssuer` | 登录末端复用 |
| 加密配置存储 | `mate_channel_config`(channel/provider_type/config_json AES/enabled/scope)+ `DbChannelConfigStore` + `ChannelCrypto` | 新增 `channel='identity'` |
| 插件 SPI 范式 | `mate-file-starter`:`@FileProvider` + `FileStorage` + `FileStorageRegistry`(`List` 注入建表)+ `ProviderDescriptor`(给 UI 的字段元数据) | **直接照搬范式** |
| 组织(部门树) | `mate_dept`(parent_id + ancestors 逗号链) | 经 `OrgProvisionPort` 写入 |
| 缓存/锁 | `mate-cache-starter`:Redis + `@DistributedLock` | token 缓存 + 同步互斥 |
| 域事件 / 定时 | `mate-mq-starter` `EventPublisher` / `mate-job-starter` `@MateJobHandler` | 同步事件 + 定时拉取 |

### 1.3 目标
一个 `mate-starters/mate-sso-starter`,使任何服务(mate-system、mate-lms…)**引入即获得** SSO 登录 + 组织同步,且:
- **开闭**:加渠道 = 加一个 `@IdentityProvider` 实现类;
- **零侵入**:`mate-auth` 的 Controller/Factory 不改;
- **不碰业务表**:starter 只管"身份映射",业务 user/dept 由消费方经 Port 提供;
- **可插拔**:`mate.sso.enabled=false` 即完全关闭。

---

## 2. 设计原则

1. **SPI 优先于分支**:渠道差异封装进 `IdentityProvider` 实现,核心代码不出现 `if(provider==…)`。
2. **模板方法固化流程**:OAuth"换 token→拉用户→拉组织"骨架在抽象基类,子类只填 vendor 差异。
3. **六边形 / 端口隔离业务**:starter 是"驱动适配器(provider)+ 核心引擎",业务落库是"被驱动端口(`OrgProvisionPort`)",由消费方实现 → starter 不依赖任何业务 schema。
4. **复用而非新建**:配置、加密、锁、事件、定时、令牌全部走平台既有件。
5. **声明式装配**:`@AutoConfiguration` + `AutoConfiguration.imports` + `@ConditionalOnProperty`。

---

## 3. 模式总览(本 RFC 的"优雅"核心)

| 设计模式 | 落点 | 解决 PlayEdu 的什么 |
|---|---|---|
| **Strategy** | `IdentityProvider`(每渠道一策略)、`SsoLoginStrategy`/`LdapLoginStrategy`(每登录方式一策略) | 消除 `LoginController` / `*Listener` 的渠道大分支 |
| **Template Method** | `AbstractOAuthIdentityProvider`(OAuth 登录与同步骨架) | 三个 OAuth Listener 的重复流程上提为骨架 |
| **Plugin / SPI + 注解发现** | `@IdentityProvider("wechat_work")` + `ProviderRegistry`(`List<IdentityProvider>` 注入建表) | 照搬 `@FileProvider`,加渠道零侵入 |
| **Facade** | `SsoTemplate`(`authenticate(code)` / `sync(provider)`) | 给消费方/登录策略一个统一门面 |
| **Ports & Adapters(六边形)** | 出站端口 `OrgProvisionPort` / `IdentityMappingPort` / `AccessTokenCache` | starter 与业务 user/dept 表解耦 |
| **Pipeline / Chain** | `SyncPipeline`:fetch→diff→provision→prune→publish | 把"全量拉取+差集删除"流程化、可插步骤 |
| **Strategy(再用)** | `SyncMode`:FULL / INCREMENTAL | 全量 vs 增量同步切换 |
| **Builder** | `ProviderDescriptor`(声明 UI 配置字段) | 后台动态渲染配置表单 |
| **Observer / 事件驱动** | `OrgSyncStartedEvent` / `OrgSyncCompletedEvent`(`mate-mq-starter`) | 解耦"同步完成后通知/统计" |
| **Singleton + 互斥(Redis 锁)** | `@DistributedLock("sso:sync:{provider}")` | 取代 PlayEdu 的 `isSynchronizeing()` 自旋标志 |
| **Cache-Aside** | `AccessTokenCache`(Redis, per-provider TTL) | 取代四个 `*Cache` |
| **Anti-Corruption Layer** | 各 provider 把 vendor JSON 翻译为统一 `ExternalUser`/`ExternalDept` | 隔离 vendor 字段差异 |

---

## 4. 模块与包结构

```
mate-starters/mate-sso-starter/
├── pom.xml                  依赖: mate-auth-spi(登录SPI契约) · mate-cache-starter · mate-channel-starter
│                                  mate-mq-starter · mate-job-starter · (LDAP: 仅 JDK javax.naming)
└── src/main/java/vip/mate/starter/sso/
    ├── SsoAutoConfiguration.java          @AutoConfiguration @ConditionalOnProperty(mate.sso.enabled)
    ├── config/
    │   ├── SsoProperties.java             @ConfigurationProperties("mate.sso")
    │   └── SsoConfigStore.java            读 mate_channel_config(channel='identity')→ ProviderConfig(已解密)
    ├── spi/
    │   ├── IdentityProvider.java          ← 渠道插件 SPI(Strategy)
    │   ├── annotation/IdentityProvider.java  @IdentityProvider("wechat_work")
    │   ├── AbstractOAuthIdentityProvider.java ← Template Method
    │   └── model/{ExternalUser, ExternalDept, OAuthContext, ProviderDescriptor, FieldSpec}.java
    ├── provider/
    │   ├── WechatWorkProvider.java · DingTalkProvider.java · FeishuProvider.java
    │   └── LdapProvider.java              (实现 IdentityProvider,但走目录绑定而非 OAuth)
    ├── core/
    │   ├── ProviderRegistry.java          List<IdentityProvider> → Map<code, provider>(Registry/Factory)
    │   ├── SsoTemplate.java               Facade: authenticate(code,provider) / sync(provider,mode)
    │   ├── AccessTokenCache.java          Cache-Aside(Redis)
    │   └── sync/
    │       ├── SyncPipeline.java          fetch→diff→provision→prune→publish(Pipeline)
    │       ├── SyncMode.java              FULL / INCREMENTAL(Strategy)
    │       └── SyncContext.java
    ├── port/                              ← 六边形出站端口(消费方实现)
    │   ├── OrgProvisionPort.java          upsert/disable/unlink 部门与成员(业务表由消费方写)
    │   ├── IdentityMappingPort.java       外部ID↔本地ID 映射(starter 提供默认 JDBC 实现)
    │   └── impl/JdbcIdentityMappingAdapter.java
    ├── auth/                              ← 登录策略(实现 mate-auth-spi 的 LoginStrategy)
    │   ├── SsoLoginStrategy.java          LoginType.SSO
    │   ├── LdapLoginStrategy.java         LoginType.LDAP
    │   └── command/{SsoLoginCommand, LdapLoginCommand}.java
    ├── event/{OrgSyncStartedEvent, OrgSyncCompletedEvent}.java   (BaseEvent)
    ├── job/OrgSyncJob.java                @MateJobHandler("sso_org_sync")
    ├── web/
    │   ├── SsoCallbackController.java     /api/v1/sso/webhook/{provider}(回调驱动同步)
    │   └── SsoJsConfigController.java     /api/v1/sso/jsconfig(企业微信 JS-SDK 签名)
    └── types/SsoErrorCode.java            SSO + A/B/C/D/E + SEQ
└── src/main/resources/
    ├── META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
    └── db/migration/V1.x.x__sso_identity.sql   (mate_identity_* 双方言)
```

> starter 不分 DDD 四层(它是库,按 file/sms-starter 的 `spi/provider/core/config` 范式组织)。

---

## 5. 核心抽象(SPI + 模板方法 + 门面)

### 5.1 渠道插件 SPI(Strategy)
```java
public interface IdentityProvider {
    String code();                              // "wechat_work" | "dingtalk" | "feishu" | "ldap"
    ProviderDescriptor descriptor();            // 给后台 UI 的配置字段元数据(Builder)
    AuthKind authKind();                        // OAUTH | DIRECTORY_BIND
    /** code → 外部用户(OAuth);或 用户名+密码 → 外部用户(LDAP) */
    ExternalUser authenticate(OAuthContext ctx, ProviderConfig cfg);
    /** 拉取组织树与成员(同步用) */
    List<ExternalDept> fetchDepartments(ProviderConfig cfg);
    List<ExternalUser> fetchUsers(ProviderConfig cfg, List<ExternalDept> depts);
}
```

### 5.2 OAuth 骨架(Template Method)—— 收敛三家重复流程
```java
public abstract class AbstractOAuthIdentityProvider implements IdentityProvider {
    public final ExternalUser authenticate(OAuthContext ctx, ProviderConfig cfg) {
        String accessToken = accessTokenCache.get(code(), () -> requestAccessToken(cfg)); // Cache-Aside
        String externalId  = exchangeCodeForUserId(ctx.code(), accessToken, cfg);          // 子类填
        return fetchUserDetail(externalId, accessToken, cfg);                              // 子类填(ACL)
    }
    public final AuthKind authKind() { return AuthKind.OAUTH; }
    protected abstract String requestAccessToken(ProviderConfig cfg);
    protected abstract String exchangeCodeForUserId(String code, String token, ProviderConfig cfg);
    protected abstract ExternalUser fetchUserDetail(String externalId, String token, ProviderConfig cfg);
    // fetchDepartments/fetchUsers 亦提供受保护的分页/重试模板
}
```
`WechatWorkProvider` 只实现三个抽象点(对应 PlayEdu `WechatHttpUtil` 的三段);`DingTalkProvider` 额外处理 `code→unionId→userId`;`FeishuProvider` 处理 `tenant_access_token→user_access_token`。`LdapProvider` 不继承该基类,`authKind()=DIRECTORY_BIND`,`authenticate()` 走 `javax.naming` 目录绑定校验密码(借鉴 `LdapUtil.loginByMailOrUid`)。

### 5.3 门面(Facade)
```java
public class SsoTemplate {
    ExternalUser authenticate(String providerCode, OAuthContext ctx);  // 登录策略调用
    SyncResult   sync(String providerCode, SyncMode mode);             // 手动/定时/回调调用
    JsSdkConfig  jsConfig(String providerCode, String url);            // 企业微信 H5 JS-SDK 签名
}
```

### 5.4 注册表(Registry/Factory)—— 照搬 FileStorageRegistry
```java
public class ProviderRegistry {
    private final Map<String, IdentityProvider> map = new ConcurrentHashMap<>();
    public ProviderRegistry(List<IdentityProvider> providers) {       // Spring 注入全部实现
        providers.forEach(p -> map.put(p.code(), p));
    }
    public IdentityProvider get(String code) { /* 缺失抛 SSO_C_PROVIDER_NOT_FOUND */ }
}
```

---

## 6. 登录集成(零侵入 mate-auth)

### 6.1 登录策略落点(实现修正:策略在 mate-auth,不抽 mate-auth-spi)
> **实现阶段结论(优于初稿)**:阅读 `AbstractLoginStrategy` 后发现它把**令牌签发(`TokenIssuerPort`)+ 失败计数 + 审计 + 构造 `AuthUser`** 全部集中在 mate-auth 内,登录策略天然耦合 mate-auth 领域。因此**最优解是把 `SsoLoginStrategy`/`LdapLoginStrategy` 放在 mate-auth**(继承 `AbstractLoginStrategy`,复用其令牌签发),而 mate-auth **常规依赖** mate-sso-starter(仅取 `SsoTemplate`/`IdentityMappingPort`/`AuthRequest`/`ExternalUser`)。
>
> 这样**无循环依赖、无需新建 `mate-auth-spi`、不动 `AuthController`/`LoginStrategyFactory`**——比初稿"下沉 SPI + starter 贡献策略"更轻、更内聚。已实现:
> - `LoginType` 新增 `SSO("sso")` / `LDAP("ldap")`;
> - `SsoLoginCommand`(providerCode + code + state)/`LdapLoginCommand`(username + password + captcha);
> - `SsoLoginStrategy`/`LdapLoginStrategy`:`@Component` + `@ConditionalOnProperty("mate.feature.sso.enabled")`(与 starter 同开关,确保 `SsoTemplate` 在场);`verify()` = `SsoTemplate.authenticate` → `IdentityMappingPort.resolveUser` → `authDomainService.loadById` → `ensureActive`;令牌签发由基类完成;
> - `AuthController` 新增 `POST /api/v1/auth/sso/login`、`/ldap/login`;
> - 渠道差异由 `providerCode` 承载,不污染枚举。
>
> ~~(初稿方案:提取 `mate-auth-spi` 让 starter 贡献策略——因策略需 `TokenIssuerPort` 故放弃。)~~

### 6.2 登录时序(OAuth)
```
前端取 code(企业微信 WWLogin / 钉钉 DDLogin / 飞书 QRLogin)
  → POST /api/v1/auth/login { type:"sso", providerCode:"wechat_work", code, state }
  → AuthController(不改) → LoginStrategyFactory.get(SSO) → SsoLoginStrategy.login()
       ├─ SsoTemplate.authenticate(provider, code) → ExternalUser(externalId, unionId, name, mobile, deptCodes)
       ├─ IdentityMappingPort.resolve(provider, externalId|unionId) → 本地 principalId(无则按策略创建/拒绝)
       └─ TokenIssuerPort.issue(principal) → Sa-Token
  → LoginResult{ token, principalId, ... }
```
`LdapLoginStrategy`:`type:"ldap"`,`SsoTemplate` 走目录绑定校验密码 → `IdentityMappingPort` 解析 → 签发。

### 6.3 账号归一
同一人多渠道:优先 `unionId`(钉钉/企业微信)→ 次 `email/mobile` → 落 `mate_identity_user_mapping`,避免重复主体。

---

## 7. 组织同步引擎(Pipeline + 锁 + 事件)

### 7.1 管道(Pipeline)
```java
public SyncResult run(String providerCode, SyncMode mode) {
  return lock.run("sso:sync:" + providerCode, () -> {          // @DistributedLock 互斥
    publish(new OrgSyncStartedEvent(providerCode));
    var depts = provider.fetchDepartments(cfg);                 // 1 fetch
    var users = provider.fetchUsers(cfg, depts);
    var diff  = differ.diff(mappingPort.snapshot(providerCode), depts, users, mode); // 2 diff(全量/增量)
    provision(diff.upserts());                                  // 3 provision(经 OrgProvisionPort 写部门树/成员)
    prune(diff.removed(), props.getLeaveStrategy());            // 4 prune(离职:禁用/解绑)
    var r = SyncResult.of(diff);
    publish(new OrgSyncCompletedEvent(providerCode, r));        // 5 publish(Observer)
    return r;
  });
}
```
- **部门树**:`OrgProvisionPort.upsertDept(extDept, parentLocalId)` —— 消费方按 `parent_id`/`ancestors` 建树(借鉴 PlayEdu parentChain;LDAP 走 OU 链逐层)。
- **差集删除**:`mode=FULL` 时本地多余映射 → `prune`;`INCREMENTAL` 仅处理变更集。
- **离职策略**:`LeaveStrategy = DISABLE | UNLINK`(借鉴 PlayEdu LDAP `userAccountControl` 差集解绑)。

### 7.2 三种触发(统一入口 `SsoTemplate.sync`)
| 触发 | 实现 | 替代 PlayEdu 的 |
|---|---|---|
| 手动 | `POST /api/v1/sso/sync/{provider}` | `SynchController` |
| 定时 | `@MateJobHandler("sso_org_sync")` 读 `mate.sso.sync.cron` | `SynchAutoJob`(Quartz)|
| 回调 | `POST /api/v1/sso/webhook/{provider}`(验签后发 `OrgSyncStartedEvent`) | (PlayEdu 仅预留) |

> 重负载拉取经 `mate-mq-starter` 异步化(对标 PlayEdu Kafka),避免 HTTP 超时。

---

## 8. 六边形端口(starter 不碰业务表)

```java
/** 消费方实现:把外部组织落到自己的领域(mate-system→mate_dept/mate_admin;LMS→mate_lms_member/dept) */
public interface OrgProvisionPort {
    Long upsertDept(ExternalDept dept, Long parentLocalId, ProviderRef ref);
    Long provisionUser(ExternalUser user, List<Long> localDeptIds, ProviderRef ref);
    void disableUser(Long localUserId);
    void unlinkUser(Long localUserId);
}
/** 身份映射:starter 自带默认 JDBC 实现(mate_identity_* 表),消费方一般无需改 */
public interface IdentityMappingPort {
    Optional<Long> resolveUser(String provider, String externalId, String unionId);
    void bindUser(String provider, String externalId, String unionId, Long localUserId);
    OrgSnapshot snapshot(String provider);   // 供 diff
    ...
}
```
- mate-system 提供 `OrgProvisionPort` 实现写 `mate_dept`/`mate_admin`;
- mate-lms 提供实现写 `mate_lms_member`/部门 —— **同一 starter,两个消费方,各写各的表**。

---

## 9. 配置与凭证(复用 mate_channel_config)

- `channel='identity'`,`provider_type ∈ {wechat_work,dingtalk,feishu,ldap}`,`config_json`(AES,`ChannelCrypto`),`enabled`,`scope`(多租户)。
- 字段由 `ProviderDescriptor` 声明(Builder),后台据此动态渲染表单(对标 `@FileProvider.descriptor()`),原型「身份接入」app 的各 provider 配置页即其 UI。
- `mate.sso.*`(yml/Nacos):`enabled`、`default-provider`、`sync.cron`、`sync.mode`、`leave-strategy`、`webhook-enabled`。

各渠道字段差异(ACL 后统一为 `ProviderConfig`):

| provider | 关键字段 | 登录端点 |
|---|---|---|
| wechat_work | corpId, agentId, secret, contactsSecret, trustDomain | WWLogin → /synch |
| dingtalk | corpId, appKey, appSecret, scanAppId | DDLogin(code→unionId→userId)|
| feishu | appId, appSecret, redirectUri | QRLogin(tenant→user token)|
| ldap | url, baseDN, adminDN, adminPass, filterScope, excludeScope | 目录绑定校验 |

---

## 10. 数据模型(starter 自有,通用,Flyway 双方言)

```sql
-- mate_identity_user_mapping:外部身份 ↔ 本地主体
id, provider, external_id, union_id, principal_id, principal_type('admin'|'member'),
external_name, external_mobile, tenant_id, last_sync_at, deleted, created_at, updated_at
UNIQUE(provider, external_id, tenant_id)

-- mate_identity_dept_mapping:外部部门 ↔ 本地部门
id, provider, external_id, local_dept_id, tenant_id, deleted, created_at, updated_at
UNIQUE(provider, external_id, tenant_id)

-- mate_identity_sync_log:同步审计
id, provider, object('dept'|'user'|'both'), trigger('manual'|'job'|'webhook'),
added, updated, removed, result('SUCCESS'|'FAIL'), error, cost_ms, tenant_id, created_at
```
> 业务 user/dept 不在此(在消费方);principal_type 区分 admin/member,使 mate-system 与 mate-lms 可共用映射表而互不干扰。

---

## 11. 安全

- 凭证 AES 存储,后台明文录入、服务端加密、永不回显(复用 `ChannelCrypto`)。
- 回调验签(各 vendor 签名)+ 防重放(nonce + timestamp + Redis)。
- access_token / jsapi_ticket Redis 缓存,TTL 略小于 vendor 有效期。
- 最小权限:仅申请通讯录读权限;同步范围受 `filterScope/scope` 限制。

---

## 12. 可插拔与装配

```
META-INF/.../AutoConfiguration.imports → vip.mate.starter.sso.SsoAutoConfiguration
SsoAutoConfiguration: @ConditionalOnProperty(name="mate.sso.enabled", havingValue="true")
  @Bean ProviderRegistry / SsoTemplate / SyncPipeline / AccessTokenCache
  @Bean SsoLoginStrategy / LdapLoginStrategy        (@ConditionalOnClass(LoginStrategy))
  @Bean @ConditionalOnMissingBean IdentityMappingPort → JdbcIdentityMappingAdapter
  (OrgProvisionPort 无默认实现:@ConditionalOnMissingBean 缺失则启动报"请实现 OrgProvisionPort")
```
- `mate.sso.enabled=false` → 整能力关闭,登录页不出现 SSO 入口。
- 仅需某几个渠道:provider bean 都在,但只有 `mate_channel_config.enabled=1` 的才生效。

---

## 13. 消费方接入(两行依赖 + 一个端口实现)

- **mate-system**:`pom` 加 `mate-sso-starter` → 实现 `OrgProvisionPort` 写 `mate_dept`/`mate_admin` → 后台「身份接入」配置渠道即可。
- **mate-lms**(RFC-055):同样依赖 + 实现 `OrgProvisionPort` 写 `mate_lms_member` → 学员从企业微信/钉钉一键导入并持续同步。

---

## 14. 里程碑

| 阶段 | 内容 |
|---|---|
| S0 | 提取 `mate-auth-spi`;新增 `LoginType.SSO/LDAP`;starter 骨架 + AutoConfig |
| S1 | SPI + `AbstractOAuthIdentityProvider` + `ProviderRegistry` + `SsoTemplate` + `mate_identity_*` 迁移 |
| S2 | `WechatWorkProvider`(OAuth + JS-SDK)+ `SsoLoginStrategy` 打通登录 |
| S3 | `SyncPipeline`(全量/增量 + 锁 + 事件)+ `OrgProvisionPort`(mate-system 实现)打通组织同步 |
| S4 | `DingTalkProvider` / `FeishuProvider` / `LdapProvider` + `LdapLoginStrategy` |
| S5 | 定时 `@MateJobHandler` + Webhook 回调 + 同步日志看板 |
| S6 | LMS(RFC-055)接入:学员同步 + 学员端 SSO 登录 |

---

## 15. 关联与附录

- **RFC-055**:LMS 是本 starter 首个消费方(学员同步 + SSO 登录)。
- **mate-auth / mate-channel-starter / mate-file-starter**:登录工厂、加密配置、`@FileProvider` 范式 —— 本 RFC 直接复用/对标。
- **原型**:`docs/prototypes/mate-lms-admin.html` 的「身份接入」平台应用(接入概览 / 企业微信·钉钉·飞书·LDAP / 登录方式 / 账号映射 / 同步策略 / 同步日志)即本 RFC 的可交互规格,「组件/字段映射」抽屉已标注对应 PO 字段与 scene。
- **唯一重构**:提取 `mate-auth-spi`(纯契约下沉),其余全为新增,`mate-auth` 业务零改动。
