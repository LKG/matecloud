<template>
  <MatePageCard
    :title="t('server.statusTitle')"
    :description="t('server.statusDesc')"
  >
    <template #actions>
      <el-button :loading="loading" @click="loadData">
        <RefreshCw :size="14" class="mr-1" />{{ t('common.refresh') }}
      </el-button>
    </template>

    <!-- Stat cards -->
    <div class="stat-grid">
      <div v-for="stat in statCards" :key="stat.label" class="stat-card">
        <div class="stat-header">
          <component :is="stat.icon" :size="16" class="stat-icon" />
          <span class="stat-label">{{ stat.label }}</span>
        </div>
        <div class="stat-value">{{ stat.value }}</div>
        <div v-if="stat.percent !== undefined" class="stat-bar">
          <div
            class="stat-bar__fill"
            :style="{ width: `${stat.percent}%`, background: barColor(stat.percent) }"
          />
        </div>
        <div v-if="stat.sub" class="stat-sub">{{ stat.sub }}</div>
      </div>
    </div>

    <!-- Detail info sections -->
    <div class="info-grid">
      <!-- CPU / System -->
      <div class="info-card">
        <h3 class="info-title">{{ t('server.systemInfo') }}</h3>
        <div class="info-row"><span>{{ t('server.osName') }}</span><b>{{ raw.os?.name }}</b></div>
        <div class="info-row"><span>{{ t('server.osVersion') }}</span><b class="mc-mono">{{ raw.os?.version }}</b></div>
        <div class="info-row"><span>{{ t('server.osArch') }}</span><b class="mc-mono">{{ raw.os?.arch }}</b></div>
        <div class="info-row"><span>{{ t('server.cpuCores') }}</span><b class="mc-mono">{{ raw.cpu?.cores }}</b></div>
        <div class="info-row"><span>{{ t('server.cpuUsage') }}</span><b class="mc-mono">{{ raw.cpu?.systemUsage || 'N/A' }}</b></div>
        <div class="info-row"><span>{{ t('server.loadAverage') }}</span><b class="mc-mono">{{ raw.cpu?.systemLoadAverage || 'N/A' }}</b></div>
      </div>

      <!-- Physical memory -->
      <div class="info-card">
        <h3 class="info-title">{{ t('server.physicalMemory') }}</h3>
        <div class="info-row"><span>{{ t('server.total') }}</span><b class="mc-mono">{{ raw.cpu?.totalPhysicalMemory || 'N/A' }}</b></div>
        <div class="info-row"><span>{{ t('server.used') }}</span><b class="mc-mono">{{ raw.cpu?.usedPhysicalMemory || 'N/A' }}</b></div>
        <div class="info-row"><span>{{ t('server.free') }}</span><b class="mc-mono">{{ raw.cpu?.freePhysicalMemory || 'N/A' }}</b></div>
        <div v-if="physMemPercent >= 0" class="info-bar-wrap">
          <div class="stat-bar">
            <div class="stat-bar__fill" :style="{ width: physMemPercent + '%', background: barColor(physMemPercent) }" />
          </div>
          <span class="info-bar-label mc-mono">{{ physMemPercent }}%</span>
        </div>
      </div>

      <!-- JVM -->
      <div class="info-card">
        <h3 class="info-title">{{ t('server.jvmInfo') }}</h3>
        <div class="info-row"><span>{{ t('server.javaVersion') }}</span><b class="mc-mono">{{ raw.jvm?.javaVersion }}</b></div>
        <div class="info-row"><span>{{ t('server.jvmName') }}</span><b>{{ raw.jvm?.jvmName }}</b></div>
        <div class="info-row"><span>{{ t('server.heapUsed') }}</span><b class="mc-mono">{{ raw.jvm?.usedMemory }} / {{ raw.jvm?.maxMemory }}</b></div>
        <div class="info-row"><span>{{ t('server.heapUsage') }}</span><b class="mc-mono">{{ raw.jvm?.usagePercent || 'N/A' }}</b></div>
        <div class="info-row"><span>{{ t('server.startTime') }}</span><b class="mc-mono">{{ raw.jvm?.startTime }}</b></div>
        <div class="info-row"><span>{{ t('server.uptime') }}</span><b class="mc-mono">{{ raw.jvm?.uptime }}</b></div>
        <div class="info-row"><span>{{ t('server.gcCountTime') }}</span><b class="mc-mono">{{ raw.jvm?.gcCount ?? '-' }} / {{ raw.jvm?.gcTime ?? '-' }}</b></div>
        <div class="info-row"><span>{{ t('server.threads') }}</span><b class="mc-mono">{{ raw.jvm?.threadCount ?? '-' }}</b></div>
        <div class="info-row"><span>{{ t('server.javaHome') }}</span><b class="mc-mono path-text">{{ raw.jvm?.javaHome }}</b></div>
      </div>

      <!-- Disk -->
      <div class="info-card">
        <h3 class="info-title">{{ t('server.disk') }}</h3>
        <div class="info-row"><span>{{ t('server.total') }}</span><b class="mc-mono">{{ raw.disk?.total }}</b></div>
        <div class="info-row"><span>{{ t('server.used') }}</span><b class="mc-mono">{{ raw.disk?.used }}</b></div>
        <div class="info-row"><span>{{ t('server.free') }}</span><b class="mc-mono">{{ raw.disk?.free }}</b></div>
        <div class="info-row"><span>{{ t('server.usage') }}</span><b class="mc-mono">{{ raw.disk?.usagePercent || 'N/A' }}</b></div>
        <div v-if="diskPercent >= 0" class="info-bar-wrap">
          <div class="stat-bar">
            <div class="stat-bar__fill" :style="{ width: diskPercent + '%', background: barColor(diskPercent) }" />
          </div>
          <span class="info-bar-label mc-mono">{{ diskPercent }}%</span>
        </div>
      </div>
    </div>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Cpu, HardDrive, MemoryStick, Activity, RefreshCw } from 'lucide-vue-next'
