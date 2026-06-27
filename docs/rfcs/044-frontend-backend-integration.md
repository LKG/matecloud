# RFC-044: Frontend-Backend Integration — Wire CRUD Pages to Real APIs

- **Status**: Done
- **Created**: 2026-04-12
- **Author**: MateCloud Team
- **Wave**: 9
- **Dependencies**: RFC-039, RFC-040

## 背景

前端 mate-ui 已有完整的 Layout、Login 和 CRUD 组件骨架。API 客户端 (`packages/core/src/api/client.ts`) 已对接 Sa-Token 认证和多租户 Header。

但有两个问题：

1. **client.ts 的 baseURL 是 `/api/v1`**（相对路径），开发环境需要 Vite proxy 或直连 Gateway
2. **RFC-040 新增的 Config/OperationLog/LoginLog 三个模块没有前端页面和 API**

本 RFC 完成三件事：
- 配置 Vite 开发代理到 Gateway
- 新增 Config/Log API 模块
- 新增 Config/OperationLog/LoginLog 三个页面
- 更新路由 VIEW_MAP

## 设计方案

### Change 1: Vite 开发代理配置

File: `mate-ui/apps/admin/vite.config.ts`（修改，增加 proxy）

在 `defineConfig` 中添加 server.proxy：

```typescript
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:9010',  // mate-gateway
      changeOrigin: true,
    },
  },
},
```

这样前端 `baseURL: '/api/v1'` 在开发时会代理到 `http://localhost:9010/api/v1`，经过 Gateway 路由到各下游服务。

### Change 2: Config API 模块

File: `mate-ui/packages/core/src/api/modules/config.ts`

```typescript
import { client } from '../client'
import type { Result } from '../../types/result'

export interface ConfigItem {
  id: string
  configKey: string
  configValue: string
  configName: string
  builtIn: boolean
  remark: string
  createdAt: string
}

export const configApi = {
  list: () =>
    client.get<any, Result<ConfigItem[]>>('/admin/configs'),

  getById: (id: string) =>
    client.get<any, Result<ConfigItem>>(`/admin/configs/${id}`),

  getByKey: (key: string) =>
    client.get<any, Result<ConfigItem>>(`/admin/configs/key/${key}`),

  create: (data: { configKey: string; configValue: string; configName: string; remark?: string }) =>
    client.post<any, Result<string>>('/admin/configs', data),

  update: (id: string, data: { configValue: string; remark?: string }) =>
    client.put<any, Result<void>>(`/admin/configs/${id}`, data),

  delete: (id: string) =>
    client.delete<any, Result<void>>(`/admin/configs/${id}`),
}
```

### Change 3: Log API 模块

File: `mate-ui/packages/core/src/api/modules/log.ts`

```typescript
import { client } from '../client'
import type { Result } from '../../types/result'

export interface OperationLogItem {
  id: string
  userId: string
  username: string
  module: string
  operationType: string
  requestMethod: string
  requestUrl: string
  clientIp: string
  status: number
  errorMsg: string
  duration: number
  createdAt: string
}

export interface LoginLogItem {
  id: string
  username: string
  clientIp: string
  userAgent: string
  loginType: string
  status: number
  failMsg: string
  createdAt: string
}

export const logApi = {
  operationLogs: (params?: { pageNum?: number; pageSize?: number; module?: string; username?: string }) =>
    client.get<any, Result<OperationLogItem[]>>('/admin/operation-logs', { params }),

  loginLogs: (params?: { pageNum?: number; pageSize?: number; username?: string; status?: number }) =>
    client.get<any, Result<LoginLogItem[]>>('/admin/login-logs', { params }),
}
```

### Change 4: API 模块导出

File: `mate-ui/packages/core/src/api/index.ts`（修改，增加导出）

```typescript
export { adminApi } from './modules/admin'
export { authApi } from './modules/auth'
export { userApi } from './modules/user'
export { configApi } from './modules/config'
export { logApi } from './modules/log'

export type { ConfigItem } from './modules/config'
export type { OperationLogItem, LoginLogItem } from './modules/log'
```

### Change 5: ConfigList 页面

