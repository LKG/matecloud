<template>
  <div ref="chartEl" class="crm-funnel-chart" />
</template>

<script setup lang="ts">
/**
 * CRM 销售漏斗横向柱状图。ECharts 按需注册, 单独成 SFC 便于
 * Dashboard 用 defineAsyncComponent 懒加载, 把 echarts 拆到独立 chunk。
 */
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { FunnelStage } from '@matecloud/core'

echarts.use([BarChart, GridComponent, TooltipComponent, CanvasRenderer])

const props = defineProps<{ stages: FunnelStage[]; labels: Record<string, string> }>()

const chartEl = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

const STAGE_ORDER = ['INITIAL', 'DEMAND', 'PLAN', 'NEGOTIATION', 'WIN']

function render() {
  if (!chartEl.value) return
  if (!chart) chart = echarts.init(chartEl.value)

  const rows = STAGE_ORDER.map((s) => {
    const hit = props.stages.find((x) => x.stage === s)
    return { name: props.labels[s] || s, count: hit?.count ?? 0 }
  })
  const dark = document.documentElement.classList.contains('dark')
  const axisColor = dark ? 'rgba(255,255,255,0.16)' : 'rgba(16,24,40,0.08)'
  const labelColor = '#98A2B3'

  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 10, right: 30, top: 10, bottom: 10, containLabel: true },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: axisColor } }, axisLabel: { color: labelColor } },
    yAxis: {
      type: 'category',
      data: rows.map((r) => r.name).reverse(),
      axisLine: { lineStyle: { color: axisColor } },
      axisLabel: { color: labelColor },
    },
    series: [{
      type: 'bar',
      data: rows.map((r) => r.count).reverse(),
      barMaxWidth: 24,
      itemStyle: {
        borderRadius: [0, 6, 6, 0],
        color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [
          { offset: 0, color: '#A4BCFD' }, { offset: 1, color: '#155AEF' }] },
      },
      label: { show: true, position: 'right', color: labelColor },
    }],
  })
}

watch(() => props.stages, render, { deep: true })
const onResize = () => chart?.resize()
let obs: MutationObserver | null = null
onMounted(() => {
  render()
  window.addEventListener('resize', onResize)
  obs = new MutationObserver(render)
  obs.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] })
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  obs?.disconnect(); obs = null
  chart?.dispose(); chart = null
})
</script>

<style scoped>
.crm-funnel-chart { width: 100%; height: 260px; }
</style>
