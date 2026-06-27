<template>
  <div class="split-sidebar" :class="`is-${variant}`">
    <!-- Left: primary menu (dark, narrow) -->
    <div class="split-primary">
      <!-- Logo -->
      <div class="split-logo" @click="$router.push('/')">
        <img src="@/assets/logo.svg" alt="M" class="split-logo-icon" />
      </div>

      <el-scrollbar class="split-primary-scroll">
        <!-- Dashboard (always first) -->
        <MateTooltip
          :content="t('layout.dashboard')"
          placement="right"
          :offset="10"
          :show-after="120"
          popper-class="split-rail-tip"
        >
          <div
            class="split-item"
            :class="{ active: !activeGroup && $route.path === '/dashboard' }"
            @click="goLeaf('/dashboard')"
          >
            <LayoutDashboard :size="18" />
            <span class="split-item-label">{{ t('layout.dashboard') }}</span>
          </div>
        </MateTooltip>

        <!-- Top-level menu groups -->
        <MateTooltip
          v-for="item in auth.menuTree"
          :key="item.id"
          :content="menuLabel(item)"
          placement="right"
          :offset="10"
          :show-after="120"
          popper-class="split-rail-tip"
        >
          <div
            class="split-item"
            :class="{ active: activeGroup?.id === item.id }"
            @click="selectGroup(item)"
          >
            <component :is="resolveIcon(item.icon)" :size="18" />
            <span class="split-item-label">{{ railLabel(item) }}</span>
          </div>
        </MateTooltip>
      </el-scrollbar>
    </div>

    <!-- Right: sub-menu — reuses the top-layout subtree (clean level-2 rows,
         indented level-3 with a guide line) so both layouts share one style. -->
    <div v-if="activeGroup?.children?.length" class="split-secondary">
      <div class="split-secondary-title">{{ menuLabel(activeGroup) }}</div>
      <sidebar-subtree :group="activeGroup" class="split-secondary-tree" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { MateTooltip } from '@matecloud/ui'
import { ref, watch, markRaw, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { LayoutDashboard, FolderOpen } from 'lucide-vue-next'
import * as LucideIcons from 'lucide-vue-next'
import * as EpIcons from '@element-plus/icons-vue'
import { useAuthStore, type MenuItem as MenuItemType } from '@matecloud/core'
import SidebarSubtree from './SidebarSubtree.vue'

// variant only changes the SKIN (dark = existing split, light = light two-column).
// Menu data + selection logic are identical and reuse the shared auth.menuTree.
withDefaults(defineProps<{ variant?: 'dark' | 'light' }>(), { variant: 'dark' })

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

/** Resolve menu display name by current locale */
function menuLabel(item: MenuItemType): string {
  if (locale.value === 'en-US' && item.nameEn) return item.nameEn
  return item.name
}

/** Rail (narrow column) label — prefer the explicit shortName when set,
 *  otherwise fall back to the locale-resolved full name. */
function railLabel(item: MenuItemType): string {
  return item.shortName?.trim() || menuLabel(item)
}

// Dynamic icon resolver — same logic as MenuTree.vue / MenuItem.vue.
// Lucide first (项目标准), Element Plus 兜底。 markRaw 避免 Vue 把图标
// 组件对象包成 reactive proxy。
// 关键修复:之前用硬编码白名单 (Settings/Users/Shield/...) 只覆盖 19 个
// 图标,任何不在表里的名字 (如 Sparkles / Plug / Cpu / MessageCircle / Box)
// 都 fallback 到 FolderOpen,导致一级菜单图标和菜单管理表单显示的不一致。
const iconCache = new Map<string, Component>()
function resolveIcon(name?: string): Component {
  if (!name) return FolderOpen
  const cached = iconCache.get(name)
  if (cached) return cached
  const comp = (LucideIcons as unknown as Record<string, Component>)[name]
    ?? (EpIcons as Record<string, Component>)[name]
  if (!comp) return FolderOpen
  const raw = markRaw(comp)
  iconCache.set(name, raw)
  return raw
}

// Track which top-level group is expanded in the right column
const activeGroup = ref<MenuItemType | null>(null)

function selectGroup(item: MenuItemType) {
  if (item.children?.length) {
    activeGroup.value = item
  } else {
    activeGroup.value = null
    router.push(item.path)
  }
}

function goLeaf(path: string) {
  activeGroup.value = null
  router.push(path)
}

// Sync activeGroup with the current route. Also keyed on menuTree.length so it
// re-resolves once the menu finishes loading (the immediate run on mount can
// fire before auth.menuTree is populated — otherwise the sub-menu column stays
// empty on a hard reload of a deep route until you click a rail item).
watch([() => route.path, () => auth.menuTree.length], () => {
  const p = route.path
  if (p === '/dashboard') { activeGroup.value = null; return }
  for (const item of auth.menuTree) {
    if (matchesTree(item, p)) { activeGroup.value = item; return }
  }
}, { immediate: true })

function matchesTree(node: MenuItemType, path: string): boolean {
  if (node.path === path) return true
  return node.children?.some(c => matchesTree(c, path)) ?? false
}
</script>

<style scoped>
.split-sidebar {
  display: flex;
  height: 100%;
}

/* ---- Left: primary column ----
   Soft vertical gradient (depth, not a flat slab) + a hairline inner edge so
   the rail reads as a distinct, premium surface against the sub-menu. */
.split-primary {
  width: 64px;
  flex-shrink: 0;
  background: linear-gradient(180deg, #23253410 0%, #181922 100%), #1d1e2c;
  box-shadow: inset -1px 0 0 rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.split-logo {
  height: var(--mc-header-height);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}
.split-logo-icon { width: 34px; height: 34px; border-radius: 8px; }

.split-primary-scroll { flex: 1; }

.split-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 11px 0;
  margin: 4px 9px;
  border-radius: 12px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.58);
  transition: background 0.18s, color 0.18s, box-shadow 0.18s;
}
.split-item :deep(svg) { transition: color 0.18s; }
/* Dark rail is icon-only (labels live in the wider sub-menu); names show on
   hover via the native title tooltip. */
.split-sidebar.is-dark .split-item-label { display: none; }
.split-sidebar.is-dark .split-item :deep(svg) { width: 21px; height: 21px; }
.split-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.06);
}
/* Soft brand-tinted pill (nuwax-style) instead of a heavy solid block:
   tint + subtle glow + brand-coloured icon, label stays high-contrast white. */
