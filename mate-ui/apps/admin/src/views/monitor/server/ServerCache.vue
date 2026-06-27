<template>
  <MatePageCard
    :title="t('cache.title')"
    :description="t('cache.description')"
  >
    <template #actions>
      <el-button :loading="infoLoading" @click="loadAll">
        <RefreshCw :size="14" class="mr-1" />{{ t('common.refresh') }}
      </el-button>
    </template>

    <!-- Redis stats header -->
    <div class="stat-grid">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <div class="stat-header">
          <component :is="card.icon" :size="16" class="stat-icon" />
          <span class="stat-label">{{ card.label }}</span>
        </div>
        <div class="stat-value">{{ card.value }}</div>
        <div v-if="card.sub" class="stat-sub">{{ card.sub }}</div>
      </div>
    </div>

    <!-- Info cards grid: Server + Memory + Stats -->
    <div class="info-grid">
      <div class="info-card">
        <h3 class="info-title">{{ t('cache.serverInfo') }}</h3>
        <div class="info-row"><span>{{ t('cache.version') }}</span><b class="mc-mono">{{ stats.version }}</b></div>
        <div class="info-row"><span>{{ t('cache.mode') }}</span><b class="mc-mono">{{ stats.mode || '-' }}</b></div>
        <div class="info-row"><span>{{ t('cache.os') }}</span><b class="mc-mono">{{ stats.os || '-' }}</b></div>
        <div class="info-row"><span>{{ t('cache.port') }}</span><b class="mc-mono">{{ stats.tcpPort || '-' }}</b></div>
        <div class="info-row"><span>{{ t('cache.uptime') }}</span><b class="mc-mono">{{ formatDays(stats.uptimeDays) }}</b></div>
      </div>

      <div class="info-card">
        <h3 class="info-title">{{ t('cache.memoryInfo') }}</h3>
        <div class="info-row"><span>{{ t('cache.used') }}</span><b class="mc-mono">{{ stats.usedMemory }}</b></div>
        <div class="info-row"><span>{{ t('cache.rss') }}</span><b class="mc-mono">{{ stats.usedMemoryRss || '-' }}</b></div>
        <div class="info-row"><span>{{ t('cache.peakMemory') }}</span><b class="mc-mono">{{ stats.usedMemoryPeak || '-' }}</b></div>
        <div class="info-row"><span>{{ t('cache.max') }}</span><b class="mc-mono">{{ stats.maxMemory }}</b></div>
        <div class="info-row"><span>{{ t('cache.policy') }}</span><b class="mc-mono">{{ stats.maxMemoryPolicy || '-' }}</b></div>
      </div>

      <div class="info-card">
        <h3 class="info-title">{{ t('cache.statsInfo') }}</h3>
        <div class="info-row"><span>{{ t('cache.keyCount') }}</span><b class="mc-mono">{{ stats.dbSize }}</b></div>
        <div class="info-row"><span>{{ t('cache.hitRate') }}</span><b class="mc-mono">{{ stats.hitRate || '-' }}</b></div>
        <div class="info-row"><span>{{ t('cache.hitsMisses') }}</span><b class="mc-mono">{{ stats.keyspaceHits || '0' }} / {{ stats.keyspaceMisses || '0' }}</b></div>
        <div class="info-row"><span>{{ t('cache.totalCommands') }}</span><b class="mc-mono">{{ Number(stats.totalCommandsProcessed || 0).toLocaleString() }}</b></div>
        <div class="info-row"><span>{{ t('cache.opsPerSec') }}</span><b class="mc-mono">{{ stats.instantaneousOpsPerSec || '-' }}</b></div>
      </div>
    </div>

    <!-- Command stats bar chart -->
    <div v-if="commandStats.length" class="cmd-card">
      <h3 class="info-title">{{ t('cache.commandStats') }}</h3>
      <div class="cmd-stats">
        <div v-for="cmd in commandStats" :key="cmd.name" class="cmd-bar">
          <span class="cmd-name mc-mono" :title="cmd.name">{{ cmd.name }}</span>
          <div class="cmd-track">
            <div class="cmd-fill" :style="{ width: cmdPercent(cmd.value) + '%' }" />
          </div>
          <span class="cmd-count mc-mono">{{ Number(cmd.value).toLocaleString() }}</span>
        </div>
      </div>
    </div>

    <!-- Key browser -->
    <h3 class="section-title">{{ t('cache.keyBrowser') }}</h3>

    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <el-input
        v-model="pattern"
        :placeholder="t('cache.searchPlaceholder')"
        clearable
        style="width: 280px"
        @keyup.enter="handleSearch"
      >
        <template #prefix><Search :size="14" /></template>
      </el-input>
    </MateSearchBar>

    <MateTable
      :columns="columns"
      :data="keys"
      :loading="keysLoading"
      row-key="key"
      :action-width="150"
      :action-label="t('common.action')"
    >
      <template #col-key="{ row }">
        <MateTooltip :content="row.key" placement="top-start" :show-after="300" :disabled="row.key.length < 45">
          <span class="mc-mono key-text">{{ row.key }}</span>
        </MateTooltip>
      </template>
      <template #col-type="{ row }">
        <el-tag size="small" :type="typeTagType(row.type)">{{ row.type }}</el-tag>
      </template>
      <template #col-size="{ row }">
        <span class="mc-mono">{{ row.size }}</span>
      </template>
      <template #col-ttl="{ row }">
        <span class="mc-mono">{{ formatTtl(row.ttl) }}</span>
      </template>
      <template #actions="{ row }">
        <button class="mc-action-btn" @click="openDetail(row)">{{ t('common.detail') }}</button>
        <MateInlineConfirm :title="t('cache.deleteConfirm')" @confirm="handleDelete(row)">
                      <button class="mc-action-btn mc-action-btn--danger">{{ t('common.delete') }}</button>
          
        </MateInlineConfirm>
      </template>
    </MateTable>

    <MatePagination
      v-model:page-num="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      @change="loadKeys"
    />

    <!-- Detail dialog -->
    <MateDialog
      v-model="detailVisible"
      :title="t('cache.detailTitle')"
      width="620px"
      :show-footer="false"
    >
      <template #footer>
        <el-button @click="detailVisible = false">{{ t('common.close') }}</el-button>
      </template>
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <div class="kv"><span>{{ t('cache.keyLabel') }}</span><b class="mc-mono key-detail-text">{{ detail.key }}</b></div>
          <div class="kv"><span>{{ t('cache.typeLabel') }}</span><el-tag size="small" :type="typeTagType(detail.type)">{{ detail.type }}</el-tag></div>
          <div class="kv"><span>{{ t('cache.sizeLabel') }}</span><b class="mc-mono">{{ detail.size }}</b></div>
          <div class="kv"><span>{{ t('cache.ttl') }}</span><b class="mc-mono">{{ formatTtl(detail.ttl) }}</b></div>
          <h4 class="detail-section-title">{{ t('cache.value') }}</h4>
          <pre class="code-block">{{ formatValue(detail.value) }}</pre>
        </template>
      </div>
    </MateDialog>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  Search, RefreshCw, Database, MemoryStick, Gauge,
  Zap, Clock, Users,
} from 'lucide-vue-next'
import { MatePageCard, MateTable, MateSearchBar, MatePagination, MateDialog, type MateColumn, MateMessage, MateInlineConfirm, MateTooltip } from '@matecloud/ui'
import { monitorApi } from '@matecloud/core'

