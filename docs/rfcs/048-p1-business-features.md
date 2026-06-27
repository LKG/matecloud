# RFC-048: P1 业务功能交付

- **Status**: Draft
- **Created**: 2026-04-15
- **Wave**: FE-6 / BE-P1
- **Dependencies**: RFC-046 (P0 CRUD 闭环 + UI 组件化基线), RFC-047 (UI 组件化), 以及本轮刚落地的权限注解 (commit `682061a`)

---

## Context

RFC-046 当初在 P1 部分列了 6 项"让体验完整"的业务能力，只做了 `Tab keep-alive` 和 `MateForm schema 统一`两项。剩下的四项一直没落地：

1. **批量操作** — 多选 + 批量禁用/启用/删除（User / Admin / Role 列表）
2. **Excel 导入/导出** — 前端 `useExport` / `useImport` composable + 后端 `@ExcelExport` / `@ExcelImport` 打通；UserList 作为示范
3. **Dashboard 真实数据** — 当前 `stats / chartData / services` 都是硬编码数组，需要对接 actuator + 日志统计
4. **密码重置弹窗** — 后端 `PUT /admin/admins/{id}/password/reset` 和 `PUT /users/{id}/password/reset` 已就绪 (本轮加了注解 `sys:admin:reset` / `sys:user:reset`)，前端 UserList / AdminList 还没有入口按钮

本轮把这四项全部交付，目标是让基础管理平台的**用户感知**达到生产可用状态（能管人、能批量、能导入历史数据、能看系统健康）。

> 本 RFC 不动权限模型 (admin `*` 通配符的临时补丁留到下个 RFC 解决) — 完全聚焦用户体验 + 业务闭环。

---

## Goals

| # | 能力 | 用户价值 | 工作量 |
|---|---|---|---|
| G1 | 批量操作 | 一次操作 10/20 行，不用点到手酸 | ~1 day |
| G2 | Excel 导入 | 历史数据一次性迁入 | ~0.5 day |
| G3 | Excel 导出 | 对账、离线分析、合规备份 | ~0.5 day |
| G4 | Dashboard 真实数据 | 登录页→Dashboard 第一眼就能看到系统是否健康 | ~1.5 day |
| G5 | 密码重置弹窗 | 管理员不必进 SQL 改表 | ~0.5 day |

合计 **~4 day**，单一 PR 可发。

---

## 1. 批量操作 (G1)

### 前端

**视图**：`UserList` / `AdminList` / `RoleList` 三个列表。

**MateTable 改动**：prop `selection` 已支持，`selectionChange` emit 已支持 — **库侧零改动**。

**每个视图加**：

```ts
const selectedIds = ref<string[]>([])
function onSelectionChange(rows: any[]) {
  selectedIds.value = rows.map(r => r.id)
}
```

**批量操作条**（在搜索栏下、表格上）：

```vue
<div v-if="selectedIds.length" class="batch-bar">
  <span>已选 {{ selectedIds.length }} 项</span>
  <el-button size="small" @click="handleBatchDisable">批量禁用</el-button>
  <el-button size="small" @click="handleBatchEnable">批量启用</el-button>
  <el-popconfirm title="确定批量删除？" @confirm="handleBatchDelete">
    <template #reference>
      <el-button size="small" type="danger">批量删除</el-button>
    </template>
  </el-popconfirm>
  <el-button size="small" link @click="selectedIds = []">取消</el-button>
</div>
```

抽成共享组件 `MateBatchBar`（`packages/ui/src/MateBatchBar/`）— 三个视图会复用同一个。

### 后端

**新增三个批量端点**（mate-admin 先做 Admin，mate-system 做 User，Role 也在 mate-admin）：

```
POST /api/v1/admin/admins/batch-disable  @RequestBody List<String> ids
POST /api/v1/admin/admins/batch-enable   @RequestBody List<String> ids
POST /api/v1/admin/admins/batch-delete   @RequestBody List<String> ids
POST /api/v1/admin/roles/batch-delete    @RequestBody List<String> ids
POST /api/v1/users/batch-freeze          @RequestBody List<String> ids
POST /api/v1/users/batch-unfreeze        @RequestBody List<String> ids
POST /api/v1/users/batch-delete          @RequestBody List<String> ids
```

