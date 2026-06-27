# RFC-035: Frontend Admin Layout, Router, Login & Dashboard

- **Status**: Draft
- **Created**: 2026-04-12
- **Author**: MateCloud Team
- **Wave**: FE-2 (parallel with RFC-033, RFC-034)

## Background

`mate-ui/apps/admin/` is the main admin SPA. This RFC covers the layout shell (sidebar + header + tab bar + main area), router with dynamic route generation from backend menus, login page (password + SMS), dashboard, permission directive, and error pages. It depends on `@matecloud/core` for stores/API and `@matecloud/ui` for shared components.

## Design

---

## Change 1: Layout — DefaultLayout.vue

Create `mate-ui/apps/admin/src/layouts/DefaultLayout.vue`

```vue
<template>
  <el-container class="layout-container">
    <!-- Sidebar -->
    <Sidebar />

    <el-container class="layout-main-container">
      <!-- Header -->
      <Header />

      <!-- Tab bar -->
      <TabBar />

      <!-- Main content area -->
      <el-main class="layout-main">
        <router-view v-slot="{ Component, route }">
          <keep-alive :include="cachedViews">
            <component :is="Component" :key="route.fullPath" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import Sidebar from './Sidebar.vue';
import Header from './Header.vue';
import TabBar from './TabBar.vue';
import { useTabStore } from '../stores/tab';

const tabStore = useTabStore();
const cachedViews = computed(() => tabStore.cachedViews);
</script>

<style scoped>
.layout-container {
  height: 100vh;
  overflow: hidden;
}

.layout-main-container {
  flex-direction: column;
  overflow: hidden;
}

.layout-main {
  padding: 16px;
  overflow-y: auto;
  background-color: var(--el-bg-color-page);
}
</style>
```

---

## Change 2: Layout — Sidebar.vue

Create `mate-ui/apps/admin/src/layouts/Sidebar.vue`

```vue
<template>
  <el-aside :width="appStore.sidebarCollapsed ? '64px' : '220px'" class="layout-sidebar">
    <!-- Logo -->
    <div class="sidebar-logo">
      <img src="/logo.svg" alt="logo" class="sidebar-logo__img" />
      <span v-show="!appStore.sidebarCollapsed" class="sidebar-logo__title">MateCloud</span>
    </div>

    <!-- Menu -->
    <el-scrollbar>
      <el-menu
        :default-active="activeMenu"
        :collapse="appStore.sidebarCollapsed"
        :unique-opened="true"
        router
        background-color="var(--el-menu-bg-color)"
        text-color="var(--el-menu-text-color)"
        active-text-color="var(--el-color-primary)"
      >
        <SidebarMenuItem
          v-for="menu in authStore.menuTree"
          :key="menu.id"
          :item="menu"
        />
      </el-menu>
    </el-scrollbar>
  </el-aside>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { useAuthStore, useAppStore } from '@matecloud/core';
import SidebarMenuItem from './SidebarMenuItem.vue';

const route = useRoute();
const authStore = useAuthStore();
const appStore = useAppStore();

const activeMenu = computed(() => route.path);
</script>

<style scoped>
.layout-sidebar {
  transition: width 0.3s;
  overflow: hidden;
  border-right: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
}

.sidebar-logo {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 16px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.sidebar-logo__img {
  width: 32px;
  height: 32px;
}

.sidebar-logo__title {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
}
</style>
```

---

## Change 3: Layout — SidebarMenuItem.vue (recursive)

Create `mate-ui/apps/admin/src/layouts/SidebarMenuItem.vue`

```vue
<template>
  <!-- Directory or parent menu with children -->
  <el-sub-menu
    v-if="item.children && item.children.length > 0"
    :index="item.path"
  >
    <template #title>
      <el-icon v-if="item.icon">
        <component :is="item.icon" />
      </el-icon>
      <span>{{ item.title }}</span>
    </template>
    <SidebarMenuItem
      v-for="child in item.children"
      :key="child.id"
      :item="child"
    />
  </el-sub-menu>

  <!-- Leaf menu item -->
  <el-menu-item v-else :index="item.path">
    <el-icon v-if="item.icon">
      <component :is="item.icon" />
    </el-icon>
    <template #title>
      <span>{{ item.title }}</span>
    </template>
  </el-menu-item>
</template>

<script setup lang="ts">
import type { MenuItem } from '@matecloud/core';

defineProps<{
  item: MenuItem;
}>();
</script>
```

