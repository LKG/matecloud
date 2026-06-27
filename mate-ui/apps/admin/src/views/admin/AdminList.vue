<template>
  <MatePageCard :title="t('admin.title')" :description="t('admin.description')">
    <template #actions>
      <MateImportExport
        export-url="/admin/admins/export"
        :export-filename="`admins-${stamp()}.xlsx`"
        export-perm="sys:admin:list"
        import-url="/admin/admins/import"
        template-url="/admin/admins/import/template"
        template-filename="admins-import-template.xlsx"
        import-perm="sys:admin:add"
        :import-dialog-title="t('admin.importTitle')"
        @imported="loadData"
      />
      <el-button v-permission="'sys:admin:add'" type="primary" @click="openDialog()">
        <Plus :size="14" class="mr-1" />{{ t('admin.createAdmin') }}
      </el-button>
    </template>

    <!-- Search -->
    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <el-input
        v-model="keyword"
        :placeholder="t('admin.username')"
        clearable
        style="width: 240px"
        @keyup.enter="handleSearch"
      >
        <template #prefix><Search :size="14" /></template>
      </el-input>
    </MateSearchBar>

    <!-- Batch action bar (visible only when ≥1 row selected) -->
    <MateBatchBar :count="selectedIds.length" @clear="clearSelection">
      <el-button
        v-permission="'sys:admin:edit'"
        size="small"
        :loading="batchRunning"
        @click="handleBatchEnable"
      >{{ t('admin.enable') }}</el-button>
      <el-button
        v-permission="'sys:admin:edit'"
        size="small"
        :loading="batchRunning"
        @click="handleBatchDisable"
      >{{ t('admin.disable') }}</el-button>
      <MateInlineConfirm
        :title="t('common.batchDeleteConfirm', { n: selectedIds.length })"
        @confirm="handleBatchDelete"
      >
                  <el-button
            v-permission="'sys:admin:delete'"
            size="small"
            type="danger"
            :loading="batchRunning"
          >{{ t('common.batchDelete') }}</el-button>
        
      </MateInlineConfirm>
    </MateBatchBar>

    <!-- Table -->
    <MateTable
      :columns="columns"
      :data="tableData"
      :loading="loading"
      row-key="id"
      selection
      :action-width="280"
      :action-label="t('common.action')"
      @selection-change="onSelectionChange"
    >
      <template #col-username="{ row }">
        <MateEntityCell :name="row.nickName || row.username" :sub="row.username" avatar />
      </template>

      <template #col-status="{ row }">
        <MateBadge :status="row.status">
          {{ row.status === 'ACTIVE' ? t('admin.active') : t('admin.disabled') }}
        </MateBadge>
      </template>

      <template #actions="{ row }">
        <button v-permission="'sys:admin:edit'" class="mc-action-btn" @click="openDialog(row)">
          {{ t('common.edit') }}
        </button>
        <button v-permission="'sys:admin:edit'" class="mc-action-btn" @click="openAssignRoles(row)">
          {{ t('admin.assignRoles') }}
        </button>
        <button
          v-if="row.status === 'ACTIVE'"
          v-permission="'sys:admin:edit'"
          class="mc-action-btn mc-action-btn--warn"
          @click="handleToggleStatus(row)"
        >{{ t('admin.disable') }}</button>
        <button
          v-else
          v-permission="'sys:admin:edit'"
          class="mc-action-btn mc-action-btn--success"
          @click="handleToggleStatus(row)"
        >{{ t('admin.enable') }}</button>
        <MateInlineConfirm :title="t('common.deleteConfirm')" @confirm="handleDelete(row)">
                      <button v-permission="'sys:admin:delete'" class="mc-action-btn mc-action-btn--danger">
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

    <!-- Create / Edit Dialog (merged) -->
    <MateDialog
      v-model="dialogVisible"
      :title="isEdit ? t('admin.editAdmin') : t('admin.createAdmin')"
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

    <!-- Assign Roles Dialog -->
    <MateDialog
      v-model="assignVisible"
      :title="t('admin.assignRoles')"
      :submitting="submitting"
      @submit="handleAssignRoles"
    >
      <el-checkbox-group v-model="selectedRoleIds">
        <el-checkbox v-for="role in allRoles" :key="role.id" :value="role.id" class="mb-2 w-full">
          {{ role.roleName }} ({{ role.roleKey }})
        </el-checkbox>
      </el-checkbox-group>
    </MateDialog>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search, Plus } from 'lucide-vue-next'
