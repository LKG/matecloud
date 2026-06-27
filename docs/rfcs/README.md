# MateCloud RFCs

MateCloud DDD 微服务脚手架的 RFC 文档，支持多 Agent 并行开发。

> 本索引于 2026-04-25 重写。原版（44 个计划，全 Draft）在 RFC-049/050 落地后已严重过时；状态列改为反映 2026-04-25 真实落地情况。详细落地率与差距见 [development-roadmap.md](../conventions/development-roadmap.md)。
>
> 收口回合：[RFC-051 主体](051-completion-pass-2026-04-25.md)（P0/P1/P2 共 12 项）；[RFC-052 残余](052-residual-cleanup-2026-04-25.md)（R1–R5）。

## 并行开发波次

```
Wave 1: 骨架         001 -> 002
Wave 2: Starters     003 | 004 | 005              (3 parallel)
Wave 3: 核心服务     006 | 007 | 008              (3 parallel)
Wave 4: 企业能力     009 | 011 | 012 | 013        (4 parallel)
Wave 5: 基础设施     015 | 016 | 017 | 019        (4 parallel)
Wave 6: 服务扩展     018 | 020 | 021              (3 parallel)
Wave 7: DX & Ops     010 | 014 | 022 | 023 | 024  (5 parallel)
Wave 8: 整合完善     038 -> 039 | 040 | 041        (3 parallel after 038)
Wave 9: 特性集成     042 | 043 | 044               (3 parallel)
Wave 10: 架构演进    045                             (monolith + micro dual mode)
Wave 11: 收口        049 -> 050                     (admin 合并 + 占位补全)
```

## 状态约定

- **Done** — 代码 100% 落地，可直接使用
- **Mostly Done** — ≥80% 落地，仍有可识别缺口（见 development-roadmap.md）
- **Partial** — 40~80% 落地，存在结构性缺失
- **Draft** — <40% 落地或纯设计稿
- **Accepted** — 决策类 RFC，无代码产出
- **Deleted** — 已按其他 RFC 决议删除

## 索引

