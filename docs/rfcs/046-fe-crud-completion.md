# RFC-046: Frontend CRUD Completion Plan

- **Status**: Draft
- **Created**: 2026-04-14
- **Wave**: FE-5 (after 036 CRUD Pages)
- **Dependencies**: RFC-032..036, RFC-040 (admin config/logs), RFC-044 (FE-BE integration)
- **Scope**: 把"基础开源脚手架前端核心 CRUD"补齐到可发布状态

---

## 0. Why this RFC

RFC-036 交付了 User / Role / Admin / Menu / Dict / Profile 等主要页面，但当前仓库里还有：
- 只完成一半的页面（DictManager 只读、Logs 分页假、Profile 改密 TODO）
- 没做的公共能力（按钮级权限、Tab 缓存、导入导出、批量操作）
- 必备但缺页面（在线用户、服务器监控、任务、文件、通知、租户）
- Dashboard 指标全写死

这个 RFC 把"一个基础开源版本要交付给外部用户"缺的部分列清楚、排好优先级。不新增"大而全"的模块，只把脚手架**核心 CRUD 闭环**补完。

---

## 1. Current State Snapshot

| 页面 / 能力 | 路径 | 状态 | 缺失点 |
|---|---|---|---|
| UserList | `views/system/UserList.vue` | ✅ 基本可用 | 无角色分配、无密码重置、无状态筛选、无批量 |
| RoleList | `views/admin/RoleList.vue` | ✅ + ⚠️ | 依赖本地 mock 分页、没有状态启/禁用 |
| AdminList | `views/admin/AdminList.vue` | ✅ 基本可用 | 无密码重置、无状态筛选 |
| MenuTree | `views/admin/MenuTree.vue` | ✅ 基本可用 | 无拖拽排序、无节点可见性/状态切换 |
| DictManager | `views/admin/DictManager.vue` | ⚠️ 只读 | **只能按类型查**；缺 DictType CRUD、DictData CRUD 双面板 |
| ConfigList | `views/system/ConfigList.vue` | ⚠️ 简易 | 无搜索/分页、没有 mock、硬编码英文 |
| LoginLog | `views/monitor/LoginLog.vue` | ⚠️ | `total=100` 写死、无日期筛选、无详情 |
| OperationLog | `views/monitor/OperationLog.vue` | ⚠️ | 同上；缺 request/response 详情 |
| Profile | `views/profile/Profile.vue` | ⚠️ | `// TODO: change-password API` 未接 |
| Dashboard | `views/dashboard/Dashboard.vue` | ⚠️ | stats / chartData 全写死 |
| Placeholder 页 | `views/common/PlaceholderView.vue` | ✅ | 由 dynamic.ts 兜底 |
| 在线用户 | — | ❌ | 菜单存在，无实现 |
| 服务器状态/缓存/接口文档 | — | ❌ | 菜单存在，无实现 |
| 定时任务 | — | ❌ | 无 |
| 文件/存储 | — | ❌ | 无 |
| 通知中心 | — | ❌ | 无 |
| 租户管理 | — | ❌ | `mate-tenant-starter` 已有，前端无 |
| 代码生成 | — | ❌ | 菜单存在，无实现 |

### 横切能力

| 能力 | 状态 | 说明 |
|---|---|---|
| 动态路由 | ✅ | `router/dynamic.ts` 做了 path → component 映射 |
| Mock 兜底 | ✅ 部分 | `packages/core/src/api/mock/data.ts` 只覆盖了 auth / menu / admin / role / user / dict-by-type |
| 按钮级权限 `v-perm` | ❌ | auth store 已存 `permissions`，但没有 Vue 指令消费 |
| Tab 路由缓存 | ⚠️ | `stores/tabbar.ts` 已有，但 `<router-view>` 外层没 `<keep-alive>` |
| 403 / 401 | ❌ | 只有 404；权限不足直接 ElMessage |
| 批量操作 | ❌ | 所有列表都单行操作 |
| Excel 导入/导出 | ❌ | 后端 `mate-excel-starter` 已有 |
| MateForm 统一封装 | ⚠️ | 包已提供，页面里仍直接用 `el-form` |
| i18n 覆盖 | ⚠️ | ConfigList / Logs 大量硬编码英文 |

