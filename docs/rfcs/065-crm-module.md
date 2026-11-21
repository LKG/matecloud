# RFC 065 — mate-crm CRM 全流程模块（对标悟空 CRM）

状态: 已实施 · 作者: mateaix · 日期: 2026-11-21

## 1. 背景与目标

参考 Wukong CRM Pro（销售主线 + 审批 + 产品/合同/回款 + 数据权限）与 Wukong-AICRM（AI 客户洞察、智能任务、对话式查询），新增可插拔业务模块 `mate-biz/mate-crm`（端口 **9070**，模块码 `crm`），覆盖线索 → 客户 → 商机 → 合同 → 回款 → 发票 → 售后 → 审批 → 工作台全链路，并纳入 mate-monolith 单体模式（主验证路径）。

严格遵循 `docs/conventions/pluggable-module-guide.md` 可插拔五要素：

1. 独立 Flyway 版本线 `V1.6.x`（18 张表，H2/MySQL 双方言、幂等 `CREATE TABLE IF NOT EXISTS`）
2. `menu-manifest.yml` 菜单自注册（CRM 顶级菜单 + 15 页面 + 按钮 perms）
3. `gateway-path` 元数据自声明（Nacos discovery metadata：`/api/v1/crm/**`）
4. 经 `mate-api` DTO 解耦（RPC 仅用于 notice 通知发送等跨模块能力）
5. DDD 四层（trigger / application / domain / infrastructure），包根 `vip.mate.crm`

## 2. 领域建模：7 个子上下文

| 子上下文 | 聚合 | 表（前缀 `mate_crm_`） |
|---|---|---|
| lead 线索 | LeadAggregate | lead |
| customer 客户 | CustomerAggregate（联系人/跟进/公海流转为其内部实体与流转规则）、ContactAggregate、FollowUpAggregate、PoolConfigAggregate | customer / contact / follow_up / pool_config |
| business 商机 | BusinessAggregate（阶段推进 + 商机产品行）、ProductAggregate | business / business_product / product |
| trade 交易 | ContractAggregate(合同+签单产品行，金额=行合计不变式)、PaymentPlanAggregate、RepaymentAggregate、InvoiceAggregate | contract / contract_product / payment_plan / repayment / invoice |
| aftersale 售后 | AfterSaleAggregate（工单 + 处理日志） | after_sale / after_sale_log |
| approval 审批 | ApprovalAggregate（通用审批实例：类型+业务 id+节点链） | approval / approval_record |
| workbench 工作台 | TaskAggregate（待办，AI 可生成）、StatsQueryService（读模型直查） | task |

共 **18 张表**。所有表含 `id / creator_id / dept_id / tenant_id / create_time / update_time / deleted` 基础列，为数据范围过滤与多租户服务。

### 2.1 核心流程（状态机内聚在聚合根）

```
线索          录入/编辑 ──跟进──▶ 转化(幂等建客户+联系人, 回填 converted_customer_id) / 废弃
客户  领取(公海→私海) / 释放 / 转移负责人 / 报备查重(名称+手机号)
      job: N 天无跟进自动回收(读 mate_crm_pool_config)
商机  创建(关联客户+产品行) ─▶ 初次接触 ▶ 需求确认 ▶ 方案报价 ▶ 商务谈判 ▶ 赢单/输单
      (阶段只许逐级推进; 赢单后可转合同)
合同  赢单商机转草稿 ─▶ 提交审批(主管→财务, 两级可配) ─▶ 通过归档生效 / 驳回回草稿
回款  合同生成回款计划(分期) ─▶ 回款登记 ─▶ 累计已回款 / 逾期提醒(job)
发票  开票申请(关联合同+回款) ─▶ 开票 / 驳回 / 作废
售后  工单创建(关联客户/合同) ─▶ 受理 ─▶ 处理(记日志) ─▶ 完成 ─▶ 评价(1-5) ─▶ 关闭
AI    客户 360 智能摘要 + 风险提醒 + 下一步建议(@Tool 对话式查询 + 专用 insight 端点)
看板  今日待办 / 销售漏斗 / 业绩·回款趋势 / 成员排行(读模型, 绕过聚合)
```

### 2.2 状态机定义（枚举 + 迁移由聚合根方法守卫）

- **Lead**：`FOLLOWING → CONVERTED | INVALID`（convert 幂等：已转化直接返回既有 `converted_customer_id`）
- **Customer**：`INITIAL → NEGOTIATING → OPPORTUNITY → DORMANT → CHURNED`；`pool_status 1公海/0私海` 独立于状态
- **Business**：阶段 `INITIAL → DEMAND → PLAN → NEGOTIATION → WIN | LOSE`；`advanceStage` 仅允许 +1 级，终态不可迁移
- **Contract**：`DRAFT → PENDING → APPROVED/REJECTED → ARCHIVED`（提交=创建审批实例；审批回调驱动状态）
- **Approval**：`PENDING → APPROVED | REJECTED | CANCELED`；节点链 `ApprovalNode[]` 顺序推进，`ApprovalRecord` 记录每人每级动作
- **PaymentPlan**：`0待回款 → 1部分回款 → 2已结清`，`due_date < now 且未结清 → 3逾期`（job 计算）
- **Invoice**：`0已申请 → 1已开票 | 2已驳回`，`→ 3已作废`
- **AfterSale**：`OPEN → PROCESSING → RESOLVED → CLOSED`，评价仅 RESOLVED/CLOSED 可提交
- **Task**：`OPEN → DONE | CANCELED`

