<template>
  <MatePageCard title="销售看板" description="线索转化 / 商机漏斗 / 业绩回款 / 成员排行, 读模型直查统计">
    <template #actions>
      <MateButton variant="ghost" :loading="loading" @click="loadAll">
        <template #icon><RefreshCw :size="14" /></template>刷新
      </MateButton>
    </template>

    <div v-loading="loading">
      <!-- 核心指标 -->
      <div class="tile-grid">
        <MateStatTile label="线索总数" :value="dash?.leadTotal ?? '—'" />
        <MateStatTile label="线索转化率" :value="rateText(dash?.leadConvertRate)" tone="info" />
        <MateStatTile label="私海客户" :value="dash?.customerPrivate ?? '—'" />
        <MateStatTile label="公海客户" :value="dash?.customerPublic ?? '—'" tone="info" />
        <MateStatTile label="商机赢单率" :value="rateText(dash?.winRate)" />
        <MateStatTile label="合同金额" :value="moneyText(dash?.contractMoney)" tone="success" />
        <MateStatTile label="回款率" :value="rateText(dash?.receivedRate)" tone="success" />
        <MateStatTile label="逾期回款计划" :value="dash?.overduePlanCount ?? '—'" :tone="(dash?.overduePlanCount ?? 0) > 0 ? 'danger' : 'info'" />
        <MateStatTile label="待回款金额" :value="moneyText(dash?.pendingPlanMoney)" tone="warning" />
        <MateStatTile label="今日待办" :value="dash?.todayTaskCount ?? '—'" />
      </div>

      <div class="chart-row">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="card-title">销售漏斗(商机阶段分布)</span></template>
          <FunnelChart v-if="funnel.length" :stages="funnel" :labels="STAGE_LABELS" />
          <el-empty v-else :image-size="60" description="暂无商机数据" />
        </el-card>
        <el-card shadow="never" class="chart-card">
          <template #header>
            <span class="card-title">业绩趋势(近 6 个月)</span>
          </template>
          <TrendChart v-if="trends.length" :trends="trends" />
          <el-empty v-else :image-size="60" description="暂无趋势数据" />
        </el-card>
      </div>

      <el-card shadow="never" class="rank-card">
        <template #header><span class="card-title">成员签约排行(Top 10)</span></template>
        <MateTable :columns="rankColumns" :data="members" :loading="loading" row-key="ownerId" empty-text="暂无成员数据">
          <template #col-contractMoney="{ row }">{{ moneyText(row.contractMoney) }}</template>
        </MateTable>
      </el-card>
    </div>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, onMounted, defineAsyncComponent } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import { statsApi, type CrmDashboard, type FunnelStage, type MemberStat, type MonthTrend } from '@matecloud/core'
import { MatePageCard, MateButton, MateStatTile, MateTable, MateMessage, type MateColumn } from '@matecloud/ui'

// echarts 图表单独成 SFC 并懒加载, 避免 rolldown Windows 打包 OOM
const FunnelChart = defineAsyncComponent(() => import('./FunnelChart.vue'))
const TrendChart = defineAsyncComponent(() => import('./TrendChart.vue'))

defineOptions({ name: 'CrmDashboard' })

const STAGE_LABELS: Record<string, string> = {
  INITIAL: '初次接触', DEMAND: '需求确认', PLAN: '方案报价', NEGOTIATION: '商务谈判', WIN: '赢单', LOSE: '输单',
}

const loading = ref(false)
const dash = ref<CrmDashboard | null>(null)
const funnel = ref<FunnelStage[]>([])
const trends = ref<MonthTrend[]>([])
const members = ref<MemberStat[]>([])

const rankColumns: MateColumn[] = [
  { prop: 'ownerName', label: '成员', minWidth: 120 },
  { prop: 'contractCount', label: '签约合同数', width: 120 },
  { prop: 'contractMoney', label: '签约金额', minWidth: 140 },
]

function rateText(v?: number | string) {
  if (v === undefined || v === null || v === '') return '—'
  return `${Number(v).toFixed(1)}%`
}
function moneyText(v?: number | string) {
  if (v === undefined || v === null || v === '') return '—'
  const n = Number(v)
  return n >= 10000 ? `¥${(n / 10000).toFixed(2)} 万` : `¥${n.toFixed(2)}`
}

async function loadAll() {
  loading.value = true
  try {
    const [o, f, t, m] = await Promise.all([
      statsApi.overview(), statsApi.funnel(), statsApi.trend(6), statsApi.memberRanking(10),
    ])
    dash.value = o.data
    funnel.value = f.data || []
    trends.value = t.data || []
    members.value = m.data || []
  } catch (e: any) {
    MateMessage.error(e?.msg || '统计加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.tile-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}
.chart-card, .rank-card { border-radius: var(--mc-radius, 8px); }
.card-title { font-size: 14px; font-weight: 600; }
@media (max-width: 900px) {
  .chart-row { grid-template-columns: 1fr; }
}
</style>
