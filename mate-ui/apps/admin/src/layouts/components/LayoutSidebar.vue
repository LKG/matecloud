<template>
  <!-- Desktop. In top-nav mode the sidebar only exists when the active
       top-level group actually has sub-menus — otherwise it's hidden and
       the content area spans full width. -->
  <aside v-if="showAside" class="layout-sidebar" :class="asideClass">
    <sidebar-subtree v-if="isTopMode" :collapsed="system.menuCollapsed" />
    <sidebar-split v-else-if="isTwoColumn" variant="light" />
    <sidebar-menu v-else :collapsed="system.menuCollapsed" />
  </aside>

  <!-- Mobile drawer — always the full menu tree (level 1→3) so the top-level
       groups stay reachable even in top-nav mode where they normally live in
       the header bar (which is hidden on phones). -->
  <MateDrawer v-model="system.menuDrawer" direction="ltr" :with-header="false" :size="drawerSize" class="mobile-drawer">
    <sidebar-menu />
  </MateDrawer>
</template>

<script setup lang="ts">
import { MateDrawer } from '@matecloud/ui'
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useSystemStore } from '@/stores/system'
import { useMenuNav } from '../composables/useMenuNav'
import SidebarMenu from './SidebarMenu.vue'
import SidebarSplit from './SidebarSplit.vue'
import SidebarSubtree from './SidebarSubtree.vue'

const route = useRoute()
const system = useSystemStore()
const { activeTop } = useMenuNav()

// Two-column styles (dark split / light column) have their own layout and
// don't support the single-column collapse. Top-nav mode is single-column
// (level-1 is in the header), so it ignores sidebarStyle entirely.
const isTopMode = computed(() => system.layoutMode === 'top')

// In top-nav mode, the sidebar is shown only when the active top-level group
// has sub-menus. Other modes always render the sidebar. On phones the
// persistent aside is never shown — navigation lives in the off-canvas drawer.
const hasSubMenu = computed(() => (activeTop.value?.children?.length ?? 0) > 0)
const showAside = computed(() => !system.isMobile && (!isTopMode.value || hasSubMenu.value))
const drawerSize = computed(() => (system.isMobile ? 256 : isTwoColumn.value ? 280 : 220))
const isTwoColumn = computed(
  () => !isTopMode.value
    && (system.sidebarStyle === 'split' || system.sidebarStyle === 'column'),
)
const isSingleAndCollapsed = computed(
  () => !isTwoColumn.value && system.menuCollapsed,
)
const asideClass = computed(() => ({
  collapsed: isSingleAndCollapsed.value,
  'is-subtree': isTopMode.value,
  'is-split-light': !isTopMode.value && isTwoColumn.value,
}))

watch(route, () => { system.menuDrawer = false })
</script>

<style scoped>
.layout-sidebar {
  width: var(--mc-aside-width);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--mc-sidebar-bg);
  border-right: 1px solid var(--mc-divider-subtle, rgb(16 24 40 / 0.04));
  transition: width 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  z-index: 10;
}
.layout-sidebar.collapsed { width: var(--mc-aside-collapsed-width); }

/* Top-nav mode: narrower sub-menu column (level-2/3 only) */
.layout-sidebar.is-subtree:not(.collapsed) { width: var(--mc-subtree-width); }

/* Two-column styles: width hugs the actual columns rendered (rail only when
   the active group has no sub-menu, rail + sub-column otherwise) — so there's
   never an empty blank where the sub-column would be. */
.layout-sidebar.is-split-light {
  width: auto;
  border-right: none; /* each column carries its own border */
  transition: none;    /* auto width can't tween; avoid jank */
}

</style>

<style>
.mobile-drawer .el-drawer__body { padding: 0 !important; }
</style>
