<template>
  <MatePageCard :title="t('log.operationTitle')" :description="t('log.operationDesc')">
    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <el-input v-model="search.module" :placeholder="t('log.module')" style="width: 140px" clearable @keyup.enter="handleSearch" />
      <el-input v-model="search.username" :placeholder="t('log.username')" style="width: 140px" clearable @keyup.enter="handleSearch" />
      <el-select v-model="search.status" :placeholder="t('log.status')" style="width: 120px" clearable>
        <el-option :label="t('log.statusOk')" :value="0" />
        <el-option :label="t('log.statusFail')" :value="1" />
      </el-select>
      <el-select v-model="search.operationType" :placeholder="t('log.operationType')" style="width: 120px" clearable>
        <el-option v-for="op in operationTypes" :key="op" :label="op" :value="op" />
      </el-select>
      <el-date-picker
        v-model="search.range"
        type="datetimerange"
        :range-separator="'~'"
        :start-placeholder="t('log.startTime')"
        :end-placeholder="t('log.endTime')"
        value-format="YYYY-MM-DD HH:mm:ss"
        style="width: min(100%, 360px)"
      />
    </MateSearchBar>

    <!-- Toolbar: batch delete -->
    <div v-if="selectedIds.length" class="toolbar">
      <el-button type="danger" size="small" @click="handleBatchDelete">
        {{ t('common.delete') }} ({{ selectedIds.length }})
      </el-button>
    </div>

    <MateTable
      :columns="columns"
      :data="logs"
      :loading="loading"
      row-key="id"
      :action-width="80"
      :action-label="t('common.action')"
      :empty-text="t('common.noData')"
      :selectable="true"
      @selection-change="onSelectionChange"
    >
      <template #col-requestMethod="{ row }">
        <span class="mc-mono">{{ row.requestMethod }}</span>
      </template>
      <template #col-requestUrl="{ row }">
        <span class="mc-mono">{{ row.requestUrl }}</span>
      </template>
      <template #col-clientIp="{ row }">
        <span class="mc-mono">{{ row.clientIp }}</span>
      </template>
      <template #col-status="{ row }">
        <MateBadge :status="row.status" domain="log">
          {{ row.status === 0 ? t('log.statusOk') : t('log.statusFail') }}
        </MateBadge>
      </template>
      <template #col-duration="{ row }">
        <span class="mc-mono">{{ row.duration }}ms</span>
      </template>
      <template #actions="{ row }">
        <button class="mc-action-btn" @click="openDetail(row)">{{ t('log.detail') }}</button>
      </template>
    </MateTable>

    <MatePagination
      v-model:page-num="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      @change="loadData"
    />

    <!-- Detail dialog -->
    <MateDialog
      v-model="detailVisible"
      :title="t('log.detail')"
      width="680px"
      :show-footer="false"
    >
      <template #footer>
        <el-button @click="detailVisible = false">{{ t('common.close') }}</el-button>
      </template>
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <div class="detail-grid">
            <div class="kv"><span>{{ t('log.username') }}</span><b>{{ detail.username }}</b></div>
            <div class="kv"><span>{{ t('log.module') }}</span><b>{{ detail.module }}</b></div>
            <div class="kv"><span>{{ t('log.operationType') }}</span><b>{{ detail.operationType }}</b></div>
            <div v-if="detail.description" class="kv"><span>{{ t('log.description') }}</span><b>{{ detail.description }}</b></div>
            <div class="kv"><span>{{ t('log.method') }}</span><b class="mc-mono">{{ detail.requestMethod }}</b></div>
            <div class="kv"><span>{{ t('log.status') }}</span>
              <MateBadge :status="detail.status" domain="log">
                {{ detail.status === 0 ? t('log.statusOk') : t('log.statusFail') }}
              </MateBadge>
            </div>
            <div class="kv"><span>{{ t('log.duration') }}</span><b class="mc-mono">{{ detail.duration }}ms</b></div>
            <div class="kv"><span>{{ t('log.ip') }}</span><b class="mc-mono">{{ detail.clientIp }}</b></div>
            <div v-if="detail.location" class="kv"><span>{{ t('log.location') }}</span><b>{{ detail.location }}</b></div>
            <div class="kv kv--full"><span>{{ t('log.url') }}</span><b class="mc-mono">{{ detail.requestUrl }}</b></div>
            <div v-if="detail.userAgent" class="kv kv--full"><span>{{ t('log.userAgent') }}</span><b class="mc-mono wrap">{{ detail.userAgent }}</b></div>
            <div class="kv kv--full"><span>{{ t('log.time') }}</span><b>{{ detail.createdAt }}</b></div>
          </div>

          <h4 class="section-title">{{ t('log.requestParams') }}</h4>
          <pre class="code-block">{{ formatJson(detail.requestParams) }}</pre>

          <h4 class="section-title">{{ t('log.responseResult') }}</h4>
          <pre class="code-block">{{ formatJson(detail.responseResult) }}</pre>

          <template v-if="detail.errorMsg">
            <h4 class="section-title">{{ t('log.errorMsg') }}</h4>
            <pre class="code-block code-block--error">{{ detail.errorMsg }}</pre>
          </template>
        </template>
      </div>
    </MateDialog>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'