### 2.3 关键不变式

- 合同金额 = Σ 签单产品行小计（`ContractAggregate` 内重算，行小计 = 单价 × 数量 × 折扣）
- 计划回款额 ≤ 合同金额；累计回款 ≤ 计划回款（回款登记时校验）
- 客户/商机/合同删除为逻辑删（`deleted`），已被下游引用的实体禁止删除
- 手机号 + 客户名称查重在创建/转移时校验（私海范围内唯一）

## 3. 数据权限与审批链取舍

- **数据权限**：弃用 `@DataPermission` 注解 SQL 改写（跨方言不可控），改为显式范围过滤：`CrmCurrentUser/CurrentUserResolver` 解析登录态 + 角色数据范围（本人/部门/全部），查询侧统一注入 `creator_id / dept_id` 条件（`CrmScope`），分页参数 `mineOnly` 支持「只看我的」。
- **审批流**：不引入重型 BPM（mate-flow-starter 骨架不足以支撑两级会签场景），在 `crm/approval` 上下文自研通用两级状态机（`ApprovalChainProvider` 解析审批人：部门主管 → 管理员兜底，可配置级数）；后续可平移到 flow-starter。
- **通知**：公海回收/逾期提醒 job 经 `mate-notice` RPC 发站内信；单体模式走本地 bean（`Local*Adapter` 优先，Dubbo 关闭时可用地）。

## 4. API 清单（REST `/api/v1/crm/**`，返回 `Result<T>`）

| 分组 | 端点 |
|---|---|
| 线索 `/leads` | GET page、GET /{id}、POST、PUT /{id}、PUT /{id}/convert、PUT /{id}/discard、PUT /{id}/owner、PUT /{id}/follow、DELETE /{id} |
| 客户 `/customers` | GET page、GET /{id}、GET /{id}/detail、POST、PUT /{id}、POST /{id}/claim、POST /{id}/release、POST /{id}/transfer、DELETE /{id} |
| 联系人 `/contacts` | GET page、GET /{id}、POST、PUT /{id}、PUT /{id}/primary、DELETE /{id} |
| 跟进 `/followups` | GET page、GET /{id}、POST（save upsert）、DELETE /{id} |
| 公海规则 `/pool-config` | GET、PUT |
| 商机 `/businesses` | GET page、GET /{id}(detail)、POST、PUT /{id}、PUT /{id}/stage、PUT /{id}/win、PUT /{id}/lose、DELETE /{id} |
| 产品 `/products` | GET page、GET /on-sale、GET /{id}、POST、PUT /{id}、PUT /{id}/status、DELETE /{id} |
| 合同 `/contracts` | GET page、GET /{id}(detail)、POST、POST /from-business/{businessId}、PUT /{id}、PUT /{id}/submit、PUT /{id}/archive、DELETE /{id} |
| 回款 `/repayments` | GET /plans、GET /contracts/{cid}/plans、POST /plans、PUT /plans/{id}、DELETE /plans/{id}、GET（记录 page）、GET /plans/{pid}/records、POST（登记）、DELETE /{id} |
| 发票 `/invoices` | GET page、GET /{id}、POST（申请）、PUT /{id}/issue、PUT /{id}/reject、PUT /{id}/void、DELETE /{id} |
| 售后 `/aftersales` | GET page、GET /{id}(detail)、POST、PUT /{id}/accept、PUT /{id}/process、PUT /{id}/resolve、PUT /{id}/close、PUT /{id}/rate、DELETE /{id} |
| 审批 `/approvals` | GET /mine、GET /todo、GET /{id}(detail)、POST /contract/{contractId}、PUT /{id}/approve、PUT /{id}/reject、PUT /{id}/cancel |
| 待办 `/tasks` | GET page、GET /today、GET /{id}、POST、PUT /{id}、PUT /{id}/done、PUT /{id}/cancel、DELETE /{id} |
| 统计 `/stats` | GET /overview、GET /funnel、GET /member-ranking、GET /trend |
| AI `/ai` | GET /insight/{customerId} |

错误码：`CrmErrorCode`，格式 `CRMB001`（域内唯一）。

## 5. AI 集成策略（单体 / 微服务差异）

