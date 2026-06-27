<template>
  <div class="sidebar-inner">
    <!-- Logo -->
    <div class="sidebar-logo" :class="{ 'sidebar-logo--mini': collapsed }" @click="$router.push('/')">
      <img src="@/assets/logo.svg" alt="MateCloud" class="logo-icon" />
      <span v-if="!collapsed" class="logo-text">MateCloud</span>
    </div>

    <!-- Nav -->
    <div class="sidebar-nav">
      <!-- Dashboard (always visible) -->
      <router-link v-slot="{ isActive, navigate }" to="/dashboard" custom>
        <button class="nav-item" :class="{ 'nav-item--active': isActive }" @click="navigate">
          <LayoutDashboard :size="18" class="nav-icon" />
          <span v-if="!collapsed" class="nav-label">{{ t('layout.dashboard') }}</span>
        </button>
      </router-link>

      <!-- Dynamic menu groups -->
      <template v-for="group in menuGroups" :key="group.label">
        <div v-if="!collapsed && group.label" class="nav-group-label">{{ group.label }}</div>
        <template v-for="item in group.items" :key="item.id">
          <!-- Leaf item (no children) -->
          <router-link v-if="!item.children?.length" v-slot="{ isActive, navigate }" :to="item.path" custom>
            <button class="nav-item" :class="{ 'nav-item--active': isActive }" @click="navigate">
              <component :is="resolveIcon(item.icon)" :size="18" class="nav-icon" />
              <span v-if="!collapsed" class="nav-label">{{ menuLabel(item) }}</span>
            </button>
          </router-link>

          <!-- Group with children -->
          <div v-else class="nav-group">
            <button class="nav-item nav-item--parent" :class="{ 'nav-item--open': isOpen(item.id) }" @click="toggleGroup(item.id)">
              <component :is="resolveIcon(item.icon)" :size="18" class="nav-icon" />
              <span v-if="!collapsed" class="nav-label">{{ menuLabel(item) }}</span>
              <ChevronRight v-if="!collapsed" :size="14" class="nav-arrow" />
            </button>
            <div v-if="!collapsed && isOpen(item.id)" class="nav-children">
              <router-link v-for="child in item.children" :key="child.id" v-slot="{ isActive, navigate }" :to="child.path" custom>
                <button class="nav-item nav-item--child" :class="{ 'nav-item--active': isActive }" @click="navigate">
                  <span class="nav-label">{{ menuLabel(child) }}</span>
                </button>
              </router-link>
            </div>
          </div>
        </template>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, markRaw } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { LayoutDashboard, ChevronRight, FolderOpen } from 'lucide-vue-next'
import { useAuthStore } from '@matecloud/core'
import type { MenuItem } from '@matecloud/core'

defineProps<{ collapsed?: boolean }>()

const { t, locale } = useI18n()
const route = useRoute()
const auth = useAuthStore()

/** Resolve display name based on current locale. Falls back to Chinese name. */
function menuLabel(item: MenuItem): string {
  if (locale.value === 'en-US' && item.nameEn) return item.nameEn
  return item.name
}

// Group menu items by top-level directories
const menuGroups = computed(() => {
  const tree = auth.menuTree || []
  // If all items are directories (type M), treat each as a group
  // Otherwise, treat all as a flat list
  const hasDirectories = tree.some(item => item.type === 'M' || item.type === 'DIRECTORY')
  if (!hasDirectories) return [{ label: '', items: tree }]

  return tree.map(item => ({
    label: menuLabel(item),
    items: item.children?.length ? item.children : [item],
  }))
})

// Track open groups
const openGroups = ref<Set<string>>(new Set())

// Auto-open the group containing the current route
const currentPath = computed(() => route.path)
const menuItems = computed(() => auth.menuTree || [])

// Find a node anywhere in the tree (the collapsible groups are level-2, so a
// flat top-level lookup misses them — that's why auto-open never fired).
function findNode(nodes: MenuItem[], id: string): MenuItem | null {
  for (const n of nodes) {
    if (n.id === id) return n
    const found = n.children?.length ? findNode(n.children, id) : null
    if (found) return found
  }
  return null
}