---

## Change 4: Layout — Header.vue

Create `mate-ui/apps/admin/src/layouts/Header.vue`

```vue
<template>
  <el-header class="layout-header" height="50px">
    <div class="header-left">
      <!-- Hamburger toggle -->
      <el-icon class="header-icon" @click="appStore.toggleSidebar">
        <Fold v-if="!appStore.sidebarCollapsed" />
        <Expand v-else />
      </el-icon>

      <!-- Breadcrumb -->
      <el-breadcrumb separator="/">
        <el-breadcrumb-item
          v-for="item in breadcrumbs"
          :key="item.path"
          :to="item.path"
        >
          {{ item.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="header-right">
      <!-- Fullscreen toggle -->
      <el-tooltip content="Fullscreen">
        <el-icon class="header-icon" @click="toggleFullscreen">
          <FullScreen />
        </el-icon>
      </el-tooltip>

      <!-- Dark mode toggle -->
      <el-tooltip :content="appStore.darkMode ? 'Light Mode' : 'Dark Mode'">
        <el-icon class="header-icon" @click="appStore.toggleDarkMode">
          <Moon v-if="!appStore.darkMode" />
          <Sunny v-else />
        </el-icon>
      </el-tooltip>

      <!-- User dropdown -->
      <el-dropdown trigger="click" @command="handleUserCommand">
        <div class="header-user">
          <el-avatar :size="28" :src="authStore.user?.avatar" />
          <span class="header-user__name">{{ authStore.user?.nickName ?? 'User' }}</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">Profile</el-dropdown-item>
            <el-dropdown-item divided command="logout">Logout</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </el-header>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Fold, Expand, FullScreen, Moon, Sunny } from '@element-plus/icons-vue';
import { useAppStore, useAuthStore } from '@matecloud/core';

const route = useRoute();
const router = useRouter();
const appStore = useAppStore();
const authStore = useAuthStore();

interface BreadcrumbItem {
  path: string;
  title: string;
}

const breadcrumbs = computed<BreadcrumbItem[]>(() => {
  const matched = route.matched.filter((r) => r.meta?.title);
  return matched.map((r) => ({
    path: r.path,
    title: r.meta.title as string,
  }));
});

function toggleFullscreen(): void {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen();
  } else {
    document.exitFullscreen();
  }
}

async function handleUserCommand(command: string): Promise<void> {
  if (command === 'logout') {
    await authStore.logout();
    router.push('/login');
  } else if (command === 'profile') {
    router.push('/profile');
  }
}
</script>

<style scoped>
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--el-border-color-light);
  padding: 0 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  font-size: 18px;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: background 0.2s;
}

.header-icon:hover {
  background: var(--el-fill-color-light);
}

.header-user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}

.header-user:hover {
  background: var(--el-fill-color-light);
}

.header-user__name {
  font-size: 14px;
}
</style>
```

---

## Change 5: Layout — TabBar.vue

Create `mate-ui/apps/admin/src/layouts/TabBar.vue`

