import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { setupDynamicRoutes, resetDynamicRoutes } from './dynamic'

/** Static routes — always registered */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/Register.vue'),
    meta: { requiresAuth: false },
  },
  {
    // Dev-only base component gallery (RFC-053 #1) — visual docs + verification.
    // Public route so it can be opened without login.
    path: '/dev/components',
    name: 'ComponentsGallery',
    component: () => import('@/views/dev/ComponentsGallery.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layouts/DefaultLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: 'Dashboard' },
      },
      // Static pages that have real Vue components.
      //
      // Tab bar + keep-alive notes:
      // - `name` is unique per-route so the tab bar tracks each tab distinctly
      //   (two tabs pointing to the same view / same component are still valid).
      // - `meta.cache` is the COMPONENT name as declared via
      //   {@code defineOptions({ name })} inside the view. This string is the
      //   one <keep-alive :include> matches on.
      //
      // System pages
      { path: 'system/user', name: 'UserList', component: () => import('@/views/system/UserList.vue'), meta: { title: 'menu.user', cache: 'UserListView' } },
      { path: 'system/users', name: 'UserListAlias', component: () => import('@/views/system/UserList.vue'), meta: { title: 'menu.user', cache: 'UserListView' } },
      { path: 'system/role', name: 'RoleList', component: () => import('@/views/admin/RoleList.vue'), meta: { title: 'menu.role', cache: 'RoleListView' } },
      { path: 'system/roles', name: 'RoleListAlias', component: () => import('@/views/admin/RoleList.vue'), meta: { title: 'menu.role', cache: 'RoleListView' } },
      { path: 'system/menu', name: 'MenuTree', component: () => import('@/views/admin/MenuTree.vue'), meta: { title: 'menu.menu', cache: 'MenuTreeView' } },
      { path: 'system/menus', name: 'MenuTreeAlias', component: () => import('@/views/admin/MenuTree.vue'), meta: { title: 'menu.menu', cache: 'MenuTreeView' } },
      { path: 'system/dict', name: 'DictManager', component: () => import('@/views/admin/DictManager.vue'), meta: { title: 'menu.dict', cache: 'DictManagerView' } },
      { path: 'system/config', name: 'ConfigList', component: () => import('@/views/system/ConfigList.vue'), meta: { title: 'menu.config', cache: 'ConfigListView' } },
      { path: 'system/channel', name: 'ChannelConfig', component: () => import('@/views/system/ChannelConfig.vue'), meta: { title: 'menu.channelConfig', cache: 'ChannelConfigView' } },
      { path: 'system/identity', name: 'IdentityConfig', component: () => import('@/views/system/IdentityConfig.vue'), meta: { title: 'menu.identityConfig', cache: 'IdentityConfigView' } },
      { path: 'system/model-config', name: 'ModelConfig', component: () => import('@/views/system/ModelConfig.vue'), meta: { title: 'menu.modelConfig', cache: 'ModelConfigView' } },
      { path: 'system/material', name: 'Material', component: () => import('@/views/system/Material.vue'), meta: { title: 'menu.material', cache: 'MaterialView' } },
      { path: 'admin/admins', name: 'AdminList', component: () => import('@/views/admin/AdminList.vue'), meta: { title: 'menu.admin', cache: 'AdminListView' } },
      { path: 'admin/roles', name: 'RoleListAdmin', component: () => import('@/views/admin/RoleList.vue'), meta: { title: 'menu.role', cache: 'RoleListView' } },
      { path: 'admin/menus', name: 'MenuTreeAdmin', component: () => import('@/views/admin/MenuTree.vue'), meta: { title: 'menu.menu', cache: 'MenuTreeView' } },
      { path: 'admin/dict', name: 'DictManagerAdmin', component: () => import('@/views/admin/DictManager.vue'), meta: { title: 'menu.dict', cache: 'DictManagerView' } },
      // Monitor
      { path: 'monitor/log', name: 'OperationLog', component: () => import('@/views/monitor/OperationLog.vue'), meta: { title: 'menu.log', cache: 'OperationLogView' } },
      { path: 'monitor/operation-log', name: 'OperationLogAlias', component: () => import('@/views/monitor/OperationLog.vue'), meta: { title: 'menu.log', cache: 'OperationLogView' } },
      { path: 'monitor/login-log', name: 'LoginLog', component: () => import('@/views/monitor/LoginLog.vue'), meta: { title: 'menu.log', cache: 'LoginLogView' } },
      { path: 'monitor/online', name: 'OnlineUsers', component: () => import('@/views/monitor/OnlineUsers.vue'), meta: { title: 'menu.online', cache: 'OnlineUsersView' } },
      { path: 'monitor/server/status', name: 'ServerStatus', component: () => import('@/views/monitor/server/ServerStatus.vue'), meta: { title: 'menu.serverStatus', cache: 'ServerStatusView' } },
      { path: 'monitor/server/cache', name: 'ServerCache', component: () => import('@/views/monitor/server/ServerCache.vue'), meta: { title: 'menu.serverCache', cache: 'ServerCacheView' } },
      { path: 'monitor/server/docs', name: 'ApiDocs', component: () => import('@/views/monitor/server/ApiDocs.vue'), meta: { title: 'menu.apiDocs', cache: 'ApiDocsView' } },
      // Tools
      { path: 'tools/codegen', name: 'CodeGen', component: () => import('@/views/tools/CodeGen.vue'), meta: { title: 'menu.codeGen', cache: 'CodeGenView' } },
      { path: 'tools/storage', name: 'Storage', component: () => import('@/views/tools/Storage.vue'), meta: { title: 'menu.storage', cache: 'StorageView' } },
      { path: 'tools/tasks/jobs', name: 'JobList', component: () => import('@/views/tools/tasks/JobList.vue'), meta: { title: 'menu.jobList', cache: 'JobListView' } },
      { path: 'tools/tasks/logs', name: 'JobLogs', component: () => import('@/views/tools/tasks/JobLogs.vue'), meta: { title: 'menu.jobLogs', cache: 'JobLogsView' } },
      // AI assistant suite (mate-ai)
      { path: 'ai/chat',    name: 'AiChat',    component: () => import('@/views/ai/ChatView.vue'),    meta: { title: 'menu.aiChat',    cache: 'AiChatView' } },
      { path: 'ai/agents',  name: 'AiAgents',  component: () => import('@/views/ai/AgentsView.vue'),  meta: { title: 'menu.aiAgents',  cache: 'AiAgentsView' } },
      { path: 'ai/library', name: 'AiLibrary', component: () => import('@/views/ai/LibraryView.vue'), meta: { title: 'menu.aiLibrary', cache: 'AiLibraryView' } },
      { path: 'ai/mcp',     name: 'AiMcp',     component: () => import('@/views/ai/McpView.vue'),     meta: { title: 'menu.aiMcp',     cache: 'AiMcpView' } },
      { path: 'ai/models',  name: 'AiModels',  component: () => import('@/views/ai/ModelsView.vue'),  meta: { title: 'menu.aiModels',  cache: 'AiModelsView' } },
      { path: 'ai/tools',   name: 'AiTools',   component: () => import('@/views/ai/ToolsView.vue'),   meta: { title: 'menu.aiTools',   cache: 'AiToolsView' } },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/Profile.vue'),
        meta: { title: 'layout.personalCenter' },
      },
    ],
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