**命令服务**：每个都是循环现有单条方法 + 聚合错误：

```java
@Transactional
public BatchResult batchDisable(List<String> ids) {
    int ok = 0; List<String> failed = new ArrayList<>();
    for (String id : ids) {
        try { disableAdmin(id); ok++; }
        catch (Exception e) { failed.add(id + ": " + e.getMessage()); }
    }
    return new BatchResult(ok, failed);
}
record BatchResult(int successCount, List<String> failed) {}
```

**权限注解**：`@SaCheckPermission(Perms.ADMIN_EDIT)` (disable/enable), `ADMIN_DELETE`, etc. — 直接复用现有码。

**API 签名** — 前端 `core/api/modules/admin.ts`：

```ts
adminBatchDisable: (ids: string[]) =>
  client.post<any, Result<{ successCount: number; failed: string[] }>>('/admin/admins/batch-disable', ids),
```

批量结果给前端 toast 显示 `"成功 N，失败 M"`。

---

## 2. Excel 导入 (G2)

**前端 composable** `packages/core/src/composables/useImport.ts`：

```ts
export function useImport() {
  async function importExcel(
    url: string,
    file: File,
    onSuccess?: (result: any) => void,
  ) {
    const form = new FormData()
    form.append('file', file)
    const res = await client.post<any, Result<any>>(url, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    onSuccess?.(res.data)
    return res.data
  }
  return { importExcel }
}
```

**UserList 接入**：搜索栏 `#trailing` 插槽放「导入」按钮，弹出 `<el-upload :auto-upload="false">` 选文件 → 调 `importExcel('/users/import', file)` → 成功提示 `"成功导入 N 行，失败 M 行"`，列表 `loadData()`。

**后端** `mate-system`：

```java
// UserController
@PostMapping("/import")
@SaCheckPermission(Perms.USER_ADD)
public Result<ImportResult> importUsers(@ExcelImport(dataClass = UserImportRow.class) List<UserImportRow> rows) {
    return Result.ok(userCommandService.importBatch(rows));
}

// DTO
public record UserImportRow(
    @ExcelProperty("用户名") String username,
    @ExcelProperty("手机号") String mobile,
    @ExcelProperty("邮箱") String email,
    @ExcelProperty("真实姓名") String realName
) {}

record ImportResult(int successCount, List<String> errors) {}
```

**容错策略**：单行失败不回滚整批 — 每行独立 try/catch（`REQUIRES_NEW` 事务），错误聚合回给前端显示。

**模板下载**：后端再加 `GET /users/import/template` 返回一个 `.xlsx` 头模板文件，前端导入对话框里放个「下载模板」链接。

---

## 3. Excel 导出 (G3)

**前端 composable** `packages/core/src/composables/useExport.ts`：

```ts
export function useExport() {
  async function exportExcel(url: string, params?: Record<string, any>, filename?: string) {
    const response = await client.get(url, { params, responseType: 'blob' })
    const blob = new Blob([response.data as any])
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = filename || 'export.xlsx'
    link.click()
    URL.revokeObjectURL(link.href)
  }
  return { exportExcel }
}
```

**UserList 接入**：搜索栏 `#trailing` 插槽放「导出」按钮 → 当前筛选条件（keyword / status）一起传给导出端点。

**后端** `mate-system`：

```java
@GetMapping("/export")
@SaCheckPermission(Perms.USER_LIST)
@ExcelExport(fileName = "users", dataClass = UserExportRow.class, sheetName = "Users")
public List<UserExportRow> exportUsers(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status) {
    return userQueryService.exportAll(keyword, status);
}
```

`mate-excel-starter.ExcelResponseBodyAdvice` 会拦 `@ExcelExport` 方法把返回 `List<T>` 转成 `.xlsx` 字节流。

**导出数量上限**：最多 5 万行，超了直接拒绝（Excel xlsx 单 sheet 上限 ~100 万但内存会爆）。用 `LIMIT` 兜底。

---

## 4. Dashboard 真实数据 (G4)