.split-item.active {
  color: #fff;
  background: rgba(var(--mc-primary-rgb), 0.20);
  box-shadow: 0 4px 14px -6px rgba(var(--mc-primary-rgb), 0.55),
              inset 0 0 0 1px rgba(var(--mc-primary-rgb), 0.28);
}
.split-item.active :deep(svg) { color: var(--mc-primary); }

.split-item-label {
  font-size: 11px;
  line-height: 1.2;
  text-align: center;
  max-width: 72px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---- Right: secondary column ---- */
.split-secondary {
  width: 208px;
  flex-shrink: 0;
  /* Faint brand wash toward the bottom (echoes the reference's soft gradient
     panel); resolves over the themed elevated surface in both light & dark. */
  background:
    linear-gradient(180deg, transparent 55%, rgba(var(--mc-primary-rgb), 0.05) 100%),
    var(--mc-bg-elevated);
  border-right: 1px solid var(--mc-border-light);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.split-secondary-title {
  height: var(--mc-header-height);
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 0 16px;
  font-size: 15px;
  font-weight: 600;
  color: var(--mc-text-primary);
  border-bottom: 1px solid var(--mc-border-light);
  flex-shrink: 0;
}

/* The reused subtree fills the column below the title and brings its own
   scroll + the level-3 guide-line styling (shared with top-nav mode). */
.split-secondary-tree { flex: 1; min-height: 0; height: auto; }

/* ============================================================
   Light variant: light icon rail + wider white
   sub-menu. Reuses the exact same structure/logic as the dark
   split — only colours and widths differ.
   ============================================================ */
.split-sidebar.is-light .split-primary {
  width: 64px;                /* icon-only, aligned with the dark rail */
  background: var(--mc-sidebar-bg);
  border-right: 1px solid var(--mc-divider-subtle, rgb(16 24 40 / 0.06));
}
.split-sidebar.is-light .split-logo {
  border-bottom: 1px solid var(--mc-divider-subtle, rgb(16 24 40 / 0.06));
}
/* Icon-only rail: names show on hover via el-tooltip. */
.split-sidebar.is-light .split-item-label { display: none; }
.split-sidebar.is-light .split-item {
  color: var(--mc-menu-text, #495464);
}
.split-sidebar.is-light .split-item :deep(svg) {
  width: 21px; height: 21px;
  opacity: 0.7; transition: opacity 0.12s;
}
.split-sidebar.is-light .split-item:hover {
  color: var(--mc-menu-text-hover, #354052);
  background: var(--mc-menu-bg-hover, rgb(200 206 218 / 0.2));
}
.split-sidebar.is-light .split-item:hover :deep(svg) { opacity: 0.9; }
/* Soft brand-tinted active pill (light) — same language as the dark rail. */
.split-sidebar.is-light .split-item.active {
  color: var(--mc-primary);
  background: rgba(var(--mc-primary-rgb), 0.10);
  box-shadow: none;
}
.split-sidebar.is-light .split-item.active :deep(svg) { opacity: 1; color: var(--mc-primary); }
</style>
