# RFC 066 — CRM 条件查询（动态高级筛选）

状态: Done · 作者: mateaix · 日期: 2026-11-22

## 1. 目标与决策

对标悟空 CRM 的"条件查询"：所有 CRM 列表页支持任意字段组合的高级筛选，并可保存个人常用筛选。四项已确认决策：

1. **全模块铺开** — 12 个标准列表页全部接入（approval 保留 mine/todo 语义不动）。
2. **字段元数据走后端 DB 配置表** — `mate_crm_query_field` 白名单，管理员维护，前端只传逻辑 `fieldKey`。
3. **组间 OR、组内 AND** — 统一查询模型 `AdvancedQuery` 是筛选的单一来源；现有快捷筛选栏的取值在前端折算成普通条件组，后端只支持一套模型。
4. **个人常用筛选** — `mate_crm_saved_filter`，按 user_id 隔离，不做团队共享。

## 2. 数据模型（Flyway）

沿用 `mate_crm_*` 风格（`id VARCHAR(64)` 主键、`tenant_id DEFAULT '1'`、`created_at/updated_at/deleted`）：

| 脚本 | 内容 |
|------|------|
| `V1.6.4__crm_query.sql` | 建 `mate_crm_query_field`（唯一键 `(module_key, field_key)`）+ `mate_crm_saved_filter` |
| `V1.6.5__crm_query_seed.sql` | 11 个 module 各 5-10 个可查字段种子；SELECT 字段带 `options_json`（与页面现有下拉一致） |
| `V1.6.6__crm_query_seed_extra.sql` | 页面接入时发现白名单缺口后补充 7 字段（followup.bizId/customerId、invoice.invoiceNo/contractId、repayment.contractId、aftersale.customerId、task.bizName/content），`INSERT IGNORE` 幂等 |

`query_field` 列：`module_key / field_key(逻辑名) / column_name(真实物理列) / label / field_type(TEXT|NUMBER|DATE|DATETIME|SELECT|USER|DEPT|BOOLEAN) / options_json / sort / visible`。元数据接口**不回传 column_name**，真实列名永不出前端。

## 3. 后端 `vip.mate.crm.query`（DDD 四层）

- **types/enums** — `FieldType`、`FilterOperator`(EQ/NE/LIKE/GT/GE/LT/LE/IN/BETWEEN/IS_NULL/IS_NOT_NULL)，每个 FieldType 声明允许算子集合。
- **domain** — `QueryField`、`SavedFilter` 模型 + `IQueryFieldRepository` / `ISavedFilterRepository` 接口（零框架依赖）。
- **infrastructure** — PO/Dao/RepositoryImpl + **`CrmDynamicQueryBuilder`（安全翻译器，见 §4）**。
- **application** — `QueryFieldService`（元数据本地缓存，低频变更）、`SavedFilterService`（个人 CRUD，`CurrentUserResolver.require().userId()` 隔离）、`AdvancedQueryService`（解析 `CrmScope` 后委派）。
- **trigger/controller** — `GET /api/v1/crm/query-fields?module=`、`GET/POST/DELETE /api/v1/crm/saved-filters`，均 `@SaCheckLogin`。

请求 DTO（`query.dto.AdvancedQuery`）：

```java
List<ConditionGroup> groups;   // 组间 OR，缺省视为单组；组内 conditions AND
String orderField; Boolean orderAsc; Boolean mineOnly;
int pageNum = 1, pageSize = 20;
// ConditionGroup → List<QueryCondition>; QueryCondition → fieldKey + operator + values[]
```

## 4. 安全翻译器（核心）

`CrmDynamicQueryBuilder.build(moduleKey, AdvancedQuery, CrmScope, ownerColumn, deptColumn[, defaultOrderColumn])` → MyBatis-Plus 非 lambda `QueryWrapper<PO>`：

1. **白名单解析** — 每个 condition 用 `(moduleKey, fieldKey)` 查元数据缓存；取不到或不可见 → `CRMB100`（字段不允许查询）；算子与字段类型不匹配 → `CRMB101`。
2. **双重列名防线** — 列名只可能来自内部元数据表，再叠加正则 `^[a-z_][a-z0-9_]*$` 兜底。
3. **值全参数绑定** — 一律经 `eq/like/ge/in/between(...)` 绑定，绝不字符串拼接；注入串只会作为绑定值。
4. **组间 OR / 组内 AND** — `outer.or(inner -> ...)`；BETWEEN 取两值、IN 取多值。
5. **数据权限** — `CrmScope` 新增 QueryWrapper 重载（复用既有 Lambda 版 code 语义）；`mineOnly` → `ownerColumn = 当前用户`；`orderField` 走白名单，非法则回落默认列 DESC。