```vue
<template>
  <div class="tab-bar" v-if="tabStore.tabs.length > 0">
    <el-scrollbar>
      <div class="tab-bar__list">
        <div
          v-for="tab in tabStore.tabs"
          :key="tab.path"
          :class="['tab-bar__item', { 'is-active': tab.path === route.path }]"
          @click="router.push(tab.path)"
        >
          <span class="tab-bar__title">{{ tab.title }}</span>
          <el-icon
            v-if="tabStore.tabs.length > 1"
            class="tab-bar__close"
            @click.stop="handleClose(tab.path)"
          >
            <Close />
          </el-icon>
        </div>
      </div>
    </el-scrollbar>

    <!-- Context menu -->
    <el-dropdown trigger="click" @command="handleCommand">
      <el-icon class="tab-bar__more">
        <ArrowDown />
      </el-icon>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="close-others">Close Others</el-dropdown-item>
          <el-dropdown-item command="close-all">Close All</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup lang="ts">
import { watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Close, ArrowDown } from '@element-plus/icons-vue';
import { useTabStore } from '../stores/tab';

const route = useRoute();
const router = useRouter();
const tabStore = useTabStore();

// Auto-add tab on route change
watch(
  () => route.path,
  () => {
    if (route.meta?.title) {
      tabStore.addTab({
        path: route.path,
        title: route.meta.title as string,
        name: route.name as string,
      });
    }
  },
  { immediate: true },
);

function handleClose(path: string): void {
  tabStore.removeTab(path);
  // If closing current tab, navigate to last remaining tab
  if (path === route.path) {
    const lastTab = tabStore.tabs[tabStore.tabs.length - 1];
    if (lastTab) {
      router.push(lastTab.path);
    } else {
      router.push('/');
    }
  }
}

function handleCommand(command: string): void {
  if (command === 'close-others') {
    tabStore.closeOthers(route.path);
  } else if (command === 'close-all') {
    tabStore.closeAll();
    router.push('/');
  }
}
</script>

<style scoped>
.tab-bar {
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--el-border-color-light);
  padding: 0 8px;
  height: 36px;
  background: var(--el-bg-color);
}

.tab-bar__list {
  display: flex;
  gap: 4px;
  white-space: nowrap;
}

.tab-bar__item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  font-size: 12px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.tab-bar__item:hover {
  background: var(--el-fill-color-light);
}

.tab-bar__item.is-active {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.tab-bar__close {
  font-size: 12px;
  border-radius: 50%;
}

.tab-bar__close:hover {
  background: var(--el-fill-color);
}

.tab-bar__more {
  font-size: 16px;
  cursor: pointer;
  margin-left: 8px;
  flex-shrink: 0;
}
</style>
```

---

## Change 6: Tab Store (local to admin app)

Create `mate-ui/apps/admin/src/stores/tab.ts`

```typescript
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export interface TabItem {
  path: string;
  title: string;
  name: string;
}

export const useTabStore = defineStore('tab', () => {
  const tabs = ref<TabItem[]>([]);

  /** List of route names to keep alive */
  const cachedViews = computed(() => tabs.value.map((t) => t.name).filter(Boolean));

  function addTab(tab: TabItem): void {
    const exists = tabs.value.find((t) => t.path === tab.path);
    if (!exists) {
      tabs.value.push(tab);
    }
  }

  function removeTab(path: string): void {
    const index = tabs.value.findIndex((t) => t.path === path);
    if (index > -1) {
      tabs.value.splice(index, 1);
    }
  }

  function closeOthers(currentPath: string): void {
    tabs.value = tabs.value.filter((t) => t.path === currentPath);
  }

  function closeAll(): void {
    tabs.value = [];
  }

  return {
    tabs,
    cachedViews,
    addTab,
    removeTab,
    closeOthers,
    closeAll,
  };
});
```

---

## Change 7: Router — index.ts

Create `mate-ui/apps/admin/src/router/index.ts`

```typescript
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { setupRouterGuard } from './guard';

/**
 * Static routes that are always available.
 */
export const staticRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/Login.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('../views/error/404.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('../views/error/403.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    name: 'Root',
    redirect: '/dashboard',
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: staticRoutes,
  scrollBehavior: () => ({ top: 0 }),
});

// Setup navigation guard
setupRouterGuard(router);

export default router;
```

---

## Change 8: Router — guard.ts

Create `mate-ui/apps/admin/src/router/guard.ts`

