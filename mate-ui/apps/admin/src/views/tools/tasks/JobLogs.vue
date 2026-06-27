<template>
  <MatePageCard
    :title="t('jobLog.title')"
    :description="t('jobLog.description')"
  >
    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <el-input
        v-model="search.jobName"
        :placeholder="t('jobLog.jobName')"
        style="width: 180px"
        clearable
        @keyup.enter="handleSearch"
      />
      <el-select v-model="search.status" clearable style="width: 130px" :placeholder="t('common.status')">
        <el-option :label="t('jobLog.success')" value="SUCCESS" />
        <el-option :label="t('jobLog.failed')" value="FAIL" />
      </el-select>
      <el-date-picker
        v-model="search.range"
        type="datetimerange"
        :range-separator="'~'"
        :start-placeholder="t('log.startTime')"
        :end-placeholder="t('log.endTime')"
        value-format="YYYY-MM-DD HH:mm:ss"
        style="width: 360px"
      />
    </MateSearchBar>

    <MateTable
      :columns="columns"
      :data="rows"
      :loading="loading"
      row-key="id"
      :action-width="80"
      :action-label="t('common.action')"
    >
      <template #col-status="{ row }">
        <MateBadge :type="row.status === 'SUCCESS' ? 'success' : 'danger'">
          {{ row.status === 'SUCCESS' ? t('jobLog.success') : t('jobLog.failed') }}
        </MateBadge>
      </template>
      <template #col-duration="{ row }">
        <span class="mc-mono">{{ row.duration }}ms</span>
      </template>
      <template #col-executor="{ row }">
        <span class="mc-mono">{{ row.executor }}</span>
      </template>
      <template #actions="{ row }">
        <button class="mc-action-btn" @click="openDetail(row)">{{ t('common.detail') }}</button>
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
      {{ t('jobLog.hint') }}
    </p>

    <MateDialog
      v-model="detailVisible"
      :title="t('jobLog.detailTitle')"
      width="620px"
      :show-footer="false"
    >
      <template #footer>
        <el-button @click="detailVisible = false">{{ t('common.close') }}</el-button>
      </template>
      <div v-if="detail" class="detail-body">
        <div class="kv"><span>{{ t('jobLog.jobName') }}</span><b>{{ detail.jobName }}</b></div>
        <div class="kv"><span>{{ t('jobLog.executor') }}</span><b class="mc-mono">{{ detail.executor }}</b></div>
        <div class="kv"><span>{{ t('jobLog.startedAt') }}</span><b>{{ detail.startedAt }}</b></div>
        <div class="kv"><span>{{ t('jobLog.duration') }}</span><b class="mc-mono">{{ detail.duration }}ms</b></div>
        <div class="kv"><span>{{ t('common.status') }}</span>
          <MateBadge :type="detail.status === 'SUCCESS' ? 'success' : 'danger'">
            {{ detail.status === 'SUCCESS' ? t('jobLog.success') : t('jobLog.failed') }}
          </MateBadge>
        </div>

        <h4 class="section-title">{{ t('jobLog.output') }}</h4>
        <pre class="code-block">{{ detail.output || '—' }}</pre>

        <template v-if="detail.status === 'FAIL' && detail.errorStack">
          <h4 class="section-title">{{ t('jobLog.errorStack') }}</h4>
          <pre class="code-block code-block--error">{{ detail.errorStack }}</pre>
        </template>
      </div>
    </MateDialog>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Info } from 'lucide-vue-next'
import {
  MatePageCard,
  MateTable,
  MateBadge,
  MateSearchBar,
  MatePagination,
  MateDialog,
  type MateColumn,
} from '@matecloud/ui'

defineOptions({ name: 'JobLogsView' })

const { t } = useI18n()

