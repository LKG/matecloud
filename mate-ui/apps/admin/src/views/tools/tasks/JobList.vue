<template>
  <MatePageCard
    :title="t('job.title')"
    :description="t('job.description')"
  >
    <template #actions>
      <el-button type="primary" @click="openDialog()">
        <Plus :size="14" class="mr-1" />{{ t('job.createJob') }}
      </el-button>
    </template>

    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <el-input
        v-model="keyword"
        :placeholder="t('job.searchPlaceholder')"
        clearable
        style="width: 240px"
        @keyup.enter="handleSearch"
      >
        <template #prefix><Search :size="14" /></template>
      </el-input>
      <el-select v-model="statusFilter" clearable style="width: 130px" :placeholder="t('common.status')">
        <el-option :label="t('job.statusRunning')" value="RUNNING" />
        <el-option :label="t('job.statusPaused')" value="PAUSED" />
      </el-select>
    </MateSearchBar>

    <MateTable
      :columns="columns"
      :data="rows"
      :loading="loading"
      row-key="id"
      :action-width="240"
      :action-label="t('common.action')"
    >
      <template #col-jobName="{ row }">
        <div class="job-cell">
          <Clock :size="14" class="job-icon" />
          <div>
            <span class="job-name">{{ row.jobName }}</span>
            <span class="job-handler mc-mono">{{ row.handler }}</span>
          </div>
        </div>
      </template>
      <template #col-cron="{ row }">
        <span class="mc-mono">{{ row.cron }}</span>
      </template>
      <template #col-status="{ row }">
        <MateBadge :type="row.status === 'RUNNING' ? 'success' : 'warning'">
          {{ row.status === 'RUNNING' ? t('job.statusRunning') : t('job.statusPaused') }}
        </MateBadge>
      </template>
      <template #col-lastRunAt="{ row }">
        <span class="mc-mono">{{ row.lastRunAt || '—' }}</span>
      </template>
      <template #col-nextRunAt="{ row }">
        <span class="mc-mono">{{ row.nextRunAt || '—' }}</span>
      </template>
      <template #actions="{ row }">
        <button class="mc-action-btn" @click="handleTriggerOnce(row)">{{ t('job.runOnce') }}</button>
        <button
          v-if="row.status === 'RUNNING'"
          class="mc-action-btn mc-action-btn--warn"
          @click="handlePause(row)"
        >{{ t('job.pause') }}</button>
        <button
          v-else
          class="mc-action-btn mc-action-btn--success"
          @click="handleResume(row)"
        >{{ t('job.resume') }}</button>
        <button class="mc-action-btn" @click="openDialog(row)">{{ t('common.edit') }}</button>
        <MateInlineConfirm :title="t('common.deleteConfirm')" @confirm="handleDelete(row)">
                      <button class="mc-action-btn mc-action-btn--danger">{{ t('common.delete') }}</button>
          
        </MateInlineConfirm>
      </template>
    </MateTable>

    <MatePagination
      v-model:page-num="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      @change="() => {}"
    />

    <p class="demo-hint">
      <Info :size="13" class="mr-1" />
      {{ t('job.hint') }}
    </p>

    <!-- Create / Edit Dialog -->
    <MateDialog
      v-model="dialogVisible"
      :title="editing ? t('job.editJob') : t('job.createJob')"
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
import { Search, Plus, Clock, Info } from 'lucide-vue-next'
import { MatePageCard, MateTable, MateBadge, MateSearchBar, MatePagination, MateDialog, MateForm, type MateColumn, type FormSchema, MateMessage, MateInlineConfirm } from '@matecloud/ui'

defineOptions({ name: 'JobListView' })

const { t } = useI18n()

const columns: MateColumn[] = [
  { prop: 'jobName', label: t('job.jobName') },
  { prop: 'cron', label: 'Cron', width: 150 },
  { prop: 'status', label: t('common.status'), width: 100 },
  { prop: 'lastRunAt', label: t('job.lastRunAt'), width: 170 },
  { prop: 'nextRunAt', label: t('job.nextRunAt'), width: 170 },
]

interface Job {
  id: string
  jobName: string
  handler: string
  cron: string
  description: string
  status: 'RUNNING' | 'PAUSED'
  lastRunAt: string
  nextRunAt: string
}

const all = ref<Job[]>([
  { id: '1', jobName: '清理过期 Token', handler: 'tokenCleanupJobHandler', cron: '0 0 2 * * ?', description: '每日凌晨清理 Sa-Token 过期记录', status: 'RUNNING', lastRunAt: '2026-04-14 02:00:01', nextRunAt: '2026-04-15 02:00:00' },
  { id: '2', jobName: '日志归档', handler: 'logArchiveJobHandler', cron: '0 30 3 * * ?', description: '操作/登录日志归档到冷存储', status: 'RUNNING', lastRunAt: '2026-04-14 03:30:03', nextRunAt: '2026-04-15 03:30:00' },
  { id: '3', jobName: '字典缓存刷新', handler: 'dictCacheRefreshJobHandler', cron: '0 */10 * * * ?', description: '每 10 分钟刷新字典 Redis 缓存', status: 'RUNNING', lastRunAt: '2026-04-14 14:10:02', nextRunAt: '2026-04-14 14:20:00' },
  { id: '4', jobName: '会员升级检查', handler: 'memberUpgradeJobHandler', cron: '0 0 0 * * ?', description: '每日凌晨检查会员等级升级', status: 'PAUSED', lastRunAt: '2026-04-10 00:00:01', nextRunAt: '' },
  { id: '5', jobName: '统计报表生成', handler: 'statisticsReportJobHandler', cron: '0 0 1 * * ?', description: '生成前日统计报表', status: 'RUNNING', lastRunAt: '2026-04-14 01:00:02', nextRunAt: '2026-04-15 01:00:00' },
  { id: '6', jobName: '短信失败重试', handler: 'smsRetryJobHandler', cron: '0 */5 * * * ?', description: '扫描失败短信并重试', status: 'RUNNING', lastRunAt: '2026-04-14 14:15:00', nextRunAt: '2026-04-14 14:20:00' },
  { id: '7', jobName: '订单超时关闭', handler: 'orderTimeoutJobHandler', cron: '0 */30 * * * ?', description: '关闭超过 30 分钟未支付订单', status: 'PAUSED', lastRunAt: '', nextRunAt: '' },
])