```typescript
import type { Router } from 'vue-router';
import { useAuthStore } from '@matecloud/core';
import { adminApi } from '@matecloud/core';
import { generateDynamicRoutes } from './dynamic';

/** White-list paths that don't require auth */
const WHITE_LIST = ['/login', '/404', '/403'];

/** Track whether dynamic routes have been added in this session */
let dynamicRoutesAdded = false;

export function setupRouterGuard(router: Router): void {
  router.beforeEach(async (to, _from, next) => {
    const authStore = useAuthStore();

    // 1. Public pages — always accessible
    if (WHITE_LIST.includes(to.path)) {
      // If already logged in and going to login, redirect to home
      if (to.path === '/login' && authStore.isLoggedIn) {
        return next('/');
      }
      return next();
    }

    // 2. Not logged in — redirect to login
    if (!authStore.isLoggedIn) {
      return next(`/login?redirect=${encodeURIComponent(to.fullPath)}`);
    }

    // 3. First visit after login — fetch user info + menus + build dynamic routes
    if (!dynamicRoutesAdded) {
      try {
        // Fetch user info (includes permissions)
        await authStore.fetchUserInfo();

        // Fetch menu tree
        const menus = await adminApi.getMenuTree();
        authStore.setMenuTree(menus);

        // Generate and add dynamic routes
        const dynamicRoutes = generateDynamicRoutes(menus);
        dynamicRoutes.forEach((route) => {
          router.addRoute(route);
        });

        // Add catch-all 404 route AFTER dynamic routes
        router.addRoute({
          path: '/:pathMatch(.*)*',
          redirect: '/404',
        });

        dynamicRoutesAdded = true;

        // Re-navigate to the intended route (now that dynamic routes exist)
        return next({ ...to, replace: true });
      } catch (error) {
        // If fetching fails, logout and redirect to login
        await authStore.logout();
        dynamicRoutesAdded = false;
        return next(`/login?redirect=${encodeURIComponent(to.fullPath)}`);
      }
    }

    // 4. Normal navigation
    next();
  });
}

/**
 * Reset dynamic routes flag (called on logout).
 */
export function resetDynamicRoutes(): void {
  dynamicRoutesAdded = false;
}
```

---

## Change 9: Router — dynamic.ts

Create `mate-ui/apps/admin/src/router/dynamic.ts`

```typescript
import type { RouteRecordRaw } from 'vue-router';
import type { MenuItem } from '@matecloud/core';
import DefaultLayout from '../layouts/DefaultLayout.vue';

/**
 * Glob import all view components under src/views/.
 * Vite resolves these at build time for code splitting.
 */
const viewModules = import.meta.glob('../views/**/*.vue');

/**
 * Transform backend MenuItem[] into Vue Router RouteRecordRaw[].
 * - Top-level items (type=0, directories) become layout routes.
 * - Children (type=1, menus) become nested routes with lazy-loaded components.
 * - Button permissions (type=2) are skipped.
 */
export function generateDynamicRoutes(menus: MenuItem[]): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = [];

  for (const menu of menus) {
    // Skip button permissions
    if (menu.type === 2) continue;

    if (menu.children && menu.children.length > 0) {
      // Directory with children -> layout wrapper
      const layoutRoute: RouteRecordRaw = {
        path: menu.path,
        name: `Layout_${menu.id}`,
        component: DefaultLayout,
        redirect: menu.redirect || undefined,
        meta: {
          title: menu.title,
          icon: menu.icon,
        },
        children: buildChildRoutes(menu.children),
      };
      routes.push(layoutRoute);
    } else {
      // Single top-level menu -> also wrap in layout
      const route: RouteRecordRaw = {
        path: '/',
        component: DefaultLayout,
        children: [
          {
            path: menu.path,
            name: menu.name,
            component: resolveComponent(menu.component),
            meta: {
              title: menu.title,
              icon: menu.icon,
              keepAlive: menu.keepAlive,
              permission: menu.permission,
            },
          },
        ],
      };
      routes.push(route);
    }
  }

  return routes;
}

/**
 * Recursively build child routes from menu items.
 */
function buildChildRoutes(menus: MenuItem[]): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = [];

  for (const menu of menus) {
    if (menu.type === 2) continue; // skip buttons

    if (menu.children && menu.children.length > 0) {
      // Nested directory
      routes.push({
        path: menu.path,
        name: menu.name,
        redirect: menu.redirect || undefined,
        meta: {
          title: menu.title,
          icon: menu.icon,
        },
        children: buildChildRoutes(menu.children),
      });
    } else {
      // Leaf menu
      routes.push({
        path: menu.path,
        name: menu.name,
        component: resolveComponent(menu.component),
        meta: {
          title: menu.title,
          icon: menu.icon,
          keepAlive: menu.keepAlive,
          permission: menu.permission,
        },
      });
    }
  }

  return routes;
}

/**
 * Resolve a component path string to a lazy-loaded module.
 * Backend stores component as e.g. "system/user/index" which maps to
 * "../views/system/user/index.vue".
 */
function resolveComponent(component: string): () => Promise<any> {
  if (!component) {
    return () => import('../views/error/404.vue');
  }

  // Normalize: ensure no leading slash, add .vue extension
  const normalized = component.replace(/^\//, '');
  const path = `../views/${normalized}.vue`;

  if (viewModules[path]) {
    return viewModules[path] as () => Promise<any>;
  }

  // Fallback: try with /index.vue
  const indexPath = `../views/${normalized}/index.vue`;
  if (viewModules[indexPath]) {
    return viewModules[indexPath] as () => Promise<any>;
  }

  console.warn(`[Router] Component not found: ${component}, resolved paths: ${path}, ${indexPath}`);
  return () => import('../views/error/404.vue');
}
```