// Check if a group should be open
function isOpen(id: string) {
  // Auto-open if the current route lives inside this group.
  const group = findNode(menuItems.value, id)
  if (group?.children?.some(c => currentPath.value === c.path || currentPath.value.startsWith(c.path + '/'))) {
    return true
  }
  return openGroups.value.has(id)
}

function toggleGroup(id: string) {
  if (openGroups.value.has(id)) {
    openGroups.value.delete(id)
  } else {
    openGroups.value.add(id)
  }
}

// Icon resolver — dynamically resolves from Lucide + Element Plus icon libraries
import * as LucideIcons from 'lucide-vue-next'
import * as EpIcons from '@element-plus/icons-vue'

const iconCache = new Map<string, any>()

function resolveIcon(name: string) {
  if (!name) return FolderOpen
  if (iconCache.has(name)) return iconCache.get(name)
  const comp = (LucideIcons as any)[name] ?? (EpIcons as any)[name]
  if (comp) {
    const raw = markRaw(comp)
    iconCache.set(name, raw)
    return raw
  }
  return FolderOpen
}
</script>

<style scoped>
.sidebar-inner {
  display: flex; flex-direction: column; height: 100%;
  background: var(--mc-sidebar-bg);
}

/* ---- Logo ---- */
.sidebar-logo {
  height: var(--mc-header-height);
  display: flex; align-items: center;
  padding: 0 16px; gap: 10px;
  border-bottom: 1px solid var(--mc-divider-subtle, rgb(16 24 40 / 0.04));
  flex-shrink: 0; cursor: pointer;
  overflow: hidden; white-space: nowrap;
}
.sidebar-logo--mini { justify-content: center; padding: 0; }
.logo-icon { width: 32px; height: 32px; flex-shrink: 0; border-radius: 8px; }
.logo-text {
  font-size: 15px; font-weight: 700;
  color: var(--mc-text-primary);
  letter-spacing: -0.2px;
}

/* ---- Nav container ---- */
.sidebar-nav {
  flex: 1; overflow-y: auto; overflow-x: hidden;
  padding: 8px;
}

/* ---- Group label ---- */
.nav-group-label {
  font-size: 11px; font-weight: 500;
  text-transform: uppercase; letter-spacing: 0.5px;
  color: var(--mc-text-muted);
  padding: 16px 12px 6px;
}

/* ---- Nav item ---- */
.nav-item {
  display: flex; align-items: center;
  width: 100%; height: 36px;
  padding: 0 12px; gap: 8px;
  border: none; background: none;
  border-radius: 8px;
  font-size: 13px; font-weight: 500;
  color: var(--mc-menu-text, #495464);
  cursor: pointer;
  transition: background 0.12s, color 0.12s;
  text-align: left;
  margin-bottom: 1px;
}
.nav-item:hover {
  background: var(--mc-menu-bg-hover, rgb(200 206 218 / 0.2));
  color: var(--mc-menu-text-hover, #354052);
}
.nav-item--active {
  background: var(--mc-menu-bg-active, rgb(21 90 239 / 0.08)) !important;
  color: var(--mc-menu-text-active, #18222F) !important;
  font-weight: 600;
}

.nav-icon {
  flex-shrink: 0;
  opacity: 0.65;
}
.nav-item:hover .nav-icon { opacity: 0.85; }
.nav-item--active .nav-icon { opacity: 1; }

.nav-label {
  flex: 1; min-width: 0;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

/* ---- Arrow for parent items ---- */
.nav-arrow {
  flex-shrink: 0;
  color: var(--mc-text-disabled);
  transition: transform 0.2s;
}
.nav-item--open .nav-arrow {
  transform: rotate(90deg);
}

/* ---- Child items (level 3) — indented with a connecting guide line,
   matching the split (分栏) sub-menu (SidebarSubtree .st-children). ---- */
.nav-children {
  position: relative;
  margin: 2px 0 4px;
  padding-left: 26px;
}
.nav-children::before {
  content: '';
  position: absolute;
  left: 19px;
  top: 2px;
  bottom: 8px;
  width: 1px;
  background: var(--mc-border, #eaecf0);
}
.nav-item--child {
  height: 34px;
  font-size: 13px;
  padding-left: 12px;
}
.nav-item--child.nav-item--active {
  color: var(--mc-primary) !important;
}
</style>
