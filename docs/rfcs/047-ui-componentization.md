# RFC-047: UI 组件化 + 共享基础设施

- **Status**: Done
- **Created**: 2026-04-15
- **Wave**: FE-6 (post-CRUD completion)
- **Dependencies**: RFC-046 (CRUD pages)

## Context

RFC-046 把前端 CRUD 闭环做完后，审计发现 `packages/ui` 的共享组件只被**部分使用**：

- 6 个视图各自手写 `.search-bar` / `.action-btn` / `.user-cell`
- `<el-dialog>` + `<el-form>` 组合 11 处逐字复刻
- `<el-pagination>` 8 处模板重复
- 导入导出 / 批量操作 / 真数据 Dashboard 这些**基础业务特性**在整个仓库里完全没有

这个 RFC 记录了两轮工作：**① 共享组件库成型**，**② G1/G2/G3/G4 四项业务基础设施落地**。

---

## Part 1 — 共享组件扩展（UI 层）

### 新增组件（packages/ui）

| 组件 | 作用 | 替代的样板 |
|---|---|---|
| `MatePagination` | v-model:page-num/size + `change` 事件，total=0 自动隐藏 | 8 处 `<el-pagination>` + flex 容器 |
| `MateDialog` | 默认 destroy-on-close + lock-scroll=false，Cancel/Confirm 底栏 | 11 处 `<el-dialog>` + `<template #footer>` |
| `MateEntityCell` | 34px 渐变头像/图标 + name + sub | `.user-cell`/`.admin-cell`/`.role-cell` 三份 CSS 拷贝 |
| `MateEmpty` | 空态小组件 | 6 处 `<div style="padding:48px 0;...">` |
| `MateImportExport` | 导出按钮 + 导入弹窗 + 模板下载三合一 | 每视图 ~120 行内联 |
| `MateBatchBar` | 选中 N 项提示条 + slot 动作按钮 | — 新能力 |

### 扩展的已有组件

- `MateForm` — 新字段类型：`password / date / datetime / daterange / datetimerange / custom(slot)` + 动态 `visible(model) / disabled(model)` 谓词
- `MateBadge` — 加 `status` 自动映射（ACTIVE→success、FROZEN→warning、DELETED→danger），log 域加 `domain="log"` 翻转 0/1
- `MateSearchBar` — 加 `#trailing` 插槽（后被回退到 `#actions` 统一位置）

### mc-action-btn 全局样式

`packages/ui/src/styles/action-btn.css` 把 7 份 `.action-btn` 复制合并为一套全局类：
```html
<button class="mc-action-btn mc-action-btn--danger">Delete</button>
```

### 组件化自检

```
class="search-bar"           0 (原 6)
<el-pagination>              0 (原 8)
<el-dialog>                  0 (原 11，DictManager 内部模板弹窗已换 MateDialog)
class="action-btn"           0 (原 7)
.user-cell/.admin-cell/.role-cell  0 (原 3)
```

---

## Part 2 — 业务基础设施（四件套）

### G2 Excel 导出（commit `a21fd2b`）

**后端** `mate-biz/mate-system`：
- `UserExportRow` DTO（6 列：用户名/姓名/手机/邮箱/状态/创建时间）
- `IUserQueryService.exportRows(keyword)` + `EXPORT_MAX=50000` 内存兜底
- `UserController.GET /users/export` + `@ExcelExport(fileName=users, dataClass=UserExportRow)`
- `mate-excel-starter.ExcelResponseBodyAdvice` 拦截 `List<T>` 返回 → 写 xlsx 流

**前端** `packages/core/src/composables/useExport.ts`：
- raw axios（不走 client，保 Content-Disposition）
- 手动注入 `mate_token` / `mate_tenant_id` header
- RFC 5987 + legacy `filename="..."` 解析

### G3 Excel 导入 + 模板（commit `5563d02`、`623fc96`）

**后端**：
- `UserImportRow` DTO + `importBatch(rows)` 单行 `REQUIRES_NEW`
  - `@Lazy self` 自引用绕过 Spring AOP 同类调用陷阱
  - 行级校验（用户名正则 + 手机号 + 密码长度）
  - 失败聚合附 Excel 行号（`Row 5: Username already exists`）
- `POST /users/import` 用 `@ExcelImport(dataClass=UserImportRow.class)` 自动解析
- `GET /users/import/template` 返 `List.of()` 让 EasyExcel 只写表头

**前端** `useImport.ts`：走 client 走拦截器，multipart 不手动设 Content-Type（让浏览器加 boundary）

**复用验证**：AdminList 套用 `UserController` 相同模式，**前端仅 ~12 行** vue 代码即可全功能接入。

### G1 批量操作（commit `293083a`）