---

## Change 10: Login Page

Create `mate-ui/apps/admin/src/views/login/Login.vue`

```vue
<template>
  <div class="login-container">
    <div class="login-card">
      <!-- Logo -->
      <div class="login-header">
        <img src="/logo.svg" alt="logo" class="login-logo" />
        <h1 class="login-title">MateCloud</h1>
        <p class="login-subtitle">DDD Microservice Platform</p>
      </div>

      <!-- Login tabs -->
      <el-tabs v-model="activeTab" class="login-tabs">
        <!-- Password login -->
        <el-tab-pane label="Password" name="password">
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            size="large"
          >
            <el-form-item prop="username">
              <el-input
                v-model="passwordForm.username"
                placeholder="Username"
                prefix-icon="User"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="passwordForm.password"
                type="password"
                placeholder="Password"
                prefix-icon="Lock"
                show-password
                @keyup.enter="handlePasswordLogin"
              />
            </el-form-item>

            <el-form-item prop="captchaCode">
              <div class="captcha-row">
                <el-input
                  v-model="passwordForm.captchaCode"
                  placeholder="Captcha"
                  @keyup.enter="handlePasswordLogin"
                />
                <img
                  :src="captchaImage"
                  alt="captcha"
                  class="captcha-img"
                  @click="refreshCaptcha"
                />
              </div>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                :loading="loading"
                class="login-btn"
                @click="handlePasswordLogin"
              >
                Log In
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- SMS login -->
        <el-tab-pane label="SMS" name="sms">
          <el-form
            ref="smsFormRef"
            :model="smsForm"
            :rules="smsRules"
            size="large"
          >
            <el-form-item prop="mobile">
              <el-input
                v-model="smsForm.mobile"
                placeholder="Mobile number"
                prefix-icon="Phone"
              />
            </el-form-item>

            <el-form-item prop="smsCode">
              <div class="captcha-row">
                <el-input
                  v-model="smsForm.smsCode"
                  placeholder="SMS code"
                  @keyup.enter="handleSmsLogin"
                />
                <el-button
                  :disabled="countdown > 0"
                  type="primary"
                  class="sms-btn"
                  @click="handleSendSms"
                >
                  {{ countdown > 0 ? `${countdown}s` : 'Send Code' }}
                </el-button>
              </div>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                :loading="loading"
                class="login-btn"
                @click="handleSmsLogin"
              >
                Log In
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { useAuthStore, authApi } from '@matecloud/core';
import type { LoginCommand, SmsLoginCommand } from '@matecloud/core';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const activeTab = ref('password');
const loading = ref(false);

// ---------------------------------------------------------------------------
// Password form
// ---------------------------------------------------------------------------
const passwordFormRef = ref<FormInstance>();
const passwordForm = reactive<LoginCommand>({
  username: '',
  password: '',
  captchaKey: '',
  captchaCode: '',
});

const passwordRules: FormRules = {
  username: [{ required: true, message: 'Please enter username', trigger: 'blur' }],
  password: [{ required: true, message: 'Please enter password', trigger: 'blur' }],
  captchaCode: [{ required: true, message: 'Please enter captcha', trigger: 'blur' }],
};

// Captcha
const captchaImage = ref('');

async function refreshCaptcha(): Promise<void> {
  try {
    const res = await authApi.getCaptcha();
    captchaImage.value = res.captchaImage;
    passwordForm.captchaKey = res.captchaKey;
  } catch (e) {
    console.error('Failed to load captcha', e);
  }
}

async function handlePasswordLogin(): Promise<void> {
  const valid = await passwordFormRef.value?.validate().catch(() => false);
  if (!valid) return;

  loading.value = true;
  try {
    await authStore.login(passwordForm);
    const redirect = (route.query.redirect as string) || '/';
    router.push(redirect);
    ElMessage.success('Login successful');
  } catch (err: any) {
    ElMessage.error(err?.msg ?? 'Login failed');
    refreshCaptcha();
  } finally {
    loading.value = false;
  }
}

// ---------------------------------------------------------------------------
// SMS form
// ---------------------------------------------------------------------------
const smsFormRef = ref<FormInstance>();
const smsForm = reactive<SmsLoginCommand>({
  mobile: '',
  smsCode: '',
});

const smsRules: FormRules = {
  mobile: [
    { required: true, message: 'Please enter mobile number', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: 'Invalid mobile number', trigger: 'blur' },
  ],
  smsCode: [{ required: true, message: 'Please enter SMS code', trigger: 'blur' }],
};

const countdown = ref(0);
let countdownTimer: ReturnType<typeof setInterval> | null = null;

async function handleSendSms(): Promise<void> {
  // Validate mobile field only
  try {
    await smsFormRef.value?.validateField('mobile');
  } catch {
    return;
  }

  try {
    await authApi.sendSms(smsForm.mobile);
    ElMessage.success('SMS code sent');

    // Start 60s countdown
    countdown.value = 60;
    countdownTimer = setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0 && countdownTimer) {
        clearInterval(countdownTimer);
        countdownTimer = null;
      }
    }, 1000);
  } catch (err: any) {
    ElMessage.error(err?.msg ?? 'Failed to send SMS');
  }
}

async function handleSmsLogin(): Promise<void> {
  const valid = await smsFormRef.value?.validate().catch(() => false);
  if (!valid) return;

  loading.value = true;
  try {
    await authStore.smsLogin(smsForm);
    const redirect = (route.query.redirect as string) || '/';
    router.push(redirect);
    ElMessage.success('Login successful');
  } catch (err: any) {
    ElMessage.error(err?.msg ?? 'Login failed');
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  refreshCaptcha();
});
</script>

<style scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 420px;
  padding: 40px;
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.login-header {
  text-align: center;
  margin-bottom: 24px;
}

.login-logo {
  width: 64px;
  height: 64px;
  margin-bottom: 12px;
}

.login-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.login-subtitle {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  margin: 4px 0 0;
}

.login-tabs {
  margin-top: 8px;
}

.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.captcha-img {
  height: 40px;
  cursor: pointer;
  border-radius: 4px;
  border: 1px solid var(--el-border-color);
  flex-shrink: 0;
}

.sms-btn {
  flex-shrink: 0;
  width: 120px;
}

.login-btn {
  width: 100%;
}
</style>
```