defineOptions({ name: 'OperationLogView' })
import { MatePageCard, MateTable, MateBadge, MateSearchBar, MatePagination, MateDialog, type MateColumn, MateMessage, MateMessageBox } from '@matecloud/ui'
import { logApi, type OperationLogItem } from '@matecloud/core'

const { t } = useI18n()

const operationTypes = computed(() => [
  t('log.opCreate'), t('log.opUpdate'), t('log.opDelete'),
  t('log.opGrant'), t('log.opImport'), t('log.opExport'),
])

const columns: MateColumn[] = [
  { prop: 'username', label: t('log.username'), width: 120 },
  { prop: 'module', label: t('log.module'), width: 110 },
  { prop: 'operationType', label: t('log.operationType'), width: 100 },
  { prop: 'requestMethod', label: t('log.method'), width: 80 },
  { prop: 'requestUrl', label: t('log.url') },
  { prop: 'clientIp', label: t('log.ip'), width: 130 },
  { prop: 'status', label: t('log.status'), width: 80, align: 'center' },
  { prop: 'duration', label: t('log.duration'), width: 90, align: 'right' },
  { prop: 'createdAt', label: t('log.time'), width: 160 },
]

const loading = ref(false)
const logs = ref<OperationLogItem[]>([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const selectedIds = ref<string[]>([])

const search = reactive({
  module: '',
  username: '',
  status: undefined as number | undefined,
  operationType: '',
  range: [] as string[],
})

async function loadData() {
  loading.value = true
  try {
    const res = await logApi.operationLogs({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      module: search.module || undefined,
      username: search.username || undefined,
      status: search.status,
      operationType: search.operationType || undefined,
      startTime: search.range?.[0],
      endTime: search.range?.[1],
    })
    const data: any = res.data ?? res
    if (Array.isArray(data)) {
      logs.value = data
      total.value = data.length
    } else {
      logs.value = data.list ?? []
      total.value = data.total ?? 0
    }
  } catch {
    logs.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() {
  search.module = ''
  search.username = ''
  search.status = undefined
  search.operationType = ''
  search.range = []
  handleSearch()
}

function onSelectionChange(rows: OperationLogItem[]) {
  selectedIds.value = rows.map(r => r.id)
}

async function handleBatchDelete() {
  if (!selectedIds.value.length) return
  try {
    await MateMessageBox.confirm(t('common.deleteConfirm'), t('common.warning'), { type: 'warning' })
    await logApi.operationLogBatchDelete(selectedIds.value)
    MateMessage.success(t('common.deleteSuccess'))
    selectedIds.value = []
    loadData()
  } catch { /* cancelled */ }
}

// ---- detail dialog ----
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)

async function openDetail(row: OperationLogItem) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const res = await logApi.operationLogDetail(row.id)
    detail.value = res.data ?? row
  } catch (e: any) {
    MateMessage.error(e?.msg || t('common.failed'))
    detail.value = row
  } finally {
    detailLoading.value = false
  }
}

function formatJson(value: unknown): string {
  if (value === null || value === undefined || value === '') return '-'
  if (typeof value !== 'string') return JSON.stringify(value, null, 2)
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

onMounted(loadData)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.detail-body { min-height: 240px; }
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 16px;
  margin-bottom: 20px;
}
.kv {
  display: flex; align-items: center; gap: 10px;
  font-size: 13px;
  min-width: 0;
}
.kv--full { grid-column: span 2; }
.kv > span {
  color: var(--mc-text-muted);
  flex-shrink: 0;
  min-width: 90px;
}
.kv > b {
  color: var(--mc-text-primary);
  font-weight: 500;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  min-width: 0;
}
.kv > b.wrap { white-space: normal; word-break: break-all; }

.section-title {
  font-size: 13px; font-weight: 600;
  color: var(--mc-text-primary);
  margin: 16px 0 8px;
}
.code-block {
  background: var(--mc-bg);
  border: 1px solid var(--mc-divider-subtle, rgb(16 24 40 / 0.06));
  border-radius: 6px;
  padding: 12px;
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
  font-size: 12px;
  color: var(--mc-text-primary);
  max-height: 200px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
.code-block--error { color: var(--mc-danger); }
</style>