const columns: MateColumn[] = [
  { prop: 'jobName', label: t('jobLog.jobName'), width: 200 },
  { prop: 'executor', label: t('jobLog.executor'), width: 150 },
  { prop: 'startedAt', label: t('jobLog.startedAt'), width: 170 },
  { prop: 'duration', label: t('jobLog.duration'), width: 100, align: 'right' },
  { prop: 'status', label: t('common.status'), width: 100 },
  { prop: 'message', label: t('jobLog.message') },
]

interface JobLog {
  id: string
  jobName: string
  executor: string
  startedAt: string
  duration: number
  status: 'SUCCESS' | 'FAIL'
  message: string
  output?: string
  errorStack?: string
}

const all = ref<JobLog[]>([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)

const search = reactive({
  jobName: '',
  status: '' as '' | 'SUCCESS' | 'FAIL',
  range: [] as string[],
})

function seed(): JobLog[] {
  const jobs = ['清理过期 Token', '日志归档', '字典缓存刷新', '统计报表生成', '短信失败重试']
  const executors = ['xxl-job-executor-1', 'xxl-job-executor-2']
  const rows: JobLog[] = []
  const now = Date.now()
  for (let i = 0; i < 60; i++) {
    const success = i % 9 !== 4
    const startMs = now - i * 10 * 60_000
    const dur = success ? 30 + (i * 13) % 400 : 2000 + (i * 7) % 3000
    rows.push({
      id: String(9000 + i),
      jobName: jobs[i % jobs.length],
      executor: executors[i % executors.length],
      startedAt: new Date(startMs).toISOString().slice(0, 19).replace('T', ' '),
      duration: dur,
      status: success ? 'SUCCESS' : 'FAIL',
      message: success ? 'OK' : '数据库连接超时',
      output: success
        ? `[${new Date(startMs).toISOString()}] Started\n[${new Date(startMs + dur).toISOString()}] Processed ${100 + (i * 3) % 200} records\n[${new Date(startMs + dur).toISOString()}] Done`
        : '',
      errorStack: success ? undefined : 'com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure\n  at vip.mate.job.handler.TokenCleanupJobHandler.execute(TokenCleanupJobHandler.java:42)\n  at com.xxl.job.core.thread.JobThread.run(JobThread.java:119)',
    })
  }
  return rows
}

async function loadData() {
  loading.value = true
  try {
    await new Promise(r => setTimeout(r, 220))
    all.value = seed()
  } finally {
    loading.value = false
  }
}

const filtered = computed(() => {
  let list = all.value
  if (search.jobName) {
    const kw = search.jobName.toLowerCase()
    list = list.filter(l => l.jobName.toLowerCase().includes(kw))
  }
  if (search.status) list = list.filter(l => l.status === search.status)
  if (search.range?.[0]) list = list.filter(l => l.startedAt >= search.range[0])
  if (search.range?.[1]) list = list.filter(l => l.startedAt <= search.range[1])
  return list
})
const total = computed(() => filtered.value.length)
const rows = computed(() => {
  const start = (pageNum.value - 1) * pageSize.value
  return filtered.value.slice(start, start + pageSize.value)
})

function handleSearch() { pageNum.value = 1 }
function handleReset() {
  search.jobName = ''
  search.status = ''
  search.range = []
  pageNum.value = 1
}

const detailVisible = ref(false)
const detail = ref<JobLog | null>(null)
function openDetail(row: JobLog) {
  detail.value = row
  detailVisible.value = true
}

onMounted(loadData)
</script>

<style scoped>
.detail-body { min-height: 220px; }
.kv {
  display: flex; align-items: center; gap: 10px;
  padding: 6px 0;
  font-size: 13px;
  border-bottom: 1px dashed var(--mc-divider-subtle, rgb(16 24 40 / 0.04));
}
.kv > span { color: var(--mc-text-muted); min-width: 100px; }
.kv > b { color: var(--mc-text-primary); font-weight: 500; }

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
  max-height: 220px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
.code-block--error { color: var(--mc-danger); }

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
