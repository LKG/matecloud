<template>
  <MatePageCard :title="t('user.title')" :description="t('user.description')">
    <template #actions>
      <MateImportExport
        export-url="/users/export"
        :export-params="keyword ? { keyword } : undefined"
        :export-filename="buildExportFileName()"
        export-perm="sys:user:list"
        import-url="/users/import"
        template-url="/users/import/template"
        template-filename="users-import-template.xlsx"
        import-perm="sys:user:add"
        :import-dialog-title="t('user.importTitle')"
        @imported="loadData"
      />
      <MateButton v-permission="'sys:user:add'" variant="primary" @click="openDialog()">
        <template #icon><Plus :size="14" /></template>{{ t('user.createUser') }}
      </MateButton>
    </template>

    <!-- Search -->
    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <MateInput
        v-model="keyword"
        :placeholder="t('user.searchPlaceholder')"
        clearable
        style="width: 260px"
        @enter="handleSearch"
      >
        <template #prefix><Search :size="14" /></template>
      </MateInput>
    </MateSearchBar>

    <!-- Table -->
    <MateTable
      :columns="columns"
      :data="tableData"
      :loading="loading"
      row-key="userId"
      :action-width="180"
      :action-label="t('common.action')"
      :empty-text="t('user.noUsers')"
    >
      <template #col-username="{ row }">
        <MateEntityCell
          :name="row.realName || row.username"
          :sub="formatUserSub(row)"
          avatar
        />
      </template>

      <template #col-status="{ row }">
        <MateTag :variant="statusVariant(row.status)" dot>
          {{ row.status === 'ACTIVE' ? t('user.active') : t('user.frozen') }}
        </MateTag>
      </template>

      <template #actions="{ row }">
        <button v-permission="'sys:user:edit'" class="mc-action-btn" @click="openDialog(row)">
          {{ t('common.edit') }}
        </button>
        <button
          v-if="row.status === 'ACTIVE'"
          v-permission="'sys:user:edit'"
          class="mc-action-btn mc-action-btn--warn"
          @click="handleFreeze(row)"
        >{{ t('user.freeze') }}</button>
        <button
          v-if="row.status === 'DISABLED'"
          v-permission="'sys:user:edit'"
          class="mc-action-btn mc-action-btn--success"
          @click="handleUnfreeze(row)"
        >{{ t('user.unfreeze') }}</button>
        <MateInlineConfirm :title="t('user.deleteConfirm')" @confirm="handleDelete(row)">
                      <button v-permission="'sys:user:delete'" class="mc-action-btn mc-action-btn--danger">
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

    <!-- Dialog -->
    <MateDialog
      v-model="dialogVisible"
      :title="editingUser ? t('user.editUser') : t('user.createUser')"
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

  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search, Plus } from 'lucide-vue-next'
import { userApi, type UserInfo } from '@matecloud/core'

defineOptions({ name: 'UserListView' })
import { MatePageCard, MateTable, MateButton, MateInput, MateTag, type TagVariantProps, MateSearchBar, MatePagination, MateDialog, MateEntityCell, MateForm, MateImportExport, type MateColumn, type FormSchema, MateMessage, MateInlineConfirm } from '@matecloud/ui'

const { t } = useI18n()

/** Map a user status to a MateTag color variant. */
function statusVariant(status?: string): NonNullable<TagVariantProps['variant']> {
  if (status === 'ACTIVE') return 'success'
  if (status === 'DISABLED' || status === 'FROZEN') return 'warning'
  return 'danger'
}

const columns: MateColumn[] = [
  { prop: 'username', label: t('login.username') },
  { prop: 'mobile', label: t('login.mobile'), width: 150 },
  { prop: 'email', label: t('user.email'), width: 180 },
  { prop: 'status', label: t('user.status'), width: 110 },
]

const loading = ref(false)
const tableData = ref<UserInfo[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')

async function loadData() {
  loading.value = true
  try {
    const res = await userApi.list({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value,
    } as any)
    tableData.value = res.data.list
    total.value = res.data.total
  } catch (e: any) {
    MateMessage.error(e?.msg || t('common.loadFailed'))
  } finally {
    loading.value = false
  }
}

function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() { keyword.value = ''; handleSearch() }

// MateImportExport (in #actions) handles export + import + template download.
// It emits `imported` after a successful import — wired to loadData below.

/** Filename suggestion passed to <MateImportExport>; server may override via Content-Disposition. */
function buildExportFileName(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const stamp = `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}-${pad(d.getHours())}${pad(d.getMinutes())}`
  return `users-${stamp}.xlsx`
}

async function handleFreeze(row: UserInfo) {
  try {
    await userApi.freeze(row.userId)
    MateMessage.success(t('common.success'))
    loadData()
  } catch (e: any) {
    MateMessage.error(e?.msg || t('common.failed'))
  }
}
async function handleUnfreeze(row: UserInfo) {
  try {
    await userApi.unfreeze(row.userId)
    MateMessage.success(t('common.success'))
    loadData()
  } catch (e: any) {
    MateMessage.error(e?.msg || t('common.failed'))
  }
}
async function handleDelete(row: UserInfo) {
  try {
    await userApi.delete(row.userId)
    MateMessage.success(t('common.success'))
    loadData()
  } catch (e: any) {
    MateMessage.error(e?.msg || t('common.failed'))
  }
}

function formatUserSub(row: UserInfo) {
  const roles = (row as any).roleCodes?.length ? (row as any).roleCodes.join(', ') : ''
  return roles ? `${row.username} · ${roles}` : row.username
}

// ---- Dialog + MateForm ----
const dialogVisible = ref(false)
const editingUser = ref<UserInfo | null>(null)
const submitting = ref(false)
const formRef = ref<InstanceType<typeof MateForm>>()
const form = reactive<Record<string, any>>({
  username: '', realName: '', mobile: '', email: '', password: '',
})

const isEdit = computed(() => !!editingUser.value)

const formSchema = computed<FormSchema[]>(() => [
  {
    field: 'username',
    label: t('login.username'),
    type: 'input',
    rules: [{ required: true, message: t('login.usernameRequired'), trigger: 'blur' }],
    disabled: () => isEdit.value,
  },
  { field: 'realName', label: t('user.displayName'), type: 'input' },
  {
    field: 'mobile',
    label: t('user.mobileNumber'),
    type: 'input',
    rules: [{ required: true, message: t('login.mobileRequired'), trigger: 'blur' }],
  },
  { field: 'email', label: t('user.emailAddress'), type: 'input' },
  {
    field: 'password',
    label: t('user.initialPassword'),
    type: 'password',
    rules: [{ required: true, message: t('login.passwordRequired'), trigger: 'blur' }],
    visible: () => !isEdit.value,
  },
])

function openDialog(user?: UserInfo) {
  editingUser.value = user || null
  form.username = user?.username || ''
  form.realName = (user as any)?.realName || ''
  form.mobile = user?.mobile || ''
  form.email = (user as any)?.email || ''
  form.password = ''
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return
  submitting.value = true
  try {
    if (editingUser.value) {
      await userApi.update(editingUser.value.userId, {
        realName: form.realName,
        mobile: form.mobile,
        email: form.email,
      } as any)
    } else {
      await userApi.create({
        mobile: form.mobile,
        nickName: form.realName,
        password: form.password,
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

onMounted(loadData)
</script>