import { adminApi, useBatch, type BatchResult } from '@matecloud/core'

defineOptions({ name: 'AdminListView' })
import { MatePageCard, MateTable, MateBadge, MateSearchBar, MatePagination, MateDialog, MateEntityCell, MateForm, MateImportExport, MateBatchBar, type MateColumn, type FormSchema, MateMessage, MateInlineConfirm } from '@matecloud/ui'

const { t } = useI18n()

const columns: MateColumn[] = [
  { prop: 'username', label: t('admin.username') },
  { prop: 'status', label: t('admin.status'), width: 110 },
  { prop: 'createdAt', label: t('common.createdAt'), type: 'datetime', width: 160 },
]

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')

async function loadData() {
  loading.value = true
  try {
    const res = await adminApi.adminList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value,
    })
    const data: any = res.data ?? res
    if (Array.isArray(data)) {
      tableData.value = data
      total.value = data.length
    } else {
      tableData.value = data.list ?? data
      total.value = data.total ?? 0
    }
  } catch (e: any) {
    MateMessage.error(e?.msg || t('common.loadFailed'))
  } finally {
    loading.value = false
  }
}

function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() { keyword.value = ''; handleSearch() }

/** yyyymmdd-hhmm timestamp used in export filenames. Tiny inline helper —
 *  not worth pulling into a shared util until ≥3 views need it. */
