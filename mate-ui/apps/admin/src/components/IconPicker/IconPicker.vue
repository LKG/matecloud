<template>
  <MatePopover
    ref="popoverRef"
    v-model:visible="visible"
    :width="480"
    trigger="click"
    placement="bottom-start"
    :show-arrow="false"
    popper-class="icon-picker-popover"
  >
    <template #reference>
      <div class="icon-picker-trigger" :class="{ 'is-active': visible }">
        <component
          :is="resolvedIcon"
          v-if="resolvedIcon"
          :size="18"
          class="trigger-icon"
        />
        <el-icon v-else-if="resolvedEpIcon" :size="18" class="trigger-icon">
          <component :is="resolvedEpIcon" />
        </el-icon>
        <span v-else class="trigger-placeholder">{{ t('menu.icon') }}</span>
        <span v-if="modelValue" class="trigger-name">{{ modelValue }}</span>
        <ChevronDown :size="14" class="trigger-arrow" />
      </div>
    </template>

    <div class="icon-picker-panel">
      <!-- Header: library tabs + search -->
      <div class="panel-header">
        <div class="lib-tabs">
          <button
            v-for="lib in libraries"
            :key="lib.value"
            class="lib-tab"
            :class="{ 'is-active': activeLib === lib.value }"
            @click="switchLib(lib.value)"
          >
            {{ lib.label }}
            <span class="lib-count">{{ lib.count }}</span>
          </button>
        </div>
        <div class="panel-search">
          <el-input
            v-model="query"
            :placeholder="t('common.search')"
            clearable
            size="small"
            :prefix-icon="SearchIcon"
            @input="handleSearch"
          />
        </div>
      </div>

      <!-- Icon grid -->
      <div class="icon-grid-wrap" v-loading="searching">
        <div v-if="displayIcons.length" class="icon-grid">
          <div
            v-for="name in displayIcons"
            :key="name"
            class="icon-cell"
            :class="{ 'is-selected': name === modelValue }"
            :title="name"
            @click="selectIcon(name)"
          >
            <!-- Lucide icons -->
            <component
              :is="getLucideComponent(name)"
              v-if="activeLib === 'lucide' && getLucideComponent(name)"
              :size="20"
            />
            <!-- Element Plus icons -->
            <el-icon v-else-if="activeLib === 'element-plus'" :size="20">
              <component :is="getEpComponent(name)" />
            </el-icon>
          </div>
        </div>
        <div v-else class="icon-empty">
          {{ t('common.noData') }}
        </div>
      </div>

      <!-- Pagination -->
      <div class="panel-footer" v-if="filteredIcons.length > pageSize">
        <span class="footer-info">
          {{ filteredIcons.length }} {{ t('common.all') }}
        </span>
        <div class="footer-pager">
          <button
            class="pager-btn"
            :disabled="currentPage <= 1"
            @click="currentPage--"
          >
            <ChevronLeft :size="14" />
          </button>
          <span class="pager-text mc-mono">{{ currentPage }} / {{ totalPages }}</span>
          <button
            class="pager-btn"
            :disabled="currentPage >= totalPages"
            @click="currentPage++"
          >
            <ChevronRight :size="14" />
          </button>
        </div>
      </div>

      <!-- Clear button -->
      <div v-if="modelValue" class="panel-clear">
        <el-button size="small" text type="danger" @click="clearIcon">
          {{ t('common.reset') }}
        </el-button>
      </div>
    </div>
  </MatePopover>
</template>

<script setup lang="ts">
import { MatePopover } from '@matecloud/ui'
import { ref, computed, watch, shallowRef, markRaw, type Component } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search as SearchIcon, ChevronDown, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import * as LucideIcons from 'lucide-vue-next'
import * as EpIcons from '@element-plus/icons-vue'
import { elementPlusIcons, lucidePopularIcons, getLucideAllIcons } from './icons'

defineOptions({ name: 'IconPicker' })

const props = withDefaults(defineProps<{
  modelValue?: string
  /** Which icon library to show first */
  defaultLib?: 'lucide' | 'element-plus'
}>(), {
  modelValue: '',
  defaultLib: 'lucide',
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const { t } = useI18n()

const visible = ref(false)
const activeLib = ref(props.defaultLib)
const query = ref('')
const currentPage = ref(1)
const pageSize = 60
const searching = ref(false)

// Full Lucide list (lazy loaded on search)
const lucideAllIcons = ref<string[]>([])
const lucideLoaded = ref(false)

const libraries = computed(() => [
  { label: 'Lucide', value: 'lucide' as const, count: lucidePopularIcons.length },
  { label: 'Element Plus', value: 'element-plus' as const, count: elementPlusIcons.length },
])

// ---- Icon list for current library ----
const currentLibIcons = computed(() => {
  if (activeLib.value === 'element-plus') return elementPlusIcons
  // If we have searched with full list, use that; otherwise popular
  return query.value && lucideLoaded.value ? lucideAllIcons.value : lucidePopularIcons
})

const filteredIcons = computed(() => {
  if (!query.value) return currentLibIcons.value
  const q = query.value.toLowerCase()
  return currentLibIcons.value.filter(name => name.toLowerCase().includes(q))
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredIcons.value.length / pageSize)))

const displayIcons = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredIcons.value.slice(start, start + pageSize)
})