---

## 2. Goals (这个 RFC 交付什么)

**必做 (P0)** — 阻塞"可发布的开源脚手架"：

1. **DictManager 双面板** — Type 列表 + Data 列表，两侧各自 CRUD
2. **Logs 真分页** — 后端 `total` 接入；新增日期范围、详情弹窗
3. **ConfigList 对齐 UserList 标准** — 搜索 + 分页 + mock + i18n
4. **Profile 改密接线** — 后端 `PUT /user/password` / `/admin/admins/{id}/password`
5. **按钮级权限 `v-perm` 指令** — 覆盖所有 "新建 / 编辑 / 删除 / 分配" 按钮
6. **Mock 扩展** — configs / logs / dict-types / online-users，保证"离线也能跑"
7. **全局 403 页面** + 路由守卫与 interceptor 联动

**应做 (P1)** — 让 CRUD 体验完整：

8. **Tab keep-alive** — 切 Tab 不重新加载列表
9. **批量删除 / 批量状态切换** — UserList / RoleList / AdminList
10. **Excel 导入导出** — UserList 先落地，抽成 `useExportExcel()` composable
11. **MateForm 统一** — 把 UserList / AdminList / ConfigList 的弹窗表单迁到 `@matecloud/ui` 的 MateForm，减少重复
12. **Dashboard 真实数据** — 对接 actuator / operation-log 统计
13. **密码重置弹窗** — UserList / AdminList 右侧操作

**不做 (Out of scope)** — 留到独立 RFC：

- ❌ 定时任务 UI  → RFC-047 (XXL-Job 前端)
- ❌ 文件管理 UI  → RFC-048 (Storage UI)
- ❌ 通知中心 UI  → RFC-049 (Notice Center)
- ❌ 租户管理 UI  → RFC-050 (Tenant UI)
- ❌ 代码生成 UI  → RFC-051 (CodeGen UI)
- ❌ 服务器监控（actuator/缓存） → RFC-052 (Server Monitor)

这些功能后端 starter 已存在，前端复用本 RFC 建立的"v-perm / MateForm / 批量 / 导入导出"基础即可。

---

## 3. Detailed Plan

### 3.1 DictManager 双面板 (P0)

**页面结构:**

```
┌──────────────────────┬────────────────────────────┐
│ DictType List (左)    │ DictData List (右, 选中后) │
│  - 新增 / 编辑 / 删除 │   - 新增 / 编辑 / 删除     │
│  - 搜索 dictName      │   - 搜索 label             │
└──────────────────────┴────────────────────────────┘
```

**后端对齐 (需要 RFC-040 完成):**

```
GET    /admin/dict/types            分页
POST   /admin/dict/types
PUT    /admin/dict/types/{id}
DELETE /admin/dict/types/{id}

GET    /admin/dict/data?typeId=…    分页
POST   /admin/dict/data
PUT    /admin/dict/data/{id}
DELETE /admin/dict/data/{id}
```

**前端改动:**

- `packages/core/src/api/modules/admin.ts` 补 `dictTypeList / dictTypeCreate / …` 和 `dictDataList / …`
- `packages/core/src/types/admin.ts` 补 `DictType / CreateDictTypeCommand / CreateDictDataCommand`
- `views/admin/DictManager.vue` 重写为双面板
- `packages/core/src/api/mock/data.ts` 补 mock handler

### 3.2 Logs 真分页 + 详情 (P0)

**OperationLog:**
- 搜索区加 `el-date-picker type="datetimerange"`
- `logApi.operationLogs` 返回 `{ list, total }`（mock 也要跟上）
- 每行新增「详情」按钮，弹窗展示 `params / result / errorMsg`，大字段用 `<pre>` + 折叠
- 表格右上导出按钮（P1）

**LoginLog:**
- 同上加日期范围；详情弹窗展示 `userAgent / failMsg / clientIp / location`

### 3.3 ConfigList 升级 (P0)