import { MatePageCard } from '@matecloud/ui'
import { monitorApi, type ServerInfoVO } from '@matecloud/core'

defineOptions({ name: 'ServerStatusView' })

const { t } = useI18n()

const loading = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null

const raw = ref<Partial<ServerInfoVO>>({})

function parseMb(s?: string): number {
  const m = s?.match(/(\d+)/)
  return m ? parseInt(m[1]) : 0
}

function parsePercent(s?: string): number {
  const m = s?.match(/([\d.]+)/)
  return m ? Math.round(parseFloat(m[1])) : -1
}

const physMemPercent = computed(() => {
  const total = parseMb(raw.value.cpu?.totalPhysicalMemory)
  const used = parseMb(raw.value.cpu?.usedPhysicalMemory)
  return total > 0 ? Math.round((used / total) * 100) : -1
})

const diskPercent = computed(() => parsePercent(raw.value.disk?.usagePercent))

async function loadData() {
  loading.value = true
  try {
    const res = await monitorApi.serverInfo()
    if (res.success && res.data) {
      raw.value = res.data
    }
  } finally {
    loading.value = false
  }
}

const statCards = computed(() => {
  const d = raw.value
  const memUsed = parseMb(d.jvm?.usedMemory)
  const memMax = parseMb(d.jvm?.maxMemory)
  const memPercent = memMax > 0 ? Math.round((memUsed / memMax) * 100) : 0
  const cpuPercent = parsePercent(d.cpu?.systemUsage)

  return [
    {
      label: t('server.cpuUsage'),
      icon: Cpu,
      value: d.cpu?.systemUsage || `${d.cpu?.cores || '-'} ${t('server.cores')}`,
      percent: cpuPercent >= 0 ? cpuPercent : undefined,
      sub: `${d.cpu?.cores || '-'} ${t('server.cores')} · ${t('server.load')} ${d.cpu?.systemLoadAverage || 'N/A'}`,
    },
    {
      label: t('server.jvmHeap'),
      icon: MemoryStick,
      value: `${memUsed} / ${memMax} MB`,
      percent: memPercent,
      sub: `${memMax - memUsed} MB ${t('server.free').toLowerCase()} · ${d.jvm?.threadCount ?? '-'} ${t('server.threads').toLowerCase()}`,
    },
    {
      label: t('server.disk'),
      icon: HardDrive,
      value: `${d.disk?.used || '-'} / ${d.disk?.total || '-'}`,
      percent: diskPercent.value >= 0 ? diskPercent.value : undefined,
      sub: `${d.disk?.free || '-'} ${t('server.free').toLowerCase()}`,
    },
    {
      label: t('server.uptime'),
      icon: Activity,
      value: d.jvm?.uptime || '-',
      sub: `${t('server.startTime')}: ${d.jvm?.startTime || '-'}`,
    },
  ] as Array<{ label: string; icon: any; value: string; percent?: number; sub?: string }>
})

function barColor(percent: number): string {
  if (percent < 60) return '#17B26A'
  if (percent < 85) return '#F79009'
  return '#F04438'
}

onMounted(() => {
  loadData()
  refreshTimer = setInterval(loadData, 30000)
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
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
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
  font-size: 22px; font-weight: 700;
  color: var(--mc-text-primary);
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
}
.stat-bar {
  margin-top: 10px;
  height: 6px;
  background: var(--mc-fill);
  border-radius: 3px;
  overflow: hidden;
}
.stat-bar__fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.4s ease;
}
.stat-sub {
  margin-top: 6px;
  font-size: 12px;
  color: var(--mc-text-muted);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 12px;
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

.info-bar-wrap {
  display: flex; align-items: center; gap: 10px;
  margin-top: 8px;
}
.info-bar-wrap .stat-bar { flex: 1; margin-top: 0; }
.info-bar-label {
  font-size: 12px; font-weight: 600;
  color: var(--mc-text-primary);
  flex-shrink: 0;
}

.path-text {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
