<template>
  <!-- Floating panel anchored to a collapsed rail icon. Teleported to <body>
       by the parent so the aside's `overflow: hidden` never clips it. -->
  <div
    class="sidebar-flyout"
    :style="posStyle"
    role="menu"
    @mouseenter="emit('enter')"
    @mouseleave="emit('leave')"
  >
    <!-- Header = the hovered level-2 group; clicking jumps to its first leaf. -->
    <button class="fly-header" :class="{ 'is-active': headerActive }" @click="onHeader">
      <component :is="resolveMenuIcon(item.icon)" :size="16" class="fly-header-icon" />
      <span class="fly-header-text">{{ menuLabel(item) }}</span>
    </button>

    <el-scrollbar v-if="item.children?.length" max-height="62vh">
      <div class="fly-body">
        <SidebarFlyoutItem
          v-for="child in item.children"
          :key="child.id"
          :item="child"
          @navigate="emit('navigate')"
        />
      </div>
    </el-scrollbar>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { MenuItem as MenuItemType } from '@matecloud/core'
import { useMenuNav, resolveMenuIcon, matchesTree, firstLeafPath } from '../composables/useMenuNav'
import SidebarFlyoutItem from './SidebarFlyoutItem.vue'

const props = defineProps<{ item: MenuItemType; top: number; left: number }>()
const emit = defineEmits<{ navigate: []; enter: []; leave: [] }>()

const route = useRoute()
const router = useRouter()
const { menuLabel } = useMenuNav()

const posStyle = computed(() => ({ top: `${props.top}px`, left: `${props.left}px` }))
const headerActive = computed(() => matchesTree(props.item, route.path))

function onHeader() {
  const p = firstLeafPath(props.item)
  if (p && p !== route.path) router.push(p)
  emit('navigate')
}
</script>

<style scoped>
.sidebar-flyout {
  position: fixed;
  z-index: 2200;
  width: 220px;
  padding: 6px;
  background: var(--mc-bg-elevated, #fff);
  border: 1px solid var(--mc-divider-subtle, rgb(16 24 40 / 0.04));
  border-radius: var(--mc-radius-lg, 12px);
  box-shadow: var(--mc-shadow-lg);
  animation: fly-in 0.14s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes fly-in {
  from { opacity: 0; transform: translateX(-4px); }
  to { opacity: 1; transform: translateX(0); }
}

.fly-header {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  padding: 7px 10px 8px;
  margin-bottom: 2px;
  border: none;
  background: none;
  border-radius: 7px;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  color: var(--mc-text-primary, #101828);
  cursor: pointer;
  text-align: left;
  border-bottom: 1px solid var(--mc-divider-subtle, rgb(16 24 40 / 0.04));
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
  transition: color 0.12s;
}
.fly-header:hover { color: var(--mc-primary); }
.fly-header.is-active { color: var(--mc-primary); }
.fly-header-icon { flex-shrink: 0; opacity: 0.8; }
.fly-header-text { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.fly-body { padding-top: 2px; }
</style>
