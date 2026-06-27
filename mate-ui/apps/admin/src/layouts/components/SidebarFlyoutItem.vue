<template>
  <!-- Leaf: navigates on click -->
  <router-link
    v-if="!item.children?.length"
    v-slot="{ isActive, navigate }"
    :to="item.path"
    custom
  >
    <button
      class="fly-item"
      :class="{ 'fly-item--active': isActive }"
      :style="indent"
      @click="(e: MouseEvent) => { navigate(e); emit('navigate') }"
    >
      <component :is="resolveMenuIcon(item.icon)" :size="16" class="fly-icon" />
      <span class="fly-label">{{ menuLabel(item) }}</span>
    </button>
  </router-link>

  <!-- Group: a header row that toggles its children open/closed -->
  <div v-else class="fly-group">
    <button
      class="fly-item fly-item--group"
      :class="{ 'fly-item--active': active }"
      :style="indent"
      @click="open = !open"
    >
      <component :is="resolveMenuIcon(item.icon)" :size="16" class="fly-icon" />
      <span class="fly-label">{{ menuLabel(item) }}</span>
      <ChevronRight :size="13" class="fly-arrow" :class="{ 'is-open': open }" />
    </button>
    <div v-show="open" class="fly-children">
      <SidebarFlyoutItem
        v-for="child in item.children"
        :key="child.id"
        :item="child"
        :depth="depth + 1"
        @navigate="emit('navigate')"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ChevronRight } from 'lucide-vue-next'
import type { MenuItem as MenuItemType } from '@matecloud/core'
import { useMenuNav, resolveMenuIcon, matchesTree } from '../composables/useMenuNav'

// Named so the component can recurse on itself for level-3+ nesting.
defineOptions({ name: 'SidebarFlyoutItem' })

const props = withDefaults(
  defineProps<{ item: MenuItemType; depth?: number }>(),
  { depth: 0 },
)
const emit = defineEmits<{ navigate: [] }>()

const route = useRoute()
const { menuLabel } = useMenuNav()

/** Highlight a group whose subtree owns the current route. */
const active = computed(() => matchesTree(props.item, route.path))

// Auto-expand the branch that owns the current route; collapsed otherwise.
const open = ref(active.value)

// Indent deeper levels so the hierarchy reads at a glance.
const indent = computed(() => ({ paddingLeft: `${12 + props.depth * 13}px` }))
</script>

<style scoped>
.fly-item {
  display: flex;
  align-items: center;
  width: 100%;
  height: 34px;
  padding: 0 12px;
  gap: 9px;
  border: none;
  background: none;
  border-radius: 7px;
  font-family: inherit;
  font-size: 13px;
  font-weight: 500;
  color: var(--mc-menu-text, #495464);
  cursor: pointer;
  text-align: left;
  transition: background 0.12s, color 0.12s;
}
.fly-item:hover {
  background: var(--mc-menu-bg-hover, rgb(200 206 218 / 0.2));
  color: var(--mc-menu-text-hover, #354052);
}
.fly-item--active {
  background: var(--mc-menu-bg-active, rgb(21 90 239 / 0.08));
  color: var(--mc-primary);
  font-weight: 600;
}
.fly-icon { flex-shrink: 0; opacity: 0.65; }
.fly-item:hover .fly-icon { opacity: 0.85; }
.fly-item--active .fly-icon { opacity: 1; color: var(--mc-primary); }
.fly-label { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.fly-arrow {
  flex-shrink: 0;
  color: var(--mc-text-disabled);
  transition: transform 0.2s;
}
.fly-arrow.is-open { transform: rotate(90deg); }

.fly-children { margin: 1px 0 2px; }
</style>
