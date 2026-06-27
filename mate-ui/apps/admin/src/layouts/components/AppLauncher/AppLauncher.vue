<template>
  <transition name="launcher-fade">
    <div v-if="system.showLauncher" class="launcher-scrim" @click="close">
      <div class="launcher-panel" @click.stop>
        <!-- Search -->
        <div class="launcher-head">
          <Search :size="18" class="launcher-search-icon" />
          <input
            ref="searchRef"
            v-model="keyword"
            class="launcher-search"
            :placeholder="t('layout.searchApps')"
            @keydown.down.prevent="move(1)"
            @keydown.up.prevent="move(-1)"
            @keydown.enter.prevent="enterActive"
          />
          <kbd class="launcher-kbd">Esc</kbd>
        </div>

        <!-- Body -->
        <div ref="bodyRef" class="launcher-body">
          <!-- Browse mode: app grid grouped by category -->
          <template v-if="!searching">
            <div v-for="group in browseGroups" :key="group.label" class="launcher-group">
              <div class="launcher-group-title">
                {{ group.label }}<span class="launcher-group-count">{{ group.items.length }}</span>
              </div>
              <div class="launcher-grid">
                <app-card
                  v-for="app in group.items"
                  :key="app.key"
                  :icon="app.icon"
                  :color="app.color"
                  :label="app.label"
                  :desc="app.desc"
                  :active="isActive(app)"
                  :pinnable="!app.dashboard"
                  :pinned="system.isPinned(app.key)"
                  @select="select(app)"
                  @toggle-pin="system.togglePinnedTop(app.key)"
                />
              </div>
            </div>
          </template>

          <!-- Search mode: keyboard-navigable result list (apps + features) -->
          <template v-else>
            <div v-if="appResults.length" class="launcher-section">
              <div class="launcher-group-title">
                {{ t('layout.appsGroup') }}<span class="launcher-group-count">{{ appResults.length }}</span>
              </div>
              <launcher-result-row
                v-for="(app, i) in appResults"
                :key="app.key"
                :icon="app.icon"
                :color="app.color"
                :label="app.label"
                :sub="app.desc"
                :query="keyword"
                :active="activeIndex === i"
                @select="select(app)"
                @hover="activeIndex = i"
              />
            </div>

            <div v-if="fnResults.length" class="launcher-section">
              <div class="launcher-group-title">
                {{ t('layout.funcsGroup') }}<span class="launcher-group-count">{{ fnResults.length }}</span>
              </div>
              <launcher-result-row
                v-for="(fn, i) in fnResults"
                :key="fn.id"
                :icon="fn.icon"
                :label="fn.label"
                :sub="fn.crumb || fn.path"
                :query="keyword"
                :active="activeIndex === appResults.length + i"
                @select="goPath(fn.path)"
                @hover="activeIndex = appResults.length + i"
              />
            </div>

            <div v-if="!navList.length" class="launcher-empty">{{ t('layout.noResults') }}</div>
          </template>
        </div>

        <!-- Footer hint bar -->
        <div class="launcher-foot">
          <span class="foot-hint"><kbd>↑</kbd><kbd>↓</kbd>{{ t('layout.navHint') }}</span>
          <span class="foot-hint"><kbd>↵</kbd>{{ t('layout.openHint') }}</span>
          <span class="foot-hint"><kbd>esc</kbd>{{ t('common.close') }}</span>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Search, LayoutDashboard } from 'lucide-vue-next'
import type { MenuItem as MenuItemType } from '@matecloud/core'
import { useSystemStore } from '@/stores/system'
import { useMenuNav, resolveMenuIcon, firstLeafPath } from '../../composables/useMenuNav'
import AppCard from './AppCard.vue'
import LauncherResultRow from './LauncherResultRow.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const system = useSystemStore()
const { tops, activeTop, menuLabel } = useMenuNav()

interface AppEntry {
  key: string
  label: string
  icon: Component
  color: string
  desc?: string
  node?: MenuItemType
  dashboard?: boolean
  category: string
}
interface FnEntry {
  id: string
  label: string
  path: string
  icon: Component
  crumb: string
}

const PALETTE = ['#155AEF', '#7A5AF8', '#06AED4', '#F79009', '#17B26A', '#EE46BC', '#F04438', '#475467']

