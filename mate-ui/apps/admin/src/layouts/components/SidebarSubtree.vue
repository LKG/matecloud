<template>
  <div class="subtree">
    <el-scrollbar class="subtree-scroll">
      <nav class="subtree-nav" :class="{ 'is-collapsed': collapsed }">
        <template v-for="item in children" :key="item.id">
          <!-- Level-2 leaf -->
          <router-link
            v-if="!item.children?.length"
            v-slot="{ isActive, navigate }"
            :to="item.path"
            custom
          >
            <button
              class="st-item"
              :class="{ 'st-item--active': isActive }"
              :title="collapsed ? menuLabel(item) : undefined"
              @click="navigate"
              @mouseenter="onItemEnter(item, $event)"
              @mouseleave="scheduleClose"
            >
              <component :is="resolveMenuIcon(item.icon)" :size="18" class="st-icon" />
              <span v-if="!collapsed" class="st-label">{{ menuLabel(item) }}</span>
            </button>
          </router-link>

          <!-- Level-2 group (has level-3 children) -->
          <div v-else class="st-group">
            <button
              class="st-item st-item--parent"
              :class="{
                'st-item--open': isOpen(item),
                'st-item--active': hasActiveChild(item) && (collapsed || !isOpen(item)),
              }"
              @click="onParentClick(item)"
              @mouseenter="onItemEnter(item, $event)"
              @mouseleave="scheduleClose"
            >
              <component :is="resolveMenuIcon(item.icon)" :size="18" class="st-icon" />
              <span v-if="!collapsed" class="st-label">{{ menuLabel(item) }}</span>
              <ChevronRight v-if="!collapsed" :size="14" class="st-arrow" />
            </button>

            <!-- Level-3 children (indented, with a connecting guide line) -->
            <div v-if="!collapsed && isOpen(item)" class="st-children">
              <router-link
                v-for="child in item.children"
                :key="child.id"
                v-slot="{ isActive, navigate }"
                :to="child.path"
                custom
              >
                <button
                  class="st-item st-item--child"
                  :class="{ 'st-item--active': isActive }"
                  @click="navigate"
                >
                  <span class="st-label">{{ menuLabel(child) }}</span>
                </button>
              </router-link>
            </div>
          </div>
        </template>
      </nav>
    </el-scrollbar>

    <!-- Collapsed rail: hovering a level-2 group reveals its level-2/3 list -->
    <Teleport to="body">
      <SidebarFlyout
        v-if="collapsed && flyItem"
        :item="flyItem"
        :top="flyTop"
        :left="flyLeft"
        @navigate="closeFlyout"
        @enter="cancelClose"
        @leave="scheduleClose"
      />
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChevronRight } from 'lucide-vue-next'
import type { MenuItem as MenuItemType } from '@matecloud/core'
import { useMenuNav, resolveMenuIcon, firstLeafPath } from '../composables/useMenuNav'
import SidebarFlyout from './SidebarFlyout.vue'

// `group` lets a caller (e.g. the side-mode split sidebar) drive the subtree
// from an explicitly-selected group instead of the route-derived active top.
const props = defineProps<{ collapsed?: boolean; group?: MenuItemType | null }>()

const route = useRoute()
const router = useRouter()
const { activeTop, menuLabel } = useMenuNav()

const children = computed<MenuItemType[]>(() => (props.group ?? activeTop.value)?.children ?? [])

/** Does the route currently live inside this node's subtree? */
function hasActiveChild(node: MenuItemType): boolean {
  return node.children?.some((c) => c.path === route.path || hasActiveChild(c)) ?? false
}

// Manually toggled groups, merged with auto-open (the group of the active route).
const manualOpen = ref<Set<string>>(new Set())
function isOpen(item: MenuItemType): boolean {
  if (manualOpen.value.has(item.id)) return true
  return hasActiveChild(item)
}
function onParentClick(item: MenuItemType) {
  if (props.collapsed) {
    // collapsed rail → jump straight to the first leaf
    const p = firstLeafPath(item)
    if (p && p !== route.path) router.push(p)
    return
  }
  const s = manualOpen.value
  if (s.has(item.id)) s.delete(item.id)
  else s.add(item.id)
}

// ---- Collapsed-rail flyout: reveal the level-2/3 list on hover ----
const flyItem = ref<MenuItemType | null>(null)
const flyTop = ref(0)
const flyLeft = ref(0)
let closeTimer: ReturnType<typeof setTimeout> | undefined

function onItemEnter(item: MenuItemType, e: MouseEvent) {
  if (!props.collapsed) return
  // Leaves rely on the native title tooltip — only groups get a flyout.
  if (!item.children?.length) { closeFlyout(); return }
  cancelClose()
  const r = (e.currentTarget as HTMLElement).getBoundingClientRect()
  flyLeft.value = r.right + 6
  // Align the panel top with the icon, clamped into the viewport.
  flyTop.value = Math.max(8, Math.min(r.top, window.innerHeight - 80))
  flyItem.value = item
}
function scheduleClose() {
  cancelClose()
  // Short grace period bridges the gap between the rail and the flyout.
  closeTimer = setTimeout(() => { flyItem.value = null }, 140)
}
function cancelClose() {
  if (closeTimer) { clearTimeout(closeTimer); closeTimer = undefined }
}
function closeFlyout() {
  cancelClose()
  flyItem.value = null
}
onBeforeUnmount(cancelClose)
</script>

<style scoped>
.subtree { display: flex; flex-direction: column; height: 100%; }
.subtree-scroll { flex: 1; }
.subtree-nav { padding: 8px; }

/* ---- Item (shared by level-2 and level-3) ---- */
.st-item {
  display: flex;
  align-items: center;
  width: 100%;
  height: 38px;
  padding: 0 12px;
  gap: 9px;
  border: none;
  background: none;
  border-radius: 8px;
  font-family: inherit;
  font-size: 13px;
  font-weight: 500;
  color: var(--mc-menu-text, #495464);
  cursor: pointer;
  text-align: left;
  margin-bottom: 1px;
  transition: background 0.12s, color 0.12s;
}
.st-item:hover {
  background: var(--mc-menu-bg-hover, rgb(200 206 218 / 0.2));
  color: var(--mc-menu-text-hover, #354052);
}
.st-item--active {
  background: var(--mc-menu-bg-active, rgb(21 90 239 / 0.08)) !important;
  color: var(--mc-primary) !important;
  font-weight: 600;
}
.st-icon { flex-shrink: 0; opacity: 0.65; }
.st-item:hover .st-icon { opacity: 0.85; }
.st-item--active .st-icon { opacity: 1; color: var(--mc-primary); }
.st-label { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* ---- Level-2 parent (collapsible) ---- */
.st-arrow { flex-shrink: 0; color: var(--mc-text-disabled); transition: transform 0.2s; }
.st-item--open .st-arrow { transform: rotate(90deg); }

/* ---- Level-3 children: indented with a connecting guide line ---- */
.st-children { position: relative; margin: 2px 0 4px; padding-left: 26px; }
.st-children::before {
  content: '';
  position: absolute;
  left: 19px;
  top: 2px;
  bottom: 8px;
  width: 1px;
  background: var(--mc-border, #eaecf0);
}
.st-item--child { height: 34px; padding-left: 12px; }

/* ---- Collapsed rail: center icons, hide labels/arrows ---- */
.subtree-nav.is-collapsed .st-item {
  justify-content: center;
  padding: 0;
  gap: 0;
}
</style>
