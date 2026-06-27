/**
 * Dynamic route generation — converts menu tree from backend into Vue Router routes.
 *
 * Strategy:
 * - If a menu item's path matches a known view component, use that component.
 * - Otherwise, render PlaceholderView (page under development).
 * - DIRECTORY-type items are skipped (they're just groupings in the sidebar).
 * - Routes are added as children of the 'Layout' route.
 */
import type { RouteRecordRaw, Router } from 'vue-router'
import type { MenuItem } from '@matecloud/core'

/**
 * Map of path → lazy-loaded component for pages that actually exist.
 * Add entries here as you build new pages.
 */
const VIEW_MAP: Record<string, () => Promise<any>> = {
  // System (covers singular, plural, and /admin/ prefix variants)
  '/system/user': () => import('@/views/system/UserList.vue'),
  '/system/users': () => import('@/views/system/UserList.vue'),
  '/system/role': () => import('@/views/admin/RoleList.vue'),
  '/system/roles': () => import('@/views/admin/RoleList.vue'),
  '/system/menu': () => import('@/views/admin/MenuTree.vue'),
  '/system/menus': () => import('@/views/admin/MenuTree.vue'),
  '/system/dict': () => import('@/views/admin/DictManager.vue'),
  '/system/config': () => import('@/views/system/ConfigList.vue'),
  '/admin/admins': () => import('@/views/admin/AdminList.vue'),
  '/admin/roles': () => import('@/views/admin/RoleList.vue'),
  '/admin/menus': () => import('@/views/admin/MenuTree.vue'),
  '/admin/dict': () => import('@/views/admin/DictManager.vue'),
  // Department
  '/system/dept': () => import('@/views/admin/DeptTree.vue'),
  '/system/depts': () => import('@/views/admin/DeptTree.vue'),
  '/admin/dept': () => import('@/views/admin/DeptTree.vue'),
  '/admin/depts': () => import('@/views/admin/DeptTree.vue'),
  // Notice center
  '/system/notice': () => import('@/views/monitor/NoticeCenter.vue'),
  '/admin/notice': () => import('@/views/monitor/NoticeCenter.vue'),
  '/monitor/notice': () => import('@/views/monitor/NoticeCenter.vue'),
  // Tenant
  '/system/tenant': () => import('@/views/system/tenant/index.vue'),
  '/system/tenants': () => import('@/views/system/tenant/index.vue'),
  '/admin/tenants': () => import('@/views/system/tenant/index.vue'),
  '/system/tenant-package': () => import('@/views/system/tenant/TenantPackageList.vue'),
  '/admin/tenant-packages': () => import('@/views/system/tenant/TenantPackageList.vue'),
  // Monitor
  '/monitor/log': () => import('@/views/monitor/OperationLog.vue'),
  '/monitor/operation-log': () => import('@/views/monitor/OperationLog.vue'),
  '/monitor/login-log': () => import('@/views/monitor/LoginLog.vue'),
  '/monitor/online': () => import('@/views/monitor/OnlineUsers.vue'),
  '/monitor/server/status': () => import('@/views/monitor/server/ServerStatus.vue'),
  '/monitor/server/cache': () => import('@/views/monitor/server/ServerCache.vue'),
  '/monitor/server/docs': () => import('@/views/monitor/server/ApiDocs.vue'),
  // Microservice governance (mate-gateway: 灰度发布 / 限流 / 超时)
  '/microservice/gray': () => import('@/views/microservice/GrayRelease.vue'),
  '/microservice/ratelimit': () => import('@/views/microservice/RateLimit.vue'),
  '/microservice/timeout': () => import('@/views/microservice/Timeout.vue'),
  // Tools
  '/tools/codegen': () => import('@/views/tools/CodeGen.vue'),
  '/tools/storage': () => import('@/views/tools/Storage.vue'),
  '/tools/tasks/jobs': () => import('@/views/tools/tasks/JobList.vue'),
  '/tools/tasks/logs': () => import('@/views/tools/tasks/JobLogs.vue'),
  // AI assistant suite (mate-ai)
  '/ai/chat':    () => import('@/views/ai/ChatView.vue'),
  '/ai/agents':  () => import('@/views/ai/AgentsView.vue'),
  '/ai/library': () => import('@/views/ai/LibraryView.vue'),
  '/ai/mcp':     () => import('@/views/ai/McpView.vue'),
  '/ai/models':  () => import('@/views/ai/ModelsView.vue'),
  '/ai/tools':   () => import('@/views/ai/ToolsView.vue'),
}

const PlaceholderView = () => import('@/views/common/PlaceholderView.vue')

/** Track which dynamic routes have been added (to avoid duplicates) */
let dynamicRoutesAdded = false

/**
 * Generate and register dynamic routes from menu tree.
 * Call this after login / fetchUserInfo.
 */
export function setupDynamicRoutes(router: Router, menuTree: MenuItem[]) {
  if (dynamicRoutesAdded) return
  const routes = flattenMenuToRoutes(menuTree)
  for (const route of routes) {
    // Only add if not already registered (static routes take precedence)
    if (!router.hasRoute(route.name!)) {
      router.addRoute('Layout', route)
    }
  }
  dynamicRoutesAdded = true
}

/**
 * Remove all dynamic routes (call on logout).
 */
export function resetDynamicRoutes() {
  dynamicRoutesAdded = false
}

/**
 * Flatten the menu tree into a flat list of route records.
 * Only MENU-type items (leaf nodes with actual pages) become routes.
 */
/** 全屏页面 (铺满内容区、去掉 layout-content 内边距)。 */
const FULLSCREEN_PATHS = new Set<string>([])

function flattenMenuToRoutes(items: MenuItem[]): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []

  function walk(nodes: MenuItem[]) {
    for (const node of nodes) {
      if ((node.type === 'MENU' || node.type === 'C') && node.path) {
        // Strip leading "/" for child route path
        const path = node.path.startsWith('/') ? node.path.slice(1) : node.path
        const name = pathToName(node.path)
        routes.push({
          path,
          name,
          component: VIEW_MAP[node.path] || PlaceholderView,
          meta: {
            title: node.name,
            icon: node.icon,
            perms: node.perms,
            fullscreen: FULLSCREEN_PATHS.has(node.path),
          },
        })
      }
      if (node.children?.length) {
        walk(node.children)
      }
    }
  }

  walk(items)
  return routes
}

/**
 * Convert path like "/monitor/server/status" to route name "MonitorServerStatus"
 */
function pathToName(path: string): string {
  return path
    .split('/')
    .filter(Boolean)
    .map((seg) => seg.charAt(0).toUpperCase() + seg.slice(1))
    .join('')
}