- 搜索区：`configKey`、`configName` 模糊；内置/自定义筛选
- 分页
- 内置配置删除按钮禁用（已有），同步禁用"修改 key / name"
- `packages/core/src/api/mock/data.ts` 补 config 列表 mock
- i18n：把所有字面量提进 `locales/*/config.json`

### 3.4 Profile 改密接线 (P0)

- `packages/core/src/api/modules/user.ts` 新增 `changePassword({ oldPassword, newPassword })`
- `Profile.vue` 移除 `TODO`，调用 `userApi.changePassword(...)`
- 成功后清空表单；401 拦截 → 重登录

### 3.5 `v-perm` 按钮权限指令 (P0)

**实现:** `packages/core/src/directives/perm.ts`

```ts
// usage: <el-button v-perm="'sys:user:delete'">Delete</el-button>
//        <el-button v-perm="['sys:user:delete', 'sys:user:update']">Edit</el-button>
```

- 读取 auth store 的 `permissions`；`*` 通配通过
- 无权限时 `el.parentNode.removeChild(el)`（而非 display:none，避免可见）
- 注册入口放 `main.ts`

**铺开:** 遍历 UserList / RoleList / AdminList / MenuTree / DictManager / ConfigList 的所有操作按钮，加 `v-perm`。perms 命名遵循 `sys:{entity}:{action}`。

### 3.6 Mock 扩展 (P0)

在 `packages/core/src/api/mock/data.ts` 补：

- `GET /admin/configs` → 分页
- `POST|PUT|DELETE /admin/configs`
- `GET /admin/dict/types` / `GET /admin/dict/data`
- `GET /monitor/operation-logs` / `GET /monitor/login-logs` → `{ list, total }`
- `GET /monitor/online` → 模拟在线用户
- `POST /user/password` → 成功

### 3.7 403 页面 + 拦截器 (P0)

- `views/error/403.vue`
- `router/index.ts` 守卫：如果路由 `meta.perms` 存在且当前用户无该权限 → `next('/403')`
- `api/client.ts` 拦截器：`code === '403'` → `router.replace('/403')`

### 3.8 Tab keep-alive (P1)

- `DefaultLayout.vue` 的 `<router-view>` 外包 `<keep-alive :include="cachedNames">`
- `stores/tabbar.ts` 维护 `cachedNames`（跟 `tabs` 同步增删）
- 页面用 `defineOptions({ name: 'XxxRoute' })` 给 keep-alive 命名

### 3.9 批量操作 (P1)

- `UserList / RoleList / AdminList` 开启 `el-table` 多选列
- 顶部出现"已选 N 项"行动条（sticky）：批量禁用 / 批量启用 / 批量删除
- API：复用单条循环；或后端加 `POST /admin/admins/batch-disable` 一次请求（优先后者）

### 3.10 Excel 导入/导出 (P1)

- `packages/core/src/composables/useExport.ts` 封装：调后端 `/export/{module}` → 返回 Blob → 下载
- `packages/core/src/composables/useImport.ts` 封装：`<input type=file>` → FormData POST
- UserList 先接入作为示范

### 3.11 MateForm 统一 (P1)

现状：UserList / AdminList / ConfigList 各自手写 `el-form + el-form-item * N`。
目标：抽成 schema 驱动。示例：

```vue
<MateForm :schema="userFormSchema" v-model="form" :rules="rules" ref="formRef" />
```

Schema 放 `views/system/user/form.schema.ts`，字段只描述 `{ prop, label, type, placeholder, options }`。

### 3.12 Dashboard 真实数据 (P1)

- `logApi.statistics()` 返回近 7 天 API 调用数 / 登录成功率 / 活跃用户数
- `/actuator/health` 健康指示灯（或 admin API 封装）
- 图表用 echarts 最小集（按需引入 BarChart）

### 3.13 密码重置 (P1)

- UserList / AdminList 操作列加「重置密码」按钮
- 弹窗：新密码（可随机生成一个按钮）
- API: `PUT /admin/admins/{id}/password/reset`，返回临时密码

---

## 4. Task Breakdown & Sequencing

