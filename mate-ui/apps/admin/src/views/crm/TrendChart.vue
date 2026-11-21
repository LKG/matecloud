<template>
  <div ref="chartEl" class="crm-trend-chart" />
</template>

<script setup lang="ts">
/**
 * CRM 业绩趋势折线图(签约/回款双序列)。ECharts 按需注册, 单独成 SFC
 * 便于 Dashboard 用 defineAsyncComponent 懒加载, 把 echarts 拆到独立 chunk。
 */
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { MonthTrend } from '@matecloud/core'

echarts.use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const props = defineProps<{ trends: MonthTrend[] }>()

const chartEl = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

function render() {
  if (!chartEl.value) return
  if (!chart) chart = echarts.init(chartEl.value)

  const months = props.trends.map((t) => t.month)
  const sign = props.trends.map((t) => Number(t.signMoney) || 0)
  const received = props.trends.map((t) => Number(t.receivedMoney) || 0)
  const dark = document.documentElement.classList.contains('dark')
  const axisColor = dark ? 'rgba(255,255,255,0.16)' : 'rgba(16,24,40,0.08)'
  const labelColor = '#98A2B3'

  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['签约金额', '回款金额'], textStyle: { color: labelColor }, top: 0 },
    grid: { left: 10, right: 20, top: 36, bottom: 10, containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: months, axisLine: { lineStyle: { color: axisColor } }, axisLabel: { color: labelColor } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: axisColor } }, axisLabel: { color: labelColor } },
    series: [
      { name: '签约金额', type: 'line', smooth: true, data: sign, itemStyle: { color: '#155AEF' }, areaStyle: { opacity: 0.08 } },
      { name: '回款金额', type: 'line', smooth: true, data: received, itemStyle: { color: '#12B76A' }, areaStyle: { opacity: 0.08 } },
    ],
  })
}

watch(() => props.trends, render, { deep: true })
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
.crm-trend-chart { width: 100%; height: 260px; }
</style>