/**
 * Navigation guard:
 * 1. Public pages (/login, /register) — allow; redirect to / if already logged in
 * 2. Protected pages — require token; redirect to /login if missing
 * 3. After login, dynamically register menu-based routes once
 */
/**
 * Guards a once-per-page-load background revalidation of the cached identity.
 * Reset implicitly on every full reload (module re-evaluates).
 */
let revalidatedThisLoad = false

router.beforeEach(async (to) => {
  const token = localStorage.getItem('mate_token')

  // ---- Public routes ----
  if (to.meta.requiresAuth === false) {
    return token && (to.name === 'Login' || to.name === 'Register')
      ? { path: '/' }
      : true
  }

  // ---- No token → login ----
  if (!token) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }

  // ---- Dynamic route registration (once per session) ----
  // Lazy-import auth store to avoid circular dependency (store needs router for logout)
  const { useAuthStore } = await import('@matecloud/core')
  const auth = useAuthStore()

  // If user info not loaded yet (no cached identity for this token), fetch it.
  // NOTE: after a normal refresh the auth store rehydrates user/menu from its
  // localStorage cache, so this branch is skipped — the session survives even
  // when the backend is momentarily slow/unreachable on reload.
  if (!auth.user) {
    try {
      await auth.fetchUserInfo()
    } catch (e: any) {
      // Only a GENUINE auth rejection ends the session. A transient network /
      // 5xx error must NOT log the user out — otherwise a reload during a
      // backend hiccup bounces them to the login screen. The response
      // interceptor already strips the token on a real 401, so we detect that
      // (explicit 401 code, or token now gone) and only then redirect.
      const tokenGone = !localStorage.getItem('mate_token')
      if (e?.code === '401' || tokenGone) {
        auth.clearSession()
        return { name: 'Login', query: { redirect: to.fullPath } }
      }
      // Degraded (backend unreachable) but still authenticated: let navigation
      // proceed; individual pages surface their own load errors and the global
      // 401 interceptor still catches a real expiry on the next call.
    }
  } else if (!revalidatedThisLoad) {
    // Served identity from cache (fast path on refresh). Revalidate ONCE in the
    // background so a token that expired while away is detected promptly — a
    // real 401 here triggers the global interceptor → logout. Non-blocking, so
    // navigation is never delayed and transient errors are ignored.
    revalidatedThisLoad = true
    void auth.fetchUserInfo().catch(() => { /* interceptor handles real 401 */ })
  }

  // Register dynamic routes from menu tree
  if (auth.menuTree.length) {
    setupDynamicRoutes(router, auth.menuTree)

    // If the current target was a dynamic route that just got registered,
    // re-navigate to resolve it (otherwise it hits the 404 catch-all).
    // Preserve query + hash explicitly — putting them inside `path` drops them,
    // which broke deep-links with query params on a hard refresh.
    if (to.name === 'NotFound' && to.fullPath !== '/404') {
      return { path: to.path, query: to.query, hash: to.hash, replace: true }
    }
  }

  // ---- Route-level permission check ----
  // Routes may declare a `perms` key in meta (set from backend menu.perms by dynamic.ts).
  // If the current user lacks it, redirect to /403 instead of rendering the page.
  const requiredPerm = to.meta?.perms as string | undefined
  if (requiredPerm && !auth.hasPermission(requiredPerm)) {
    return { name: 'Forbidden' }
  }

  return true
})

/** Call on logout to reset dynamic route state */
export function onLogout() {
  resetDynamicRoutes()
}

export default router