当前 `Dashboard.vue` 4 处硬编码：`greeting / displayName`（OK，取自 auth store）、`stats`、`chartData`、`services`。后三者要接后端。

### 后端：新增 `MonitorController`（在 mate-admin）

```
GET /api/v1/admin/monitor/dashboard   →  DashboardVO
```

```java
public record DashboardVO(
    // 4 张小卡
    long userCount,          // 用户总数 (SELECT COUNT FROM mate_user)
    long todayLoginCount,    // 今日登录次数 (login_log WHERE date(created_at)=CURDATE())
    long onlineCount,        // 在线会话数 (Sa-Token StpUtil.searchTokenValue 或 Redis SCAN)
    long todayOpCount,       // 今日操作数 (operation_log 同上)
    // 7 天折线图
    List<DailyStat> last7Days,
    // 服务健康状态
    List<ServiceStatus> services
) {}

public record DailyStat(String date, long apiCalls, long logins) {}
public record ServiceStatus(String name, String url, String state, Long latencyMs) {}
```

### Service 实现

- **userCount**：Dubbo RPC `rpcUserService.count()` 或直接查 `mate_user`
- **todayLoginCount / todayOpCount**：`loginLogDao.countByDate(today)` / `operationLogDao.countByDate(today)` — 加两个 `@Select`
- **onlineCount**：`Redis SCAN "Authorization:login:token:*"`（Sa-Token token key 前缀）然后 COUNT。写个 `OnlineUsersQueryService` 封装
- **last7Days**：一次 GROUP BY `DATE(created_at)` 查 operation_log + login_log
- **services**：写一个 `ServiceHealthChecker` 对 `mate-auth / mate-system / mate-admin / mate-gateway` 的 `/actuator/health` 并发 HEAD，5 秒超时，取状态 + 延迟

### actuator 接入

