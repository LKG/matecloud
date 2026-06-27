<template>
  <MatePageCard :title="t('tenantPkg.title')" :description="t('tenantPkg.desc')">
    <template #actions>
      <el-button type="primary" @click="openDialog()">
        <Plus :size="14" class="mr-1" />{{ t('tenantPkg.create') }}
      </el-button>
    </template>

    <MateTable
      :columns="columns"
      :data="rows"
      :loading="loading"
      row-key="id"
      :action-width="140"
      :action-label="t('common.action')"
      :empty-text="t('common.noData')"
    >
      <template #col-packageCode="{ row }">
        <MateEntityCell :name="row.packageName" :sub="row.packageCode" sub-mono />
      </template>
      <template #col-maxStorage="{ row }">
        <span class="mc-mono">{{ formatStorage(row.maxStorage) }}</span>
      </template>
      <template #actions="{ row }">
        <button class="mc-action-btn" @click="openDialog(row)">{{ t('common.edit') }}</button>
        <MateInlineConfirm :title="t('common.deleteConfirm')" @confirm="handleDelete(row)">
                      <button class="mc-action-btn mc-action-btn--danger">{{ t('common.delete') }}</button>
          
        </MateInlineConfirm>
      </template>
    </MateTable>

    <MateDialog
      v-model="dialogVisible"
      :title="isEdit ? t('tenantPkg.edit') : t('tenantPkg.create')"
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
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { Plus, Box } from 'lucide-vue-next'

defineOptions({ name: 'TenantPackageListView' })
import { MatePageCard, MateTable, MateDialog, MateForm, MateEntityCell, type MateColumn, type FormSchema, MateMessage, MateInlineConfirm } from '@matecloud/ui'
import { tenantApi, type TenantPackageInfo } from '@matecloud/core'

const { t } = useI18n()

const columns: MateColumn[] = [
  { prop: 'packageCode', label: t('tenantPkg.name') },
  { prop: 'maxUsers', label: t('tenantPkg.maxUsers'), width: 120, mono: true },
  { prop: 'maxStorage', label: t('tenantPkg.maxStorage'), width: 140 },
  { prop: 'features', label: t('tenantPkg.features') },
  { prop: 'remark', label: t('tenantPkg.remark') },
]

const loading = ref(false)
const rows = ref<TenantPackageInfo[]>([])

async function loadData() {
  loading.value = true
  try {
    const res = await tenantApi.packages()
    rows.value = ((res.data as any) ?? res ?? []) as TenantPackageInfo[]
  } catch {
    rows.value = []
  } finally {
    loading.value = false
  }
}

function formatStorage(bytes: number): string {
  if (!bytes || bytes <= 0) return '∞'
  const gb = bytes / (1024 * 1024 * 1024)
  return gb >= 1 ? `${gb.toFixed(0)} GB` : `${(bytes / (1024 * 1024)).toFixed(0)} MB`
}

// ---- Create / Edit ----
const dialogVisible = ref(false)
const editing = ref<TenantPackageInfo | null>(null)
const submitting = ref(false)
const formRef = ref<InstanceType<typeof MateForm>>()
const isEdit = computed(() => !!editing.value)

const form = reactive<Record<string, any>>({
  packageCode: '', packageName: '', maxUsers: 10, maxStorage: 0, features: '', remark: '',
})

const formSchema = computed<FormSchema[]>(() => [
  {
    field: 'packageCode',
    label: t('tenantPkg.code'),
    type: 'input',
    disabled: () => isEdit.value,
    rules: [{ required: true, message: t('tenantPkg.codeRequired'), trigger: 'blur' }],
  },
  {
    field: 'packageName',
    label: t('tenantPkg.displayName'),
    type: 'input',
    rules: [{ required: true, message: t('tenantPkg.nameRequired'), trigger: 'blur' }],
  },
  { field: 'maxUsers', label: t('tenantPkg.maxUsers'), type: 'number' },
  { field: 'maxStorage', label: t('tenantPkg.maxStorageBytes'), type: 'number' },
  { field: 'features', label: t('tenantPkg.features'), type: 'input', placeholder: 'core,advanced-rbac,audit' },
  { field: 'remark', label: t('tenantPkg.remark'), type: 'textarea' },
])

function openDialog(row?: TenantPackageInfo) {
  editing.value = row ?? null
  form.packageCode = row?.packageCode ?? ''
  form.packageName = row?.packageName ?? ''
  form.maxUsers = row?.maxUsers ?? 10
  form.maxStorage = row?.maxStorage ?? 0
  form.features = row?.features ?? ''
  form.remark = row?.remark ?? ''
  dialogVisible.value = true
}

watch(dialogVisible, (open) => {
  if (open) nextTick(() => formRef.value?.setModel(form))
})

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return
  submitting.value = true
  try {
    const payload = {
      packageCode: form.packageCode,
      packageName: form.packageName,
      maxUsers: form.maxUsers,
      maxStorage: form.maxStorage,
      features: form.features,
      remark: form.remark,
    }
    if (isEdit.value) {
      await tenantApi.packageUpdate(editing.value!.id, payload)
    } else {
      await tenantApi.packageCreate(payload)
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

async function handleDelete(row: TenantPackageInfo) {
  await tenantApi.packageDelete(row.id)
  MateMessage.success(t('common.success'))
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.mc-mono { font-family: var(--mc-font-mono, monospace); }
</style>
