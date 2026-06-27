/**
 * Endpoint constants — URLs only, no request implementation.
 *
 * <p>Use these from any frontend (admin / mobile / desktop) to keep the path
 * registry in one place. The admin app uses {@code client.ts} (axios) which
 * already uses these paths; the uni-app and Tauri apps call the same paths
 * via {@code uni.request} / {@code fetch} respectively.
 */
export const Endpoints = {
  // ── Auth (mate-auth, port 9020 / gateway 9010) ──────────────────────
  login:        '/auth/login',
  smsSend:      '/auth/sms/send',
  smsLogin:     '/auth/sms/login',
  logout:       '/auth/logout',
  whoami:       '/auth/info',
  captcha:      '/auth/captcha',

  // ── User & profile ──────────────────────────────────────────────────
  users:        '/users',
  userById:     (id: string) => `/users/${id}`,
  changePassword: '/users/password',
  resetPassword: (id: string) => `/users/${id}/reset-password`,

  // ── Admin (RBAC) ────────────────────────────────────────────────────
  admins:       '/admin/admins',
  roles:        '/admin/roles',
  menus:        '/admin/menus',
  dictTypes:    '/admin/dict-types',
  dictData:     '/admin/dict-data',
  configs:      '/admin/configs',

  // ── Logs ────────────────────────────────────────────────────────────
  operationLogs: '/admin/operation-logs',
  loginLogs:     '/admin/login-logs',

  // ── Monitor ─────────────────────────────────────────────────────────
  dashboard:    '/admin/monitor/dashboard',
  serverInfo:   '/admin/monitor/server',
  cacheInfo:    '/admin/monitor/cache',
  onlineUsers:  '/admin/online-users',

  // ── Tooling (RFC-050 P2) ────────────────────────────────────────────
  storage:      '/admin/storage',
  storageUpload: '/admin/storage/upload',
  codegenTables: '/admin/codegen/tables',
  codegenPreview: '/admin/codegen/preview',
  jobAdminUrl:   '/admin/jobs/admin-url',

  // ── Tenant (RFC-051 / RFC-012) ──────────────────────────────────────
  tenants:      '/admin/tenants',
  tenantPackages: '/admin/tenants/packages',
} as const

export type EndpointKey = keyof typeof Endpoints