**数据契约** `mate-base/result/BatchResult`：
```java
public class BatchResult {
    int successCount;
    int failCount;
    List<BatchFailure> failures; // [{id, message}]
}
```

**后端**：
- `runBatch(ids, Consumer<id> action)` 私有 helper
- 不外包 `@Transactional` — 单行命令自带事务，避免一行失败回滚整批
- UserController `POST /users/batch-{freeze,unfreeze,delete}`
- AdminController `POST /admin/admins/batch-{enable,disable,delete}`

**前端** `useBatch.ts` 两种形态：
- `runBatch(api, ids)` — 后端有真 batch 端点（推荐）
- `runEach(api, ids)` — 循环单条 API 做 fallback

**UI**：`MateBatchBar` slot 驱动，父视图自定义按钮 + `@clear` 发回重置选中。AdminList 已接入验证。

### G4 Dashboard 真数据（commit `be49217`）

**后端** `mate-admin/MonitorController`：
- 聚合 4 源：SQL（admin 总数 / 今日登录/操作 / 7 天趋势）+ Sa-Token Redis（在线会话数）+ HTTP probe（并发请各服务 actuator/health）
- `LoginLogDao.countByDateSince` / `OperationLogDao.countByDateSince` 用 `GROUP BY DATE(created_at)`
- `mate.monitor.services` 可 Nacos 下发：`gateway=http://...,auth=http://...`

**前端**：
- `DashboardTrendChart.vue` 独立 SFC + `defineAsyncComponent` 懒加载
  - 规避 Vite 8 rolldown 在 Windows 打 echarts 时 OOM
  - 产出 `DashboardTrendChart-*.js 475KB / 159KB gzip` 独立 chunk
- `vite.config.ts` 切 `minify: 'esbuild'`（rolldown 不稳）
- 双系列柱状图（API 调用 + 登录次数）+ 服务健康卡片（UP/DOWN/UNKNOWN + 延迟）

---

## 验证

| 维度 | 结果 |
|---|---|
| `pnpm typecheck` | ✅ 0 errors |
| `pnpm build` | ✅ all chunks |
| `pnpm lint` | ✅ 0 errors（88 warnings，全 `any`） |
| `mvn clean compile` | ✅ 18 modules |
| 组件复用自检 | ✅ 5/5 项归零 |
| MateImportExport 复用度 | ✅ 第二次接入仅 12 行 |

---

## 文件清单

### 新增
- `packages/ui/src/MatePagination/MatePagination.vue`
- `packages/ui/src/MateDialog/MateDialog.vue`
- `packages/ui/src/MateEntityCell/MateEntityCell.vue`
- `packages/ui/src/MateEmpty/MateEmpty.vue`
- `packages/ui/src/MateImportExport/MateImportExport.vue`
- `packages/ui/src/MateBatchBar/MateBatchBar.vue`
- `packages/ui/src/styles/action-btn.css`
- `packages/core/src/composables/useExport.ts`
- `packages/core/src/composables/useImport.ts`
- `packages/core/src/composables/useBatch.ts`
- `packages/core/src/api/modules/dashboard.ts`
- `apps/admin/src/views/dashboard/DashboardTrendChart.vue`
- `mate-common/mate-base/.../result/BatchResult.java`
- `mate-biz/mate-system/.../excel/UserExportRow.java`、`UserImportRow.java`
- `mate-admin/.../excel/AdminExportRow.java`、`AdminImportRow.java`
- `mate-admin/.../query/IMonitorQueryService.java` + impl + `MonitorController.java`

### 修改
所有 7 个 CRUD 视图（UserList/AdminList/RoleList/ConfigList/DictManager/LoginLog/OperationLog）整体迁移到共享组件；`MateForm` / `MateBadge` / `MateSearchBar` 扩展；`mate-starters/mate-sa-token-starter` 加 SaInterceptor + RedisStpInterface 让 `@SaCheckPermission` 真正生效。

---

## 遗留 / 后续

### UserList 批量未接
`MateListView` 没有 selection 支持。两条路二选一：
1. 给 `MateListView` 加 selection 列（保留非 table 视觉）
2. 把 UserList 迁 `MateTable`（跟 AdminList 视觉一致）

### 日志写入路径（RFC-049 候选）
`mate_login_log` / `mate_operation_log` 两张表只有读接口，没有写入点：
- 需要 mate-auth 发 `LoginSuccess/LoginFail` 事件
- 需要 web-starter 加 `@AuditLog` AOP 切面拦所有 mutation

### 密码重置弹窗（G5）
`adminApi.adminResetPassword` + `userApi.resetPassword` 后端已就绪，UI 未接。

### 测试覆盖
新增代码 3000+ 行、0 单测。至少应补：
- `BatchResult` 序列化形状
- `importBatch` mock repo 测试
- `MateImportExport` Vitest 快照