const loading = ref(false)
const keyword = ref('')
const statusFilter = ref<'' | 'RUNNING' | 'PAUSED'>('')
const pageNum = ref(1)
const pageSize = ref(10)

const filtered = computed(() => {
  let list = all.value
  if (statusFilter.value) list = list.filter(j => j.status === statusFilter.value)
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    list = list.filter(j =>
      j.jobName.toLowerCase().includes(kw) ||
      j.handler.toLowerCase().includes(kw),
    )
  }
  return list
})
const total = computed(() => filtered.value.length)
const rows = computed(() => {
  const start = (pageNum.value - 1) * pageSize.value
  return filtered.value.slice(start, start + pageSize.value)
})

function handleSearch() { pageNum.value = 1 }
function handleReset() { keyword.value = ''; statusFilter.value = ''; pageNum.value = 1 }

async function handleTriggerOnce(row: Job) {
  MateMessage.success(t('job.triggered', { name: row.jobName }))
  row.lastRunAt = new Date().toISOString().slice(0, 19).replace('T', ' ')
}
async function handlePause(row: Job) {
  row.status = 'PAUSED'
  row.nextRunAt = ''
  MateMessage.success(t('job.pausedMsg', { name: row.jobName }))
}
async function handleResume(row: Job) {
  row.status = 'RUNNING'
  row.nextRunAt = new Date(Date.now() + 600_000).toISOString().slice(0, 19).replace('T', ' ')
  MateMessage.success(t('job.resumedMsg', { name: row.jobName }))
}
async function handleDelete(row: Job) {
  all.value = all.value.filter(j => j.id !== row.id)
  MateMessage.success(t('common.deleteSuccess'))
}

// ---- dialog ----
const dialogVisible = ref(false)
const editing = ref<Job | null>(null)
const submitting = ref(false)
const formRef = ref<InstanceType<typeof MateForm>>()
const form = reactive<Record<string, any>>({
  jobName: '', handler: '', cron: '', description: '', status: 'RUNNING',
})

const formSchema = computed<FormSchema[]>(() => [
  {
    field: 'jobName',
    label: t('job.jobName'),
    type: 'input',
    rules: [{ required: true, message: t('job.nameRequired'), trigger: 'blur' }],
  },
  {
    field: 'handler',
    label: t('job.handler'),
    type: 'input',
    placeholder: 'xxxJobHandler',
    rules: [{ required: true, message: t('job.handlerRequired'), trigger: 'blur' }],
  },
  {
    field: 'cron',
    label: 'Cron',
    type: 'input',
    placeholder: '0 0 2 * * ?',
    rules: [{ required: true, message: t('job.cronRequired'), trigger: 'blur' }],
  },
  { field: 'description', label: t('job.descField'), type: 'textarea' },
  {
    field: 'status',
    label: t('common.status'),
    type: 'radio',
    options: [
      { label: t('job.statusRunning'), value: 'RUNNING' },
      { label: t('job.statusPaused'), value: 'PAUSED' },
    ],
  },
])

function openDialog(row?: Job) {
  editing.value = row ?? null
  form.jobName = row?.jobName ?? ''
  form.handler = row?.handler ?? ''
  form.cron = row?.cron ?? ''
  form.description = row?.description ?? ''
  form.status = row?.status ?? 'RUNNING'
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return
  submitting.value = true
  try {
    await new Promise(r => setTimeout(r, 300))
    if (editing.value) {
      Object.assign(editing.value, form)
      MateMessage.success(t('common.updateSuccess'))
    } else {
      all.value.unshift({
        id: String(Date.now()),
        ...(form as any),
        lastRunAt: '',
        nextRunAt: form.status === 'RUNNING' ? new Date(Date.now() + 600_000).toISOString().slice(0, 19).replace('T', ' ') : '',
      })
      MateMessage.success(t('common.createSuccess'))
    }
    dialogVisible.value = false
  } finally {
    submitting.value = false
  }
}

onMounted(() => { /* static */ })
</script>

<style scoped>
.job-cell { display: flex; align-items: center; gap: 10px; }
.job-icon { color: var(--mc-primary); flex-shrink: 0; }
.job-name {
  display: block;
  font-size: 13px; font-weight: 500;
  color: var(--mc-text-primary);
}
.job-handler {
  display: block;
  font-size: 12px;
  color: var(--mc-text-muted);
}

.demo-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  padding: 8px 12px;
  font-size: 12px;
  color: var(--mc-text-muted);
  background: rgb(54 191 250 / 0.06);
  border-radius: 6px;
}
</style>
