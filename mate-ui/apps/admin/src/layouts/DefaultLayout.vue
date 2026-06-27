<template>
  <!-- Top-nav mode: header spans the full width on top, sidebar sits below it -->
  <div v-if="system.layoutMode === 'top'" class="layout layout--top">
    <layout-header />
    <div class="layout-row">
      <layout-sidebar />
      <div class="layout-body">
        <layout-tab-bar />
        <main class="layout-main">
          <div class="layout-content" :class="{ 'is-fullscreen': isFullscreen }">
            <router-view v-slot="{ Component }">
              <transition name="page" mode="out-in">
                <keep-alive :include="tabbar.cachedNames">
                  <component :is="Component" :key="route.fullPath" />
                </keep-alive>
              </transition>
            </router-view>
          </div>
        </main>
      </div>
    </div>
    <settings-drawer v-model="system.showSettings" />
  </div>

  <!-- Side mode (default): sidebar on the left, header inside the body -->
  <div v-else class="layout">
    <layout-sidebar />
    <div class="layout-body">
      <layout-header />
      <layout-tab-bar />
      <main class="layout-main">
        <div class="layout-content">
          <router-view v-slot="{ Component }">
            <transition name="page" mode="out-in">
              <keep-alive :include="tabbar.cachedNames">
                <component :is="Component" :key="route.fullPath" />
              </keep-alive>
            </transition>
          </router-view>
        </div>
      </main>
    </div>
    <settings-drawer v-model="system.showSettings" />
  </div>

  <!-- App launcher (⊞ all apps + ⌘K) — shared overlay, works in both modes -->
  <app-launcher />
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@matecloud/core'
import { useTabBarStore } from '@/stores/tabbar'
import { useSystemStore } from '@/stores/system'
import LayoutSidebar from './components/LayoutSidebar.vue'
import LayoutHeader from './components/LayoutHeader.vue'
import LayoutTabBar from './components/LayoutTabBar.vue'
import SettingsDrawer from './components/SettingsDrawer.vue'
import AppLauncher from './components/AppLauncher/AppLauncher.vue'

const route = useRoute()
const auth = useAuthStore()
const tabbar = useTabBarStore()
const system = useSystemStore()

// 全屏页面 (工作流画布等): 去掉内容区内边距, 让页面铺满
const isFullscreen = computed(() => route.meta?.fullscreen === true)

// Sync route changes to tab bar (keep-alive)
watch(route, (r) => tabbar.addTab(r), { immediate: true })

// Hydrate user info on first mount
onMounted(async () => {
  if (auth.isLoggedIn && !auth.user) {
    try { await auth.fetchUserInfo() } catch { /* interceptor handles 401 */ }
  }
})
</script>

<style scoped>
.layout {
  display: flex;
  width: 100%;
  height: 100vh;
  background: var(--mc-bg);
  color: var(--mc-text-primary);
  overflow: hidden;
}

/* Top-nav mode: column stack — full-width header, then sidebar+content row */
.layout--top { flex-direction: column; }
.layout--top .layout-row {
  flex: 1;
  display: flex;
  min-height: 0;
  overflow: hidden;
}

.layout-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 0;
  background: var(--mc-bg);
  overflow: hidden;
}

.layout-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 8px;     /* 紧凑外边距:外 8 + 内 8 = 16px(原 16+24=40px) */
}

/* 全屏页面 (工作流画布): 铺满内容区, 由页面自行管理边距 */
.layout-content.is-fullscreen { padding: 0; }

/* Page transition */
.page-enter-active { transition: opacity 0.2s ease-out, transform 0.2s ease-out; }
.page-leave-active { transition: opacity 0.15s ease-in, transform 0.15s ease-in; }
.page-enter-from  { opacity: 0; transform: translateY(8px); }
.page-leave-to    { opacity: 0; transform: translateY(-8px); }
</style>
