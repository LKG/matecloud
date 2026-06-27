<template>
  <header class="layout-header" :class="{ 'layout-header--top': system.layoutMode === 'top' }">
    <!-- Brand logo — top-left corner (top-nav mode only; side mode keeps it in the sidebar).
         When the sub-menu sidebar is visible, the logo zone matches its width so the
         divider line continues straight down the sidebar's right edge. -->
    <div
      v-if="system.layoutMode === 'top'"
      class="header-brand"
      :class="{ 'header-brand--aligned': asideVisible, 'header-brand--mini': asideVisible && system.menuCollapsed }"
      @click="router.push('/')"
    >
      <img src="@/assets/logo.svg" alt="MateCloud" class="header-logo" />
      <span class="header-brand-text">MateCloud</span>
    </div>

    <div class="header-left">
      <!-- Collapse toggle (also triggers mobile drawer on small screens).
           On phones it reads as a plain hamburger that opens the nav drawer. -->
      <button class="header-btn" aria-label="Toggle sidebar" @click="handleCollapse">
        <component :is="system.isMobile ? IconMenu : (system.menuCollapsed ? IconExpand : IconFold)" :size="18" />
      </button>

      <!-- All-apps launcher entry (top-nav mode) -->
      <button
        v-if="system.layoutMode === 'top'"
        class="header-apps-btn"
        :title="t('layout.allApps') + ' (⌘K)'"
        @click="system.showLauncher = true"
      >
        <span class="header-apps-icon" aria-hidden="true">
          <IconGrid :size="15" />
        </span>
        <span class="header-apps-text">{{ t('layout.allApps') }}</span>
        <kbd class="header-apps-kbd">⌘K</kbd>
      </button>

      <!-- Top-nav mode: level-1 menu lives here; otherwise show breadcrumb.
           On phones both are hidden — navigation is in the drawer. -->
      <template v-if="!system.isMobile">
        <layout-top-nav v-if="system.layoutMode === 'top'" class="header-topnav" />
        <el-breadcrumb v-else separator="/" class="header-breadcrumb">
          <el-breadcrumb-item :to="{ path: '/' }">{{ t('layout.dashboard') }}</el-breadcrumb-item>
          <el-breadcrumb-item v-for="item in breadcrumb" :key="item.path">
            {{ item.meta?.title ? t(item.meta.title as string) : '' }}
          </el-breadcrumb-item>
        </el-breadcrumb>
      </template>
    </div>

    <div class="header-right">
      <!-- Fullscreen -->
      <MateTooltip :content="isFullscreen ? t('layout.exitFullscreen') : t('layout.fullscreen')" :show-after="400">
        <button class="header-btn header-btn--desktop" :aria-label="isFullscreen ? 'Exit fullscreen' : 'Fullscreen'" @click="toggle">
          <component :is="isFullscreen ? IconMinimize : IconMaximize" :size="16" />
        </button>
      </MateTooltip>

      <!-- Dark / Light -->
      <MateTooltip :content="system.isDark ? t('layout.lightMode') : t('layout.darkMode')" :show-after="400">
        <button class="header-btn" :aria-label="system.isDark ? 'Switch to light mode' : 'Switch to dark mode'" @click="system.toggleDark()">
          <component :is="system.isDark ? IconSun : IconMoon" :size="16" />
        </button>
      </MateTooltip>

      <!-- Language -->
      <MateDropdown trigger="click" @command="changeLocale">
        <button class="header-btn" aria-label="Change language"><IconLanguages :size="16" /></button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="zh-CN">
              <span :class="{ 'font-semibold': locale === 'zh-CN' }">中文</span>
            </el-dropdown-item>
            <el-dropdown-item command="en-US">
              <span :class="{ 'font-semibold': locale === 'en-US' }">English</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </MateDropdown>

      <!-- Settings (layout/theme options — desktop only) -->
      <button class="header-btn header-btn--desktop" aria-label="Settings" @click="system.showSettings = true">
        <IconSettings :size="16" />
      </button>

      <!-- Divider -->
      <div class="header-divider" />

      <!-- User -->
      <MateDropdown trigger="click">
        <div class="user-trigger" role="button" aria-label="User menu" tabindex="0">
          <div class="user-avatar">{{ userInitial }}</div>
          <span class="user-name">{{ userLabel }}</span>
          <IconChevronDown :size="14" class="user-arrow" />
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="router.push('/profile')">
              <IconUser :size="14" class="mr-2" />{{ t('layout.personalCenter') }}
            </el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">
              <IconLogOut :size="14" class="mr-2" />{{ t('layout.logout') }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </MateDropdown>
    </div>

  </header>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useFullscreen } from '@vueuse/core'
