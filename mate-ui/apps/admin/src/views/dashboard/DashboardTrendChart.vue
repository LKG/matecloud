<template>
  <div ref="chartEl" class="trend-chart" />
</template>

<script setup lang="ts">
/**
 * Tiny wrapper around an ECharts bar chart. Lives in its own SFC so the
 * Dashboard view can import it via {@code defineAsyncComponent}, which
 * tells Vite to ship echarts in a dedicated lazy chunk. Without this split
 * the production bundler (Vite 8 / rolldown) OOMs on Windows trying to
 * minify echarts + the rest of the app at once.
 *
 * Props:
 *   days       — chronological list (oldest first), 7 entries expected
 *   apiLabel   — legend / series label for the API-calls bars
 *   loginLabel — legend / series label for the login bars
 *
 * The chart re-renders whenever {@code days} changes; resize is wired up to
 * the window so the bars track viewport width.
 */
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { DailyStat } from '@matecloud/core'

echarts.use([BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const props = defineProps<{
  days: DailyStat[]
  apiLabel: string
  loginLabel: string
}>()

const chartEl = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

function render() {
  if (!chartEl.value) return
  if (!chart) chart = echarts.init(chartEl.value)

  // Truncate ISO date to MM-DD for compact x-axis labels.
  const xLabels = props.days.map(d => d.date.slice(5))

  // Axis / grid lines must follow the theme — slate lines vanish on a dark
  // canvas. Label gray (#98A2B3) reads in both modes, so only the lines flip.
  const dark = document.documentElement.classList.contains('dark')
  const axisColor = dark ? 'rgba(255,255,255,0.16)' : 'rgba(16,24,40,0.08)'
  const splitColor = dark ? 'rgba(255,255,255,0.08)' : 'rgba(16,24,40,0.05)'

  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: {
      top: 0,
      right: 0,
      data: [props.apiLabel, props.loginLabel],
      textStyle: { color: '#98A2B3', fontSize: 11 },
    },
    grid: { left: 10, right: 10, bottom: 0, top: 30, containLabel: true },
    xAxis: {
      type: 'category',
      data: xLabels,
      axisLine: { lineStyle: { color: axisColor } },
      axisLabel: { color: '#98A2B3', fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: splitColor } },
      axisLabel: { color: '#98A2B3', fontSize: 11 },
    },
    series: [
      {
        name: props.apiLabel,
        type: 'bar',
        data: props.days.map(d => d.apiCalls),
        itemStyle: {
          color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [
            { offset: 0, color: '#A4BCFD' }, { offset: 1, color: '#155AEF' },
          ] },
          borderRadius: [6, 6, 0, 0],
        },
        barMaxWidth: 28,
      },
      {
        name: props.loginLabel,
        type: 'bar',
        data: props.days.map(d => d.logins),
        itemStyle: {
          color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [
            { offset: 0, color: '#84E1BC' }, { offset: 1, color: '#17B26A' },
          ] },
          borderRadius: [6, 6, 0, 0],
        },
        barMaxWidth: 28,
      },
    ],
  })
}

watch(() => props.days, render, { deep: true })

const onResize = () => chart?.resize()

// Re-render axis/grid colors when the user toggles dark mode (html.dark class).
let themeObserver: MutationObserver | null = null

onMounted(() => {
  render()
  window.addEventListener('resize', onResize)
  themeObserver = new MutationObserver(render)
  themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['class'] })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  themeObserver?.disconnect()
  themeObserver = null
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.trend-chart {
  width: 100%;
  height: 240px;
}
</style>