// ---- Top-level apps (browse grid + app search results) ----
const apps = computed<AppEntry[]>(() => {
  const list: AppEntry[] = [{
    key: '__dashboard__',
    label: t('layout.dashboard'),
    icon: LayoutDashboard,
    color: PALETTE[0],
    desc: '/dashboard',
    dashboard: true,
    category: t('layout.appsGroup'),
  }]
  tops.value.forEach((it, i) => {
    list.push({
      key: it.id,
      label: menuLabel(it),
      icon: resolveMenuIcon(it.icon),
      color: PALETTE[(i + 1) % PALETTE.length],
      desc: firstLeafPath(it) ?? undefined,
      node: it,
      category: (it as unknown as { category?: string }).category || t('layout.appsGroup'),
    })
  })
  return list
})

// ---- Flat feature index: every navigable leaf, with its breadcrumb ----
const flatMenu = computed<FnEntry[]>(() => {
  const out: FnEntry[] = []
  const walk = (node: MenuItemType, trail: string[]) => {
    const label = menuLabel(node)
    if (node.children?.length) {
      node.children.forEach((c) => walk(c, [...trail, label]))
    } else if (node.path) {
      out.push({ id: node.id, label, path: node.path, icon: resolveMenuIcon(node.icon), crumb: trail.join(' / ') })
    }
  }
  tops.value.forEach((it) => walk(it, []))
  return out
})

// ---- Search state ----
const keyword = ref('')
const searching = computed(() => keyword.value.trim().length > 0)
const kw = computed(() => keyword.value.trim().toLowerCase())

const browseGroups = computed(() => {
  const map = new Map<string, AppEntry[]>()
  for (const a of apps.value) {
    const arr = map.get(a.category) ?? []
    arr.push(a)
    map.set(a.category, arr)
  }
  return [...map.entries()].map(([label, items]) => ({ label, items }))
})

const appResults = computed(() => {
  if (!searching.value) return []
  return apps.value.filter(
    (a) => a.label.toLowerCase().includes(kw.value) || (a.desc ?? '').toLowerCase().includes(kw.value),
  )
})
const fnResults = computed(() => {
  if (!searching.value) return []
  return flatMenu.value
    .filter(
      (f) =>
        f.label.toLowerCase().includes(kw.value)
        || f.crumb.toLowerCase().includes(kw.value)
        || f.path.toLowerCase().includes(kw.value),
    )
    .slice(0, 40)
})

// Flat list backing keyboard navigation (apps first, then features).
type NavItem = { type: 'app'; app: AppEntry } | { type: 'fn'; fn: FnEntry }
const navList = computed<NavItem[]>(() => [
  ...appResults.value.map((app) => ({ type: 'app', app }) as NavItem),
  ...fnResults.value.map((fn) => ({ type: 'fn', fn }) as NavItem),
])

const activeIndex = ref(0)
watch([kw, navList], () => { activeIndex.value = 0 })

function move(delta: number) {
  const n = navList.value.length
  if (!n) return
  activeIndex.value = (activeIndex.value + delta + n) % n
  scrollActiveIntoView()
}
function enterActive() {
  if (!searching.value) return
  const item = navList.value[activeIndex.value]
  if (item?.type === 'app') select(item.app)
  else if (item?.type === 'fn') goPath(item.fn.path)
}

const bodyRef = ref<HTMLElement>()
function scrollActiveIntoView() {
  nextTick(() => {
    bodyRef.value?.querySelector<HTMLElement>('.res-row.is-active')
      ?.scrollIntoView({ block: 'nearest' })
  })
}

function isActive(app: AppEntry): boolean {
  if (app.dashboard) return !activeTop.value && route.path === '/dashboard'
  return activeTop.value?.id === app.key
}

function go(app: AppEntry) {
  const path = app.dashboard ? '/dashboard' : firstLeafPath(app.node!)
  if (path) goPath(path)
}
function goPath(path: string) {
  if (path && path !== route.path) router.push(path)
  close()
}
function select(app: AppEntry) {
  go(app)
}
function close() {
  system.showLauncher = false
}

const searchRef = ref<HTMLInputElement>()
watch(() => system.showLauncher, async (open) => {
  if (open) {
    keyword.value = ''
    activeIndex.value = 0
    await nextTick()
    searchRef.value?.focus()
  }
})