defineOptions({ name: 'ServerCacheView' })

const { t } = useI18n()

const columns: MateColumn[] = [
  { prop: 'key', label: t('cache.keyLabel'), minWidth: 260 },
  { prop: 'type', label: t('cache.typeLabel'), width: 100 },
  { prop: 'size', label: t('cache.sizeLabel'), width: 120 },
  { prop: 'ttl', label: t('cache.ttl'), width: 100 },
]

// ---- Redis info + command stats ----
const stats = ref<Record<string, any>>({
  version: '-', usedMemory: '-', maxMemory: '-',
  connectedClients: '-', uptimeDays: '-', dbSize: 0, hitRate: '-',
})
const commandStats = ref<Array<{ name: string; value: string }>>([])
const infoLoading = ref(false)

// ---- Stat cards with icons ----
const statCards = computed(() => [
  {
    label: t('cache.keyCount'),
    icon: Database,
    value: Number(stats.value.dbSize || 0).toLocaleString(),
  },
  {
    label: t('cache.memUsed'),
    icon: MemoryStick,
    value: stats.value.usedMemory || '-',
    sub: `${t('cache.peakMemory')}: ${stats.value.usedMemoryPeak || '-'}`,
  },
  {
    label: t('cache.hitRate'),
    icon: Gauge,
    value: stats.value.hitRate || '-',
    sub: `${stats.value.keyspaceHits || '0'} / ${stats.value.keyspaceMisses || '0'}`,
  },
  {
    label: t('cache.opsPerSec'),
    icon: Zap,
    value: stats.value.instantaneousOpsPerSec || '-',
    sub: `${t('cache.totalCommands')}: ${Number(stats.value.totalCommandsProcessed || 0).toLocaleString()}`,
  },
  {
    label: t('cache.uptime'),
    icon: Clock,
    value: formatDays(stats.value.uptimeDays),
  },
  {
    label: t('cache.clients'),
    icon: Users,
    value: stats.value.connectedClients || '-',
    sub: stats.value.blockedClients ? `${stats.value.blockedClients} blocked` : undefined,
  },
])

