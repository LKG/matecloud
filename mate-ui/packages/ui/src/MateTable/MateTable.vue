<template>
  <div class="mate-table">
    <!-- ≤768px: rows collapse into stacked label-value cards so the page never
         scrolls horizontally and no column is hidden off-screen. Re-uses the
         very same col-/actions slots the desktop table uses, so consuming
         pages need zero changes. -->
    <div v-if="isMobile" v-loading="loading" class="mate-table__cards">
      <template v-if="data.length">
        <div
          v-for="(row, i) in data"
          :key="rowId(row, i)"
          class="mc-card"
          :class="{ 'mc-card--selected': selection && isSelected(row, i) }"
        >
          <el-checkbox
            v-if="selection"
            class="mc-card__check"
            :model-value="isSelected(row, i)"
            @change="() => toggleSelect(row, i)"
          />
          <div class="mc-card__fields">
            <div v-for="col in visibleColumns" :key="col.prop" class="mc-card__row">
              <span class="mc-card__label">{{ col.label }}</span>
              <div class="mc-card__value" :class="{ 'mc-mono': col.mono }">
                <slot :name="'col-' + col.prop" :row="row" :value="row[col.prop]">
                  <span v-if="col.type === 'datetime'">{{ formatDate(row[col.prop]) }}</span>
                  <el-tag v-else-if="col.type === 'status'" :type="statusTagType(row[col.prop])" size="small">
                    {{ row[col.prop] }}
                  </el-tag>
                  <span v-else>{{ row[col.prop] ?? '—' }}</span>
                </slot>
              </div>
            </div>
          </div>
          <div v-if="$slots.actions" class="mc-card__actions">
            <slot name="actions" :row="row" :$index="i" />
          </div>
        </div>
      </template>
      <div v-else class="mate-table__empty">
        <p>{{ displayEmptyText }}</p>
      </div>
    </div>

    <!-- >768px: standard data table -->
    <div v-else class="mate-table__body">
      <el-table
        v-loading="loading"
        :data="data"
        :row-key="rowKey"
        highlight-current-row
        :height="maxHeight ? undefined : '100%'"
        :max-height="maxHeight"
        @selection-change="(rows: any[]) => emit('selectionChange', rows)"
      >
        <template #empty>
          <div class="mate-table__empty">
            <p>{{ displayEmptyText }}</p>
          </div>
        </template>

        <el-table-column v-if="selection" type="selection" width="50" />

        <el-table-column
          v-for="col in visibleColumns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :min-width="col.minWidth"
          :fixed="col.fixed"
          :align="col.align || 'left'"
          :class-name="col.mono ? 'mate-table__cell--mono' : ''"
        >
          <template #default="{ row }">
            <slot :name="'col-' + col.prop" :row="row" :value="row[col.prop]">
              <span v-if="col.type === 'datetime'">{{ formatDate(row[col.prop]) }}</span>
              <el-tag v-else-if="col.type === 'status'" :type="statusTagType(row[col.prop])" size="small">
                {{ row[col.prop] }}
              </el-tag>
              <span v-else>{{ row[col.prop] ?? '—' }}</span>
            </slot>
          </template>
        </el-table-column>

        <!-- Actions slot -->
        <el-table-column
          v-if="$slots.actions"
          :label="displayActionLabel"
          fixed="right"
          :width="actionWidth"
          align="right"
        >
          <template #default="scope">
            <div class="mate-table__actions">
              <slot name="actions" v-bind="scope" />
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'
import type { MateColumn } from './types'

const { t } = useI18n()

/**
 * Tiny matchMedia hook — kept inline so this shared package stays free of a
 * @vueuse/core dependency. Tracks the same ≤768px phone breakpoint the app
 * shell (system store `isMobile`) uses, so the table flips to card layout in
 * lockstep with the drawer nav / condensed header.
 */
const isMobile = ref(false)
let mql: MediaQueryList | undefined
function syncMobile() { isMobile.value = mql?.matches ?? false }
onMounted(() => {
  if (typeof window === 'undefined' || !window.matchMedia) return
  mql = window.matchMedia('(max-width: 768px)')
  syncMobile()
  mql.addEventListener('change', syncMobile)
})
onBeforeUnmount(() => mql?.removeEventListener('change', syncMobile))

const props = withDefaults(defineProps<{
  /** Row data array — managed by parent */
  data: any[]
  /** Column definitions */
  columns: MateColumn[]
  /** Show loading overlay */
  loading?: boolean
  /** Row key property */
  rowKey?: string
  /** Show selection checkboxes */
  selection?: boolean
  /** Action column width */
  actionWidth?: number | string
  /** Action column header label */
  actionLabel?: string
  /** Empty state text */
  emptyText?: string
  /**
   * Optional max-height cap for the table body. When set, the table grows
   * up to this value and scrolls internally beyond it; the default behaviour
   * (when omitted) is to FILL the parent card via height=100% — internal
   * scroll with a sticky header — so the scrollbar lives inside the list and
   * the search bar / pagination stay anchored.
   */
  maxHeight?: string | number
}>(), {
  loading: false,
  rowKey: 'id',
  selection: false,
  actionWidth: 200,
  actionLabel: undefined,
  emptyText: undefined,
  maxHeight: undefined,
})

