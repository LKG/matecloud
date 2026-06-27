<template>
  <MatePageCard :title="t('ai.agents.title')" :description="t('ai.agents.description')">
    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <el-input v-model="search.keyword" :placeholder="t('ai.agents.searchPlaceholder')"
                style="width: 200px" clearable @keyup.enter="handleSearch" />
      <el-select v-model="search.enabled" :placeholder="t('ai.agents.statusPlaceholder')" clearable style="width: 130px">
        <el-option :label="t('ai.agents.statusEnabled')" :value="1" />
        <el-option :label="t('ai.agents.statusDisabled')" :value="0" />
      </el-select>
      <el-button type="primary" :icon="Plus" @click="openCreate">{{ t('ai.agents.createBtn') }}</el-button>
    </MateSearchBar>

    <MateTable
      :columns="columns"
      :data="rows"
      :loading="loading"
      row-key="id"
      :action-width="160"
      :action-label="t('common.action')"
    >
      <template #col-name="{ row }">
        <AiAgentBadge :code="row.code" :label="row.name" size="md" />
      </template>
      <template #col-category="{ row }">
        <el-tag size="small" type="info">{{ row.category }}</el-tag>
      </template>
      <template #col-defaultModel="{ row }">
        <span class="mc-mono">{{ row.defaultModel || '-' }}</span>
      </template>
      <template #col-builtIn="{ row }">
        <MateBadge v-if="row.builtIn === 1" type="info">{{ t('ai.common.builtIn') }}</MateBadge>
        <MateBadge v-else type="default">{{ t('ai.common.custom') }}</MateBadge>
      </template>
      <template #col-enabled="{ row }">
        <el-switch :model-value="row.enabled === 1"
                   @change="(v: string | number | boolean) => toggleEnabled(row, Boolean(v))" />
      </template>
      <template #actions="{ row }">
        <button class="mc-action-btn" @click="openEdit(row)">{{ t('ai.common.edit') }}</button>
        <button class="mc-action-btn mc-action-btn--danger"
                :disabled="row.builtIn === 1"
                @click="remove(row)">{{ t('ai.common.delete') }}</button>
      </template>
    </MateTable>

    <MatePagination v-model:page-num="pageNum" v-model:page-size="pageSize"
                    :total="total" @change="loadData" />

    <MateDialog v-model="formVisible" :title="isEdit ? t('ai.agents.editTitle') : t('ai.agents.createTitle')" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item :label="t('ai.common.code')" prop="code">
          <el-input v-model="form.code" :disabled="isEdit" :placeholder="t('ai.agents.codePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('ai.agents.nameLabel')" prop="name">
          <el-input v-model="form.name" :placeholder="t('ai.agents.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('ai.agents.categoryLabel')">
          <el-select v-model="form.category" style="width: 100%">
            <el-option label="CODING" value="CODING" />
            <el-option label="CHAT" value="CHAT" />
            <el-option label="REVIEW" value="REVIEW" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('ai.agents.vendorLabel')">
          <el-input v-model="form.provider" :placeholder="t('ai.agents.vendorPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('ai.agents.defaultModelLabel')">
          <el-input v-model="form.defaultModel" :placeholder="t('ai.agents.defaultModelPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('ai.agents.descriptionLabel')">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item :label="t('ai.agents.installCmdLabel')">
          <el-input v-model="form.installCmd" :placeholder="t('ai.agents.installCmdPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">{{ t('ai.common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ t('ai.common.save') }}</el-button>
      </template>
    </MateDialog>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { aiApi, type AgentView } from '@matecloud/core'
import { MatePageCard, MateSearchBar, MateTable, MateBadge, MatePagination, MateDialog, AiAgentBadge, type MateColumn, MateMessage, MateMessageBox } from '@matecloud/ui'

defineOptions({ name: 'AiAgentsView' })

const { t } = useI18n()

const columns = computed<MateColumn[]>(() => [
  { prop: 'name', label: t('ai.agents.colAgent'), width: 240 },
  { prop: 'category', label: t('ai.agents.colCategory'), width: 100 },
  { prop: 'provider', label: t('ai.agents.colVendor'), width: 120 },
  { prop: 'defaultModel', label: t('ai.agents.colDefaultModel'), width: 200 },
  { prop: 'description', label: t('ai.agents.colDescription') },
  { prop: 'builtIn', label: t('ai.agents.colType'), width: 100, align: 'center' },
  { prop: 'enabled', label: t('ai.agents.colEnabled'), width: 80, align: 'center' },
])

const loading = ref(false)
const rows = ref<AgentView[]>([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const search = reactive({ keyword: '', enabled: undefined as number | undefined })

async function loadData() {
  loading.value = true
  try {
    const res: any = await aiApi.agentPage({
      pageNum: pageNum.value, pageSize: pageSize.value,
      keyword: search.keyword || undefined, enabled: search.enabled,
    })
    rows.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() {
  search.keyword = ''; search.enabled = undefined; handleSearch()
}

const formVisible = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<Partial<AgentView>>({
  code: '', name: '', category: 'CODING', provider: '',
  defaultModel: '', description: '', installCmd: '',
})
const rules = computed(() => ({
  code: [{ required: true, message: t('ai.agents.codeRequired'), trigger: 'blur' }],
  name: [{ required: true, message: t('ai.agents.nameRequired'), trigger: 'blur' }],
}))

function openCreate() {
  isEdit.value = false; editingId.value = null
  Object.assign(form, { code: '', name: '', category: 'CODING', provider: '',
    defaultModel: '', description: '', installCmd: '' })
  formVisible.value = true
}
function openEdit(row: AgentView) {
  isEdit.value = true; editingId.value = row.id
  Object.assign(form, row)
  formVisible.value = true
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value && editingId.value) {
      await aiApi.agentUpdate(editingId.value, form)
    } else {
      await aiApi.agentCreate(form)
    }
    MateMessage.success(t('ai.common.saveSuccess'))
    formVisible.value = false
    loadData()
  } finally { saving.value = false }
}

async function toggleEnabled(row: AgentView, val: boolean) {
  await aiApi.agentToggle(row.id, val)
  row.enabled = val ? 1 : 0
}

async function remove(row: AgentView) {
  if (row.builtIn === 1) { MateMessage.warning(t('ai.agents.builtInLocked')); return }
  await MateMessageBox.confirm(t('ai.agents.deleteConfirm', { name: row.name }), t('ai.common.confirmTitle'), { type: 'warning' })
  await aiApi.agentDelete(row.id)
  MateMessage.success(t('ai.common.deleted'))
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.mc-mono { font-family: var(--mc-font-mono, monospace); font-size: 12px; }
</style>