// ---- Search handler (lazy load full lucide list) ----
let searchTimer: ReturnType<typeof setTimeout> | null = null
function handleSearch() {
  currentPage.value = 1
  if (activeLib.value === 'lucide' && query.value && !lucideLoaded.value) {
    if (searchTimer) clearTimeout(searchTimer)
    searchTimer = setTimeout(async () => {
      searching.value = true
      try {
        lucideAllIcons.value = await getLucideAllIcons()
        lucideLoaded.value = true
      } finally {
        searching.value = false
      }
    }, 300)
  }
}

function switchLib(lib: 'lucide' | 'element-plus') {
  activeLib.value = lib
  currentPage.value = 1
  query.value = ''
}

// ---- Icon component resolution ----
const lucideComponentCache = new Map<string, Component>()
function getLucideComponent(name: string): Component | null {
  if (lucideComponentCache.has(name)) return lucideComponentCache.get(name)!
  const comp = (LucideIcons as unknown as Record<string, Component>)[name]
  if (comp) {
    const raw = markRaw(comp)
    lucideComponentCache.set(name, raw)
    return raw
  }
  return null
}

const epComponentCache = new Map<string, Component>()
function getEpComponent(name: string): Component | null {
  if (epComponentCache.has(name)) return epComponentCache.get(name)!
  const comp = (EpIcons as Record<string, Component>)[name]
  if (comp) {
    const raw = markRaw(comp)
    epComponentCache.set(name, raw)
    return raw
  }
  return null
}

// ---- Trigger icon preview ----
const resolvedIcon = computed(() => {
  if (!props.modelValue) return null
  return getLucideComponent(props.modelValue)
})
const resolvedEpIcon = computed(() => {
  if (!props.modelValue || resolvedIcon.value) return null
  return getEpComponent(props.modelValue)
})

// ---- Selection ----
function selectIcon(name: string) {
  emit('update:modelValue', name)
  visible.value = false
}

function clearIcon() {
  emit('update:modelValue', '')
  visible.value = false
}

// Reset page when query changes
watch(query, () => { currentPage.value = 1 })
</script>

<style scoped>
.icon-picker-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  height: 32px;
  border: 1px solid var(--el-border-color, #dcdfe6);
  border-radius: var(--el-border-radius-base, 4px);
  background: var(--el-fill-color-blank, #fff);
  cursor: pointer;
  font-size: 13px;
  color: var(--mc-text-primary);
  transition: border-color 0.2s;
  min-width: 160px;
  box-sizing: border-box;
}
.icon-picker-trigger:hover,
.icon-picker-trigger.is-active {
  border-color: var(--mc-primary, #4318FF);
}
.trigger-icon { color: var(--mc-text-primary); flex-shrink: 0; }
.trigger-name {
  flex: 1; min-width: 0;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
  font-size: 12px;
}
.trigger-placeholder {
  flex: 1;
  color: var(--el-text-color-placeholder, #a8abb2);
  font-size: 13px;
}
.trigger-arrow {
  color: var(--mc-text-muted);
  flex-shrink: 0;
  transition: transform 0.2s;
}
.is-active .trigger-arrow { transform: rotate(180deg); }

/* ---- Panel ---- */
.icon-picker-panel {
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.lib-tabs {
  display: flex;
  gap: 4px;
}
.lib-tab {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: 1px solid var(--mc-divider-regular, rgb(16 24 40 / 0.08));
  border-radius: 6px;
  background: transparent;
  color: var(--mc-text-muted);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}
.lib-tab:hover { background: var(--mc-fill); }
.lib-tab.is-active {
  background: var(--mc-primary);
  color: #fff;
  border-color: var(--mc-primary);
}
.lib-count {
  font-size: 10px;
  opacity: 0.7;
}

/* ---- Grid ---- */
.icon-grid-wrap {
  min-height: 180px;
  max-height: 320px;
  overflow-y: auto;
  overflow-x: hidden;
}
.icon-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 6px;
}
.icon-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  border-radius: 6px;
  cursor: pointer;
  color: var(--mc-text-primary);
  transition: all 0.15s;
  border: 1px solid transparent;
}
.icon-cell:hover {
  background: var(--mc-fill);
  border-color: var(--mc-divider-regular, rgb(16 24 40 / 0.08));
}
.icon-cell.is-selected {
  background: var(--mc-primary);
  color: #fff;
  border-color: var(--mc-primary);
}

.icon-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 180px;
  color: var(--mc-text-disabled);
  font-size: 13px;
}

/* ---- Footer ---- */
.panel-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--mc-divider-subtle, rgb(16 24 40 / 0.04));
}
.footer-info {
  font-size: 11px;
  color: var(--mc-text-muted);
}
.footer-pager {
  display: flex;
  align-items: center;
  gap: 6px;
}
.pager-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px; height: 24px;
  border: 1px solid var(--mc-divider-regular, rgb(16 24 40 / 0.08));
  border-radius: 4px;
  background: transparent;
  color: var(--mc-text-primary);
  cursor: pointer;
  transition: all 0.15s;
}
.pager-btn:hover:not(:disabled) { background: var(--mc-fill); }
.pager-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.pager-text { font-size: 11px; color: var(--mc-text-muted); }

.panel-clear {
  display: flex;
  justify-content: flex-end;
  margin-top: 4px;
}
</style>