const emit = defineEmits<{
  selectionChange: [rows: any[]]
}>()

const displayActionLabel = computed(() => props.actionLabel || t('common.action'))
const displayEmptyText = computed(() => props.emptyText || t('common.noData'))

const visibleColumns = computed(() =>
  props.columns.filter(c => c.show !== false),
)

// ---- Card-mode selection ----------------------------------------------------
// el-table owns selection on desktop; in card mode we re-implement the minimum
// needed to keep `selection-change` firing (used by the batch-action bar). We
// track row keys in a Set and forward the matching rows, mirroring el-table's
// payload shape so consuming pages stay agnostic to which layout is active.
function rowId(row: any, i: number): any {
  return props.rowKey ? row[props.rowKey] ?? i : i
}
const selectedKeys = ref<Set<any>>(new Set())
function isSelected(row: any, i: number): boolean {
  return selectedKeys.value.has(rowId(row, i))
}
function toggleSelect(row: any, i: number) {
  const id = rowId(row, i)
  const next = new Set(selectedKeys.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  selectedKeys.value = next
  emit('selectionChange', props.data.filter((r, idx) => next.has(rowId(r, idx))))
}

function formatDate(val: any): string {
  if (!val) return '—'
  const d = new Date(val)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

type TagType = 'success' | 'warning' | 'danger' | 'info' | 'primary'
function statusTagType(status: string): TagType {
  const map: Record<string, TagType> = { ACTIVE: 'success', DISABLED: 'warning', DELETED: 'danger' }
  return map[status] || 'info'
}
</script>

<style scoped>
.mate-table {
  /* Fill the card's flex body so el-table height=100% works,
     yielding internal scroll + sticky header (scrollbar is INSIDE the list,
     not on the page perimeter). */
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.mate-table__body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.mate-table__empty {
  padding: 48px 0;
  text-align: center;
  color: var(--mc-text-muted);
  font-size: 13px;
}
.mate-table__actions {
  display: flex;
  gap: 2px;
  justify-content: flex-end;
}

/* ---- Mobile card layout (≤768px) ---- */
.mate-table__cards {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 2px;   /* room for focus rings / selected outline */
}
.mc-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  border-radius: var(--mc-radius, 12px);
  border: 1px solid var(--mc-border, rgb(16 24 40 / 0.08));
  background: var(--mc-surface, var(--mc-bg-elevated, #fff));
  box-shadow: var(--mc-shadow-xs, 0 1px 2px rgb(16 24 40 / 0.04));
}
.mc-card--selected {
  border-color: var(--mc-primary, #155aef);
  box-shadow: 0 0 0 1px var(--mc-primary, #155aef);
}
/* Reserve a top-right slot for the selection checkbox so it never overlaps
   a field value. */
.mc-card__check {
  position: absolute;
  top: 10px;
  right: 10px;
  height: auto;
}
.mc-card--selected .mc-card__fields,
.mc-card .mc-card__check + .mc-card__fields {
  padding-right: 26px;   /* keep the first field clear of the checkbox */
}
.mc-card__fields {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.mc-card__row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  min-height: 22px;
}
.mc-card__label {
  flex: 0 0 auto;
  font-size: 12px;
  font-weight: 500;
  color: var(--mc-text-muted);
  line-height: 1.5;
  padding-top: 1px;
}
.mc-card__value {
  flex: 1 1 auto;
  min-width: 0;
  text-align: right;
  font-size: 13px;
  color: var(--mc-text-primary);
  line-height: 1.5;
  word-break: break-word;
}
.mc-card__value.mc-mono {
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
}
/* Identity cells (avatar/icon + name + sub) are a left-aligned flex row by
   design; nudge the whole block to the right edge so it lines up with the
   other right-aligned values instead of floating mid-cell. */
.mc-card__value :deep(.mc-entity-cell) {
  justify-content: flex-end;
}
/* The actions slot ships row buttons (.mc-action-btn) meant for a right-aligned
   desktop cell; on a card we let them wrap and align left under a divider. */
.mc-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding-top: 10px;
  border-top: 1px solid var(--mc-border, rgb(16 24 40 / 0.08));
  margin-top: 2px;
}
</style>

<style>
/* Mono cell style (not scoped — el-table injects class on td) */
.mate-table__cell--mono {
  font-family: 'SF Mono', 'JetBrains Mono', monospace !important;
}
</style>