import { MateMessage, MateMessageBox, MateTooltip, MateDropdown } from '@matecloud/ui'
import {
  PanelLeftClose as IconFold, PanelLeftOpen as IconExpand,
  Maximize as IconMaximize, Minimize as IconMinimize,
  Moon as IconMoon, Sun as IconSun,
  Languages as IconLanguages, Settings as IconSettings,
  User as IconUser, LogOut as IconLogOut, ChevronDown as IconChevronDown,
  LayoutGrid as IconGrid, Menu as IconMenu,
} from 'lucide-vue-next'
import { useAuthStore } from '@matecloud/core'
import { useSystemStore } from '@/stores/system'
import { useMenuNav } from '../composables/useMenuNav'
import { setLocale } from '@/i18n'
import { onLogout } from '@/router'
import LayoutTopNav from './LayoutTopNav.vue'

const { t, locale } = useI18n()
const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const system = useSystemStore()
const { activeTop } = useMenuNav()
const { isFullscreen, toggle } = useFullscreen()

// In top-nav mode the sub-menu sidebar only shows when the active group has
// children. The logo zone matches the sidebar width only when it's visible.
const asideVisible = computed(
  () => system.layoutMode === 'top' && !system.isMobile && (activeTop.value?.children?.length ?? 0) > 0,
)

const userLabel = computed(() => auth.user?.realName || auth.user?.username || 'Guest')
const userInitial = computed(() => userLabel.value.charAt(0).toUpperCase())

const breadcrumb = computed(() => {
  return route.matched
    .filter((item) => item.meta?.title && item.path !== '/')
})

function handleCollapse() {
  if (system.isMobile) {
    // Phone: the toggle is a hamburger that opens the off-canvas nav drawer.
    system.menuDrawer = true
    system.menuCollapsed = false
  } else {
    system.toggleMenuCollapse()
  }
}

function changeLocale(lang: string) {
  setLocale(lang as 'zh-CN' | 'en-US')
}

async function handleLogout() {
  try {
    await MateMessageBox.confirm(t('layout.logoutConfirm'), t('layout.logout'), { type: 'warning' })
  } catch { return }
  await auth.logout()
  onLogout() // reset dynamic routes
  MateMessage.success(t('common.success'))
  router.push('/login')
}
</script>

<style scoped>
.layout-header {
  position: relative;       /* z-index needs a positioned element */
  z-index: 10;              /* above el-main / el-scrollbar */
  height: var(--mc-header-height);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: var(--mc-header-bg);
  backdrop-filter: blur(12px) saturate(150%);
  -webkit-backdrop-filter: blur(12px) saturate(150%);
  border-bottom: 1px solid var(--mc-divider-subtle, rgb(16 24 40 / 0.04));
}

.header-left { display: flex; align-items: center; gap: 12px; min-width: 0; flex: 1; }
.header-right { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
.header-topnav { flex: 1; min-width: 0; }

/* All-apps launcher entry — the brand gateway to every app (⌘K).
   Theme-aware: everything keys off --mc-primary / --mc-primary-rgb, so it
   follows the runtime theme color the user picks. */
.header-apps-btn {
  display: inline-flex; align-items: center; gap: 8px;
  height: 34px; padding: 0 10px 0 6px;
  border: 1px solid rgba(var(--mc-primary-rgb), 0.22);
  background:
    linear-gradient(135deg, rgba(var(--mc-primary-rgb), 0.11), rgba(var(--mc-primary-rgb), 0.02));
  border-radius: 999px;
  font-size: 13px; font-weight: 600; font-family: inherit;
  color: var(--mc-primary); cursor: pointer; flex-shrink: 0;
  -webkit-backdrop-filter: blur(6px); backdrop-filter: blur(6px);
  transition: border-color 0.18s, background 0.18s, box-shadow 0.18s, transform 0.18s;
}
.header-apps-btn:hover {
  border-color: rgba(var(--mc-primary-rgb), 0.4);
  background:
    linear-gradient(135deg, rgba(var(--mc-primary-rgb), 0.18), rgba(var(--mc-primary-rgb), 0.05));
  box-shadow: 0 6px 16px -6px rgba(var(--mc-primary-rgb), 0.45);
  transform: translateY(-1px);
}
.header-apps-btn:active { transform: translateY(0); box-shadow: 0 2px 8px -4px rgba(var(--mc-primary-rgb), 0.4); }
.header-apps-btn:focus-visible { outline: 2px solid var(--mc-primary); outline-offset: 2px; }

/* Filled icon chip — reads as a little "app tile" */
.header-apps-icon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 22px; height: 22px; border-radius: 7px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.22), rgba(255, 255, 255, 0) 60%), var(--mc-primary);
  color: #fff; flex-shrink: 0;
  box-shadow: 0 2px 6px -1px rgba(var(--mc-primary-rgb), 0.5),
              inset 0 1px 0 rgba(255, 255, 255, 0.25);
  transition: transform 0.18s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.header-apps-btn:hover .header-apps-icon { transform: scale(1.08) rotate(-4deg); }