---

## Change 11: Dashboard

Create `mate-ui/apps/admin/src/views/dashboard/Dashboard.vue`

```vue
<template>
  <div class="dashboard">
    <!-- Welcome card -->
    <el-card class="dashboard-welcome" shadow="never">
      <div class="welcome-content">
        <div>
          <h2 class="welcome-title">Welcome back, {{ authStore.user?.nickName ?? 'Admin' }}</h2>
          <p class="welcome-desc">MateCloud DDD Microservice Platform</p>
        </div>
      </div>
    </el-card>

    <!-- Stat cards -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6" v-for="stat in stats" :key="stat.title">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-card__content">
            <div class="stat-card__info">
              <span class="stat-card__title">{{ stat.title }}</span>
              <span class="stat-card__value">{{ stat.value }}</span>
            </div>
            <el-icon :size="48" :color="stat.color" class="stat-card__icon">
              <component :is="stat.icon" />
            </el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card shadow="never" header="Visits Trend">
          <v-chart :option="lineChartOption" autoresize style="height: 350px" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" header="Module Usage">
          <v-chart :option="barChartOption" autoresize style="height: 350px" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue';
import { User, ShoppingCart, Money, View } from '@element-plus/icons-vue';
import { useAuthStore } from '@matecloud/core';
import VChart from 'vue-echarts';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { LineChart, BarChart } from 'echarts/charts';
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
} from 'echarts/components';

// Register ECharts modules
use([
  CanvasRenderer,
  LineChart,
  BarChart,
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
]);

const authStore = useAuthStore();

// Stat cards data
const stats = reactive([
  { title: 'Users', value: '12,846', icon: User, color: '#409eff' },
  { title: 'Orders', value: '5,230', icon: ShoppingCart, color: '#67c23a' },
  { title: 'Revenue', value: '328,500', icon: Money, color: '#e6a23c' },
  { title: 'Visits', value: '98,120', icon: View, color: '#f56c6c' },
]);

// Line chart — visits trend
const lineChartOption = reactive({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
    boundaryGap: false,
  },
  yAxis: { type: 'value' },
  series: [
    {
      name: 'Visits',
      type: 'line',
      smooth: true,
      data: [820, 932, 901, 1234, 1290, 1330, 1520],
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.02)' },
          ],
        },
      },
      lineStyle: { color: '#409eff' },
      itemStyle: { color: '#409eff' },
    },
  ],
});

// Bar chart — module usage
const barChartOption = reactive({
  tooltip: { trigger: 'axis' },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    data: ['System', 'Admin', 'Auth', 'Notice', 'Gateway'],
  },
  yAxis: { type: 'value' },
  series: [
    {
      name: 'API Calls',
      type: 'bar',
      data: [18203, 23489, 29034, 10487, 42000],
      barWidth: '40%',
      itemStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: '#409eff' },
            { offset: 1, color: '#67c23a' },
          ],
        },
        borderRadius: [4, 4, 0, 0],
      },
    },
  ],
});
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dashboard-welcome {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
}

.dashboard-welcome :deep(.el-card__body) {
  padding: 24px 32px;
}

.welcome-title {
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 4px;
  color: #fff;
}

.welcome-desc {
  font-size: 14px;
  margin: 0;
  opacity: 0.85;
  color: #fff;
}

.stat-row {
  /* gap handled by el-row gutter */
}

.stat-card__content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-card__info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-card__title {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.stat-card__value {
  font-size: 28px;
  font-weight: 700;
}

.stat-card__icon {
  opacity: 0.6;
}

.chart-row {
  /* gap handled by el-row gutter */
}
</style>
```