File: `mate-ui/apps/admin/src/views/system/ConfigList.vue`

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { configApi, type ConfigItem } from '@matecloud/core'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const configs = ref<ConfigItem[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({ id: '', configKey: '', configValue: '', configName: '', remark: '' })

async function loadData() {
  loading.value = true
  try {
    const res = await configApi.list()
    configs.value = res.data ?? []
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  form.value = { id: '', configKey: '', configValue: '', configName: '', remark: '' }
  isEdit.value = false
  dialogVisible.value = true
}

function handleEdit(row: ConfigItem) {
  form.value = { ...row }
  isEdit.value = true
  dialogVisible.value = true
}

async function handleSubmit() {
  if (isEdit.value) {
    await configApi.update(form.value.id, {
      configValue: form.value.configValue,
      remark: form.value.remark,
    })
    ElMessage.success('Updated')
  } else {
    await configApi.create({
      configKey: form.value.configKey,
      configValue: form.value.configValue,
      configName: form.value.configName,
      remark: form.value.remark,
    })
    ElMessage.success('Created')
  }
  dialogVisible.value = false
  loadData()
}

async function handleDelete(row: ConfigItem) {
  if (row.builtIn) {
    ElMessage.warning('Built-in config cannot be deleted')
    return
  }
  await ElMessageBox.confirm('Delete this config?', 'Confirm')
  await configApi.delete(row.id)
  ElMessage.success('Deleted')
  loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="p-4">
    <div class="mb-4 flex justify-between">
      <h2 class="text-lg font-bold">System Config</h2>
      <el-button type="primary" @click="handleAdd">Add Config</el-button>
    </div>

    <el-table :data="configs" v-loading="loading" stripe>
      <el-table-column prop="configKey" label="Key" width="250" />
      <el-table-column prop="configValue" label="Value" />
      <el-table-column prop="configName" label="Name" width="200" />
      <el-table-column prop="builtIn" label="Built-in" width="100">
        <template #default="{ row }">
          <el-tag :type="row.builtIn ? 'warning' : 'info'" size="small">
            {{ row.builtIn ? 'Yes' : 'No' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="180">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">Edit</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)"
                     :disabled="row.builtIn">Delete</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? 'Edit Config' : 'Add Config'" width="500">
      <el-form :model="form" label-width="100px">
        <el-form-item label="Key" v-if="!isEdit">
          <el-input v-model="form.configKey" placeholder="sys.user.initPassword" />
        </el-form-item>
        <el-form-item label="Name" v-if="!isEdit">
          <el-input v-model="form.configName" />
        </el-form-item>
        <el-form-item label="Value">
          <el-input v-model="form.configValue" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="Remark">
          <el-input v-model="form.remark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="handleSubmit">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>
```

### Change 6: OperationLog 页面

File: `mate-ui/apps/admin/src/views/monitor/OperationLog.vue`

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { logApi, type OperationLogItem } from '@matecloud/core'

const loading = ref(false)
const logs = ref<OperationLogItem[]>([])
const searchModule = ref('')
const searchUsername = ref('')
const pageNum = ref(1)
const pageSize = ref(20)

async function loadData() {
  loading.value = true
  try {
    const res = await logApi.operationLogs({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      module: searchModule.value || undefined,
      username: searchUsername.value || undefined,
    })
    logs.value = res.data ?? []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="p-4">
    <h2 class="text-lg font-bold mb-4">Operation Logs</h2>

    <div class="mb-4 flex gap-2">
      <el-input v-model="searchModule" placeholder="Module" style="width: 150px" clearable />
      <el-input v-model="searchUsername" placeholder="Username" style="width: 150px" clearable />
      <el-button type="primary" @click="handleSearch">Search</el-button>
    </div>

    <el-table :data="logs" v-loading="loading" stripe>
      <el-table-column prop="username" label="User" width="120" />
      <el-table-column prop="module" label="Module" width="120" />
      <el-table-column prop="operationType" label="Type" width="100" />
      <el-table-column prop="requestMethod" label="Method" width="80" />
      <el-table-column prop="requestUrl" label="URL" show-overflow-tooltip />
      <el-table-column prop="clientIp" label="IP" width="130" />
      <el-table-column prop="status" label="Status" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
            {{ row.status === 0 ? 'OK' : 'Fail' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="duration" label="Time(ms)" width="100" />
      <el-table-column prop="createdAt" label="Created" width="180" />
    </el-table>

    <div class="mt-4 flex justify-end">
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        layout="prev, pager, next"
        :total="100"
        @current-change="loadData"
      />
    </div>
  </div>
</template>
```

### Change 7: LoginLog 页面

File: `mate-ui/apps/admin/src/views/monitor/LoginLog.vue`

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { logApi, type LoginLogItem } from '@matecloud/core'

const loading = ref(false)
const logs = ref<LoginLogItem[]>([])
const searchUsername = ref('')
const searchStatus = ref<number | undefined>(undefined)
const pageNum = ref(1)
const pageSize = ref(20)

async function loadData() {
  loading.value = true
  try {
    const res = await logApi.loginLogs({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      username: searchUsername.value || undefined,
      status: searchStatus.value,
    })
    logs.value = res.data ?? []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="p-4">
    <h2 class="text-lg font-bold mb-4">Login Logs</h2>

    <div class="mb-4 flex gap-2">
      <el-input v-model="searchUsername" placeholder="Username" style="width: 150px" clearable />
      <el-select v-model="searchStatus" placeholder="Status" clearable style="width: 120px">
        <el-option label="Success" :value="0" />
        <el-option label="Failed" :value="1" />
      </el-select>
      <el-button type="primary" @click="handleSearch">Search</el-button>
    </div>

    <el-table :data="logs" v-loading="loading" stripe>
      <el-table-column prop="username" label="Username" width="150" />
      <el-table-column prop="clientIp" label="IP" width="130" />
      <el-table-column prop="loginType" label="Type" width="100" />
      <el-table-column prop="status" label="Status" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
            {{ row.status === 0 ? 'OK' : 'Fail' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="failMsg" label="Message" show-overflow-tooltip />
      <el-table-column prop="userAgent" label="User Agent" show-overflow-tooltip width="200" />
      <el-table-column prop="createdAt" label="Time" width="180" />
    </el-table>

    <div class="mt-4 flex justify-end">
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        layout="prev, pager, next"
        :total="100"
        @current-change="loadData"
      />
    </div>
  </div>
</template>
```

### Change 8: 路由 VIEW_MAP 更新

在前端路由的动态路由映射中注册新页面。

修改 `mate-ui/apps/admin/src/router/dynamic.ts`（或等效的路由映射文件），添加：

```typescript
// 在 VIEW_MAP 中新增：
'system/ConfigList': () => import('@/views/system/ConfigList.vue'),
'monitor/OperationLog': () => import('@/views/monitor/OperationLog.vue'),
'monitor/LoginLog': () => import('@/views/monitor/LoginLog.vue'),
```

这些 key 对应种子数据中 `mate_menu.component` 字段的值（RFC-041 V2__admin_seed_data.sql）。

## 涉及文件

| 文件 | 类型 | 说明 |
|------|------|------|
| `mate-ui/apps/admin/vite.config.ts` | Modify | 增加 /api 代理到 Gateway |
| `mate-ui/packages/core/src/api/modules/config.ts` | New | Config API |
| `mate-ui/packages/core/src/api/modules/log.ts` | New | Log API |
| `mate-ui/packages/core/src/api/index.ts` | Modify | 增加导出 |
| `mate-ui/apps/admin/src/views/system/ConfigList.vue` | New | 系统配置页面 |
| `mate-ui/apps/admin/src/views/monitor/OperationLog.vue` | New | 操作日志页面 |
| `mate-ui/apps/admin/src/views/monitor/LoginLog.vue` | New | 登录日志页面 |
| `mate-ui/apps/admin/src/router/dynamic.ts` | Modify | 注册新路由 |

## 验证方案

1. 启动 Gateway + Admin 后端
2. `cd mate-ui && pnpm dev` — 启动前端
3. 登录后访问 `http://localhost:5173`
4. 侧边栏应出现「System Management > Config Management」菜单
5. 点击进入 ConfigList 页面，应能看到种子数据中的 3 条配置
6. 测试增删改查功能
7. 侧边栏「Monitor > Operation Logs」— 应展示操作日志列表
8. 侧边栏「Monitor > Login Logs」— 应展示登录日志列表
9. `pnpm build` — 前端构建通过

## 注意事项

- **Vite proxy 仅限开发**：生产环境前端构建为静态文件，通过 Nginx 反代到 Gateway
- **API 路径一致性**：前端 `/admin/configs` → Gateway 路由到 `lb://mate-admin` → AdminController `/api/v1/admin/configs`
- **分页 total**：OperationLog 和 LoginLog 页面的 `:total="100"` 是临时硬编码。后续需要后端 QueryService 返回分页元数据 `PageResult<T>`（含 total、pages 字段），前端用 `res.data.total` 替换硬编码值
- **Mock fallback 仍在**：`client.ts` 有 mock 降级逻辑，当 Gateway 不可达时自动使用 mock 数据，不影响开发
- **菜单与路由映射**：VIEW_MAP 使用菜单的 `path` 字段（如 `/system/config`）作为 key，而非 `component` 字段。种子数据中 `mate_menu.path = '/system/config'` 对应 `VIEW_MAP['/system/config']`
