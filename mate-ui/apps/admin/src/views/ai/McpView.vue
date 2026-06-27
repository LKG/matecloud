<template>
  <MatePageCard :title="t('ai.mcp.title')" :description="t('ai.mcp.description')">
    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <el-input v-model="search.keyword" :placeholder="t('ai.mcp.searchPlaceholder')"
                style="width: 220px" clearable @keyup.enter="handleSearch" />
      <el-select v-model="search.status" :placeholder="t('ai.mcp.statusPlaceholder')" clearable style="width: 130px">
        <el-option :label="t('ai.mcp.statusEnabled')" :value="1" />
        <el-option :label="t('ai.mcp.statusDisabled')" :value="0" />
        <el-option :label="t('ai.mcp.statusErrored')" :value="2" />
      </el-select>
      <el-button type="primary" :icon="Plus" @click="openCreate">{{ t('ai.mcp.createBtn') }}</el-button>
    </MateSearchBar>

    <MateTable :columns="columns" :data="rows" :loading="loading"
               row-key="id" :action-width="160" :action-label="t('common.action')">
      <template #col-name="{ row }">
        <MateEntityCell :name="row.name" :sub="row.code" :icon="Connection" sub-mono />
      </template>
      <template #col-transport="{ row }">
        <el-tag size="small">{{ row.transport }}</el-tag>
      </template>
      <template #col-toolCount="{ row }">
        <a class="ai-mcp__count" @click="goTools(row)">{{ row.toolCount || 0 }} {{ t('ai.mcp.toolCountSuffix') }}</a>
      </template>
      <template #col-status="{ row }">
        <AiStatusDot
          :state="row.status === 1 ? 'online' : row.status === 2 ? 'error' : 'offline'"
          :label="statusLabel(row.status)"
        />
      </template>
      <template #actions="{ row }">
        <button class="mc-action-btn" @click="openEdit(row)">{{ t('ai.common.edit') }}</button>
        <button class="mc-action-btn mc-action-btn--danger" @click="remove(row)">{{ t('ai.common.delete') }}</button>
      </template>
    </MateTable>

    <MatePagination v-model:page-num="pageNum" v-model:page-size="pageSize"
                    :total="total" @change="loadData" />

    <MateDialog v-model="formVisible" :title="isEdit ? t('ai.mcp.editTitle') : t('ai.mcp.createTitle')" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item :label="t('ai.common.code')" prop="code">
          <el-input v-model="form.code" :disabled="isEdit" :placeholder="t('ai.mcp.codePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('ai.mcp.nameLabel')" prop="name">
          <el-input v-model="form.name" :placeholder="t('ai.mcp.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('ai.mcp.transportLabel')">
          <el-select v-model="form.transport" style="width: 100%">
            <el-option label="STDIO" value="STDIO" />
            <el-option label="SSE" value="SSE" />
            <el-option label="HTTP" value="HTTP" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.transport === 'STDIO'" :label="t('ai.mcp.commandLabel')">
          <el-input v-model="form.command" :placeholder="t('ai.mcp.commandPlaceholder')" />
        </el-form-item>
        <el-form-item v-if="form.transport === 'STDIO'" :label="t('ai.mcp.argsLabel')">
          <el-input v-model="form.args" type="textarea" :rows="2"
                    :placeholder="t('ai.mcp.argsPlaceholder')" />
        </el-form-item>
        <el-form-item v-else :label="t('ai.mcp.endpointLabel')">
          <el-input v-model="form.endpoint" :placeholder="t('ai.mcp.endpointPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('ai.mcp.descriptionLabel')">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item :label="t('ai.mcp.enabledLabel')">
          <el-switch v-model="enabledSwitch" />
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
import { useRouter } from 'vue-router'
import type { FormInstance } from 'element-plus'
import { Plus, Connection } from '@element-plus/icons-vue'
import { aiApi, type McpServerView } from '@matecloud/core'
import { MatePageCard, MateSearchBar, MateTable, MatePagination, MateDialog, MateEntityCell, AiStatusDot, type MateColumn, MateMessage, MateMessageBox } from '@matecloud/ui'

defineOptions({ name: 'AiMcpView' })

const { t } = useI18n()
const router = useRouter()

const columns = computed<MateColumn[]>(() => [
  { prop: 'name', label: t('ai.mcp.colName'), width: 240 },
  { prop: 'transport', label: t('ai.mcp.colTransport'), width: 100 },
  { prop: 'description', label: t('ai.mcp.colDescription') },
  { prop: 'toolCount', label: t('ai.mcp.colToolCount'), width: 100, align: 'center' },
  { prop: 'status', label: t('ai.mcp.colStatus'), width: 110 },
  { prop: 'lastCheckAt', label: t('ai.mcp.colLastCheck'), width: 170 },
])

function statusLabel(s: number): string {
  if (s === 1) return t('ai.mcp.statusOnline')
  if (s === 2) return t('ai.mcp.statusError')
  return t('ai.mcp.statusOffline')
}

const loading = ref(false)
const rows = ref<McpServerView[]>([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const search = reactive({ keyword: '', status: undefined as number | undefined })

async function loadData() {
  loading.value = true
  try {
    const res: any = await aiApi.mcpPage({
      pageNum: pageNum.value, pageSize: pageSize.value,
      keyword: search.keyword || undefined, status: search.status,
    })
    rows.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() {
  search.keyword = ''; search.status = undefined; handleSearch()
}

function goTools(row: McpServerView) {
  router.push({ path: '/ai/tools', query: { mcp: row.code } })
}

const formVisible = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<Partial<McpServerView>>({
  code: '', name: '', transport: 'STDIO',
  command: '', args: '', endpoint: '', description: '', status: 1,
})
const enabledSwitch = computed({
  get: () => form.status !== 0,
  set: (v: boolean) => { form.status = v ? 1 : 0 },
})
const rules = computed(() => ({
  code: [{ required: true, message: t('ai.mcp.codeRequired'), trigger: 'blur' }],
  name: [{ required: true, message: t('ai.mcp.nameRequired'), trigger: 'blur' }],
}))

function openCreate() {
  isEdit.value = false; editingId.value = null
  Object.assign(form, { code: '', name: '', transport: 'STDIO',
    command: '', args: '', endpoint: '', description: '', status: 1 })
  formVisible.value = true
}

function openEdit(row: McpServerView) {
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
      await aiApi.mcpUpdate(editingId.value, form)
    } else {
      await aiApi.mcpCreate(form)
    }
    MateMessage.success(t('ai.common.saveSuccess'))
    formVisible.value = false
    loadData()
  } finally { saving.value = false }
}

async function remove(row: McpServerView) {
  await MateMessageBox.confirm(t('ai.mcp.deleteConfirm', { name: row.name }), t('ai.common.confirmTitle'), { type: 'warning' })
  await aiApi.mcpDelete(row.id)
  MateMessage.success(t('ai.common.deleted'))
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.ai-mcp__count {
  color: var(--mc-primary);
  cursor: pointer;
  font-family: var(--mc-font-mono, monospace);
}
.ai-mcp__count:hover { text-decoration: underline; }
</style>
