# RFC-051: 收口落地 — 2026-04-25 完成回合

> Status: **Done**
> Date: 2026-04-25
> Predecessor: [development-roadmap.md](../conventions/development-roadmap.md) 2026-04-25 重写版

## 背景

development-roadmap 2026-04-25 版列出了 P0/P1/P2 共 12 项剩余缺口。本 RFC 记录这一回合内完成的落地工作以及随之而来的代码改动。

---

## 本轮完成清单

### P0 — 阻塞使用

| 编号 | 项 | 改动 |
|------|---|------|
| **B1** | mate-auth → mate-notice Dubbo 调用打通 | 新增 [IRpcNoticeService](../../mate-common/mate-api/src/main/java/vip/mate/api/notice/service/IRpcNoticeService.java) + [RpcNoticeServiceImpl](../../mate-biz/mate-notice/src/main/java/vip/mate/notice/trigger/rpc/RpcNoticeServiceImpl.java)；mate-auth 引入 [NoticeDispatcher](../../mate-auth/src/main/java/vip/mate/auth/domain/adapter/port/NoticeDispatcher.java) 抽象端口 + [DubboNoticeDispatcher](../../mate-auth/src/main/java/vip/mate/auth/infrastructure/adapter/notice/DubboNoticeDispatcher.java) 实现；[RedisSmsCodePort](../../mate-auth/src/main/java/vip/mate/auth/infrastructure/adapter/sms/RedisSmsCodePort.java) 重写为通过 dispatcher 调度，保留无 dispatcher 时的日志 fallback |
| **B2** | mate-notice 真实短信网关 | 抽出 [SmsProvider](../../mate-biz/mate-notice/src/main/java/vip/mate/notice/infrastructure/adapter/sms/SmsProvider.java) SPI；提供 [LogSmsProvider](../../mate-biz/mate-notice/src/main/java/vip/mate/notice/infrastructure/adapter/sms/LogSmsProvider.java)（默认）/ [AliyunSmsProvider](../../mate-biz/mate-notice/src/main/java/vip/mate/notice/infrastructure/adapter/sms/AliyunSmsProvider.java)（HMAC-SHA1 + Dysmsapi v1）/ [TencentSmsProvider](../../mate-biz/mate-notice/src/main/java/vip/mate/notice/infrastructure/adapter/sms/TencentSmsProvider.java)（TC3-HMAC-SHA256），无新增三方 SDK 依赖 |
| **F1** | RoleList 状态 i18n | [RoleList.vue:43](../../mate-ui/apps/admin/src/views/admin/RoleList.vue) 改用 `t('common.enable')` / `t('common.disable')` |

### P1 — 完整性

| 编号 | 项 | 改动 |
|------|---|------|
| **T1** | 集成测试代码框架 | [RedisSmsCodePortTest](../../mate-auth/src/test/java/vip/mate/auth/infrastructure/adapter/sms/RedisSmsCodePortTest.java)（5 用例）+ [RpcNoticeServiceImplTest](../../mate-biz/mate-notice/src/test/java/vip/mate/notice/trigger/rpc/RpcNoticeServiceImplTest.java)（5 用例）+ [NoticeCommandServiceTest](../../mate-biz/mate-notice/src/test/java/vip/mate/notice/application/command/NoticeCommandServiceTest.java)（5 用例）+ [LogSmsProviderTest](../../mate-biz/mate-notice/src/test/java/vip/mate/notice/infrastructure/adapter/sms/LogSmsProviderTest.java)。mate-auth / mate-notice pom 加入 mate-test-starter |
| **T2** | Smart-Doc 各模块独立配置 | mate-auth / mate-system / mate-notice 各自 `src/main/resources/smart-doc.json`，packageFilters 限定到本模块 trigger.controller |
| **T3** | UserQueryService 关键字下沉 | [UserQueryServiceImpl.exportRows](../../mate-biz/mate-system/src/main/java/vip/mate/system/application/query/impl/UserQueryServiceImpl.java) 移除客户端 stream 过滤，依赖 UserRepositoryImpl 中已有的 `wrapper.like(username/realName/mobile)` |
| **T4** | mate-cli 模板 | [GenCodeTemplate](../../mate-cli/src/main/java/vip/mate/cli/template/GenCodeTemplate.java) 在 `generateRepositoryImpl` 中按 columns 自动生成 toPO/toEntity 字段映射；[AggregateTemplate](../../mate-cli/src/main/java/vip/mate/cli/template/AggregateTemplate.java) 移除 `// TODO: add fields`，改为指引性注释 |
| **T5** | mate.rpc.mode 双模收口 | mate-auth 引入 NoticeDispatcher 抽象层（同 B1）；mate-monolith 新增 [LocalNoticeDispatcher](../../mate-monolith/src/main/java/vip/mate/monolith/adapter/LocalNoticeDispatcher.java)；mate-monolith [application.yml](../../mate-monolith/src/main/resources/application.yml) 完善 CORS 与 nacos imports |
| **T6** | RFC-050 P2 后端 API | [StorageController](../../mate-biz/mate-system/src/main/java/vip/mate/system/admin/trigger/controller/StorageController.java)（list/upload/delete/url，依赖 MinioTemplate） + [CodeGenController](../../mate-biz/mate-system/src/main/java/vip/mate/system/admin/trigger/controller/CodeGenController.java)（tables/describe/preview） + [JobController](../../mate-biz/mate-system/src/main/java/vip/mate/system/admin/trigger/controller/JobController.java)（admin-url，前端 iframe XXL-Job）；mate-system pom 增加 mate-file-starter 依赖 |

