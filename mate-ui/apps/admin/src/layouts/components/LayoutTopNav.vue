<template>
  <nav ref="navRef" class="top-nav">
    <!-- Hidden measurer: all items + a "more" button, used for width math only -->
    <div ref="measureRef" class="top-nav-measure" aria-hidden="true">
      <button v-for="it in allItems" :key="'m-' + it.key" class="top-nav-item" tabindex="-1">
        <component :is="iconOf(it)" :size="16" /><span>{{ it.label }}</span>
      </button>
      <button class="top-nav-item top-nav-more" tabindex="-1">
        <span>{{ t('layout.more') }}</span><ChevronDown :size="14" />
      </button>
    </div>

    <!-- Visible items -->
    <button
      v-for="it in visibleItems"
      :key="it.key"
      class="top-nav-item"
      :class="{ active: isActive(it) }"
      @click="go(it)"
    >
      <component :is="iconOf(it)" :size="16" /><span>{{ it.label }}</span>
    </button>

    <!-- Overflow "more" dropdown -->
    <MateDropdown v-if="overflowItems.length" trigger="click" class="top-nav-more-wrap" @command="onCommand">
      <button class="top-nav-item top-nav-more" :class="{ active: overflowActive }">
        <span>{{ t('layout.more') }}</span><ChevronDown :size="14" />
      </button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item
            v-for="it in overflowItems"
            :key="it.key"
            :command="it.key"
            :class="{ 'is-active-top': isActive(it) }"
          >
            <component :is="iconOf(it)" :size="14" class="more-item-icon" />{{ it.label }}
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </MateDropdown>
  </nav>
</template>

<script setup lang="ts">
import { MateDropdown } from '@matecloud/ui'
import { computed, ref, onMounted, onBeforeUnmount, nextTick, watch, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { LayoutDashboard, ChevronDown } from 'lucide-vue-next'
import type { MenuItem as MenuItemType } from '@matecloud/core'
import { useSystemStore } from '@/stores/system'
import { useMenuNav, resolveMenuIcon, firstLeafPath } from '../composables/useMenuNav'

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const system = useSystemStore()
const { tops, activeTop, menuLabel } = useMenuNav()

interface NavEntry { key: string; label: string; dashboard?: boolean; node?: MenuItemType; iconName?: string }

// Pinned top-level groups float to the front of the bar (set via the launcher).
const orderedTops = computed(() => {
  const pinned = system.pinnedTops
  if (!pinned.length) return tops.value
  const rank = (id: string) => {
    const i = pinned.indexOf(id)
    return i === -1 ? pinned.length : i
  }
  return [...tops.value].sort((a, b) => rank(a.id) - rank(b.id))
})

const allItems = computed<NavEntry[]>(() => [
  { key: '__dashboard__', label: t('layout.dashboard'), dashboard: true },
  ...orderedTops.value.map((it) => ({ key: it.id, label: menuLabel(it), node: it, iconName: it.icon })),
])

function iconOf(it: NavEntry): Component {
  return it.dashboard ? LayoutDashboard : resolveMenuIcon(it.iconName)
}
function isActive(it: NavEntry): boolean {
  if (it.dashboard) return !activeTop.value && route.path === '/dashboard'
  return activeTop.value?.id === it.key
}
function go(it: NavEntry) {
  const path = it.dashboard ? '/dashboard' : firstLeafPath(it.node!)
  if (path && path !== route.path) router.push(path)
}
function onCommand(key: string) {
  const it = allItems.value.find((x) => x.key === key)
  if (it) go(it)
}

// ---- Overflow math: fit as many as possible, rest go into "more" ----
const navRef = ref<HTMLElement>()
const measureRef = ref<HTMLElement>()
const visibleCount = ref(allItems.value.length)
const GAP = 2 // matches .top-nav gap

const visibleItems = computed(() => allItems.value.slice(0, visibleCount.value))
const overflowItems = computed(() => allItems.value.slice(visibleCount.value))
const overflowActive = computed(() => overflowItems.value.some(isActive))

function recompute() {
  const nav = navRef.value
  const measure = measureRef.value
  if (!nav || !measure) return
  const children = Array.from(measure.children) as HTMLElement[]
  if (children.length < 1) return
  const moreW = children[children.length - 1].offsetWidth + GAP
  const itemW = children.slice(0, -1).map((el) => el.offsetWidth + GAP)
  const avail = nav.clientWidth
  const total = itemW.reduce((a, b) => a + b, 0)
  if (total <= avail) {
    visibleCount.value = itemW.length
    return
  }
  let used = moreW
  let n = 0
  for (const w of itemW) {
    if (used + w <= avail) { used += w; n++ } else break
  }
  visibleCount.value = Math.max(1, n)
}

let ro: ResizeObserver | undefined
onMounted(async () => {
  await nextTick()
  recompute()
  if (typeof ResizeObserver !== 'undefined' && navRef.value) {
    ro = new ResizeObserver(() => recompute())
    ro.observe(navRef.value)
  }
})
onBeforeUnmount(() => ro?.disconnect())

// Re-measure when the menu set or language changes (label widths change)
watch([() => allItems.value.length, locale], async () => {
  await nextTick()
  recompute()
})
</script>

<style scoped>
.top-nav {
  position: relative;
  display: flex;
  align-items: center;
  gap: 2px;
  height: 100%;
  min-width: 0;
  flex: 1;
  overflow: hidden;
}

/* hidden measurer — laid out but invisible, doesn't affect nav width */
.top-nav-measure {
  position: absolute;
  top: 0;
  left: 0;
  display: flex;
  gap: 2px;
  visibility: hidden;
  pointer-events: none;
  white-space: nowrap;
}

.top-nav-item {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 36px;
  padding: 0 13px;
  border: none;
  background: transparent;
  border-radius: var(--mc-radius);
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  white-space: nowrap;
  cursor: pointer;
  color: var(--mc-menu-text);
  transition: background 0.15s, color 0.15s;
}
.top-nav-item:hover {
  background: var(--mc-menu-bg-hover);
  color: var(--mc-menu-text-active);
}
.top-nav-item.active {
  color: var(--mc-primary);
  background: var(--mc-menu-bg-active);
}
.top-nav-item :deep(svg) { opacity: 0.85; flex-shrink: 0; }
.top-nav-item.active :deep(svg) { opacity: 1; }

.top-nav-more { gap: 3px; }
.top-nav-more-wrap { flex-shrink: 0; height: 36px; display: inline-flex; align-items: center; }

.more-item-icon { margin-right: 8px; vertical-align: -2px; }
:global(.el-dropdown-menu__item.is-active-top) {
  color: var(--mc-primary);
  font-weight: 600;
}
</style>