function stamp(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}-${pad(d.getHours())}${pad(d.getMinutes())}`
}

// ---- Merged Create / Edit dialog ----
const dialogVisible = ref(false)
const editing = ref<any>(null)
const submitting = ref(false)
const formRef = ref<InstanceType<typeof MateForm>>()
const form = reactive<Record<string, any>>({
  username: '', password: '', nickName: '', realName: '', mobile: '', email: '', avatar: '', deptId: '',
})
const deptOptions = ref<{ label: string; value: any }[]>([])

const isEdit = computed(() => !!editing.value)

const formSchema = computed<FormSchema[]>(() => [
  {
    field: 'username',
    label: t('admin.username'),
    type: 'input',
    visible: () => !isEdit.value,
    rules: [{ required: true, message: t('admin.usernameRequired'), trigger: 'blur' }],
  },
  {
    field: 'password',
    label: t('admin.password'),
    type: 'password',
    visible: () => !isEdit.value,
    rules: [{ required: true, message: t('admin.passwordRequired'), trigger: 'blur' }],
  },
  { field: 'nickName', label: t('admin.nickName'), type: 'input' },
  { field: 'realName', label: t('admin.realName'), type: 'input' },
  { field: 'mobile', label: t('admin.mobile'), type: 'input' },
  { field: 'email', label: t('admin.email'), type: 'input' },
  {
    field: 'avatar',
    label: t('admin.avatar'),
    type: 'input',
    visible: () => isEdit.value,
    placeholder: t('admin.avatarPlaceholder'),
  },
  {
    field: 'deptId',
    label: t('admin.dept'),
    type: 'select',
    visible: () => isEdit.value,
    options: deptOptions.value,
  },
])

async function openDialog(row?: any) {
  editing.value = row ?? null
  form.username = row?.username ?? ''
  form.password = ''
  form.nickName = row?.nickName ?? ''
  form.realName = row?.realName ?? ''
  form.mobile = row?.mobile ?? ''
  form.email = row?.email ?? ''
  form.avatar = row?.avatar ?? ''
  form.deptId = row?.deptId ?? ''
  if (row && !deptOptions.value.length) {
    try {
      const res = await adminApi.deptList()
      const list: any[] = (res.data as any) ?? res ?? []
      deptOptions.value = [{ label: t('admin.deptNone'), value: '' },
        ...list.map(d => ({ label: d.deptName, value: d.id }))]
    } catch { deptOptions.value = [] }
  }
  dialogVisible.value = true
}

// MateForm only reseeds on a modelValue reference change; we mutate `form` in
// place, so repopulate explicitly each time the dialog opens (otherwise the
// edit form shows the previously-opened row's values).
watch(dialogVisible, (open) => {
  if (open) nextTick(() => formRef.value?.setModel(form))
})

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await adminApi.adminUpdate(editing.value.id, {
        nickName: form.nickName,
        realName: form.realName || undefined,
        mobile: form.mobile || undefined,
        email: form.email || undefined,
        avatar: form.avatar,
        deptId: form.deptId || undefined,
      })
    } else {
      await adminApi.adminCreate({
        username: form.username,
        password: form.password,
        nickName: form.nickName,
        realName: form.realName || undefined,
        mobile: form.mobile || undefined,
        email: form.email || undefined,
      })
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

// ---- Assign Roles ----
const assignVisible = ref(false)
const allRoles = ref<any[]>([])
const selectedRoleIds = ref<string[]>([])
const assigningRow = ref<any>(null)

async function openAssignRoles(row: any) {
  assigningRow.value = row
  selectedRoleIds.value = row.roleIds ? [...row.roleIds] : []
  try {
    const res = await adminApi.roleList()
    const data: any = res.data ?? res
    allRoles.value = Array.isArray(data) ? data : (data?.list ?? [])
  } catch {
    allRoles.value = []
  }
  assignVisible.value = true
}

async function handleAssignRoles() {
  submitting.value = true
  try {
    await adminApi.adminAssignRoles(assigningRow.value.id, selectedRoleIds.value)
    MateMessage.success(t('common.success'))
    assignVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

// ---- Toggle status / delete ----
async function handleToggleStatus(row: any) {
  try {
    if (row.status === 'ACTIVE') {
      await adminApi.adminDisable(row.id)
    } else {
      await adminApi.adminEnable(row.id)
    }
    MateMessage.success(t('common.success'))
    loadData()
  } catch { /* interceptor */ }
}

async function handleDelete(row: any) {
  await adminApi.adminDelete(row.id)
  MateMessage.success(t('common.success'))
  loadData()
}

// ========== Batch ops ==========
const selectedIds = ref<string[]>([])
const tableSelection = ref<any[]>([])
const { runBatch, running: batchRunning } = useBatch()

function onSelectionChange(rows: any[]) {
  // MateTable forwards el-table's selection-change verbatim. We keep both the
  // raw rows (for any future "what was selected" UI) and a flat id list
  // (for the batch HTTP calls, which just need ids).
  tableSelection.value = rows
  selectedIds.value = rows.map(r => r.id)
}

function clearSelection() {
  selectedIds.value = []
  tableSelection.value = []
  // No imperative el-table reset needed — emptying selectedIds re-renders the
  // bar away. The checkboxes themselves stay ticked until the user clicks the
  // header checkbox; that's an el-table limitation we accept for the demo.
}

async function handleBatchEnable() {
  await runAndReport(adminApi.adminBatchEnable)
}
async function handleBatchDisable() {
  await runAndReport(adminApi.adminBatchDisable)
}
async function handleBatchDelete() {
  await runAndReport(adminApi.adminBatchDelete)
}

async function runAndReport(api: (ids: string[]) => Promise<{ data: BatchResult }>) {
  const result = await runBatch(api, selectedIds.value)
  if (result.failCount === 0) {
    MateMessage.success(t('common.batchAllSuccess', { n: result.successCount }))
  } else {
    // Show first failure inline so admin sees the cause; full list goes in console
    const first = result.failures[0]?.message
    MateMessage.warning(
      t('common.batchPartial', { ok: result.successCount, bad: result.failCount })
        + (first ? ` — ${first}` : ''),
    )
    // Help the admin diagnose without overflowing the toast
    console.warn('[batch] failures:', result.failures)
  }
  clearSelection()
  loadData()
}

onMounted(loadData)
</script>