// ⌘K / Ctrl+K toggles the launcher anywhere; Esc closes it.
function onKey(e: KeyboardEvent) {
  if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    system.showLauncher = !system.showLauncher
  } else if (e.key === 'Escape' && system.showLauncher) {
    close()
  }
}
onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>

<style scoped>
.launcher-scrim {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(16, 24, 40, 0.32);
  backdrop-filter: blur(6px) saturate(120%);
  -webkit-backdrop-filter: blur(6px) saturate(120%);
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 64px 16px 16px;
}

/* Frosted-glass command panel */
.launcher-panel {
  position: relative;
  width: 100%;
  max-width: 720px;
  max-height: calc(100vh - 96px);
  background: var(--mc-panel-raised, rgba(255, 255, 255, 0.86));
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border: 1px solid var(--mc-divider-regular, rgb(16 24 40 / 0.08));
  border-radius: var(--mc-radius-xl);
  box-shadow: var(--mc-shadow-strong, 0 24px 70px rgba(16, 24, 40, 0.18));
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
/* Soft brand glow bleeding from the top edge — adds depth without noise. */
.launcher-panel::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 140px;
  background: var(--mc-glow);
  pointer-events: none;
}

.launcher-head {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 18px;
  border-bottom: 1px solid var(--mc-divider-subtle, rgb(16 24 40 / 0.06));
}
.launcher-search-icon { color: var(--mc-text-muted); flex-shrink: 0; }
.launcher-search {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 16px;
  font-family: inherit;
  color: var(--mc-text-primary);
}
.launcher-search::placeholder { color: var(--mc-text-muted); }
.launcher-kbd {
  font-size: 11px;
  color: var(--mc-text-disabled);
  border: 1px solid var(--mc-border);
  border-radius: 5px;
  padding: 1px 7px;
  flex-shrink: 0;
}

.launcher-body { position: relative; padding: 14px 16px 18px; overflow-y: auto; }
.launcher-group { margin-top: 18px; }
.launcher-group:first-child { margin-top: 2px; }
.launcher-section { margin-bottom: 6px; }
.launcher-section + .launcher-section { margin-top: 12px; }
.launcher-group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 800;
  color: var(--mc-text-secondary);
  margin-bottom: 11px;
  padding: 0 4px;
}
.launcher-group-count {
  font-size: 10px;
  font-weight: 700;
  color: var(--mc-text-disabled);
  background: var(--mc-bg-soft);
  padding: 1px 7px;
  border-radius: 999px;
}
.launcher-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(184px, 1fr));
  gap: 10px;
}
.launcher-empty {
  text-align: center;
  color: var(--mc-text-muted);
  padding: 48px;
  font-size: 13px;
}

/* Footer keyboard-hint bar */
.launcher-foot {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 9px 18px;
  border-top: 1px solid var(--mc-divider-subtle, rgb(16 24 40 / 0.06));
  background: var(--mc-surface-overlay, rgba(255, 255, 255, 0.6));
}
.foot-hint {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: var(--mc-text-muted);
}
.foot-hint kbd {
  font-size: 11px;
  color: var(--mc-text-secondary);
  background: var(--mc-bg-elevated);
  border: 1px solid var(--mc-border);
  border-radius: 4px;
  padding: 0 5px;
  line-height: 16px;
  min-width: 16px;
  text-align: center;
}

/* mobile: full-screen sheet */
@media (max-width: 768px) {
  .launcher-scrim { padding: 0; }
  .launcher-panel { max-width: 100%; max-height: 100%; height: 100%; border-radius: 0; border: none; }
  .launcher-grid { grid-template-columns: 1fr 1fr; }
  .launcher-foot { display: none; }
}

.launcher-fade-enter-active,
.launcher-fade-leave-active { transition: opacity 0.16s ease; }
.launcher-fade-enter-from,
.launcher-fade-leave-to { opacity: 0; }
.launcher-fade-enter-active .launcher-panel { transition: transform 0.2s cubic-bezier(0.2, 0.8, 0.3, 1); }
.launcher-fade-enter-from .launcher-panel { transform: translateY(-10px) scale(0.99); }
</style>