### P2 — 生态

| 编号 | 项 | 改动 |
|------|---|------|
| **E1** | 多租户业务集成 | [V1.1.3__tenant_schema.sql](../../mate-biz/mate-system/src/main/resources/db/migration/V1.1.3__tenant_schema.sql) — 建 mate_tenant + mate_tenant_package + 给 mate_user/mate_admin/mate_role 加 tenant_id；新建 `vip.mate.system.tenant.*` 子域（DDD 4 层）：[TenantAggregate](../../mate-biz/mate-system/src/main/java/vip/mate/system/tenant/domain/model/aggregate/TenantAggregate.java) / [Tenant](../../mate-biz/mate-system/src/main/java/vip/mate/system/tenant/domain/model/entity/Tenant.java) / [TenantPackage](../../mate-biz/mate-system/src/main/java/vip/mate/system/tenant/domain/model/entity/TenantPackage.java) / [TenantStatus](../../mate-biz/mate-system/src/main/java/vip/mate/system/tenant/domain/model/valobj/TenantStatus.java) / Repository / PO / Dao / [TenantCommandService](../../mate-biz/mate-system/src/main/java/vip/mate/system/tenant/application/command/TenantCommandService.java) / [TenantQueryService](../../mate-biz/mate-system/src/main/java/vip/mate/system/tenant/application/query/TenantQueryService.java) / [TenantController](../../mate-biz/mate-system/src/main/java/vip/mate/system/tenant/trigger/controller/TenantController.java)（CRUD + suspend/activate/renew + listPackages） |
| **E2** | mate-bom | 新建 [mate-bom/pom.xml](../../mate-bom/pom.xml)（packaging=pom，dependencyManagement 列全部 mate 模块）；root pom 加入 modules |
| **E3** | 移动端骨架 | [apps/mobile](../../mate-ui/apps/mobile)：uni-app + Vue 3 + pnpm workspace；package.json / vite.config.ts / tsconfig / pages.json / manifest.json / 3 个页面（login/home/profile）+ index.html |
| **E4** | 桌面端骨架 | [apps/desktop](../../mate-ui/apps/desktop)：Tauri 2 + Vue 3 + Element Plus + Vue Router；前端入口 + 2 个页面（Login/Home）；src-tauri/（Cargo.toml + tauri.conf.json + capabilities + Rust 入口） |

---

## 设计取舍

1. **NoticeDispatcher vs IRpcNoticeService 直接注入** — 选择 dispatcher 抽象，原因：mate-auth 的 outbound port 应当与传输方式（Dubbo / Local）解耦；ObjectProvider 注入让 dev / monolith / 缺失场景全部 graceful。
2. **SmsProvider 不引入 SDK 依赖** — Aliyun / Tencent 都用 JDK HttpClient + 自实现签名，避免单租户场景被强制拉入 90 MB SDK；想要功能更全的客户可自行 override AliyunSmsProvider 子类。
3. **多租户先建表不强制过滤** — V1.1.3 给业务表加 tenant_id 默认值 `'1'`（system tenant），存量代码在 RFC-012 完整集成前仍按单租户运行，避免破坏性变更。
4. **CodeGen REST 不写文件** — 后端只生成代码字符串，文件落盘交给 mate-cli `gen code`；前端 UI 提供复制 / 下载即可，避免给运行中的服务装载文件 IO 风险。
5. **mobile/desktop 骨架仅做最小可运行版本** — 登录 + 首页两屏；具体业务卡片由后续 RFC 在 packages/core 增加平台适配器后再扩展。

---

## 下一步（不在本轮范围）

- DubboNoticeDispatcher 在 application 层缺一个端到端集成测试（Testcontainers + Dubbo），需要 Docker 环境
- TenantContext 与 mate-auth 登录链路尚未串通（Sa-Token 会话注入 tenantId）
- mobile / desktop 的 packages/core 平台适配器（uni.request / fetch 抽象）尚未抽出
- AliyunSmsProvider 模板参数仅识别 `${code}`，复杂模板（订单号、金额等）需要调用方提供 templateParams Map（接口扩展）

这些都已记入 [development-roadmap.md](../conventions/development-roadmap.md) 后续迭代清单。
