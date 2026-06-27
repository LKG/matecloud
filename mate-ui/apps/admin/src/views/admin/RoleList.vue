<template>
  <MatePageCard :title="t('role.title')" :description="t('role.description')">
    <template #actions>
      <el-button v-permission="'sys:role:add'" type="primary" @click="openDialog()">
        <Plus :size="14" class="mr-1" />{{ t('role.createRole') }}
      </el-button>
    </template>

    <!-- Search -->
    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <el-input
        v-model="keyword"
        :placeholder="t('role.searchPlaceholder')"
        clearable
        style="width: 240px"
        @keyup.enter="handleSearch"
      >
        <template #prefix><Search :size="14" /></template>
      </el-input>
    </MateSearchBar>

    <!-- Table -->
    <MateTable
      :columns="columns"
      :data="tableData"
      :loading="loading"
      row-key="id"
      :action-width="300"
      :action-label="t('common.action')"
      :empty-text="t('role.noRoles')"
    >
      <template #col-roleKey="{ row }">
        <MateEntityCell
          :name="row.roleName"
          :sub="row.roleKey"
          sub-mono
        />
      </template>

      <template #col-status="{ row }">
        <MateBadge :status="row.status">
          {{ row.status === 'ACTIVE' ? t('common.enable') : t('common.disable') }}
        </MateBadge>
      </template>

      <template #actions="{ row }">
        <button v-permission="'sys:role:edit'" class="mc-action-btn" @click="openDialog(row)">
          {{ t('common.edit') }}
        </button>
        <button v-permission="'sys:role:edit'" class="mc-action-btn" @click="openAssignMenus(row)">
          {{ t('role.assignMenus') }}
        </button>
        <button v-permission="'sys:role:edit'" class="mc-action-btn" @click="openDataScope(row)">
          {{ t('role.dataScope') }}
        </button>
        <MateInlineConfirm :title="t('common.deleteConfirm')" @confirm="handleDelete(row)">
                      <button v-permission="'sys:role:delete'" class="mc-action-btn mc-action-btn--danger">
              {{ t('common.delete') }}
            </button>
          
        </MateInlineConfirm>
      </template>
    </MateTable>

    <!-- Pagination -->
    <MatePagination
      v-model:page-num="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      @change="loadData"
    />

    <!-- Create / Edit Dialog -->
    <MateDialog
      v-model="dialogVisible"
      :title="isEdit ? t('role.editRole') : t('role.createRole')"
      :submitting="submitting"
      @submit="handleSubmit"
    >
      <MateForm
        ref="formRef"
        :schema="formSchema"
        :model-value="form"
        label-position="top"
        @update:model-value="(v: any) => Object.assign(form, v)"
      />
    </MateDialog>

    <!-- Assign Menus Dialog -->
    <MateDialog
      v-model="assignMenuVisible"
      :title="t('role.assignMenus')"
      width="560px"
      :submitting="submitting"
      @submit="handleAssignMenus"
    >
      <div class="menu-tree-wrap">
        <el-tree
          ref="menuTreeRef"
          :data="menuTreeData"
          :props="treeProps"
          show-checkbox
          node-key="id"
          default-expand-all
        />
      </div>
    </MateDialog>

    <!-- Data Scope Dialog -->
    <MateDialog
      v-model="dataScopeVisible"
      :title="t('role.dataScope')"
      width="480px"
      :submitting="submitting"
      @submit="handleDataScopeSubmit"
    >
      <el-form label-position="top">
        <el-form-item :label="t('role.dataScope')">
          <el-select v-model="dataScopeForm.dataScope" style="width: 100%">
            <el-option :label="t('role.scopeAll')" :value="1" />
            <el-option :label="t('role.scopeDept')" :value="2" />
            <el-option :label="t('role.scopeDeptChild')" :value="3" />
            <el-option :label="t('role.scopeSelf')" :value="4" />
            <el-option :label="t('role.scopeCustom')" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="dataScopeForm.dataScope === 5" :label="t('role.customDepts')">
          <el-select v-model="dataScopeForm.customDeptIds" multiple filterable style="width: 100%">
            <el-option v-for="d in deptOptions" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
      </el-form>
    </MateDialog>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search, Plus, Shield } from 'lucide-vue-next'