`CrmSearchSupport.search(dao, moduleKey, query, scope, ownerCol, deptCol)` 返回 `Page<PO>`，供各仓储复用。

## 5. 全模块接入（12 列表）

每个 `XxxRepositoryImpl` 增 `search(AdvancedQuery)`（内部调 `CrmSearchSupport`，用现成 `toEntity` 映射）；`IXxxRepository` / `IXxxQueryService` 同步加方法；`XxxController` 增 `@PostMapping("/search")` + `@SaCheckPermission("crm:xxx:list")`，复用 `PageResult` 响应。覆盖 lead/customer/contact/followup/business/product/contract/repayment(records)/invoice/aftersale/task。

## 6. 前端

- **core** — `types/crm.ts` 增 `QueryFieldMeta/QueryCondition/ConditionGroup/AdvancedQuery/SavedFilter`；`crm.ts` 增 `queryApi` + 各 `xxxApi.search()`。
- **ui/组件** — `MateConditionBuilder`（条件组卡片：字段→算子(按 field_type 过滤)→值控件）；`CrmAdvancedFilter.vue`（拉元数据 + 常用筛选下拉：套用/另存/删除）；`useCrmAdvancedQuery.ts`（`buildAdvancedQuery/hasValidAdvanced`：把快捷栏 keyword 按模块扇出列 × 用户组折算，base 条件 AND 折叠进每个输出组）。
- **12 列表页** — 搜索栏尾部挂 `CrmAdvancedFilter`；`loadData` 混合分流：有合法高级条件走 `xxxApi.search(buildAdvancedQuery(fold))`，否则维持 `xxxApi.page({...})`；重置同时清 `advGroups`。AcceptanceList 高级筛选仅作用 records 页签（moduleKey `repayment`）。
- 不新增菜单节点；`menu-manifest.yml` 不动。

## 7. 验证结果（2026-11-22）

- **单测** — `CrmDynamicQueryBuilderTest` 9 用例（LIKE 绑定、注入串只进绑定值、非法/不可见字段 CRMB100、SELECT 禁 LIKE CRMB101、组 OR/组内 AND、BETWEEN→BigDecimal、mineOnly、orderField 白名单、缺省排序）+ `SavedFilterServiceTest` 4 用例（归属强制、个人隔离、跨用户改删 CRMB002、本人覆盖）；`mvn test -pl mate-biz/mate-crm` 57 用例全绿。
- **构建** — `mvn -o clean install -Pmonolith -DskipTests` 通过；`vue-tsc --noEmit` 零错误。
- **冒烟** — `scripts/crm-e2e-smoke.ps1` 扩展 RFC-066 段并全绿：`QUERY-FIELDS OK count=9` → `ADV-SEARCH OK`（组内 AND + 组间 OR + BETWEEN）→ `ADV-SEARCH-DENY OK code=CRMB100`（fieldKey=password 被白名单拒绝）→ 常用筛选存/取/删 → `QUERY-SMOKE PASS`；主流程 `FLOW-CHECK PASS` 证明旧链路零回归。

## 8. 踩坑记录

- **Jackson 派生 getter 陷阱** — `AdvancedQuery.isEmpty()` 被序列化为 `"empty"` 属性；Spring 注入的宽松 ObjectMapper 掩盖了它，但 `new ObjectMapper()`（saved-filter JSON 往返）严格反序列化直接失败。修复：计算属性加 `@JsonIgnore`。DTO 上的任何非字段 getter 都应默认加。
- **旧 jar 静默替换失败** — Windows 上运行中的 `java -jar` 会锁 fat jar，spring-boot repackage 报 BUILD SUCCESS 但磁盘 artifact 未更新，导致冒烟打到旧代码（NoHandlerFound）。重建 monolith 前先停进程，构建后核验 jar 内 `BOOT-INF/lib/mate-crm-*.jar` 含新类。
- **PowerShell 单引号串不解 `\u0022`** — raw JSON 体含中文/引号时用单引号 here-string（`@' ... '@`）直写，别依赖转义；断言非 2xx 业务错误码需 `-SkipHttpErrorCheck`。
