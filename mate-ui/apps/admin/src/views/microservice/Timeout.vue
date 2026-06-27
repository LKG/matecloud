<template>
  <MatePageCard title="超时查看" description="网关全局连接 / 响应超时,以及各服务的路由级覆盖。覆盖保存后即时重建路由生效">
    <template #actions>
      <el-button :loading="loading" @click="reload"><RefreshCw :size="14" class="mr-1" />刷新</el-button>
    </template>

    <div class="stat-grid">
      <MateStatTile label="全局连接超时" :value="globalConnect" />
      <MateStatTile label="全局响应超时" :value="globalResponse" />
      <MateStatTile label="注册服务" :value="services.length" />
      <MateStatTile label="已配覆盖" :value="overrideCount" tone="warning" />
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
        <el-tag size="small" :type="row.timeout.enabled ? 'warning' : 'info'" disable-transitions>
          {{ row.timeout.enabled ? '已覆盖' : '默认' }}
        </el-tag>
      </template>
      <template #col-connect="{ row }">
        <span class="mc-mono">{{ row.timeout.enabled ? row.timeout.connectTimeoutMs + ' ms' : '-' }}</span>
      </template>
      <template #col-response="{ row }">
        <span class="mc-mono">{{ row.timeout.enabled ? row.timeout.responseTimeoutMs + ' ms' : '-' }}</span>
      </template>
      <template #actions="{ row }">
        <el-button link type="primary" @click="edit(row)">编辑</el-button>
      </template>
    </MateTable>

    <TimeoutDialog v-model="dialog" :service="current" @saved="reload" />
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { RefreshCw, Server } from 'lucide-vue-next'
import { MatePageCard, MateTable, MateStatTile, type MateColumn } from '@matecloud/ui'
import { gatewayApi, type ServiceView } from '@matecloud/core'
import { useGatewayServices } from './composables/useGatewayServices'
import TimeoutDialog from './components/TimeoutDialog.vue'

defineOptions({ name: 'TimeoutView' })

const { services, loading, load } = useGatewayServices()

const globalConnect = ref<number | string>('-')
const globalResponse = ref<number | string>('-')

const overrideCount = computed(() => services.value.filter((s) => s.timeout.enabled).length)

const columns: MateColumn[] = [
  { prop: 'service', label: '服务名', minWidth: 150 },
  { prop: 'status', label: '状态', width: 100 },
  { prop: 'connect', label: '连接超时', minWidth: 120 },
  { prop: 'response', label: '响应超时', minWidth: 120 },
]

async function loadGlobal() {
  try {
    const res = await gatewayApi.timeoutView()
    if (res.success && res.data) {
      globalConnect.value = res.data.globalConnectTimeoutMs != null ? `${res.data.globalConnectTimeoutMs} ms` : '默认'
      globalResponse.value = res.data.globalResponseTimeoutMs != null ? `${res.data.globalResponseTimeoutMs} ms` : '默认'
    }
  } catch {
    // 全局值取不到不致命,保持占位。
  }
}

async function reload() {
  await Promise.all([load(), loadGlobal()])
}

const dialog = ref(false)
const current = ref<ServiceView | null>(null)
function edit(row: ServiceView) {
  current.value = row
  dialog.value = true
}

reload()
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
.mc-mono { font-family: 'SF Mono', 'JetBrains Mono', monospace; }
</style>