import { adminApi } from '@matecloud/core'

defineOptions({ name: 'RoleListView' })
import { MatePageCard, MateTable, MateBadge, MateSearchBar, MatePagination, MateDialog, MateEntityCell, MateForm, type MateColumn, type FormSchema, MateMessage, MateInlineConfirm } from '@matecloud/ui'

const { t } = useI18n()

const columns: MateColumn[] = [
  { prop: 'roleKey', label: t('role.roleName') },
  { prop: 'sort', label: t('role.sort'), width: 100, mono: true },
  { prop: 'status', label: t('role.status'), width: 110 },
]

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')

// Mock data kept for offline development (until backend returns paginated results)
function generateMockRoles(): any[] {
  const names = [
    'Super Admin', 'Admin', 'Editor', 'Viewer', 'Operator',
    'Auditor', 'Finance', 'HR Manager', 'Dev Lead', 'QA Lead',
    'PM', 'Designer', 'Support', 'Marketing', 'Sales',
    'Data Analyst', 'Security', 'DBA', 'DevOps', 'Intern',
    'CTO', 'VP Eng', 'Tech Lead', 'Architect',
  ]
  return names.map((name, i) => ({
    id: String(i + 1),
    roleKey: name.toLowerCase().replace(/\s+/g, '_'),
    roleName: name,
    sort: i,
    status: i % 5 === 4 ? 'DISABLED' : 'ACTIVE',
  }))
}

async function loadData() {
  loading.value = true
  try {
    const res = await adminApi.roleList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
    })
    // roleList returns either a Role[] (legacy) or PageResult<Role>; normalize.
    const payload = res.data
    if (Array.isArray(payload)) {
      tableData.value = payload
      total.value = payload.length
    } else {
      tableData.value = payload.list ?? []
      total.value = payload.total ?? 0
    }
  } catch {
    // API unavailable — use mock data for development
    MateMessage.warning(t('common.apiUnavailable'))
    const all = generateMockRoles()
    const filtered = keyword.value
      ? all.filter(r => r.roleName.toLowerCase().includes(keyword.value.toLowerCase()) || r.roleKey.includes(keyword.value.toLowerCase()))
      : all
    total.value = filtered.length
    const start = (pageNum.value - 1) * pageSize.value
    tableData.value = filtered.slice(start, start + pageSize.value)
  } finally {
    loading.value = false
  }
}

function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() { keyword.value = ''; handleSearch() }

// ---- Create / Edit Dialog ----
const dialogVisible = ref(false)
const editingRole = ref<any>(null)
const submitting = ref(false)
const formRef = ref<InstanceType<typeof MateForm>>()
const form = reactive<Record<string, any>>({ roleKey: '', roleName: '', sort: 0 })

const isEdit = computed(() => !!editingRole.value)

const formSchema = computed<FormSchema[]>(() => [
  {
    field: 'roleKey',
    label: t('role.roleKey'),
    type: 'input',
    rules: [{ required: true, message: t('role.roleKeyRequired'), trigger: 'blur' }],
    disabled: () => isEdit.value,
  },
  {
    field: 'roleName',
    label: t('role.roleName'),
    type: 'input',
    rules: [{ required: true, message: t('role.roleNameRequired'), trigger: 'blur' }],
  },
  { field: 'sort', label: t('role.sort'), type: 'number' },
])

function openDialog(role?: any) {
  editingRole.value = role || null
  form.roleKey = role?.roleKey || ''
  form.roleName = role?.roleName || ''
  form.sort = role?.sort ?? 0
  dialogVisible.value = true
}