/* ⌘K affordance */
.header-apps-kbd {
  font-family: inherit; font-size: 11px; font-weight: 600; line-height: 1;
  padding: 3px 5px; border-radius: 5px; flex-shrink: 0;
  color: var(--mc-primary);
  background: rgba(var(--mc-primary-rgb), 0.10);
  border: 1px solid rgba(var(--mc-primary-rgb), 0.18);
}
@media (max-width: 1100px) { .header-apps-kbd { display: none; } }
@media (max-width: 768px) {
  .header-apps-text { display: none; }
  .header-apps-btn { padding: 0 6px; }
}

/* Brand logo — pinned to the top-left corner in top-nav mode */
.layout-header--top { padding-left: 0; }
.header-brand {
  display: flex; align-items: center; gap: 9px;
  height: 100%; flex-shrink: 0; cursor: pointer;
  padding: 0 18px; margin-right: 4px;
  box-sizing: border-box;
  white-space: nowrap; overflow: hidden;
  border-right: 1px solid var(--mc-divider-subtle, rgb(16 24 40 / 0.04));
  transition: width 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}
/* Match the sub-menu sidebar's OUTER width (content-box + 1px border) so the
   brand's right border lands exactly on the sidebar's right border. */
.header-brand--aligned { width: calc(var(--mc-subtree-width) + 1px); margin-right: 0; }
.header-brand--aligned.header-brand--mini {
  width: calc(var(--mc-aside-collapsed-width) + 1px);
  justify-content: center; padding: 0;
}
.header-brand--mini .header-brand-text { display: none; }
.header-logo { width: 30px; height: 30px; border-radius: 8px; flex-shrink: 0; }
.header-brand-text {
  font-size: 15px; font-weight: 700;
  color: var(--mc-text-primary); letter-spacing: -0.2px;
}

.header-btn {
  display: inline-flex; align-items: center; justify-content: center;
  width: 34px; height: 34px;
  border: none; background: transparent;
  border-radius: var(--mc-radius);
  cursor: pointer;
  color: var(--mc-text-secondary);
  transition: background 0.2s, color 0.2s;
}
.header-btn:hover {
  background: var(--mc-state-hover, rgb(200 206 218 / 0.2));
  color: var(--mc-primary);
}
.header-btn:focus-visible {
  outline: 2px solid var(--mc-primary);
  outline-offset: 2px;
}

.header-breadcrumb { font-size: 13px; }
.header-divider { width: 1px; height: 20px; background: var(--mc-border); margin: 0 6px; }

.user-trigger {
  display: flex; align-items: center; cursor: pointer;
  padding: 4px 8px 4px 4px;
  border-radius: 999px; gap: 8px;
  transition: background 0.2s;
}
.user-trigger:hover { background: var(--mc-fill); }

.user-avatar {
  width: 30px; height: 30px; border-radius: 50%;
  background: var(--mc-primary); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700;
  box-shadow: 0 2px 8px rgba(var(--mc-primary-rgb), 0.35);
}
.user-name {
  font-size: 13px; font-weight: 500;
  color: var(--mc-text-secondary);
  max-width: 100px; overflow: hidden;
  text-overflow: ellipsis; white-space: nowrap;
}
.user-arrow { color: var(--mc-text-muted); }

/* ---- Phone layout (≤768px) ----
   The bar condenses to: ☰ drawer toggle · brand logo · ⊞ launcher  |  lang ·
   dark · avatar. Top-nav / breadcrumb / fullscreen / settings are dropped
   (nav moves into the drawer; those controls are desktop-oriented). */
@media (max-width: 768px) {
  .layout-header { padding: 0 10px; }
  .header-left { gap: 6px; }
  .header-right { gap: 2px; }

  /* desktop-only affordances */
  .header-btn--desktop,
  .header-divider,
  .user-name,
  .user-arrow { display: none; }

  /* brand collapses to the logo only, no width-snapping to a (hidden) sidebar */
  .header-brand { padding: 0; margin: 0; border-right: none; }
  .header-brand-text { display: none; }

  /* roomier tap targets on touch */
  .header-btn { width: 40px; height: 40px; }
  .user-trigger { padding: 4px; }
}
</style>
