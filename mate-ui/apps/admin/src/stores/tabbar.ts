/**
 * TabBar store — manages open page tabs with keep-alive.
 * Adapted from reference admin template's tabbar store.
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { RouteLocationNormalized } from 'vue-router'

export interface TabItem {
  name: string        // route name (unique key)
  path: string
  title: string
  query: Record<string, any>
  /**
   * Component name to feed to {@code <keep-alive :include>}. Derived from
   * {@code route.meta.cache}. When present, the view stays mounted as the
   * user switches tabs.
   */
  cache?: string
}

export const useTabBarStore = defineStore('tabbar', () => {
  const tabs = ref<Map<string, TabItem>>(new Map())

  const tabList = computed(() => Array.from(tabs.value.values()))
  const tabCount = computed(() => tabs.value.size)
  /**
   * Component names currently eligible for keep-alive.
   *
   * Note: {@code <keep-alive :include>} matches against the component's own
   * {@code name} option (declared via {@code defineOptions({ name })}), not
   * against the route name — which is why we track a separate {@code cache}
   * field per tab rather than re-using {@code route.name}.
   */
  const cachedNames = computed(() =>
    tabList.value
      .map((t) => t.cache)
      .filter((n): n is string => !!n),
  )

  /** Add or update a tab from current route */
  function addTab(route: RouteLocationNormalized) {
    if (!route.name || !route.meta?.title) return // skip unnamed or untitled routes
    const key = route.name as string
    tabs.value.set(key, {
      name: key,
      path: route.path,
      title: route.meta.title as string,
      query: { ...route.query },
      cache: route.meta?.cache as string | undefined,
    })
  }

  function removeTab(name: string) {
    tabs.value.delete(name)
  }

  function closeLeft(name: string) {
    const keys = Array.from(tabs.value.keys())
    const idx = keys.indexOf(name)
    if (idx <= 0) return
    for (let i = 0; i < idx; i++) tabs.value.delete(keys[i])
  }

  function closeRight(name: string) {
    const keys = Array.from(tabs.value.keys())
    const idx = keys.indexOf(name)
    if (idx < 0 || idx >= keys.length - 1) return
    for (let i = idx + 1; i < keys.length; i++) tabs.value.delete(keys[i])
  }

  function closeOthers(name: string) {
    const keep = tabs.value.get(name)
    tabs.value.clear()
    if (keep) tabs.value.set(name, keep)
  }

  function clearAll() {
    tabs.value.clear()
  }

  return {
    tabs,
    tabList,
    tabCount,
    cachedNames,
    addTab,
    removeTab,
    closeLeft,
    closeRight,
    closeOthers,
    clearAll,
  }
})
