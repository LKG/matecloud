<template>
  <div class="mate-stat-tile" :class="tone ? `tone-${tone}` : ''">
    <span class="mate-stat-tile__label">{{ label }}</span>
    <span class="mate-stat-tile__value">{{ value }}</span>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'MateStatTile' })
defineProps<{
  label: string
  value: string | number
  tone?: 'info' | 'success' | 'warning' | 'danger'
}>()
</script>

<style scoped>
.mate-stat-tile {
  padding: 12px 16px;
  border-radius: var(--mc-radius, 8px);
  background: var(--mc-bg-secondary, var(--el-fill-color-light, #f5f7fa));
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.mate-stat-tile__label {
  font-size: 13px;
  color: var(--mc-text-muted, var(--el-text-color-secondary, #909399));
}
.mate-stat-tile__value {
  font-size: 22px;
  font-weight: 500;
  color: var(--mc-text-primary, var(--el-text-color-primary, #303133));
}
.tone-info .mate-stat-tile__value { color: var(--el-color-primary, #409eff); }
.tone-success .mate-stat-tile__value { color: var(--el-color-success, #67c23a); }
.tone-warning .mate-stat-tile__value { color: var(--el-color-warning, #e6a23c); }
.tone-danger .mate-stat-tile__value { color: var(--el-color-danger, #f56c6c); }
</style>
