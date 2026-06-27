/**
 * Shared menu navigation helpers for the top-nav layout mode (RFC-053 #11–14).
 *
 * Reuses the same `auth.menuTree` as the sidebar. Top-level groups render
 * horizontally in the header; the active group's children render in the left
 * sidebar. The "active group" is DERIVED from the current route (stateless) —
 * clicking a top item navigates to its first leaf, which drives the rest.
 */
import { computed, markRaw, type Component } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { FolderOpen } from 'lucide-vue-next'
import * as LucideIcons from 'lucide-vue-next'
import * as EpIcons from '@element-plus/icons-vue'
import { useAuthStore, type MenuItem as MenuItemType } from '@matecloud/core'

const iconCache = new Map<string, Component>()

/** Resolve a menu icon by name — Lucide first (project standard), EP fallback. */
export function resolveMenuIcon(name?: string): Component {
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

/** Does `node` (or any descendant) own `path`? */
export function matchesTree(node: MenuItemType, path: string): boolean {
  if (node.path === path) return true
  return node.children?.some((c) => matchesTree(c, path)) ?? false
}

/** First navigable leaf path under `node` (depth-first), or null. */
export function firstLeafPath(node: MenuItemType): string | null {
  if (!node.children?.length) return node.path || null
  for (const child of node.children) {
    const p = firstLeafPath(child)
    if (p) return p
  }
  return node.path || null
}

export function useMenuNav() {
  const route = useRoute()
  const auth = useAuthStore()
  const { locale } = useI18n()

  const tops = computed<MenuItemType[]>(() => auth.menuTree)

  /** Top-level group that owns the current route. */
  const activeTop = computed<MenuItemType | null>(
    () => tops.value.find((t) => matchesTree(t, route.path)) ?? null,
  )

  function menuLabel(item: MenuItemType): string {
    return locale.value === 'en-US' && item.nameEn ? item.nameEn : item.name
  }

  return { tops, activeTop, menuLabel }
}
