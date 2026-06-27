/**
 * System store — manages layout settings, theme, dark mode.
 * Adapted from reference admin template's system store.
 */
import { defineStore } from 'pinia'
import { useDark, useToggle, useMediaQuery } from '@vueuse/core'
import { ref, computed, watch } from 'vue'

export const useSystemStore = defineStore('system', () => {
  // ---- Responsive ----
  // Single source of truth for "are we on a phone-sized viewport". The shell
  // (sidebar → drawer, condensed header, hidden top-nav) keys off this so the
  // breakpoint lives in one place. Matches the CSS @media (max-width: 768px).
  const isMobile = useMediaQuery('(max-width: 768px)')

  // ---- Sidebar ----
  const menuCollapsed = ref(false)
  const menuDrawer = ref(false) // mobile drawer

  function toggleMenuCollapse(value?: boolean) {
    menuCollapsed.value = value ?? !menuCollapsed.value
  }

  // Leaving mobile (e.g. rotate / resize to desktop) must close the off-canvas
  // drawer, otherwise it lingers as a floating panel on the wide layout.
  watch(isMobile, (mobile) => { if (!mobile) menuDrawer.value = false })

  // ---- Dark mode (managed by @vueuse/core, toggles html class="dark") ----
  // storageKey controls the localStorage key, stored values are 'dark' or 'auto'.
  // valueDark/valueLight default to 'dark'/'' which correctly toggles <html class="dark">.
  const isDark = useDark({ storageKey: 'mate_dark' })
  const toggleDark = useToggle(isDark)

  // ---- Theme color ----
  const themeColor = ref(localStorage.getItem('mate_theme') || '#155AEF')

  function setThemeColor(color: string) {
    themeColor.value = color
    localStorage.setItem('mate_theme', color)
    applyThemeColor(color)
  }

  function applyThemeColor(color: string) {
    const el = document.documentElement
    const s = (k: string, v: string) => el.style.setProperty(k, v)
    s('--mc-primary', color)
    s('--el-color-primary', color)
    const rgb = hexToRgb(color)
    if (!rgb) return
    s('--mc-primary-rgb', `${rgb.r}, ${rgb.g}, ${rgb.b}`)
    // Menu active/hover tints must follow the chosen theme — otherwise they stay
    // on the default blue (hardcoded in tokens.css) and clash with a green/red/…
    // theme (e.g. green active text on a blue pill in dark mode).
    s('--mc-menu-bg-active', `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, ${isDark.value ? 0.16 : 0.10})`)
    const mix = isDark.value ? shade : tint
    for (const [k, a] of [[3, 0.3], [5, 0.5], [7, 0.7], [8, 0.8], [9, 0.9]] as const) {
      s(`--el-color-primary-light-${k}`, mix(rgb, a))
    }
    s('--el-color-primary-dark-2', shade(rgb, 0.2))
  }

  // ---- Tab bar visibility ----
  const showTabs = ref(localStorage.getItem('mate_tabs') !== '0')
  function setShowTabs(val: boolean) {
    showTabs.value = val
    localStorage.setItem('mate_tabs', val ? '1' : '0')
  }

  // ---- Sidebar style ----
  // default/dark/rounded = single-column; column = (light) two-column.
  // All share the same menuTree. ('split' = the removed dark two-column —
  // migrated to 'column' on load so it never instantiates.)
  type SidebarStyle = 'default' | 'dark' | 'rounded' | 'split' | 'column'
  const storedStyle = localStorage.getItem('mate_sidebar_style')
  const sidebarStyle = ref<SidebarStyle>(
    (storedStyle === 'split' ? 'column' : (storedStyle as SidebarStyle)) || 'rounded',
  )
  if (storedStyle === 'split') localStorage.setItem('mate_sidebar_style', 'column')
  function setSidebarStyle(style: SidebarStyle) {
    sidebarStyle.value = style
    localStorage.setItem('mate_sidebar_style', style)
  }

  // ---- Layout mode (RFC-053 #11) ----
  // 'side' = traditional full sidebar (level-1/2/3 all on the left)
  // 'top'  = level-1 horizontal in the header, level-2/3 in the left sidebar
  type LayoutMode = 'side' | 'top'
  const layoutMode = ref<LayoutMode>(
    (localStorage.getItem('mate_layout_mode') as LayoutMode) || 'top',
  )
  function setLayoutMode(mode: LayoutMode) {
    layoutMode.value = mode
    localStorage.setItem('mate_layout_mode', mode)
  }

  // ---- Settings drawer ----
  const showSettings = ref(false)

  // ---- App launcher (RFC-053 #12) — "all apps" mega panel + ⌘K ----
  const showLauncher = ref(false)
  function readPinned(): string[] {
    try { return JSON.parse(localStorage.getItem('mate_pinned_tops') || '[]') } catch { return [] }
  }
  // Top-level menu ids pinned to the front of the top-nav bar. Empty = natural order.
  const pinnedTops = ref<string[]>(readPinned())
  function togglePinnedTop(id: string) {
    const i = pinnedTops.value.indexOf(id)
    if (i >= 0) pinnedTops.value.splice(i, 1)
    else pinnedTops.value.push(id)
    localStorage.setItem('mate_pinned_tops', JSON.stringify(pinnedTops.value))
  }
  function isPinned(id: string) {
    return pinnedTops.value.includes(id)
  }

  // ---- Language ----
  const lang = ref(localStorage.getItem('mate_locale') || 'zh-CN')

  // Apply theme on init
  applyThemeColor(themeColor.value)

  // Re-apply when dark mode changes (need different shade/tint direction)
  watch(isDark, () => applyThemeColor(themeColor.value))

  return {
    isMobile,
    menuCollapsed,
    menuDrawer,
    toggleMenuCollapse,
    isDark,
    toggleDark,
    themeColor,
    setThemeColor,
    showTabs,
    setShowTabs,
    sidebarStyle,
    setSidebarStyle,
    layoutMode,
    setLayoutMode,
    showSettings,
    showLauncher,
    pinnedTops,
    togglePinnedTop,
    isPinned,
    lang,
  }
})

// ---- Color helpers ----
function hexToRgb(hex: string) {
  const m = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  return m ? { r: parseInt(m[1], 16), g: parseInt(m[2], 16), b: parseInt(m[3], 16) } : null
}

function tint(rgb: { r: number; g: number; b: number }, amount: number) {
  const r = Math.round(rgb.r + (255 - rgb.r) * amount)
  const g = Math.round(rgb.g + (255 - rgb.g) * amount)
  const b = Math.round(rgb.b + (255 - rgb.b) * amount)
  return `rgb(${r}, ${g}, ${b})`
}

function shade(rgb: { r: number; g: number; b: number }, amount: number) {
  const r = Math.round(rgb.r * (1 - amount))
  const g = Math.round(rgb.g * (1 - amount))
  const b = Math.round(rgb.b * (1 - amount))
  return `rgb(${r}, ${g}, ${b})`
}