- `CrmCopilotTools`（`@Tool`：查客户摘要、商机停滞列表、生成跟进建议、AI 生成待办）自动注册进同 JVM 的 `AiToolRegistry`。
- **单体模式（主验证路径）**：mate-ai 与 mate-crm 同进程，`/api/v1/ai/chat` 对话页直接可调用 crm 工具（`-Pmonolith` 构建后 `/api/v1/ai/tools` 可见）。
- **微服务模式**：mate-ai 独立 JVM 看不到 crm 工具；CRM 专属分析走自己的 `/api/v1/crm/ai/insight/{customerId}`（`CrmInsightService` 直调 LLM 能力端口），对话式全量能力需后续经 MCP/HTTP 工具桥接（本期不做）。

## 6. 中心文件改动（5 处）

- `mate-biz/pom.xml`：`<module>mate-crm</module>`
- `mate-monolith/pom.xml` + `MateMonolithApplication`：依赖 + `excludeFilters` 排除 `MateCrmApplication`
- `mate-gateway/application.yml`：兜底路由 `mate-crm → lb://mate-crm, Path=/api/v1/crm/**`
- `docker-compose.yml`：mate-crm 服务（9070）
- **mate-menu-starter 单体兼容增强**：`MenuAutoRegistrar` 注入 `ObjectProvider<IRpcMenuRegistry>`，本地 bean 优先、RPC 回退；mate-monolith 新增 `LocalMenuRegistryAdapter` 委托 mate-system 菜单落库服务

pom 特殊点：`spring-boot-maven-plugin` 配 `<classifier>exec</classifier>`（双角色模块：自身 exec jar + 被 monolith 以普通 jar 依赖）。

## 7. 前端交付物（mate-ui）

- `packages/core`：`types/crm.ts`（全量类型）、`api/modules/crm.ts`（15 个分组 api + `crmApi` 聚合）、`endpoints.ts` crm 端点组、`index.ts` 导出
- `apps/admin/src/views/crm/` 15 页面 + 2 个 echarts 懒加载图表（`FunnelChart`/`TrendChart`，`defineAsyncComponent` 拆分独立 chunk 规避 rolldown Windows 打包 OOM）：
  Dashboard / LeadList / CustomerList / CustomerDetail / ContactList / FollowUpList / BusinessList（列表+看板双视图）/ ProductList / ContractList / AcceptanceList（计划+回款双 tab）/ InvoiceList / AfterSaleList（处理抽屉+时间线）/ ApprovalCenter（我发起/待我审批）/ PoolConfig / TaskList
- `router/dynamic.ts` VIEW_MAP 注册 15 条路径，与 menu-manifest path 一一对应
- 页面基于 `@matecloud/ui` Mate* 组件族 + `v-permission` 按钮权限；AI 对话复用既有 `/ai/chat`

## 8. 分期路线

| 期 | 内容 | 状态 |
|---|---|---|
| P1（本期） | 7 上下文 18 表 + 15 页面 + AI 工具/洞察 + 2 job + 菜单自注册 + 单体接入 | ✅ |
| P2 | 营销模块（活动/回访）、审批链可视化配置、flow-starter 平移 | 计划 |
| P3 | 微服务模式下 AI 工具桥接（MCP）、外部通道（短信/AI 外呼）复用 starter | 计划 |

## 9. 验证（已于 2026-11-21 全部执行通过）

- 后端：`mvn test -pl mate-biz/mate-crm` 44 项聚合状态机单测通过（仓储修复后复跑仍全绿）✅
- 前端：`vue-tsc --noEmit` 零错误（含 ContractList 提交审批编排修复后复检）✅
- 单体：`mvn clean install -Pmonolith -DskipTests` 构建通过；启动后 Flyway 迁移至 v1.6.3（18 表 + product_id 约束放宽）；日志 `[menu] aggregated 1 top-level node(s)` + `self-registered 58 nodes`，`mate_menu` 落库 `/crm/*` 路径 15 行 + `crm:*` 权限 57 行✅
- 冒烟（纯脚本登录：滑块验证码 AES-ECB + 传输密码 AES-CFB128）：主链路 PASS + 审批闭环 FLOW-CHECK PASS——客户→商机(带产品行)→逐级赢单→转合同(沿用产品行合计为合同额)→submitContract 建两级审批→approve×2→状态 APPROVED→archive→回款计划 500→登记 200→overview 回款率 20%→AI 洞察✅
- AI：`GET /api/v1/ai/tools` 含 crm 工具（createFollowUpTask/summarizeCustomer/stalledBusinesses/followUpSuggestions 等，单体进程内注册）✅

验证过程中发现并修复的三处缺陷：
1. 子表仓储「删后重插」与 `@TableLogic` 软删残留行主键冲突（审批记录/售后日志改仅追加，商机/合同产品行改差量同步）
2. `ContractList.vue` 提交审批误调 `contractApi.submit`（不建审批实例）→ 改 `approvalApi.submitContract`
3. `mate_crm_business_product.product_id` NOT NULL 与目录外自由行项矛盾 → V1.6.3 迁移放开

待验（需人工）：`pnpm dev` 浏览器逐页联调 CRM 菜单/CRUD/审批/看板/AI。