`mate-defaults.yml` 加：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when_authorized
```

`/actuator/health` 需要放开 SaInterceptor 白名单（上轮已经写了 `/actuator/**` 在 exclude 里）。

### 前端

`views/dashboard/Dashboard.vue` 用 composable 拉数据：

```ts
const dashboardApi = {
  get: () => client.get<any, Result<DashboardVO>>('/admin/monitor/dashboard')
}

const data = ref<DashboardVO | null>(null)
onMounted(async () => { data.value = (await dashboardApi.get()).data })
```

图表用 `ECharts` 最小集（按需引入 `BarChart` + `LineChart`），替掉当前手画的 `.bar-wrap` div。

---

## 5. 密码重置弹窗 (G5)

**后端**：已就绪，无改动。

**前端**：

UserList / AdminList 各加一个「重置密码」操作按钮（带 `v-permission="'sys:user:reset'"` / `'sys:admin:reset'`）：

```vue
<button v-permission="'sys:user:reset'" class="mc-action-btn" @click="openResetDialog(row)">
  {{ t('common.resetPassword') }}
</button>
```

**弹窗（MateDialog + MateForm）**：

```ts
const resetForm = reactive({ newPassword: '', confirm: '' })
const resetSchema: FormSchema[] = [
  {
    field: 'newPassword',
    label: t('common.newPassword'),
    type: 'password',
    rules: [{ required: true, min: 6, max: 64 }]
  },
  {
    field: 'confirm',
    label: t('common.confirmPassword'),
    type: 'password',
    rules: [
      { required: true },
      {
        validator: (_, val, cb) =>
          val === resetForm.newPassword ? cb() : cb(new Error(t('profile.passwordMismatch')))
      }
    ]
  }
]
```

**随机密码生成按钮**：弹窗内加个「生成 12 位随机密码」链接，点一下填入 newPassword + confirm，方便管理员批量改。用 `Math.random` + `crypto.getRandomValues` 简单实现即可（demo 场景）。

**提交**：`userApi.resetPassword(row.id, resetForm.newPassword)` / `adminApi.adminResetPassword(row.id, resetForm.newPassword)` → 成功 toast「新密码已生效，请告知用户」。

---

## 文件清单

### 前端

**新增**
- `packages/ui/src/MateBatchBar/MateBatchBar.vue` — 批量操作条组件
- `packages/core/src/composables/useExport.ts`
- `packages/core/src/composables/useImport.ts`
- `packages/core/src/api/modules/dashboard.ts` — 新 API 模块
- `packages/core/src/types/dashboard.ts` — DashboardVO / DailyStat / ServiceStatus

**修改**
- `views/system/UserList.vue` — 批量 + 导入 + 导出 + 重置密码
- `views/admin/AdminList.vue` — 批量 + 重置密码
- `views/admin/RoleList.vue` — 批量删除
- `views/dashboard/Dashboard.vue` — 接真实数据 + ECharts
- `packages/ui/src/index.ts` — 导出 MateBatchBar
- `packages/core/src/index.ts` — 导出新 composables / dashboardApi
- i18n `en-US.ts` / `zh-CN.ts` — batch / export / import / resetPassword / dashboard 新增 key

### 后端

**新增**
- `mate-admin/.../trigger/controller/MonitorController.java` — Dashboard 数据
- `mate-admin/.../application/query/IMonitorQueryService.java` + impl
- `mate-admin/.../application/query/IOnlineQueryService.java` + impl (Redis SCAN)
- `mate-biz/mate-system/.../trigger/controller/UserImportExportController.java`（或直接并入 UserController）
- `mate-biz/mate-system/.../application/command/UserImportCommand.java`
- `mate-biz/mate-system/.../types/UserImportRow.java` / `UserExportRow.java`

**修改**
- `AdminController` / `RoleController` / `UserController` — batch endpoints
- `AdminCommandService` / `UserCommandService` — batchXxx 方法
- Perms 常量类 — 若需新增 `sys:admin:import` / `sys:user:import` (复用 ADD 也可)
- `mate-defaults.yml` — actuator 配置

---

## 验收标准

**前端**（`pnpm --filter admin dev` + admin/admin123 登录）：

1. `/system/user` 勾选 3 行 → 顶部出「已选 3 项 / 批量禁用 / 批量启用 / 批量删除 / 取消」
2. 点「导出」→ 浏览器下载 `users.xlsx`，用 Excel 打开字段正确
3. 点「导入」→ 选 `.xlsx` 模板（先点「下载模板」）→ 填好上传 → 提示成功/失败条数
4. `/dashboard` 4 张卡片显示真实数字；「7 天 API 调用」是 ECharts 折线图；服务状态列表有绿/红 badge 对 `mate-auth`/`mate-system`/`mate-admin`/`mate-gateway`
5. UserList 行操作多出「重置密码」；点击弹 MateDialog 填新密码+确认 → 成功提示，旧 token 被 kick（上轮逻辑联动）

**后端**（curl / Postman）：

```bash
# 批量禁用
curl -X POST http://localhost:9040/api/v1/admin/admins/batch-disable \
  -H "Authorization: $TOKEN" -H "Content-Type: application/json" \
  -d '["id1","id2","id3"]'

# 导出
curl -o users.xlsx http://localhost:9030/api/v1/users/export?keyword=admin \
  -H "Authorization: $TOKEN"

# Dashboard
curl http://localhost:9040/api/v1/admin/monitor/dashboard \
  -H "Authorization: $TOKEN" | jq
```

**编译**：`mvn clean compile` + `pnpm typecheck` + `pnpm build` 全绿。

---

## 明确不做

- ❌ 权限模型收尾（role→menu→perms 真正加载）— 留 RFC-049
- ❌ 单元测试 — 留 RFC-049 或 RFC-050
- ❌ 批量 import 的异步化 / 进度条 — 当前同步提交，5000 行内可接受
- ❌ 自定义导出字段（用户选列）— 当前固定字段导出
- ❌ 图表主题 / 自定义时间范围（当前固定近 7 天）

---

## 预估排期

| 项 | 工作量 |
|---|---|
| G1 批量操作（前 + 后 7 endpoints） | 1.0d |
| G2 Excel 导入（前端 composable + 后端 UserImport） | 0.5d |
| G3 Excel 导出（前端 composable + 后端 UserExport） | 0.5d |
| G4 Dashboard 真实数据（后端聚合 + 前端 ECharts） | 1.5d |
| G5 密码重置弹窗 | 0.5d |
| **合计** | **~4d** |
