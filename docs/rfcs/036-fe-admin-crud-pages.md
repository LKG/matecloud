# RFC-036: Frontend Admin CRUD Pages

- **Status**: Draft
- **Created**: 2026-04-12
- **Wave**: FE-3 (after 033-035: skeleton, core/ui packages, layout/login)
- **Dependencies**: RFC-032 (skeleton), RFC-033 (core + ui components), RFC-034 (layout), RFC-035 (login + auth)

## Scope

`mate-ui/apps/admin/src/views/` -- actual CRUD pages using MateTable + MateForm + MateSearch + MateDialog components from `@matecloud/ui`. Plus route registration for all modules.

Deliverables:
- `views/system/user/UserList.vue` -- User CRUD with search, dict status, permissions
- `views/system/user/index.ts` -- Route config export
- `views/admin/role/RoleList.vue` -- Role CRUD with menu tree assignment
- `views/admin/menu/MenuTree.vue` -- Menu hierarchy management
- `views/admin/dict/DictManager.vue` -- Dict type + data dual-panel management
- `views/profile/Profile.vue` -- Avatar, nickname, password change
- `router/modules/system.ts` -- /system/* route config
- `router/modules/admin.ts` -- /admin/* route config

---

## 1. API Type Definitions (packages/core prerequisites)

These types are consumed by the pages. They live in `packages/core/src/types/` and `packages/core/src/api/modules/`.

### packages/core/src/types/admin.ts

```typescript
// packages/core/src/types/admin.ts

export interface Role {
  id: string
  roleName: string
  roleKey: string
  status: number
  sort: number
  remark?: string
  menuIds?: string[]
  createdAt: string
}

export interface CreateRoleCommand {
  roleName: string
  roleKey: string
  status: number
  sort: number
  remark?: string
  menuIds: string[]
}

export interface UpdateRoleCommand extends CreateRoleCommand {
  id: string
}

export interface MenuItem {
  id: string
  parentId: string
  name: string
  path: string
  component?: string
  icon?: string
  perms?: string
  type: 'DIRECTORY' | 'MENU' | 'BUTTON'
  sort: number
  visible: boolean
  status: number
  children?: MenuItem[]
  createdAt: string
}

export interface CreateMenuCommand {
  parentId: string
  name: string
  path: string
  component?: string
  icon?: string
  perms?: string
  type: 'DIRECTORY' | 'MENU' | 'BUTTON'
  sort: number
  visible: boolean
  status: number
}

export interface UpdateMenuCommand extends CreateMenuCommand {
  id: string
}

export interface DictType {
  id: string
  dictName: string
  dictType: string
  status: number
  remark?: string
  createdAt: string
}

export interface CreateDictTypeCommand {
  dictName: string
  dictType: string
  status: number
  remark?: string
}

export interface DictData {
  id: string
  dictType: string
  dictLabel: string
  dictValue: string
  sort: number
  status: number
  cssClass?: string
  listClass?: string
  remark?: string
  createdAt: string
}

export interface CreateDictDataCommand {
  dictType: string
  dictLabel: string
  dictValue: string
  sort: number
  status: number
  cssClass?: string
  listClass?: string
  remark?: string
}
```

### packages/core/src/types/system.ts

```typescript
// packages/core/src/types/system.ts

export interface UserInfo {
  id: string
  mobile: string
  nickName: string
  avatar?: string
  gender?: number
  status: number
  createdAt: string
}

export enum UserStatus {
  ACTIVE = 0,
  FROZEN = 1,
}

export interface CreateUserCommand {
  mobile: string
  nickName: string
  gender?: number
  status: number
  password?: string
}

export interface UpdateUserCommand extends CreateUserCommand {
  id: string
}

export interface PageQuery {
  page: number
  size: number
  [key: string]: unknown
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface ChangePasswordCommand {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

export interface UpdateProfileCommand {
  nickName: string
  avatar?: string
}
```

### packages/core/src/api/modules/system.ts

```typescript
// packages/core/src/api/modules/system.ts
import { client } from '../client'
import type { Result } from '../types'
import type {
  UserInfo,
  CreateUserCommand,
  UpdateUserCommand,
  PageQuery,
  PageResult,
  ChangePasswordCommand,
  UpdateProfileCommand,
} from '../../types/system'

export const userApi = {
  list: (params: PageQuery) =>
    client.get<Result<PageResult<UserInfo>>>('/system/users', { params }),

  getById: (id: string) =>
    client.get<Result<UserInfo>>(`/system/users/${id}`),

  create: (data: CreateUserCommand) =>
    client.post<Result<string>>('/system/users', data),

  update: (data: UpdateUserCommand) =>
    client.put<Result<void>>(`/system/users/${data.id}`, data),

  remove: (id: string) =>
    client.delete<Result<void>>(`/system/users/${id}`),

  freeze: (id: string) =>
    client.put<Result<void>>(`/system/users/${id}/freeze`),

  unfreeze: (id: string) =>
    client.put<Result<void>>(`/system/users/${id}/unfreeze`),

  changePassword: (data: ChangePasswordCommand) =>
    client.put<Result<void>>('/system/users/password', data),

  updateProfile: (data: UpdateProfileCommand) =>
    client.put<Result<void>>('/system/users/profile', data),

  uploadAvatar: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return client.post<Result<string>>('/system/users/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}
```

### packages/core/src/api/modules/admin.ts

```typescript
// packages/core/src/api/modules/admin.ts
import { client } from '../client'
import type { Result } from '../types'
import type {
  Role,
  CreateRoleCommand,
  UpdateRoleCommand,
  MenuItem,
  CreateMenuCommand,
  UpdateMenuCommand,
  DictType,
  CreateDictTypeCommand,
  DictData,
  CreateDictDataCommand,
} from '../../types/admin'
import type { PageQuery, PageResult } from '../../types/system'

export const adminApi = {
  // --- Role ---
  roleList: (params?: PageQuery) =>
    client.get<Result<PageResult<Role>>>('/admin/roles', { params }),

  roleCreate: (data: CreateRoleCommand) =>
    client.post<Result<string>>('/admin/roles', data),

  roleUpdate: (data: UpdateRoleCommand) =>
    client.put<Result<void>>(`/admin/roles/${data.id}`, data),

  roleRemove: (id: string) =>
    client.delete<Result<void>>(`/admin/roles/${id}`),

  roleDetail: (id: string) =>
    client.get<Result<Role>>(`/admin/roles/${id}`),

  // --- Menu ---
  menuTree: () =>
    client.get<Result<MenuItem[]>>('/admin/menus/tree'),

  menuCreate: (data: CreateMenuCommand) =>
    client.post<Result<string>>('/admin/menus', data),

  menuUpdate: (data: UpdateMenuCommand) =>
    client.put<Result<void>>(`/admin/menus/${data.id}`, data),

  menuRemove: (id: string) =>
    client.delete<Result<void>>(`/admin/menus/${id}`),

  // --- Dict Type ---
  dictTypeList: (params?: PageQuery) =>
    client.get<Result<PageResult<DictType>>>('/admin/dict/types', { params }),

  dictTypeCreate: (data: CreateDictTypeCommand) =>
    client.post<Result<string>>('/admin/dict/types', data),

  dictTypeUpdate: (data: CreateDictTypeCommand & { id: string }) =>
    client.put<Result<void>>(`/admin/dict/types/${data.id}`, data),

  dictTypeRemove: (id: string) =>
    client.delete<Result<void>>(`/admin/dict/types/${id}`),

  // --- Dict Data ---
  dictDataList: (dictType: string) =>
    client.get<Result<DictData[]>>(`/admin/dict/data/${dictType}`),

  dictDataCreate: (data: CreateDictDataCommand) =>
    client.post<Result<string>>('/admin/dict/data', data),

  dictDataUpdate: (data: CreateDictDataCommand & { id: string }) =>
    client.put<Result<void>>(`/admin/dict/data/${data.id}`, data),

  dictDataRemove: (id: string) =>
    client.delete<Result<void>>(`/admin/dict/data/${id}`),
}
```

---

## 2. Views Source Code

### 2.1 views/system/user/UserList.vue

```vue
<!-- apps/admin/src/views/system/user/UserList.vue -->
<template>
  <div class="p-4">
    <!-- Search Bar -->
    <el-card shadow="never" class="mb-4">
      <el-form :model="searchForm" inline>
        <el-form-item label="手机号">
          <el-input
            v-model="searchForm.mobile"
            placeholder="请输入手机号"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option
              v-for="item in userStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Toolbar + Table -->
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-lg font-bold">用户列表</span>
          <el-button
            v-permission="'system:user:create'"
            type="primary"
            @click="handleCreate"
          >
            <el-icon><Plus /></el-icon>
            新增用户
          </el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        row-key="id"
      >
        <el-table-column prop="mobile" label="手机号" width="140" />
        <el-table-column prop="nickName" label="昵称" min-width="120" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">
              {{ userStatusMap[row.status] || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-permission="'system:user:edit'"
              type="primary"
              link
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-permission="'system:user:freeze'"
              :type="row.status === 0 ? 'warning' : 'success'"
              link
              @click="handleToggleFreeze(row)"
            >
              {{ row.status === 0 ? '冻结' : '解冻' }}
            </el-button>
            <el-popconfirm
              title="确认删除该用户？"
              confirm-button-text="确认"
              cancel-button-text="取消"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button
                  v-permission="'system:user:delete'"
                  type="danger"
                  link
                >
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="flex justify-end mt-4">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="80px"
      >
        <el-form-item label="手机号" prop="mobile">
          <el-input
            v-model="formData.mobile"
            placeholder="请输入手机号"
            maxlength="11"
          />
        </el-form-item>
        <el-form-item label="昵称" prop="nickName">
          <el-input
            v-model="formData.nickName"
            placeholder="请输入昵称"
            maxlength="32"
          />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="formData.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
            <el-radio :value="0">未知</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="formData.status" placeholder="请选择状态">
            <el-option
              v-for="item in userStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!formData.id" label="密码" prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="请输入初始密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { userApi } from '@matecloud/core'
import type { UserInfo, CreateUserCommand, UpdateUserCommand } from '@matecloud/core'
import type { FormInstance, FormRules } from 'element-plus'

defineOptions({ name: 'UserList' })

// --- Dict ---
const userStatusOptions = [
  { label: '正常', value: 0 },
  { label: '冻结', value: 1 },
]
const userStatusMap: Record<number, string> = {
  0: '正常',
  1: '冻结',
}

// --- Search ---
const searchForm = reactive({
  mobile: '',
  status: undefined as number | undefined,
})

// --- Pagination ---
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

// --- Table ---
const loading = ref(false)
const tableData = ref<UserInfo[]>([])

async function fetchList() {
  loading.value = true
  try {
    const res = await userApi.list({
      page: pagination.page,
      size: pagination.size,
      mobile: searchForm.mobile || undefined,
      status: searchForm.status,
    })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch (err: unknown) {
    ElMessage.error('加载用户列表失败')
    console.error(err)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchList()
}

function handleReset() {
  searchForm.mobile = ''
  searchForm.status = undefined
  handleSearch()
}

// --- Dialog ---
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive<CreateUserCommand & { id?: string }>({
  id: undefined,
  mobile: '',
  nickName: '',
  gender: 0,
  status: 0,
  password: '',
})

const formRules: FormRules = {
  mobile: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  nickName: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' },
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' },
  ],
}

function handleCreate() {
  dialogTitle.value = '新增用户'
  Object.assign(formData, {
    id: undefined,
    mobile: '',
    nickName: '',
    gender: 0,
    status: 0,
    password: '',
  })
  dialogVisible.value = true
}

function handleEdit(row: UserInfo) {
  dialogTitle.value = '编辑用户'
  Object.assign(formData, {
    id: row.id,
    mobile: row.mobile,
    nickName: row.nickName,
    gender: row.gender ?? 0,
    status: row.status,
    password: '',
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (formData.id) {
      await userApi.update(formData as UpdateUserCommand)
      ElMessage.success('更新成功')
    } else {
      await userApi.create(formData as CreateUserCommand)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch (err: unknown) {
    ElMessage.error(formData.id ? '更新失败' : '创建失败')
    console.error(err)
  } finally {
    submitLoading.value = false
  }
}

function resetForm() {
  formRef.value?.resetFields()
}

// --- Actions ---
async function handleToggleFreeze(row: UserInfo) {
  try {
    if (row.status === 0) {
      await userApi.freeze(row.id)
      ElMessage.success('已冻结')
    } else {
      await userApi.unfreeze(row.id)
      ElMessage.success('已解冻')
    }
    fetchList()
  } catch (err: unknown) {
    ElMessage.error('操作失败')
    console.error(err)
  }
}

async function handleDelete(row: UserInfo) {
  try {
    await userApi.remove(row.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch (err: unknown) {
    ElMessage.error('删除失败')
    console.error(err)
  }
}

// --- Utils ---
function formatDateTime(val: string): string {
  if (!val) return ''
  const d = new Date(val)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

// --- Init ---
onMounted(() => {
  fetchList()
})
</script>

<style scoped>
/* Layout utilities come from UnoCSS; no extra styles needed */
</style>
```

### 2.2 views/system/user/index.ts

```typescript
// apps/admin/src/views/system/user/index.ts
export { default as UserList } from './UserList.vue'
```

### 2.3 views/admin/role/RoleList.vue

```vue
<!-- apps/admin/src/views/admin/role/RoleList.vue -->
<template>
  <div class="p-4">
    <!-- Toolbar + Table -->
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="text-lg font-bold">角色管理</span>
          <el-button
            v-permission="'admin:role:create'"
            type="primary"
            @click="handleCreate"
          >
            <el-icon><Plus /></el-icon>
            新增角色
          </el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        row-key="id"
      >
        <el-table-column prop="roleName" label="角色名称" min-width="120" />
        <el-table-column prop="roleKey" label="权限标识" min-width="120" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">
              {{ row.status === 0 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-permission="'admin:role:edit'"
              type="primary"
              link
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-popconfirm
              title="确认删除该角色？"
              confirm-button-text="确认"
              cancel-button-text="取消"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button
                  v-permission="'admin:role:delete'"
                  type="danger"
                  link
                >
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="flex justify-end mt-4">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <!-- Create/Edit Dialog with Menu Tree -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="formData.roleName" placeholder="请输入角色名称" maxlength="32" />
        </el-form-item>
        <el-form-item label="权限标识" prop="roleKey">
          <el-input v-model="formData.roleKey" placeholder="请输入权限标识（如 admin）" maxlength="64" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="formData.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="2"
            placeholder="请输入备注"
            maxlength="200"
          />
        </el-form-item>
        <el-form-item label="菜单权限">
          <div class="w-full border border-gray-200 rounded p-2 max-h-64 overflow-y-auto">
            <el-tree
              ref="menuTreeRef"
              :data="menuTreeData"
              :props="menuTreeProps"
              show-checkbox
              node-key="id"
              :default-checked-keys="formData.menuIds"
              check-strictly
              highlight-current
            />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { adminApi } from '@matecloud/core'
import type { Role, CreateRoleCommand, UpdateRoleCommand, MenuItem } from '@matecloud/core'
import type { FormInstance, FormRules } from 'element-plus'
import type ElTree from 'element-plus/es/components/tree'

defineOptions({ name: 'RoleList' })

// --- Pagination ---
const pagination = reactive({ page: 1, size: 10, total: 0 })

// --- Table ---
const loading = ref(false)
const tableData = ref<Role[]>([])

async function fetchList() {
  loading.value = true
  try {
    const res = await adminApi.roleList({
      page: pagination.page,
      size: pagination.size,
    })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch {
    ElMessage.error('加载角色列表失败')
  } finally {
    loading.value = false
  }
}

// --- Menu Tree ---
const menuTreeRef = ref<InstanceType<typeof ElTree>>()
const menuTreeData = ref<MenuItem[]>([])
const menuTreeProps = {
  label: 'name',
  children: 'children',
}

async function fetchMenuTree() {
  try {
    const res = await adminApi.menuTree()
    menuTreeData.value = res.data
  } catch {
    ElMessage.error('加载菜单树失败')
  }
}

// --- Dialog ---
const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive<CreateRoleCommand & { id?: string }>({
  id: undefined,
  roleName: '',
  roleKey: '',
  status: 0,
  sort: 0,
  remark: '',
  menuIds: [],
})

const formRules: FormRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [
    { required: true, message: '请输入权限标识', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '只能包含字母、数字、下划线，以字母开头', trigger: 'blur' },
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

function handleCreate() {
  dialogTitle.value = '新增角色'
  Object.assign(formData, {
    id: undefined,
    roleName: '',
    roleKey: '',
    status: 0,
    sort: 0,
    remark: '',
    menuIds: [],
  })
  dialogVisible.value = true
  nextTick(() => {
    menuTreeRef.value?.setCheckedKeys([])
  })
}

async function handleEdit(row: Role) {
  dialogTitle.value = '编辑角色'
  try {
    const res = await adminApi.roleDetail(row.id)
    const detail = res.data
    Object.assign(formData, {
      id: detail.id,
      roleName: detail.roleName,
      roleKey: detail.roleKey,
      status: detail.status,
      sort: detail.sort,
      remark: detail.remark || '',
      menuIds: detail.menuIds || [],
    })
    dialogVisible.value = true
    nextTick(() => {
      menuTreeRef.value?.setCheckedKeys(formData.menuIds)
    })
  } catch {
    ElMessage.error('加载角色详情失败')
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // Gather checked menu IDs from tree
  const checkedKeys = menuTreeRef.value?.getCheckedKeys(false) as string[] ?? []
  const halfCheckedKeys = menuTreeRef.value?.getHalfCheckedKeys() as string[] ?? []
  const allMenuIds = [...checkedKeys, ...halfCheckedKeys]

  submitLoading.value = true
  try {
    if (formData.id) {
      await adminApi.roleUpdate({
        ...formData,
        id: formData.id,
        menuIds: allMenuIds,
      } as UpdateRoleCommand)
      ElMessage.success('更新成功')
    } else {
      await adminApi.roleCreate({
        ...formData,
        menuIds: allMenuIds,
      } as CreateRoleCommand)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch {
    ElMessage.error(formData.id ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

function resetForm() {
  formRef.value?.resetFields()
  menuTreeRef.value?.setCheckedKeys([])
}

async function handleDelete(row: Role) {
  try {
    await adminApi.roleRemove(row.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch {
    ElMessage.error('删除失败')
  }
}

// --- Utils ---
function formatDateTime(val: string): string {
  if (!val) return ''
  const d = new Date(val)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

// --- Init ---
onMounted(() => {
  fetchList()
  fetchMenuTree()
})
</script>

<style scoped>
/* UnoCSS provides all layout utilities */
</style>
```

### 2.4 views/admin/menu/MenuTree.vue

```vue
<!-- apps/admin/src/views/admin/menu/MenuTree.vue -->
<template>
  <div class="p-4 flex gap-4 h-[calc(100vh-120px)]">
    <!-- Left: Menu Tree -->
    <el-card shadow="never" class="w-1/3 flex flex-col">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-bold">菜单树</span>
          <el-button
            v-permission="'admin:menu:create'"
            type="primary"
            size="small"
            @click="handleCreateRoot"
          >
            <el-icon><Plus /></el-icon>
            新增根菜单
          </el-button>
        </div>
      </template>
      <div class="flex-1 overflow-y-auto">
        <el-tree
          ref="treeRef"
          v-loading="treeLoading"
          :data="menuTreeData"
          :props="treeProps"
          node-key="id"
          default-expand-all
          highlight-current
          :expand-on-click-node="false"
          @node-click="handleNodeClick"
        >
          <template #default="{ node, data }">
            <div class="flex items-center justify-between w-full pr-2">
              <span class="flex items-center gap-1">
                <el-icon v-if="data.icon" :size="14">
                  <component :is="data.icon" />
                </el-icon>
                <span>{{ node.label }}</span>
                <el-tag v-if="data.type === 'BUTTON'" size="small" type="warning" class="ml-1">
                  按钮
                </el-tag>
              </span>
              <span class="flex gap-1">
                <el-button
                  v-permission="'admin:menu:create'"
                  type="primary"
                  link
                  size="small"
                  @click.stop="handleCreateChild(data)"
                >
                  添加
                </el-button>
                <el-popconfirm
                  title="确认删除该菜单？子菜单将一并删除。"
                  confirm-button-text="确认"
                  cancel-button-text="取消"
                  @confirm="handleDelete(data)"
                >
                  <template #reference>
                    <el-button
                      v-permission="'admin:menu:delete'"
                      type="danger"
                      link
                      size="small"
                      @click.stop
                    >
                      删除
                    </el-button>
                  </template>
                </el-popconfirm>
              </span>
            </div>
          </template>
        </el-tree>
      </div>
    </el-card>

    <!-- Right: Menu Form -->
    <el-card shadow="never" class="w-2/3">
      <template #header>
        <span class="font-bold">{{ formTitle }}</span>
      </template>

      <el-form
        v-if="showForm"
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
        class="max-w-lg"
      >
        <el-form-item label="上级菜单">
          <el-input :model-value="parentName" disabled />
        </el-form-item>
        <el-form-item label="菜单类型" prop="type">
          <el-radio-group v-model="formData.type">
            <el-radio-button value="DIRECTORY">目录</el-radio-button>
            <el-radio-button value="MENU">菜单</el-radio-button>
            <el-radio-button value="BUTTON">按钮</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入菜单名称" maxlength="32" />
        </el-form-item>
        <el-form-item v-if="formData.type !== 'BUTTON'" label="路由路径" prop="path">
          <el-input v-model="formData.path" placeholder="请输入路由路径（如 /system/user）" />
        </el-form-item>
        <el-form-item v-if="formData.type === 'MENU'" label="组件路径" prop="component">
          <el-input v-model="formData.component" placeholder="请输入组件路径（如 system/user/UserList）" />
        </el-form-item>
        <el-form-item v-if="formData.type !== 'BUTTON'" label="图标" prop="icon">
          <el-input v-model="formData.icon" placeholder="请输入图标名称（如 Setting）" />
        </el-form-item>
        <el-form-item label="权限标识" prop="perms">
          <el-input v-model="formData.perms" placeholder="请输入权限标识（如 system:user:list）" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="formData.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item v-if="formData.type !== 'BUTTON'" label="是否可见" prop="visible">
          <el-switch v-model="formData.visible" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            {{ isEdit ? '更新' : '创建' }}
          </el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>

      <el-empty v-else description="请从左侧树选择菜单或点击新增" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { adminApi } from '@matecloud/core'
import type { MenuItem, CreateMenuCommand, UpdateMenuCommand } from '@matecloud/core'
import type { FormInstance, FormRules } from 'element-plus'

defineOptions({ name: 'MenuTree' })

// --- Tree ---
const treeRef = ref()
const treeLoading = ref(false)
const menuTreeData = ref<MenuItem[]>([])
const treeProps = { label: 'name', children: 'children' }

async function fetchMenuTree() {
  treeLoading.value = true
  try {
    const res = await adminApi.menuTree()
    menuTreeData.value = res.data
  } catch {
    ElMessage.error('加载菜单树失败')
  } finally {
    treeLoading.value = false
  }
}

// --- Form State ---
const showForm = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const parentName = ref('根目录')

const formTitle = computed(() => {
  if (!showForm.value) return '菜单详情'
  return isEdit.value ? '编辑菜单' : '新增菜单'
})

const formData = reactive<CreateMenuCommand & { id?: string }>({
  id: undefined,
  parentId: '0',
  name: '',
  path: '',
  component: '',
  icon: '',
  perms: '',
  type: 'MENU',
  sort: 0,
  visible: true,
  status: 0,
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
  path: [{ required: true, message: '请输入路由路径', trigger: 'blur' }],
  sort: [{ required: true, message: '请输入排序值', trigger: 'blur' }],
}

function resetFormData(parentId: string = '0', parentLabel: string = '根目录') {
  Object.assign(formData, {
    id: undefined,
    parentId,
    name: '',
    path: '',
    component: '',
    icon: '',
    perms: '',
    type: 'MENU' as const,
    sort: 0,
    visible: true,
    status: 0,
  })
  parentName.value = parentLabel
  isEdit.value = false
  showForm.value = true
}

// --- Tree Actions ---
function handleNodeClick(data: MenuItem) {
  Object.assign(formData, {
    id: data.id,
    parentId: data.parentId,
    name: data.name,
    path: data.path || '',
    component: data.component || '',
    icon: data.icon || '',
    perms: data.perms || '',
    type: data.type,
    sort: data.sort,
    visible: data.visible,
    status: data.status,
  })
  // Find parent name
  const parent = findNodeById(menuTreeData.value, data.parentId)
  parentName.value = parent ? parent.name : '根目录'
  isEdit.value = true
  showForm.value = true
}

function handleCreateRoot() {
  resetFormData('0', '根目录')
}

function handleCreateChild(parentNode: MenuItem) {
  resetFormData(parentNode.id, parentNode.name)
}

function handleCancel() {
  showForm.value = false
  formRef.value?.resetFields()
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await adminApi.menuUpdate(formData as UpdateMenuCommand)
      ElMessage.success('更新成功')
    } else {
      await adminApi.menuCreate(formData as CreateMenuCommand)
      ElMessage.success('创建成功')
    }
    await fetchMenuTree()
    showForm.value = false
  } catch {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(data: MenuItem) {
  try {
    await adminApi.menuRemove(data.id)
    ElMessage.success('删除成功')
    showForm.value = false
    await fetchMenuTree()
  } catch {
    ElMessage.error('删除失败')
  }
}

// --- Helpers ---
function findNodeById(tree: MenuItem[], id: string): MenuItem | null {
  for (const node of tree) {
    if (node.id === id) return node
    if (node.children) {
      const found = findNodeById(node.children, id)
      if (found) return found
    }
  }
  return null
}

// --- Init ---
onMounted(() => {
  fetchMenuTree()
})
</script>

<style scoped>
/* UnoCSS provides all layout utilities */
</style>
```

### 2.5 views/admin/dict/DictManager.vue

```vue
<!-- apps/admin/src/views/admin/dict/DictManager.vue -->
<template>
  <div class="p-4 flex gap-4 h-[calc(100vh-120px)]">
    <!-- Left: Dict Type List -->
    <el-card shadow="never" class="w-2/5 flex flex-col">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-bold">字典类型</span>
          <el-button
            v-permission="'admin:dict:create'"
            type="primary"
            size="small"
            @click="handleCreateType"
          >
            <el-icon><Plus /></el-icon>
            新增
          </el-button>
        </div>
      </template>

      <div class="flex-1 overflow-y-auto">
        <el-table
          v-loading="typeLoading"
          :data="typeList"
          border
          stripe
          highlight-current-row
          size="small"
          @current-change="handleTypeSelect"
        >
          <el-table-column prop="dictName" label="字典名称" min-width="100" />
          <el-table-column prop="dictType" label="字典类型" min-width="100" />
          <el-table-column prop="status" label="状态" width="70" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 0 ? 'success' : 'danger'">
                {{ row.status === 0 ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110" align="center">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleEditType(row)">
                编辑
              </el-button>
              <el-popconfirm
                title="确认删除该字典类型？关联数据将一并删除。"
                @confirm="handleDeleteType(row)"
              >
                <template #reference>
                  <el-button type="danger" link size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- Right: Dict Data List -->
    <el-card shadow="never" class="w-3/5 flex flex-col">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-bold">
            字典数据
            <el-tag v-if="selectedType" class="ml-2" size="small">{{ selectedType.dictName }}</el-tag>
          </span>
          <el-button
            v-if="selectedType"
            v-permission="'admin:dict:create'"
            type="primary"
            size="small"
            @click="handleCreateData"
          >
            <el-icon><Plus /></el-icon>
            新增数据
          </el-button>
        </div>
      </template>

      <div v-if="selectedType" class="flex-1 overflow-y-auto">
        <el-table
          v-loading="dataLoading"
          :data="dataList"
          border
          stripe
          size="small"
        >
          <el-table-column prop="dictLabel" label="标签" min-width="100" />
          <el-table-column prop="dictValue" label="值" min-width="80" />
          <el-table-column prop="sort" label="排序" width="70" align="center" />
          <el-table-column prop="status" label="状态" width="70" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 0 ? 'success' : 'danger'">
                {{ row.status === 0 ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="cssClass" label="样式" width="90" />
          <el-table-column label="操作" width="110" align="center">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleEditData(row)">
                编辑
              </el-button>
              <el-popconfirm
                title="确认删除该字典数据？"
                @confirm="handleDeleteData(row)"
              >
                <template #reference>
                  <el-button type="danger" link size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <el-empty v-else description="请从左侧选择字典类型" />
    </el-card>

    <!-- Dict Type Dialog -->
    <el-dialog
      v-model="typeDialogVisible"
      :title="typeDialogTitle"
      width="450px"
      destroy-on-close
    >
      <el-form
        ref="typeFormRef"
        :model="typeFormData"
        :rules="typeFormRules"
        label-width="90px"
      >
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="typeFormData.dictName" placeholder="请输入字典名称" maxlength="32" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-input
            v-model="typeFormData.dictType"
            placeholder="请输入字典类型（如 user_status）"
            maxlength="64"
            :disabled="!!typeFormData.id"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="typeFormData.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="typeFormData.remark" type="textarea" :rows="2" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="typeSubmitLoading" @click="handleTypeSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- Dict Data Dialog -->
    <el-dialog
      v-model="dataDialogVisible"
      :title="dataDialogTitle"
      width="450px"
      destroy-on-close
    >
      <el-form
        ref="dataFormRef"
        :model="dataFormData"
        :rules="dataFormRules"
        label-width="90px"
      >
        <el-form-item label="标签" prop="dictLabel">
          <el-input v-model="dataFormData.dictLabel" placeholder="请输入标签名（如 正常）" maxlength="32" />
        </el-form-item>
        <el-form-item label="值" prop="dictValue">
          <el-input v-model="dataFormData.dictValue" placeholder="请输入值（如 0）" maxlength="32" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="dataFormData.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="样式属性" prop="cssClass">
          <el-input v-model="dataFormData.cssClass" placeholder="css class（可选）" maxlength="64" />
        </el-form-item>
        <el-form-item label="回显样式" prop="listClass">
          <el-select v-model="dataFormData.listClass" placeholder="请选择" clearable>
            <el-option label="默认(default)" value="default" />
            <el-option label="主要(primary)" value="primary" />
            <el-option label="成功(success)" value="success" />
            <el-option label="警告(warning)" value="warning" />
            <el-option label="危险(danger)" value="danger" />
            <el-option label="信息(info)" value="info" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="dataFormData.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="dataFormData.remark" type="textarea" :rows="2" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dataSubmitLoading" @click="handleDataSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { adminApi } from '@matecloud/core'
import type {
  DictType,
  CreateDictTypeCommand,
  DictData,
  CreateDictDataCommand,
} from '@matecloud/core'
import type { FormInstance, FormRules } from 'element-plus'

defineOptions({ name: 'DictManager' })

// ============================================================
// Dict Type (Left Panel)
// ============================================================
const typeLoading = ref(false)
const typeList = ref<DictType[]>([])
const selectedType = ref<DictType | null>(null)

async function fetchTypeList() {
  typeLoading.value = true
  try {
    const res = await adminApi.dictTypeList({ page: 1, size: 999 })
    typeList.value = res.data.records
  } catch {
    ElMessage.error('加载字典类型失败')
  } finally {
    typeLoading.value = false
  }
}

function handleTypeSelect(row: DictType | null) {
  selectedType.value = row
  if (row) {
    fetchDataList(row.dictType)
  } else {
    dataList.value = []
  }
}

// -- Type Dialog --
const typeDialogVisible = ref(false)
const typeDialogTitle = ref('新增字典类型')
const typeSubmitLoading = ref(false)
const typeFormRef = ref<FormInstance>()

const typeFormData = reactive<CreateDictTypeCommand & { id?: string }>({
  id: undefined,
  dictName: '',
  dictType: '',
  status: 0,
  remark: '',
})

const typeFormRules: FormRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [
    { required: true, message: '请输入字典类型', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9_]*$/, message: '小写字母开头，只含小写字母、数字、下划线', trigger: 'blur' },
  ],
}

function handleCreateType() {
  typeDialogTitle.value = '新增字典类型'
  Object.assign(typeFormData, { id: undefined, dictName: '', dictType: '', status: 0, remark: '' })
  typeDialogVisible.value = true
}

function handleEditType(row: DictType) {
  typeDialogTitle.value = '编辑字典类型'
  Object.assign(typeFormData, {
    id: row.id,
    dictName: row.dictName,
    dictType: row.dictType,
    status: row.status,
    remark: row.remark || '',
  })
  typeDialogVisible.value = true
}

async function handleTypeSubmit() {
  if (!typeFormRef.value) return
  const valid = await typeFormRef.value.validate().catch(() => false)
  if (!valid) return

  typeSubmitLoading.value = true
  try {
    if (typeFormData.id) {
      await adminApi.dictTypeUpdate({ ...typeFormData, id: typeFormData.id })
      ElMessage.success('更新成功')
    } else {
      await adminApi.dictTypeCreate(typeFormData)
      ElMessage.success('创建成功')
    }
    typeDialogVisible.value = false
    fetchTypeList()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    typeSubmitLoading.value = false
  }
}

async function handleDeleteType(row: DictType) {
  try {
    await adminApi.dictTypeRemove(row.id)
    ElMessage.success('删除成功')
    if (selectedType.value?.id === row.id) {
      selectedType.value = null
      dataList.value = []
    }
    fetchTypeList()
  } catch {
    ElMessage.error('删除失败')
  }
}

// ============================================================
// Dict Data (Right Panel)
// ============================================================
const dataLoading = ref(false)
const dataList = ref<DictData[]>([])

async function fetchDataList(dictType: string) {
  dataLoading.value = true
  try {
    const res = await adminApi.dictDataList(dictType)
    dataList.value = res.data
  } catch {
    ElMessage.error('加载字典数据失败')
  } finally {
    dataLoading.value = false
  }
}

// -- Data Dialog --
const dataDialogVisible = ref(false)
const dataDialogTitle = ref('新增字典数据')
const dataSubmitLoading = ref(false)
const dataFormRef = ref<FormInstance>()

const dataFormData = reactive<CreateDictDataCommand & { id?: string }>({
  id: undefined,
  dictType: '',
  dictLabel: '',
  dictValue: '',
  sort: 0,
  status: 0,
  cssClass: '',
  listClass: '',
  remark: '',
})

const dataFormRules: FormRules = {
  dictLabel: [{ required: true, message: '请输入标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入值', trigger: 'blur' }],
}

function handleCreateData() {
  if (!selectedType.value) return
  dataDialogTitle.value = '新增字典数据'
  Object.assign(dataFormData, {
    id: undefined,
    dictType: selectedType.value.dictType,
    dictLabel: '',
    dictValue: '',
    sort: 0,
    status: 0,
    cssClass: '',
    listClass: '',
    remark: '',
  })
  dataDialogVisible.value = true
}

function handleEditData(row: DictData) {
  dataDialogTitle.value = '编辑字典数据'
  Object.assign(dataFormData, {
    id: row.id,
    dictType: row.dictType,
    dictLabel: row.dictLabel,
    dictValue: row.dictValue,
    sort: row.sort,
    status: row.status,
    cssClass: row.cssClass || '',
    listClass: row.listClass || '',
    remark: row.remark || '',
  })
  dataDialogVisible.value = true
}

async function handleDataSubmit() {
  if (!dataFormRef.value) return
  const valid = await dataFormRef.value.validate().catch(() => false)
  if (!valid) return

  dataSubmitLoading.value = true
  try {
    if (dataFormData.id) {
      await adminApi.dictDataUpdate({ ...dataFormData, id: dataFormData.id })
      ElMessage.success('更新成功')
    } else {
      await adminApi.dictDataCreate(dataFormData)
      ElMessage.success('创建成功')
    }
    dataDialogVisible.value = false
    if (selectedType.value) {
      fetchDataList(selectedType.value.dictType)
    }
  } catch {
    ElMessage.error('操作失败')
  } finally {
    dataSubmitLoading.value = false
  }
}

async function handleDeleteData(row: DictData) {
  try {
    await adminApi.dictDataRemove(row.id)
    ElMessage.success('删除成功')
    if (selectedType.value) {
      fetchDataList(selectedType.value.dictType)
    }
  } catch {
    ElMessage.error('删除失败')
  }
}

// --- Init ---
onMounted(() => {
  fetchTypeList()
})
</script>

<style scoped>
/* UnoCSS provides all layout utilities */
</style>
```

### 2.6 views/profile/Profile.vue

```vue
<!-- apps/admin/src/views/profile/Profile.vue -->
<template>
  <div class="p-4 max-w-2xl mx-auto">
    <!-- Avatar & Profile Section -->
    <el-card shadow="never" class="mb-4">
      <template #header>
        <span class="font-bold">个人信息</span>
      </template>

      <div class="flex items-center gap-6 mb-6">
        <div class="relative">
          <el-avatar :size="80" :src="avatarUrl" class="cursor-pointer">
            <el-icon :size="32"><UserFilled /></el-icon>
          </el-avatar>
          <el-upload
            ref="uploadRef"
            action=""
            :auto-upload="false"
            :show-file-list="false"
            accept="image/png,image/jpeg,image/gif"
            :on-change="handleAvatarChange"
          >
            <template #trigger>
              <el-button
                class="absolute -bottom-1 -right-1"
                type="primary"
                :icon="Camera"
                circle
                size="small"
              />
            </template>
          </el-upload>
        </div>
        <div>
          <p class="text-lg font-bold">{{ authStore.user?.nickName || '未设置' }}</p>
          <p class="text-gray-500">{{ authStore.user?.mobile || '' }}</p>
        </div>
      </div>

      <el-form
        ref="profileFormRef"
        :model="profileForm"
        :rules="profileRules"
        label-width="80px"
        class="max-w-md"
      >
        <el-form-item label="昵称" prop="nickName">
          <el-input v-model="profileForm.nickName" placeholder="请输入昵称" maxlength="32" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="profileLoading" @click="handleProfileSubmit">
            保存修改
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Change Password Section -->
    <el-card shadow="never">
      <template #header>
        <span class="font-bold">修改密码</span>
      </template>

      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
        class="max-w-md"
      >
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入旧密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码（6-20位）"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="passwordLoading" @click="handlePasswordSubmit">
            修改密码
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UserFilled, Camera } from '@element-plus/icons-vue'
import { userApi } from '@matecloud/core'
import { useAuthStore } from '@matecloud/core'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'

defineOptions({ name: 'Profile' })

const authStore = useAuthStore()

// --- Avatar ---
const avatarUrl = computed(() => authStore.user?.avatar || '')

async function handleAvatarChange(uploadFile: UploadFile) {
  if (!uploadFile.raw) return

  // Validate file size (max 2MB)
  if (uploadFile.raw.size > 2 * 1024 * 1024) {
    ElMessage.error('头像文件不能超过 2MB')
    return
  }

  try {
    const res = await userApi.uploadAvatar(uploadFile.raw)
    // Update store user avatar
    if (authStore.user) {
      authStore.user.avatar = res.data
    }
    ElMessage.success('头像上传成功')
  } catch {
    ElMessage.error('头像上传失败')
  }
}

// --- Profile ---
const profileLoading = ref(false)
const profileFormRef = ref<FormInstance>()

const profileForm = reactive({
  nickName: '',
})

const profileRules: FormRules = {
  nickName: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 32, message: '昵称长度 2-32 位', trigger: 'blur' },
  ],
}

async function handleProfileSubmit() {
  if (!profileFormRef.value) return
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return

  profileLoading.value = true
  try {
    await userApi.updateProfile({
      nickName: profileForm.nickName,
      avatar: authStore.user?.avatar,
    })
    // Update store
    if (authStore.user) {
      authStore.user.nickName = profileForm.nickName
    }
    ElMessage.success('修改成功')
  } catch {
    ElMessage.error('修改失败')
  } finally {
    profileLoading.value = false
  }
}

// --- Password ---
const passwordLoading = ref(false)
const passwordFormRef = ref<FormInstance>()

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

async function handlePasswordSubmit() {
  if (!passwordFormRef.value) return
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  passwordLoading.value = true
  try {
    await userApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    // Optionally logout
    authStore.logout()
  } catch {
    ElMessage.error('密码修改失败')
  } finally {
    passwordLoading.value = false
  }
}

// --- Init ---
onMounted(() => {
  if (authStore.user) {
    profileForm.nickName = authStore.user.nickName
  }
})
</script>

<style scoped>
/* UnoCSS provides all layout utilities */
</style>
```

---

## 3. Route Configuration

### 3.1 router/modules/system.ts

```typescript
// apps/admin/src/router/modules/system.ts
import type { RouteRecordRaw } from 'vue-router'

const systemRoutes: RouteRecordRaw = {
  path: '/system',
  name: 'System',
  redirect: '/system/user',
  meta: {
    title: '系统管理',
    icon: 'Setting',
    sort: 1,
  },
  children: [
    {
      path: 'user',
      name: 'SystemUser',
      component: () => import('@/views/system/user/UserList.vue'),
      meta: {
        title: '用户管理',
        icon: 'User',
        permissions: ['system:user:list'],
      },
    },
  ],
}

export default systemRoutes
```

### 3.2 router/modules/admin.ts

```typescript
// apps/admin/src/router/modules/admin.ts
import type { RouteRecordRaw } from 'vue-router'

const adminRoutes: RouteRecordRaw = {
  path: '/admin',
  name: 'Admin',
  redirect: '/admin/role',
  meta: {
    title: '权限管理',
    icon: 'Lock',
    sort: 2,
  },
  children: [
    {
      path: 'role',
      name: 'AdminRole',
      component: () => import('@/views/admin/role/RoleList.vue'),
      meta: {
        title: '角色管理',
        icon: 'Stamp',
        permissions: ['admin:role:list'],
      },
    },
    {
      path: 'menu',
      name: 'AdminMenu',
      component: () => import('@/views/admin/menu/MenuTree.vue'),
      meta: {
        title: '菜单管理',
        icon: 'Menu',
        permissions: ['admin:menu:list'],
      },
    },
    {
      path: 'dict',
      name: 'AdminDict',
      component: () => import('@/views/admin/dict/DictManager.vue'),
      meta: {
        title: '字典管理',
        icon: 'Reading',
        permissions: ['admin:dict:list'],
      },
    },
  ],
}

export default adminRoutes
```

### 3.3 router/modules/profile.ts

```typescript
// apps/admin/src/router/modules/profile.ts
import type { RouteRecordRaw } from 'vue-router'

const profileRoutes: RouteRecordRaw = {
  path: '/profile',
  name: 'Profile',
  component: () => import('@/views/profile/Profile.vue'),
  meta: {
    title: '个人中心',
    icon: 'UserFilled',
    hidden: true, // not shown in sidebar menu
  },
}

export default profileRoutes
```

### 3.4 router/index.ts (updated to include module routes)

```typescript
// apps/admin/src/router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import systemRoutes from './modules/system'
import adminRoutes from './modules/admin'
import profileRoutes from './modules/profile'

const DefaultLayout = () => import('@/layouts/DefaultLayout.vue')

// Static public routes (no auth required)
const publicRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { title: '登录', hidden: true },
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404', hidden: true },
  },
]

// Layout-wrapped authenticated routes
const layoutRoutes: RouteRecordRaw = {
  path: '/',
  component: DefaultLayout,
  redirect: '/dashboard',
  children: [
    {
      path: 'dashboard',
      name: 'Dashboard',
      component: () => import('@/views/dashboard/Dashboard.vue'),
      meta: { title: '控制台', icon: 'Odometer', affix: true },
    },
    // Module routes are spread as children of DefaultLayout
    ...systemRoutes.children!,
    ...adminRoutes.children!,
    profileRoutes,
  ],
}

// Nest system/admin routes under layout for sidebar rendering
// The meta (title, icon, sort) is used by Sidebar.vue to build menus
export const menuRoutes: RouteRecordRaw[] = [
  {
    path: '/dashboard',
    name: 'DashboardMenu',
    meta: { title: '控制台', icon: 'Odometer', sort: 0 },
    children: [],
  } as RouteRecordRaw,
  systemRoutes,
  adminRoutes,
]

const router = createRouter({
  history: createWebHistory(),
  routes: [
    ...publicRoutes,
    layoutRoutes,
    { path: '/:pathMatch(.*)*', redirect: '/404' },
  ],
})

export default router
```

---

## 4. File Tree Summary

```
apps/admin/src/
├── views/
│   ├── system/
│   │   └── user/
│   │       ├── UserList.vue          # Full CRUD with search/pagination/dialog
│   │       └── index.ts              # Re-export
│   ├── admin/
│   │   ├── role/
│   │   │   └── RoleList.vue          # Role CRUD + menu tree checkbox
│   │   ├── menu/
│   │   │   └── MenuTree.vue          # Left tree + right form
│   │   └── dict/
│   │       └── DictManager.vue       # Left type list + right data list
│   └── profile/
│       └── Profile.vue               # Avatar + nickname + password
├── router/
│   ├── index.ts                      # Router setup with all modules
│   └── modules/
│       ├── system.ts                 # /system/* routes
│       ├── admin.ts                  # /admin/* routes
│       └── profile.ts               # /profile route
```

## 5. Key Patterns Used

1. **v-permission directive** -- gate buttons by permission string, removing the element if the user lacks the permission
2. **Dict rendering** -- status columns use inline maps; production would use a `useDict` composable from `@matecloud/hooks` that fetches from cache
3. **formatDateTime** -- inline utility; production would use `@matecloud/utils` shared formatter
4. **el-popconfirm** -- all destructive actions (delete, freeze) require user confirmation
5. **Pagination** -- all list pages use `el-pagination` with `v-model:current-page` and `v-model:page-size`
6. **Form validation** -- `el-form` with `:rules` and `formRef.validate()` before submit
7. **Loading states** -- separate `loading` for table and `submitLoading` for dialog submit buttons
8. **destroy-on-close** -- all dialogs use this to reset internal state cleanly

## 6. Dependencies on Prior RFCs

| Dependency | RFC | What it provides |
|------------|-----|-----------------|
| Project skeleton | RFC-032 | pnpm workspace, vite config, tsconfig |
| Core package | RFC-033 | `@matecloud/core` (API client, types, stores) |
| UI components | RFC-033 | `@matecloud/ui` (MateTable, MateForm, MateSearch) |
| Layout + Login | RFC-034/035 | DefaultLayout, login page, auth guard, v-permission directive |
| Backend APIs | RFC-008/009 | mate-system + mate-admin REST endpoints |
