<template>
  <div class="dist">
    <div class="dist-bar">
      <div
        v-for="(seg, i) in computed"
        :key="seg.label"
        class="dist-seg"
        :style="{ width: seg.pct + '%', background: seg.color || RAMP[i % RAMP.length] }"
        :title="`${seg.label} ${seg.pct}%`"
      />
    </div>
    <div v-if="showLegend" class="dist-legend">
      <span v-for="(seg, i) in computed" :key="seg.label" class="dist-item">
        <i class="dot" :style="{ background: seg.color || RAMP[i % RAMP.length] }" />
        <span class="mc-mono">{{ seg.label }}</span>
        <span class="dist-pct">{{ seg.pct }}%</span>
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed as vComputed } from 'vue'

interface Segment {
  label: string
  value: number
  color?: string
}

const props = withDefaults(defineProps<{ segments: Segment[]; showLegend?: boolean }>(), {
  showLegend: false,
})

// 与编辑弹窗权重条一致的色板:主蓝 / 琥珀 / 翠绿 / 紫。
const RAMP = ['#155AEF', '#F79009', '#17B26A', '#7F77DD']

const computed = vComputed(() => {
  const sum = props.segments.reduce((a, s) => a + (s.value || 0), 0) || 1
  return props.segments.map((s) => ({ ...s, pct: Math.round((s.value || 0) / sum * 100) }))
})
</script>

<style scoped>
.dist-bar {
  height: 6px;
  background: var(--mc-fill, #f2f4f7);
  border-radius: 3px;
  overflow: hidden;
  display: flex;
}
.dist-seg { height: 100%; transition: width 0.3s ease; }
.dist-legend { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 5px; }
.dist-item { display: inline-flex; align-items: center; gap: 4px; font-size: 11px; color: var(--mc-text-secondary); }
.dot { width: 8px; height: 8px; border-radius: 2px; display: inline-block; }
.dist-pct { color: var(--mc-text-muted); }
.mc-mono { font-family: 'SF Mono', 'JetBrains Mono', monospace; }
</style>
