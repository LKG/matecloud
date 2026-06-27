<template>
  <div
    v-if="total > 0"
    class="mc-pagination"
    :class="{
      'mc-pagination--start': align === 'start',
      'mc-pagination--center': align === 'center',
    }"
  >
    <el-pagination
      :current-page="pageNum"
      :page-size="pageSize"
      :total="total"
      :page-sizes="pageSizes"
      :layout="layout"
      :small="small"
      :background="background"
      @update:current-page="onPageChange"
      @update:page-size="onSizeChange"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * MatePagination — small wrapper around {@code el-pagination}.
 *
 * Purpose: every CRUD view used to repeat the same
 * {@code <el-pagination v-model:current-page ... v-model:page-size ... :total ...>}
 * block with the same layout string and flex-end container. This collapses
 * all of that into one shared component.
 *
 * Behavior:
 * - Hides itself when {@code total === 0} (caller no longer needs v-if).
 * - Supports {@code v-model:pageNum} and {@code v-model:pageSize}.
 * - Emits a single {@code change} event on either page or size change so the
 *   parent can wire {@code @change="loadData"} and drop the ad-hoc watchers.
 */
const props = withDefaults(defineProps<{
  pageNum: number
  pageSize: number
  total: number
  pageSizes?: number[]
  layout?: string
  /** Alignment inside the container (defaults to right / end) */
  align?: 'start' | 'center' | 'end'
  small?: boolean
  background?: boolean
}>(), {
  pageSizes: () => [10, 20, 50],
  layout: 'total, sizes, prev, pager, next',
  align: 'end',
  small: false,
  background: false,
})

const emit = defineEmits<{
  'update:pageNum': [val: number]
  'update:pageSize': [val: number]
  change: [payload: { pageNum: number; pageSize: number }]
}>()

function onPageChange(val: number) {
  emit('update:pageNum', val)
  emit('change', { pageNum: val, pageSize: props.pageSize })
}

function onSizeChange(val: number) {
  // el-pagination resets pageNum to 1 when size changes.
  emit('update:pageSize', val)
  emit('update:pageNum', 1)
  emit('change', { pageNum: 1, pageSize: val })
}
</script>

<style scoped>
.mc-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  flex-shrink: 0;
}
.mc-pagination--start { justify-content: flex-start; }
.mc-pagination--center { justify-content: center; }
</style>