```
Week 1: P0 基础
  ├─ 3.6  Mock 扩展               (解锁并行开发，~0.5d)
  ├─ 3.5  v-perm 指令             (~0.5d)
  ├─ 3.7  403 页面 + 守卫         (~0.5d)
  ├─ 3.1  DictManager 双面板       (~1.5d)
  ├─ 3.2  Logs 分页 + 详情        (~1d)
  ├─ 3.3  ConfigList 升级          (~0.5d)
  └─ 3.4  Profile 改密接线        (~0.5d)

Week 2: P1 体验
  ├─ 3.8  Tab keep-alive           (~0.5d)
  ├─ 3.9  批量操作                 (~1d)
  ├─ 3.10 Excel 导入/导出          (~1d)
  ├─ 3.11 MateForm schema 统一    (~1.5d)
  ├─ 3.12 Dashboard 真实数据      (~1d)
  └─ 3.13 密码重置弹窗            (~0.5d)
```

---

## 5. Acceptance Criteria

一个新用户 `git clone` 仓库，只跑前端（后端不启动）应当能：

1. 访问所有菜单项，列表页有 mock 数据（不再空白）
2. 新增/编辑/删除/搜索 user / role / admin / menu / dict / config 全流程可走通
3. 没权限的按钮直接隐藏（通过 `auth.permissions` 控制）
4. 切 Tab 不丢列表分页状态
5. 操作日志 / 登录日志能看详情、能筛日期

接上后端（RFC-040 完成后）应当能：
- Logs 真实分页生效
- DictType/Data 真实 CRUD 生效
- Profile 改密真实生效
- Dashboard 数字是真的

---

## 6. Non-goals

- 不引入新 UI 框架（继续 Element Plus + 自研 Mate*）
- 不做移动端（见 RFC-037）
- 不做实时 WebSocket（在线用户数用轮询即可）
- 不做主题编辑器（dark mode 已 OK）

---

## 7. File Change Summary

```
packages/core/src/
  directives/perm.ts                       [NEW]
  composables/useExport.ts                 [NEW]
  composables/useImport.ts                 [NEW]
  api/modules/admin.ts                     [EDIT] + dictType*/dictData*
  api/modules/config.ts                    [EDIT] + 分页参数
  api/modules/log.ts                       [EDIT] + total + 日期参数
  api/modules/user.ts                      [EDIT] + changePassword
  api/mock/data.ts                         [EDIT] 大量补充
  types/admin.ts                           [EDIT] + DictType/DictData
  index.ts                                 [EDIT] 导出新符号

apps/admin/src/
  main.ts                                  [EDIT] 注册 v-perm
  router/index.ts                          [EDIT] 403 守卫
  layouts/DefaultLayout.vue                [EDIT] keep-alive
  stores/tabbar.ts                         [EDIT] cachedNames
  views/admin/DictManager.vue              [REWRITE] 双面板
  views/system/ConfigList.vue              [EDIT] 搜索+分页+i18n
  views/monitor/LoginLog.vue               [EDIT] 日期+详情
  views/monitor/OperationLog.vue           [EDIT] 日期+详情
  views/profile/Profile.vue                [EDIT] 改密接线
  views/system/UserList.vue                [EDIT] 批量+导出+重置密码
  views/admin/AdminList.vue                [EDIT] 批量+重置密码
  views/admin/RoleList.vue                 [EDIT] 真实分页+状态切换
  views/dashboard/Dashboard.vue            [EDIT] 真实指标
  views/error/403.vue                      [NEW]
  i18n/locales/zh-CN/*.json                [EDIT] 补齐
  i18n/locales/en-US/*.json                [EDIT] 补齐
```

---

## 8. Follow-up RFCs (不在本 RFC 范围)

| RFC | 内容 |
|---|---|
| 047 | 定时任务前端（XXL-Job 管理页） |
| 048 | 文件/存储管理前端（基于 `mate-file-starter`） |
| 049 | 通知中心前端（基于 `mate-notice`） |
| 050 | 租户管理前端（基于 `mate-tenant-starter`） |
| 051 | 代码生成前端（基于 `mate-cli gen code`） |
| 052 | 服务器/缓存/接口文档监控前端（基于 actuator + smart-doc） |
