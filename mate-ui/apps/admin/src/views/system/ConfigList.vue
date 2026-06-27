<template>
  <MatePageCard :title="t('config.title')" :description="t('config.description')">
    <template #actions>
      <el-button v-permission="'sys:config:add'" type="primary" @click="openDialog()">
        <Plus :size="14" class="mr-1" />{{ t('config.createConfig') }}
      </el-button>
    </template>

    <!-- Search -->
    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <el-input
        v-model="keyword"
        :placeholder="t('config.searchPlaceholder')"
        clearable
        style="width: 260px"
        @keyup.enter="handleSearch"
      >
        <template #prefix><Search :size="14" /></template>
      </el-input>
      <el-select v-model="builtInFilter" style="width: 140px">
        <el-option :label="t('config.filterAll')" value="" />
        <el-option :label="t('config.filterBuiltIn')" value="true" />
        <el-option :label="t('config.filterCustom')" value="false" />
      </el-select>
    </MateSearchBar>

    <MateTable
      :columns="columns"
      :data="rows"
      :loading="loading"
      row-key="id"
      :action-width="170"
      :action-label="t('common.action')"
      :empty-text="t('common.noData')"
    >
      <template #col-builtIn="{ row }">
        <MateBadge :type="row.builtIn ? 'warning' : 'info'">
          {{ row.builtIn ? t('config.builtInYes') : t('config.builtInNo') }}
        </MateBadge>
      </template>

      <template #actions="{ row }">
        <button v-permission="'sys:config:edit'" class="mc-action-btn" @click="openDialog(row)">
          {{ t('common.edit') }}
        </button>
        <MateInlineConfirm
          v-if="!row.builtIn"
          :title="t('common.deleteConfirm')"
          @confirm="handleDelete(row)"
        >
                      <button v-permission="'sys:config:delete'" class="mc-action-btn mc-action-btn--danger">
              {{ t('common.delete') }}
            </button>
          
        </MateInlineConfirm>
        <button
          v-else
          class="mc-action-btn"
          :title="t('config.builtInLocked')"
          disabled
        >{{ t('common.delete') }}</button>
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
      :title="editing ? t('config.editConfig') : t('config.createConfig')"
      width="520px"
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
import { Plus, Search } from 'lucide-vue-next'
import { configApi, type ConfigItem } from '@matecloud/core'

defineOptions({ name: 'ConfigListView' })
import { MatePageCard, MateTable, MateBadge, MateSearchBar, MatePagination, MateDialog, MateForm, type MateColumn, type FormSchema, MateMessage, MateInlineConfirm } from '@matecloud/ui'

const { t } = useI18n()

const columns: MateColumn[] = [
  { prop: 'configKey', label: t('config.configKey'), mono: true, minWidth: 220 },
  { prop: 'configValue', label: t('config.configValue'), minWidth: 200 },
  { prop: 'configName', label: t('config.configName'), width: 180 },
  { prop: 'builtIn', label: t('config.builtIn'), width: 110, align: 'center' },
  { prop: 'createdAt', label: t('common.createdAt'), width: 170, type: 'datetime' },
]

const loading = ref(false)
const rows = ref<ConfigItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const builtInFilter = ref<'' | 'true' | 'false'>('')

async function loadData() {
  loading.value = true
  try {
    const res = await configApi.list({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      builtIn: builtInFilter.value === '' ? undefined : builtInFilter.value === 'true',
    })
    const data: any = res.data ?? res
    if (Array.isArray(data)) {
      rows.value = data
      total.value = data.length
    } else {
      rows.value = data.list ?? []
      total.value = data.total ?? 0
    }
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() {
  keyword.value = ''
  builtInFilter.value = ''
  handleSearch()
}

// ---- dialog ----
const dialogVisible = ref(false)
const submitting = ref(false)
const editing = ref<ConfigItem | null>(null)
const formRef = ref<InstanceType<typeof MateForm>>()
const form = reactive<Record<string, any>>({
  configKey: '', configName: '', configValue: '', remark: '',
})

const isEdit = computed(() => !!editing.value)

const formSchema = computed<FormSchema[]>(() => [
  {
    field: 'configKey',
    label: t('config.configKey'),
    type: 'input',
    placeholder: 'sys.xxx.yyy',
    disabled: () => isEdit.value,
    rules: [{ required: true, message: t('config.keyRequired'), trigger: 'blur' }],
  },
  {
    field: 'configName',
    label: t('config.configName'),
    type: 'input',
    disabled: () => isEdit.value,
    rules: [{ required: true, message: t('config.nameRequired'), trigger: 'blur' }],
  },
  {
    field: 'configValue',
    label: t('config.configValue'),
    type: 'textarea',
    rules: [{ required: true, message: t('config.valueRequired'), trigger: 'blur' }],
  },
  { field: 'remark', label: t('config.remark'), type: 'textarea' },
])

function openDialog(row?: ConfigItem) {
  editing.value = row ?? null
  form.configKey = row?.configKey ?? ''
  form.configName = row?.configName ?? ''
  form.configValue = row?.configValue ?? ''
  form.remark = row?.remark ?? ''
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return
  submitting.value = true
  try {
    if (editing.value) {
      await configApi.update(editing.value.id, {
        configValue: form.configValue,
        remark: form.remark,
      })
      MateMessage.success(t('common.updateSuccess'))
    } else {
      await configApi.create({
        configKey: form.configKey,
        configName: form.configName,
        configValue: form.configValue,
        remark: form.remark,
      })
      MateMessage.success(t('common.createSuccess'))
    }
    dialogVisible.value = false
    loadData()
  } catch (e: any) {
    MateMessage.error(e?.msg || t('common.failed'))
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: ConfigItem) {
  if (row.builtIn) {
    MateMessage.warning(t('config.builtInLocked'))
    return
  }
  await configApi.delete(row.id)
  MateMessage.success(t('common.deleteSuccess'))
  loadData()
}

onMounted(loadData)
</script>
