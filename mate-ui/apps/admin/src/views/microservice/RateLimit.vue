<template>
  <MatePageCard title="限流" description="基于 Redisson 分布式令牌桶，按 IP / 用户 / 路径 / 服务维度限流，规则即时热生效">
    <template #actions>
      <el-button :loading="loading" @click="load"><RefreshCw :size="14" class="mr-1" />刷新</el-button>
    </template>

    <div class="stat-grid">
      <MateStatTile label="注册服务" :value="services.length" />
      <MateStatTile label="已开限流" :value="enabledCount" tone="info" />
      <MateStatTile label="实例总数" :value="instanceTotal" />
    </div>

    <MateTable
      :columns="columns"
      :data="services"
      :loading="loading"
      row-key="service"
      :action-width="90"
      action-label="操作"
    >
      <template #col-service="{ row }">
        <span class="svc-name"><Server :size="14" class="svc-ico" />{{ row.service }}</span>
      </template>
      <template #col-status="{ row }">
        <el-tag size="small" :type="row.rateLimit.enabled ? 'success' : 'info'" disable-transitions>
          {{ row.rateLimit.enabled ? '已启用' : '未启用' }}
        </el-tag>
      </template>
      <template #col-key="{ row }">
        <el-tag size="small" disable-transitions>{{ keyLabel(row.rateLimit.key) }}</el-tag>
      </template>
      <template #col-rate="{ row }">
        <span class="mc-mono summary">{{ row.rateLimit.rate }} 次 / {{ row.rateLimit.interval }} 秒</span>
      </template>
      <template #actions="{ row }">
        <el-button link type="primary" @click="edit(row)">编辑</el-button>
      </template>
    </MateTable>

    <RateLimitDialog v-model="dialog" :service="current" @saved="load" />
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { RefreshCw, Server } from 'lucide-vue-next'
import { MatePageCard, MateTable, MateStatTile, type MateColumn } from '@matecloud/ui'
import type { ServiceView, RateLimitKeyType } from '@matecloud/core'
import { useGatewayServices } from './composables/useGatewayServices'
import RateLimitDialog from './components/RateLimitDialog.vue'

defineOptions({ name: 'RateLimitView' })

const { services, loading, load, instanceTotal } = useGatewayServices()

const enabledCount = computed(() => services.value.filter((s) => s.rateLimit.enabled).length)

const columns: MateColumn[] = [
  { prop: 'service', label: '服务名', minWidth: 150 },
  { prop: 'status', label: '状态', width: 100 },
  { prop: 'key', label: '维度', width: 120 },
  { prop: 'rate', label: '速率', minWidth: 160 },
]

const KEY_LABELS: Record<RateLimitKeyType, string> = { IP: '按 IP', USER: '按用户', PATH: '按路径', SERVICE: '按服务' }
function keyLabel(k: RateLimitKeyType) { return KEY_LABELS[k] }

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
.mr-1 { margin-right: 4px; }
.svc-name { display: inline-flex; align-items: center; gap: 6px; font-weight: 500; }
.svc-ico { color: var(--mc-text-muted); }
.summary { font-size: 12px; color: var(--mc-text-secondary); }
.mc-mono { font-family: 'SF Mono', 'JetBrains Mono', monospace; }
</style>