function formatDays(days?: string): string {
  if (!days || days === '-') return '-'
  return `${days} ${t('cache.days')}`
}

function formatTtl(ttl: number): string {
  if (ttl < 0) return '∞'
  if (ttl < 60) return ttl + 's'
  if (ttl < 3600) return Math.floor(ttl / 60) + 'm ' + (ttl % 60) + 's'
  if (ttl < 86400) return Math.floor(ttl / 3600) + 'h ' + Math.floor((ttl % 3600) / 60) + 'm'
  return Math.floor(ttl / 86400) + 'd ' + Math.floor((ttl % 86400) / 3600) + 'h'
}

// Compute max calls for bar chart width
function cmdPercent(value: string): number {
  const max = commandStats.value.length
    ? Math.max(...commandStats.value.map(c => Number(c.value) || 0))
    : 1
  return max > 0 ? ((Number(value) || 0) / max) * 100 : 0
}

async function loadCacheInfo() {
  infoLoading.value = true
  try {
    const res = await monitorApi.cacheInfo()
    if (res.success && res.data) {
      stats.value = res.data.redis ?? {}
      commandStats.value = res.data.commandStats ?? []
    }
  } catch {
    // keep defaults
  } finally {
    infoLoading.value = false
  }
}

// ---- Key browser (real SCAN) ----
const keys = ref<any[]>([])
const keysLoading = ref(false)
const pattern = ref('*')
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

async function loadKeys() {
  keysLoading.value = true
  try {
    const res = await monitorApi.cacheKeys({
      pattern: pattern.value || '*',
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    if (res.success && res.data) {
      keys.value = res.data.list ?? []
      total.value = res.data.total ?? 0
    }
  } catch {
    keys.value = []
    total.value = 0
  } finally {
    keysLoading.value = false
  }
}

function handleSearch() { pageNum.value = 1; loadKeys() }
function handleReset() { pattern.value = '*'; pageNum.value = 1; loadKeys() }

function loadAll() {
  loadCacheInfo()
  loadKeys()
}

function typeTagType(type: string) {
  return ({ string: '', hash: 'success', list: 'info', set: 'warning', zset: 'danger' } as Record<string, any>)[type] ?? ''
}

// ---- Detail ----
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)

async function openDetail(row: any) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const res = await monitorApi.cacheKeyDetail(row.key)
    detail.value = res.data ?? row
  } catch (e: any) {
    MateMessage.error(e?.msg || t('cache.loadKeyFailed'))
    detail.value = row
  } finally {
    detailLoading.value = false
  }
}