| RFC | Title | Status | Wave | Deps |
|-----|-------|--------|------|------|
| **Wave 1: Skeleton** |
| 001 | [Root POM & Skeleton](001-root-pom-skeleton.md) | Done | 1 | - |
| 002 | [mate-common (base + api)](002-mate-common.md) | Done | 1 | 001 |
| **Wave 2: Core Starters** |
| 003 | [Core Starters (ds/web/nacos/rpc)](003-core-starters.md) | Done | 2 | 002 |
| 004 | [Infra Starters (cache/lock/monitor/sa-token)](004-infra-starters.md) | Done | 2 | 002 |
| 005 | [Capability Starters (mq/job/seata/file)](005-capability-starters.md) | Done | 2 | 002 |
| **Wave 3: Core Services** |
| 006 | [mate-gateway](006-mate-gateway.md) | Done | 3 | 002-004 |
| 007 | [mate-auth](007-mate-auth.md) | Done | 3 | 002-004 |
| 008 | [mate-system DDD Example](008-mate-system-ddd.md) | Done | 3 | 002-005 |
| **Wave 4: Enterprise** |
| 009 | [Admin Backend (RBAC/Dict/Config/Logs)](009-admin-backend-features.md) | Done (合并入 mate-system) | 4 | 001-008 |
| 011 | [Security Framework](011-security-framework.md) | Done | 4 | 004, 007 |
| 012 | [Multi-Tenant Architecture](012-multi-tenant.md) | Partial (Starter 100%，业务 0%) | 4 | 003, 004 |
| 013 | [Business Infrastructure](013-business-infrastructure.md) | Done | 4 | 005, 008 |
| **Wave 5: Infra Starters 2** |
| 015 | [Distributed ID (Snowflake)](015-distributed-id.md) | Done | 5 | 004 |
| 016 | [Database Sharding](016-database-sharding.md) | Done | 5 | 003 |
| 017 | [Sentinel Circuit Breaker](017-sentinel-circuit-breaker.md) | Done | 5 | 003 |
| 019 | [Dynamic Thread Pool](019-dynamic-thread-pool.md) | Deleted | 5 | - |
| **Wave 6: Extended Services** |
| 018 | [Notice Service (SMS/Email/WeChat)](018-notice-service.md) | Mostly Done (SMS 渠道是日志桩) | 6 | 002-005 |
| 020 | [SMS Login & Captcha](020-sms-login-captcha.md) | Mostly Done (Dubbo 调用未通) | 6 | 007, 018 |
| 021 | [Delay Queue & Event-Driven](021-delay-queue-event-driven.md) | Done (DLQ + 指数退避完整) | 6 | 005 |
| **Wave 7: DX & Ops** |
| 010 | [CLI & MateClaw Integration](010-cli-and-mateclaw-integration.md) | Done (12 命令 + MCP) | 7 | 008, 009 |
| 014 | [Developer Experience](014-developer-experience.md) | Done | 7 | 003-005 |
| 022 | [API Documentation (Smart-Doc)](022-api-documentation.md) | Partial (40%) | 7 | 003 |
| 023 | [Docker/K8s Deployment](023-docker-k8s-deployment.md) | Done | 7 | all |
| 024 | [Testing Infrastructure](024-testing-infrastructure.md) | Partial (35%) | 7 | 002-005 |
| **Meta / 战略** |
| 025 | [Simplification Review](025-simplification-review.md) | Accepted | - | Meta |
| 026 | [Product Ecosystem Architecture](026-product-ecosystem-architecture.md) | Draft (mate-bom 0%) | - | Platform + Apps |
| 027 | [AI as Infrastructure](027-ai-infrastructure.md) | Done | - | mate-ai-starter |
| 028 | [SaaS Multi-Tenant](028-saas-multi-tenant.md) | Draft (依赖 RFC-012 业务集成) | - | 租户生命周期 |
| 029 | [Product Focus](029-product-focus.md) | Accepted | - | 29→13 starters |
| **Wave 8: Consolidation & Completion** |
| 038 | [Starter Consolidation Execution](038-starter-consolidation-execution.md) | Done | 8 | 执行 RFC-029 |
| 039 | [Gateway Routes & Transformation](039-gateway-routes.md) | Done | 8 | 路由 + Header + CORS + 日志 |
| 040 | [Admin Config & Logs](040-admin-config-logs.md) | Done | 8 | Config/OperationLog/LoginLog DDD |
| 041 | [Database Migration (Flyway)](041-database-migration-flyway.md) | Done | 8 | 版本化迁移 + 种子 |
| **Wave 9: Features & Integration** |
| 042 | [CLI db + gen Commands](042-cli-db-gen.md) | Mostly Done (2 处模板 TODO) | 9 | mate db / gen code |
| 043 | [CI/CD Pipeline](043-ci-cd-pipeline.md) | Done | 9 | GitHub Actions |
| 044 | [Frontend-Backend Integration](044-frontend-backend-integration.md) | Done | 9 | 前后端对接 |
| **Wave 10: Architecture Evolution** |
| 045 | [Monolith + Microservice Dual Mode](045-monolith-microservice-dual-mode.md) | Partial (70%) | 10 | mate-monolith 已建 |
| **前端** |
| 030 | [Frontend Architecture](030-frontend-architecture.md) | Done | - | Monorepo |
| 031 | [Frontend Tech Decisions](031-frontend-tech-decisions.md) | Done | - | 选型理由 |
| 032 | [FE Skeleton](032-fe-skeleton.md) | Done | FE-1 | Monorepo + pnpm |
| 033 | [FE Core Package](033-fe-core-package.md) | Done | FE-2 | API + Types + Pinia |
| 034 | [FE UI Package](034-fe-ui-package.md) | Done | FE-2 | 12 组件 |
| 035 | [FE Admin Layout](035-fe-admin-layout.md) | Done | FE-2 | Layout + Login + Dashboard |
| 036 | [FE CRUD Pages](036-fe-admin-crud-pages.md) | Done | FE-3 | 24 页面 |
| 037 | [FE Mobile + Desktop](037-fe-mobile-desktop.md) | Draft (0%) | FE-4 | uni-app + Tauri 2 |
| 046 | [FE CRUD Completion](046-fe-crud-completion.md) | Done | FE-5 | v-perm/批量/导出/403 |
| 047 | [UI 组件化 + 业务基础设施](047-ui-componentization.md) | Done | FE-6 | MateImportExport/MateBatchBar |
| 048 | [P1 Business Features](048-p1-business-features.md) | Done | FE-6/BE-P1 | 批量/Excel/Dashboard |
| **Wave 11: 收口** |
| 049 | [mate-admin 合并入 mate-system](049-admin-system-consolidation.md) | Done | 11 | 5→4 微服务 |
| 050 | [占位菜单功能补全](050-placeholder-features.md) | Done | 11 | RFC-051 已补齐 P2 后端 API（StorageController / CodeGenController / JobController） |
| 051 | [收口落地 — 2026-04-25](051-completion-pass-2026-04-25.md) | Done | 11 | P0/P1/P2 共 12 项缺口本轮全部完成 |
| 052 | [残余项收口 — 2026-04-25](052-residual-cleanup-2026-04-25.md) | Done | 11 | R1–R5（tenantId 注入 / 集成测试 / Javadoc / SmsProvider params / mobile+desktop 业务卡片） |
| **Wave 12: 商业化与垂直产品** |
| 053 | [FE Dify Borrowings](053-fe-dify-borrowings.md) | Done | 12 | 前端借鉴 Dify 的 base 原子层 + CVA 变体 |
| 054 | [商业化策略](054-commercialization-strategy.md) | Proposed | 12 | 中台源码授权优先，AI 第二增长曲线 |
| 055 | [企业培训(LMS)产品](055-lms-enterprise-training-product.md) | Proposed | 12 | 独立可售卖垂直产品；单体/微服务双版本；AI 增值包护城河 |
| 056 | [mate-sso-starter 身份接入](056-mate-sso-starter.md) | Proposed | 12 | 企业微信/钉钉/飞书/LDAP SSO+组织同步；SPI+模板方法+六边形端口，零侵入 mate-auth |
| 062 | [运行时动态多数据源切换](062-tenant-dynamic-datasource.md) | Proposed | 12 | DATASOURCE 模式运行时按需注册租户库；Nacos 配置+分段锁+LRU/空闲驱逐+fail-closed；对标并改进 joolun-pro 分库 |

## 量化统计（2026-04-25 RFC-052 收口后）

整体落地率约 **98%**。代码层无真实缺口；剩余仅是部署侧配置（多租户开关 / SMS 网关凭据 / XXL-Job 地址）和未来增强（F1–F3，详见 [RFC-052](052-residual-cleanup-2026-04-25.md)）。