---

## Change 12: Permission Directive

Create `mate-ui/apps/admin/src/directives/permission.ts`

```typescript
import type { App, Directive, DirectiveBinding } from 'vue';
import { useAuthStore } from '@matecloud/core';

/**
 * v-permission directive.
 *
 * Usage:
 *   <el-button v-permission="'system:user:create'">Create</el-button>
 *   <el-button v-permission="['system:user:update', 'system:user:delete']">Edit/Delete</el-button>
 *
 * The element is removed from the DOM if the user lacks the required permission(s).
 * For array values, the user needs at least ONE of the listed permissions.
 */
const permissionDirective: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    checkPermission(el, binding);
  },
  updated(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    checkPermission(el, binding);
  },
};

function checkPermission(el: HTMLElement, binding: DirectiveBinding<string | string[]>): void {
  const { value } = binding;
  if (!value) return;

  const authStore = useAuthStore();

  const permissions = Array.isArray(value) ? value : [value];
  const hasPermission = permissions.some((perm) => authStore.hasPermission(perm));

  if (!hasPermission) {
    // Remove the element from the DOM
    el.parentNode?.removeChild(el);
  }
}

/**
 * Register the v-permission directive on the app.
 */
export function setupPermissionDirective(app: App): void {
  app.directive('permission', permissionDirective);
}
```

