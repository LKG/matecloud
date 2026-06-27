<template>
  <div class="mc-list">
    <!-- Header row -->
    <div class="mc-list__header">
      <div
        v-for="col in columns"
        :key="col.key"
        class="mc-list__hcell"
        :style="cellStyle(col)"
      >
        {{ col.label }}
      </div>
      <div v-if="$slots.actions" class="mc-list__hcell mc-list__hcell--actions" :style="{ width: actionWidth }">
        {{ actionLabel }}
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="mc-list__empty">
      <el-icon class="is-loading" :size="20"><Loading /></el-icon>
    </div>

    <!-- Empty -->
    <div v-else-if="!data.length" class="mc-list__empty">
      <slot name="empty">
        <p>{{ emptyText }}</p>
      </slot>
    </div>

    <!-- Rows -->
    <div v-else class="mc-list__body">
      <div v-for="(row, idx) in data" :key="rowKey ? row[rowKey] : idx" class="mc-list__row">
        <div
          v-for="col in columns"
          :key="col.key"
          class="mc-list__cell"
          :class="{ 'mc-list__cell--mono': col.mono }"
          :style="cellStyle(col)"
        >
          <slot :name="'col-' + col.key" :row="row" :value="row[col.key]">
            {{ row[col.key] ?? '—' }}
          </slot>
        </div>
        <div v-if="$slots.actions" class="mc-list__cell mc-list__cell--actions" :style="{ width: actionWidth }">
          <slot name="actions" :row="row" :index="idx" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Loading } from '@element-plus/icons-vue'
import type { ListColumn } from './types'

withDefaults(defineProps<{
  columns: ListColumn[]
  data: any[]
  loading?: boolean
  rowKey?: string
  actionWidth?: string
  actionLabel?: string
  emptyText?: string
}>(), {
  columns: () => [],
  data: () => [],
  loading: false,
  rowKey: 'id',
  actionWidth: '160px',
  actionLabel: '操作',
  emptyText: '暂无数据',
})

function cellStyle(col: ListColumn) {
  if (col.width) return { width: col.width, flexShrink: 0 }
  return { flex: 1, minWidth: 0 }
}
</script>

<style scoped>
.mc-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}
.mc-list__header {
  flex-shrink: 0;
  display: flex; align-items: center;
  padding: 7px 0;
  border-bottom: 1px solid var(--mc-divider-regular, rgb(16 24 40 / 0.08));
}
.mc-list__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}
.mc-list__hcell {
  font-size: 12px; font-weight: 500;
  text-transform: uppercase; letter-spacing: 0.3px;
  color: var(--mc-text-muted);
  padding: 0 12px;
}
.mc-list__hcell--actions {
  text-align: right; flex-shrink: 0;
}

.mc-list__row {
  display: flex; align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--mc-divider-subtle, rgb(16 24 40 / 0.04));
  transition: background 0.12s;
}
.mc-list__row:hover {
  background: var(--mc-bg-soft, #f9fafb);
}
.mc-list__row:last-child {
  border-bottom: none;
}

.mc-list__cell {
  padding: 0 12px;
  font-size: 13px;
  color: var(--mc-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mc-list__cell--mono {
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
}
.mc-list__cell--actions {
  flex-shrink: 0;
  display: flex; gap: 2px;
  justify-content: flex-end;
  overflow: visible;
  white-space: normal;
}

.mc-list__empty {
  padding: 48px 0; text-align: center;
  color: var(--mc-text-muted); font-size: 13px;
}
</style>
