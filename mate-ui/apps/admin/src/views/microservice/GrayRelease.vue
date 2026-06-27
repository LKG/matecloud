<template>
  <MatePageCard title="灰度发布" description="按版本路由 / 权重灰度 / 降级兜底 —— 实例版本来自 Nacos metadata，规则即时热生效">
    <template #actions>
      <el-tag size="small" :type="grayCount ? 'warning' : 'info'" effect="light" disable-transitions class="status-pill">
        {{ grayCount ? `灰度生效中 · ${grayCount}` : '全部轮询' }}
      </el-tag>
      <el-button :loading="loading" @click="load"><RefreshCw :size="14" class="mr-1" />刷新</el-button>
    </template>

    <div class="stat-grid">
      <MateStatTile label="注册服务" :value="services.length" />
      <MateStatTile label="已开灰度" :value="grayCount" tone="info" />
      <MateStatTile label="实例总数" :value="instanceTotal" />
      <MateStatTile label="配置兜底" :value="fallbackCount" tone="warning" />
    </div>

    <MateTable
      :columns="columns"
      :data="services"
      :loading="loading"
      row-key="service"
      :max-height="380"
      :action-width="90"
      action-label="操作"
    >
      <template #col-service="{ row }">
        <span class="svc-name"><Server :size="14" class="svc-ico" />{{ row.service }}</span>
      </template>
      <template #col-versions="{ row }">
        <ServiceVersionTags :versions="row.versions" :instances="row.instances" />
      </template>
      <template #col-mode="{ row }">
        <el-tag size="small" :type="modeTag(row.gray)" disable-transitions>{{ modeLabel(row.gray) }}</el-tag>
      </template>
      <template #col-summary="{ row }">
        <DistributionBar v-if="row.gray.enabled && row.gray.mode === 'WEIGHTED'" :segments="weightSegments(row)" show-legend />
        <span v-else class="mc-mono summary">{{ summary(row.gray) }}</span>
      </template>
      <template #col-fallback="{ row }"><span class="mc-mono">{{ row.gray.fallbackVersion || '-' }}</span></template>
      <template #actions="{ row }">
        <el-button link type="primary" @click="edit(row)">编辑</el-button>
      </template>
    </MateTable>

    <RouteTester :services="services" class="mt16" />

    <GrayRuleDialog v-model="dialog" :service="current" @saved="load" />
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { RefreshCw, Server } from 'lucide-vue-next'
import { MatePageCard, MateTable, MateStatTile, type MateColumn } from '@matecloud/ui'
import type { ServiceView, GrayRule } from '@matecloud/core'
import { useGatewayServices } from './composables/useGatewayServices'
import ServiceVersionTags from './components/ServiceVersionTags.vue'
import DistributionBar from './components/DistributionBar.vue'
import RouteTester from './components/RouteTester.vue'
import GrayRuleDialog from './components/GrayRuleDialog.vue'

defineOptions({ name: 'GrayReleaseView' })

const { services, loading, load, instanceTotal } = useGatewayServices()

const grayCount = computed(() => services.value.filter((s) => s.gray.enabled && s.gray.mode !== 'OFF').length)
const fallbackCount = computed(() => services.value.filter((s) => s.gray.fallbackVersion).length)

const columns: MateColumn[] = [
  { prop: 'service', label: '服务名', minWidth: 150 },
  { prop: 'versions', label: '实例 / 版本分布', minWidth: 200 },
  { prop: 'mode', label: '路由模式', width: 120 },
  { prop: 'summary', label: '灰度规则', minWidth: 160 },
  { prop: 'fallback', label: '兜底', width: 90 },
]

function modeLabel(g: GrayRule) {
  if (!g.enabled || g.mode === 'OFF') return '轮询'
  return g.mode === 'HEADER' ? '按请求头' : '按权重灰度'
}
function modeTag(g: GrayRule): 'primary' | 'info' | 'warning' {
  if (!g.enabled || g.mode === 'OFF') return 'info'
  return g.mode === 'HEADER' ? 'primary' : 'warning'
}
function summary(g: GrayRule) {
  if (!g.enabled || g.mode === 'OFF') return '轮询'
  if (g.mode === 'HEADER') return g.headerName
  return Object.entries(g.weights || {}).map(([v, w]) => `${v}:${w}%`).join(' / ') || '-'
}

/** 权重灰度的分配条数据:按服务实际存在的版本取配置权重。 */
function weightSegments(row: ServiceView) {
  return row.versions.map((v) => ({ label: v.version, value: row.gray.weights?.[v.version] || 0 }))
}

const dialog = ref(false)
const current = ref<ServiceView | null>(null)
function edit(row: ServiceView) {
  current.value = row
  dialog.value = true
}

load()
</script>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.mt16 { margin-top: 16px; }
.mr-1 { margin-right: 4px; }
.status-pill { margin-right: 10px; }
.svc-name { display: inline-flex; align-items: center; gap: 6px; font-weight: 500; }
.svc-ico { color: var(--mc-text-muted); }
.summary { font-size: 12px; color: var(--mc-text-secondary); }
.mc-mono { font-family: 'SF Mono', 'JetBrains Mono', monospace; }
</style>