function formatValue(value: unknown): string {
  if (value === null || value === undefined || value === '') return '-'
  if (typeof value !== 'string') return JSON.stringify(value, null, 2)
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

// ---- Delete ----
async function handleDelete(row: any) {
  try {
    await monitorApi.cacheKeyDelete(row.key)
    MateMessage.success(t('common.deleteSuccess'))
    loadKeys()
    // Refresh dbSize
    loadCacheInfo()
  } catch (e: any) {
    MateMessage.error(e?.msg || t('cache.deleteKeyFailed'))
  }
}

// ---- Lifecycle ----
let refreshTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  loadAll()
  refreshTimer = setInterval(loadCacheInfo, 30000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})
</script>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}
.stat-card {
  border: 1px solid var(--mc-divider-regular, rgb(16 24 40 / 0.08));
  border-radius: var(--mc-radius);
  padding: 16px;
  background: var(--mc-bg-elevated);
}
.stat-header {
  display: flex; align-items: center; gap: 6px;
  color: var(--mc-text-muted);
  font-size: 12px;
  margin-bottom: 8px;
}
.stat-icon { color: var(--mc-primary); }
.stat-label { font-weight: 500; }
.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--mc-text-primary);
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
}
.stat-sub {
  margin-top: 6px;
  font-size: 12px;
  color: var(--mc-text-muted);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}
.info-card {
  border: 1px solid var(--mc-divider-regular, rgb(16 24 40 / 0.08));
  border-radius: var(--mc-radius);
  padding: 16px;
  background: var(--mc-bg-elevated);
}
.info-title {
  font-size: 13px; font-weight: 600;
  margin: 0 0 12px;
  color: var(--mc-text-primary);
}
.info-row {
  display: flex; align-items: center; justify-content: space-between;
  gap: 12px;
  padding: 6px 0;
  font-size: 13px;
  border-bottom: 1px dashed var(--mc-divider-subtle, rgb(16 24 40 / 0.04));
}
.info-row:last-child { border-bottom: none; }
.info-row > span { color: var(--mc-text-muted); flex-shrink: 0; }
.info-row > b {
  color: var(--mc-text-primary); font-weight: 500;
  text-align: right; min-width: 0;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

/* Command stats card */
.cmd-card {
  border: 1px solid var(--mc-divider-regular, rgb(16 24 40 / 0.08));
  border-radius: var(--mc-radius);
  padding: 16px;
  background: var(--mc-bg-elevated);
  margin-bottom: 20px;
}
.section-title {
  font-size: 14px; font-weight: 600;
  color: var(--mc-text-primary);
  margin: 0 0 12px;
}
.cmd-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
  gap: 6px 32px;
}
.cmd-bar {
  display: flex; align-items: center; gap: 10px;
  font-size: 12px;
}
.cmd-name {
  width: 100px; flex-shrink: 0;
  text-align: right;
  color: var(--mc-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cmd-track {
  flex: 1; height: 16px;
  background: var(--mc-divider-subtle, rgb(16 24 40 / 0.04));
  border-radius: 4px;
  overflow: hidden;
}
.cmd-fill {
  height: 100%;
  background: var(--mc-primary);
  border-radius: 4px;
  transition: width 0.3s ease;
  min-width: 2px;
}
.cmd-count {
  width: 70px; flex-shrink: 0;
  color: var(--mc-text-primary);
  font-weight: 500;
}

.key-text {
  max-width: 500px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
}
.key-detail-text {
  word-break: break-all;
  white-space: normal;
}

.detail-body { min-height: 200px; }
.kv {
  display: flex; align-items: center; gap: 10px;
  padding: 6px 0;
  font-size: 13px;
  border-bottom: 1px dashed var(--mc-divider-subtle, rgb(16 24 40 / 0.04));
}
.kv > span { color: var(--mc-text-muted); min-width: 80px; flex-shrink: 0; }
.kv > b { color: var(--mc-text-primary); font-weight: 500; }

.detail-section-title {
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
  max-height: 260px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
</style>