---

## Change 13: Error Pages — 404

Create `mate-ui/apps/admin/src/views/error/404.vue`

```vue
<template>
  <div class="error-page">
    <div class="error-content">
      <h1 class="error-code">404</h1>
      <h2 class="error-title">Page Not Found</h2>
      <p class="error-desc">
        The page you are looking for does not exist or has been moved.
      </p>
      <el-button type="primary" @click="router.push('/')">Back to Home</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();
</script>

<style scoped>
.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
  text-align: center;
}

.error-code {
  font-size: 120px;
  font-weight: 700;
  color: var(--el-color-primary);
  margin: 0;
  line-height: 1;
}

.error-title {
  font-size: 24px;
  margin: 16px 0 8px;
}

.error-desc {
  color: var(--el-text-color-secondary);
  margin-bottom: 24px;
}
</style>
```

---

## Change 14: Error Pages — 403

Create `mate-ui/apps/admin/src/views/error/403.vue`

```vue
<template>
  <div class="error-page">
    <div class="error-content">
      <h1 class="error-code">403</h1>
      <h2 class="error-title">Access Forbidden</h2>
      <p class="error-desc">
        You do not have permission to access this page.
      </p>
      <el-button type="primary" @click="router.push('/')">Back to Home</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();
</script>

<style scoped>
.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
  text-align: center;
}

.error-code {
  font-size: 120px;
  font-weight: 700;
  color: var(--el-color-danger);
  margin: 0;
  line-height: 1;
}

.error-title {
  font-size: 24px;
  margin: 16px 0 8px;
}

.error-desc {
  color: var(--el-text-color-secondary);
  margin-bottom: 24px;
}
</style>
```

---

## Change 15: main.ts

Create `mate-ui/apps/admin/src/main.ts`

```typescript
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import 'element-plus/dist/index.css';
import 'virtual:uno.css';

import App from './App.vue';
import router from './router';
import { setupPermissionDirective } from './directives/permission';
import { BizError } from '@matecloud/core';

const app = createApp(App);

// Pinia
const pinia = createPinia();
app.use(pinia);

// Router
app.use(router);

// Element Plus
app.use(ElementPlus);

// Register all Element Plus icons globally
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

// Permission directive
setupPermissionDirective(app);

// Global error handler
app.config.errorHandler = (err, _instance, info) => {
  if (err instanceof BizError) {
    console.error(`[BizError] ${err.code}: ${err.msg}`);
  } else {
    console.error(`[Error] ${info}:`, err);
  }
};

app.mount('#app');
```

---

## Change 16: App.vue

Create `mate-ui/apps/admin/src/App.vue`

```vue
<template>
  <el-config-provider :locale="locale">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useAppStore } from '@matecloud/core';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import en from 'element-plus/es/locale/lang/en';

const appStore = useAppStore();

const localeMap: Record<string, any> = {
  'zh-CN': zhCn,
  'en': en,
};

const locale = computed(() => localeMap[appStore.locale] ?? zhCn);
</script>
```

---

## Change 17: Route meta type augmentation

Create `mate-ui/apps/admin/src/types/router.d.ts`

```typescript
import 'vue-router';

declare module 'vue-router' {
  interface RouteMeta {
    /** Page title displayed in breadcrumb and tab bar */
    title?: string;
    /** Icon name for sidebar */
    icon?: string;
    /** Whether this route requires auth (default true) */
    requiresAuth?: boolean;
    /** Whether to keep the component alive */
    keepAlive?: boolean;
    /** Permission code for this route */
    permission?: string;
  }
}
```