// MateForm only reseeds on a modelValue reference change; we mutate `form` in
// place, so repopulate explicitly each time the dialog opens (otherwise the
// form shows the previously-edited row's values on the 2nd+ open).
watch(dialogVisible, (open) => {
  if (open) nextTick(() => formRef.value?.setModel(form))
})

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return
  submitting.value = true
  try {
    if (editingRole.value) {
      await adminApi.roleUpdate(editingRole.value.id, { roleName: form.roleName, sort: form.sort })
    } else {
      await adminApi.roleCreate({ roleKey: form.roleKey, roleName: form.roleName, sort: form.sort })
    }
    MateMessage.success(t('common.success'))
    dialogVisible.value = false
    loadData()
  } catch (e: any) {
    MateMessage.error(e?.msg || t('common.failed'))
  } finally {
    submitting.value = false
  }
}

// ---- Assign Menus ----
const assignMenuVisible = ref(false)
const menuTreeRef = ref()
const menuTreeData = ref<any[]>([])
const treeProps = { label: 'name', children: 'children' }
const currentRole = ref<any>(null)

async function openAssignMenus(row: any) {
  currentRole.value = row
  try {
    const [treeRes, roleRes] = await Promise.all([
      adminApi.menuTree(),
      adminApi.roleGetById(row.id),
    ])
    menuTreeData.value = (treeRes.data as any) ?? treeRes
    const role: any = roleRes.data ?? roleRes
    const menuIds: string[] = role.menuIds ?? []
    assignMenuVisible.value = true
    await nextTick()
    const leafIds = getLeafIds(menuTreeData.value, menuIds)
    menuTreeRef.value?.setCheckedKeys(leafIds)
  } catch {
    menuTreeData.value = []
    assignMenuVisible.value = true
  }
}

function getLeafIds(tree: any[], checkedIds: string[]): string[] {
  const leaves: string[] = []
  function walk(nodes: any[]) {
    for (const node of nodes) {
      if (node.children?.length) walk(node.children)
      else if (checkedIds.includes(node.id)) leaves.push(node.id)
    }
  }
  walk(tree)
  return leaves
}

async function handleAssignMenus() {
  submitting.value = true
  try {
    const checkedKeys = menuTreeRef.value?.getCheckedKeys() ?? []
    const halfCheckedKeys = menuTreeRef.value?.getHalfCheckedKeys() ?? []
    await adminApi.roleAssignMenus(currentRole.value.id, [...checkedKeys, ...halfCheckedKeys])
    MateMessage.success(t('common.success'))
    assignMenuVisible.value = false
  } finally {
    submitting.value = false
  }
}

// ---- Data Scope ----
const dataScopeVisible = ref(false)
const dataScopeRoleId = ref<string>('')
const dataScopeForm = reactive<{ dataScope: number; customDeptIds: string[] }>({ dataScope: 1, customDeptIds: [] })
const deptOptions = ref<{ value: string; label: string }[]>([])

async function openDataScope(row: any) {
  dataScopeRoleId.value = row.id
  dataScopeForm.dataScope = row.dataScope ?? 1
  dataScopeForm.customDeptIds = row.customDeptIds ? String(row.customDeptIds).split(',').filter(Boolean) : []
  if (!deptOptions.value.length) {
    try {
      const res = await adminApi.deptList()
      const list: any[] = (res.data as any) ?? res ?? []
      deptOptions.value = list.map(d => ({ value: d.id, label: d.deptName }))
    } catch { deptOptions.value = [] }
  }
  dataScopeVisible.value = true
}

async function handleDataScopeSubmit() {
  submitting.value = true
  try {
    await adminApi.roleUpdateDataScope(dataScopeRoleId.value, {
      dataScope: dataScopeForm.dataScope,
      customDeptIds: dataScopeForm.dataScope === 5 ? dataScopeForm.customDeptIds.join(',') : undefined,
    })
    MateMessage.success(t('common.success'))
    dataScopeVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

// ---- Delete ----
async function handleDelete(row: any) {
  await adminApi.roleDelete(row.id)
  MateMessage.success(t('common.success'))
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.menu-tree-wrap {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid var(--mc-border);
  border-radius: var(--mc-radius);
  padding: 12px;
}
</style>
