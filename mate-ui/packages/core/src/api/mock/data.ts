/**
 * Mock data — returned when the backend (mate-gateway) is unreachable.
 * Allows frontend development / demos without any Java service running.
 *
 * Default credentials:  admin / admin123
 */
import type { Result } from '../../types/result'

// ---- helpers ----
function ok<T>(data: T): Result<T> {
  // Mirrors backend ResponseCode.SUCCESS ("00000"). The interceptor keys success
  // off the `success` flag, not this code — kept aligned for consistency only.
  return { code: '00000', msg: 'ok', success: true, data }
}

function fail(code: string, msg: string): Result<never> {
  return { code, msg, success: false } as Result<never>
}

const MOCK_TOKEN = 'mock-satoken-' + Date.now()
const MOCK_TOKEN_NAME = 'Authorization'

// ---- mock user ----
const ADMIN_USER = {
  userId: '1',
  username: 'admin',
  realName: 'Admin',
  mobile: '13800138000',
  email: 'admin@mate.vip',
  roleCodes: ['super_admin'],
  permissions: ['*'],
}

const LOGIN_RESULT = {
  ...ADMIN_USER,
  tokenName: MOCK_TOKEN_NAME,
  tokenValue: MOCK_TOKEN,
  expiresInSeconds: 86400,
}

// ---- mock menu tree (3 levels: 一级目录 → 二级分组 → 页面菜单),与 V1.1.14 对齐 ----
const D = 'DIRECTORY' as const
const M = 'MENU' as const
const MENU_TREE = [
  {
    id: '100', parentId: '0', name: '系统管理', nameEn: 'System', path: '/system',
    component: '', perms: '', type: D, icon: 'Settings', sort: 1,
    children: [
      {
        id: '120', parentId: '100', name: '权限管理', nameEn: 'Access Control', path: '/system/access',
        component: '', perms: '', type: D, icon: 'ShieldCheck', sort: 1,
        children: [
          { id: '101', parentId: '120', name: '用户管理', nameEn: 'Users', path: '/system/users', component: 'system/UserList', perms: 'sys:user:list', type: M, icon: 'Users', sort: 1 },
          { id: '103', parentId: '120', name: '角色管理', nameEn: 'Roles', path: '/system/role', component: 'admin/RoleList', perms: 'sys:role:list', type: M, icon: 'Key', sort: 2 },
          { id: '104', parentId: '120', name: '菜单管理', nameEn: 'Menus', path: '/system/menu', component: 'admin/MenuTree', perms: 'sys:menu:list', type: M, icon: 'List', sort: 3 },
          { id: '108', parentId: '120', name: '部门管理', nameEn: 'Departments', path: '/system/dept', component: 'admin/DeptTree', perms: 'sys:dept:list', type: M, icon: 'FolderTree', sort: 4 },
          { id: '102', parentId: '120', name: '管理员', nameEn: 'Admins', path: '/admin/admins', component: 'admin/AdminList', perms: 'sys:admin:list', type: M, icon: 'Shield', sort: 5 },
        ],
      },
      {
        id: '121', parentId: '100', name: '配置中心', nameEn: 'Configuration', path: '/system/settings',
        component: '', perms: '', type: D, icon: 'SlidersHorizontal', sort: 2,
        children: [
          { id: '105', parentId: '121', name: '字典管理', nameEn: 'Dictionary', path: '/system/dict', component: 'admin/DictManager', perms: 'sys:dict:list', type: M, icon: 'BookOpen', sort: 1 },
          { id: '106', parentId: '121', name: '参数设置', nameEn: 'Config', path: '/system/config', component: 'system/ConfigList', perms: 'sys:config:list', type: M, icon: 'Wrench', sort: 2 },
        ],
      },
      {
        id: '122', parentId: '100', name: '租户中心', nameEn: 'Tenant', path: '/system/tenant-center',
        component: '', perms: '', type: D, icon: 'Building2', sort: 3,
        children: [
          { id: '107', parentId: '122', name: '租户管理', nameEn: 'Tenants', path: '/system/tenant', component: 'system/tenant/index', perms: 'sys:tenant:list', type: M, icon: 'Building', sort: 1 },
          { id: '110', parentId: '122', name: '套餐管理', nameEn: 'Packages', path: '/system/tenant-package', component: 'system/tenant/TenantPackageList', perms: 'sys:tenant:package:list', type: M, icon: 'Box', sort: 2 },
        ],
      },
      {
        id: '123', parentId: '100', name: '消息中心', nameEn: 'Message', path: '/system/message',
        component: '', perms: '', type: D, icon: 'Bell', sort: 4,
        children: [
          { id: '109', parentId: '123', name: '通知中心', nameEn: 'Notice', path: '/system/notice', component: 'monitor/NoticeCenter', perms: 'sys:notice:list', type: M, icon: 'Bell', sort: 1 },
        ],
      },
    ],
  },
  {
    id: '200', parentId: '0', name: '系统监控', nameEn: 'Monitor', path: '/monitor',
    component: '', perms: '', type: D, icon: 'Activity', sort: 2,
    children: [
      {
        id: '220', parentId: '200', name: '日志审计', nameEn: 'Audit Logs', path: '/monitor/logs',
        component: '', perms: '', type: D, icon: 'ScrollText', sort: 1,
        children: [
          { id: '201', parentId: '220', name: '操作日志', nameEn: 'Operation Log', path: '/monitor/operation-log', component: 'monitor/OperationLog', perms: 'sys:log:list', type: M, icon: 'FileText', sort: 1 },
          { id: '202', parentId: '220', name: '登录日志', nameEn: 'Login Log', path: '/monitor/login-log', component: 'monitor/LoginLog', perms: 'sys:log:list', type: M, icon: 'Clock', sort: 2 },
        ],
      },
      {
        id: '221', parentId: '200', name: '服务监控', nameEn: 'Service', path: '/monitor/service',
        component: '', perms: '', type: D, icon: 'ServerCog', sort: 2,
        children: [
          { id: '203', parentId: '221', name: '在线用户', nameEn: 'Online Users', path: '/monitor/online', component: 'monitor/OnlineUsers', perms: 'sys:log:list', type: M, icon: 'Wifi', sort: 1 },
          { id: '204', parentId: '221', name: '服务状态', nameEn: 'Status', path: '/monitor/server/status', component: 'monitor/server/ServerStatus', perms: '', type: M, icon: 'HeartPulse', sort: 2 },
          { id: '205', parentId: '221', name: '缓存监控', nameEn: 'Cache', path: '/monitor/server/cache', component: 'monitor/server/ServerCache', perms: '', type: M, icon: 'Database', sort: 3 },
        ],
      },
    ],
  },
]

// ---- mock system configs ----
const MOCK_CONFIGS = [
  { id: '1', configKey: 'sys.user.initPassword', configValue: '123456', configName: '默认初始密码', builtIn: true, remark: '新建用户的初始密码', createdAt: '2025-01-01 10:00:00' },
  { id: '2', configKey: 'sys.login.captchaEnabled', configValue: 'true', configName: '登录验证码开关', builtIn: true, remark: '是否开启图形验证码', createdAt: '2025-01-01 10:00:00' },
  { id: '3', configKey: 'sys.login.maxFailCount', configValue: '5', configName: '登录失败锁定次数', builtIn: true, remark: '达到次数后锁定账号', createdAt: '2025-01-01 10:00:00' },
  { id: '4', configKey: 'sys.site.title', configValue: 'MateCloud Admin', configName: '站点标题', builtIn: false, remark: '', createdAt: '2025-02-10 09:30:00' },
  { id: '5', configKey: 'sys.site.copyright', configValue: '© 2026 MateCloud', configName: '版权信息', builtIn: false, remark: '', createdAt: '2025-02-10 09:31:00' },
  { id: '6', configKey: 'sys.oss.provider', configValue: 'minio', configName: '对象存储提供商', builtIn: false, remark: 'minio / aliyun / qiniu / s3', createdAt: '2025-03-15 14:22:00' },
  { id: '7', configKey: 'sys.oss.endpoint', configValue: 'http://minio:9000', configName: 'OSS Endpoint', builtIn: false, remark: '', createdAt: '2025-03-15 14:23:00' },
  { id: '8', configKey: 'sys.mail.host', configValue: 'smtp.example.com', configName: '邮件服务器', builtIn: false, remark: '', createdAt: '2025-04-01 11:00:00' },
  { id: '9', configKey: 'sys.sms.signature', configValue: 'MateCloud', configName: '短信签名', builtIn: false, remark: '', createdAt: '2025-04-02 11:00:00' },
  { id: '10', configKey: 'sys.tenant.enabled', configValue: 'false', configName: '多租户开关', builtIn: true, remark: '开启后数据自动按租户隔离', createdAt: '2025-01-01 10:00:00' },
]

// ---- mock dict types ----
const MOCK_DICT_TYPES = [
  { id: '1', dictName: '用户状态', dictType: 'sys_status', status: 1, remark: '', createdAt: '2025-01-01 10:00:00' },
  { id: '2', dictName: '性别', dictType: 'sys_gender', status: 1, remark: '', createdAt: '2025-01-01 10:00:00' },
  { id: '3', dictName: '操作类型', dictType: 'sys_oper_type', status: 1, remark: '', createdAt: '2025-01-01 10:00:00' },
  { id: '4', dictName: '登录方式', dictType: 'sys_login_type', status: 1, remark: '', createdAt: '2025-01-01 10:00:00' },
  { id: '5', dictName: '通知渠道', dictType: 'notice_channel', status: 1, remark: '', createdAt: '2025-02-10 09:00:00' },
  { id: '6', dictName: '菜单类型', dictType: 'sys_menu_type', status: 1, remark: '', createdAt: '2025-01-01 10:00:00' },
  { id: '7', dictName: '是/否', dictType: 'sys_yes_no', status: 1, remark: '', createdAt: '2025-01-01 10:00:00' },
  { id: '8', dictName: '业务开关', dictType: 'sys_biz_switch', status: 0, remark: '暂未启用', createdAt: '2025-03-01 10:00:00' },
]

// ---- mock dict data (by type) ----
const MOCK_DICT_DATA: Record<string, Array<{ id: string; dictType: string; dictLabel: string; dictValue: string; sort: number; status: number; remark?: string; createdAt: string }>> = {
  sys_status: [
    { id: '101', dictType: 'sys_status', dictLabel: '启用', dictValue: 'ACTIVE', sort: 1, status: 1, remark: '', createdAt: '2025-01-01 10:00:00' },
    { id: '102', dictType: 'sys_status', dictLabel: '停用', dictValue: 'DISABLED', sort: 2, status: 1, remark: '', createdAt: '2025-01-01 10:00:00' },
    { id: '103', dictType: 'sys_status', dictLabel: '删除', dictValue: 'DELETED', sort: 3, status: 1, remark: '', createdAt: '2025-01-01 10:00:00' },
  ],
  sys_gender: [
    { id: '201', dictType: 'sys_gender', dictLabel: '未知', dictValue: '0', sort: 0, status: 1, createdAt: '2025-01-01 10:00:00' },
    { id: '202', dictType: 'sys_gender', dictLabel: '男', dictValue: '1', sort: 1, status: 1, createdAt: '2025-01-01 10:00:00' },
    { id: '203', dictType: 'sys_gender', dictLabel: '女', dictValue: '2', sort: 2, status: 1, createdAt: '2025-01-01 10:00:00' },
  ],
  sys_oper_type: [
    { id: '301', dictType: 'sys_oper_type', dictLabel: '新增', dictValue: 'INSERT', sort: 1, status: 1, createdAt: '2025-01-01 10:00:00' },
    { id: '302', dictType: 'sys_oper_type', dictLabel: '修改', dictValue: 'UPDATE', sort: 2, status: 1, createdAt: '2025-01-01 10:00:00' },
    { id: '303', dictType: 'sys_oper_type', dictLabel: '删除', dictValue: 'DELETE', sort: 3, status: 1, createdAt: '2025-01-01 10:00:00' },
    { id: '304', dictType: 'sys_oper_type', dictLabel: '查询', dictValue: 'SELECT', sort: 4, status: 1, createdAt: '2025-01-01 10:00:00' },
    { id: '305', dictType: 'sys_oper_type', dictLabel: '导出', dictValue: 'EXPORT', sort: 5, status: 1, createdAt: '2025-01-01 10:00:00' },
    { id: '306', dictType: 'sys_oper_type', dictLabel: '导入', dictValue: 'IMPORT', sort: 6, status: 1, createdAt: '2025-01-01 10:00:00' },
  ],
  sys_login_type: [
    { id: '401', dictType: 'sys_login_type', dictLabel: '密码登录', dictValue: 'PASSWORD', sort: 1, status: 1, createdAt: '2025-01-01 10:00:00' },
    { id: '402', dictType: 'sys_login_type', dictLabel: '短信登录', dictValue: 'SMS', sort: 2, status: 1, createdAt: '2025-01-01 10:00:00' },
    { id: '403', dictType: 'sys_login_type', dictLabel: '第三方', dictValue: 'OAUTH', sort: 3, status: 1, createdAt: '2025-01-01 10:00:00' },
  ],
  notice_channel: [
    { id: '501', dictType: 'notice_channel', dictLabel: '站内信', dictValue: 'SITE', sort: 1, status: 1, createdAt: '2025-02-10 09:00:00' },
    { id: '502', dictType: 'notice_channel', dictLabel: '短信', dictValue: 'SMS', sort: 2, status: 1, createdAt: '2025-02-10 09:00:00' },
    { id: '503', dictType: 'notice_channel', dictLabel: '邮件', dictValue: 'EMAIL', sort: 3, status: 1, createdAt: '2025-02-10 09:00:00' },
    { id: '504', dictType: 'notice_channel', dictLabel: '企业微信', dictValue: 'WECOM', sort: 4, status: 1, createdAt: '2025-02-10 09:00:00' },
  ],
  sys_menu_type: [
    { id: '601', dictType: 'sys_menu_type', dictLabel: '目录', dictValue: 'M', sort: 1, status: 1, createdAt: '2025-01-01 10:00:00' },
    { id: '602', dictType: 'sys_menu_type', dictLabel: '菜单', dictValue: 'C', sort: 2, status: 1, createdAt: '2025-01-01 10:00:00' },
    { id: '603', dictType: 'sys_menu_type', dictLabel: '按钮', dictValue: 'F', sort: 3, status: 1, createdAt: '2025-01-01 10:00:00' },
  ],
  sys_yes_no: [
    { id: '701', dictType: 'sys_yes_no', dictLabel: '是', dictValue: 'Y', sort: 1, status: 1, createdAt: '2025-01-01 10:00:00' },
    { id: '702', dictType: 'sys_yes_no', dictLabel: '否', dictValue: 'N', sort: 2, status: 1, createdAt: '2025-01-01 10:00:00' },
  ],
  sys_biz_switch: [],
}

// ---- mock operation logs (60 records) ----
const MODULES = ['User', 'Role', 'Menu', 'Dict', 'Config', 'Admin', 'Auth', 'File']
const OPER_TYPES = ['INSERT', 'UPDATE', 'DELETE', 'SELECT', 'EXPORT']
const METHODS: Record<string, string> = { INSERT: 'POST', UPDATE: 'PUT', DELETE: 'DELETE', SELECT: 'GET', EXPORT: 'GET' }
const MOCK_OPERATION_LOGS = Array.from({ length: 60 }, (_, i) => {
  const mod = MODULES[i % MODULES.length]
  const type = OPER_TYPES[i % OPER_TYPES.length]
  const ok = i % 11 !== 3
  const day = String((i % 28) + 1).padStart(2, '0')
  return {
    id: String(1000 + i),
    userId: '1',
    username: i % 5 === 0 ? 'operator' : 'admin',
    module: mod,
    operationType: type,
    requestMethod: METHODS[type],
    requestUrl: `/api/v1/admin/${mod.toLowerCase()}s${type === 'UPDATE' || type === 'DELETE' ? '/' + (100 + i) : ''}`,
    requestParams: type === 'INSERT' ? '{"name":"Test","status":1}' : '{}',
    responseResult: ok ? '{"code":"200","success":true}' : '{"code":"USRB001","success":false,"msg":"failed"}',
    clientIp: `192.168.${(i % 5) + 1}.${(i % 200) + 10}`,
    location: i % 3 === 0 ? '北京' : '上海',
    userAgent: 'Mozilla/5.0 (Windows NT 10.0) Chrome/120',
    status: ok ? 0 : 1,
    errorMsg: ok ? '' : '参数校验失败',
    duration: 20 + (i * 7) % 400,
    createdAt: `2026-04-${day} ${String((i % 24)).padStart(2, '0')}:${String((i * 7) % 60).padStart(2, '0')}:00`,
  }
})

// ---- mock login logs (50 records) ----
const MOCK_LOGIN_LOGS = Array.from({ length: 50 }, (_, i) => {
  const ok = i % 9 !== 4
  const day = String((i % 28) + 1).padStart(2, '0')
  return {
    id: String(2000 + i),
    username: i === 0 ? 'admin' : ['operator', 'auditor', 'admin', 'user003'][i % 4],
    clientIp: `10.0.${(i % 10)}.${(i % 255)}`,
    location: i % 3 === 0 ? '北京' : i % 3 === 1 ? '上海' : '杭州',
    userAgent: 'Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 Chrome/120',
    browser: 'Chrome 120',
    os: 'Windows 10',
    loginType: i % 4 === 0 ? 'SMS' : 'PASSWORD',
    status: ok ? 0 : 1,
    failMsg: ok ? '' : '密码错误',
    createdAt: `2026-04-${day} ${String((i % 24)).padStart(2, '0')}:${String((i * 5) % 60).padStart(2, '0')}:00`,
  }
})

function paginate<T>(list: T[], url: string): { list: T[]; total: number } {
  const u = new URL(url, 'http://localhost')
  const pageNum = parseInt(u.searchParams.get('pageNum') || '1')
  const pageSize = parseInt(u.searchParams.get('pageSize') || '10')
  const start = (pageNum - 1) * pageSize
  return { list: list.slice(start, start + pageSize), total: list.length }
}

// ---- mock user list (20 records) ----
const MOCK_USERS = Array.from({ length: 20 }, (_, i) => ({
  userId: String(i + 1),
  username: i === 0 ? 'admin' : `user${String(i).padStart(3, '0')}`,
  realName: i === 0 ? 'Admin' : ['张三', '李四', '王五', '赵六', '钱七', '孙八', '周九', '吴十', '郑凯', '陈伟', '林涛', '黄勇', '刘洋', '杨帆', '朱磊', '许峰', '何军', '宋明', '谢浩'][i - 1] || `用户${i}`,
  mobile: `138${String(10000000 + i).slice(-8)}`,
  email: i === 0 ? 'admin@mate.vip' : `user${i}@mate.vip`,
  gender: i % 3 === 0 ? 0 : 1,
  status: i === 18 ? 'DISABLED' : i === 19 ? 'DELETED' : 'ACTIVE',
  roleCodes: i === 0 ? ['super_admin'] : i < 5 ? ['admin'] : ['user'],
  permissions: i === 0 ? ['*'] : [],
  createdAt: `2025-${String((i % 12) + 1).padStart(2, '0')}-${String((i % 28) + 1).padStart(2, '0')} 10:00:00`,
}))

// ---- captcha-plus mock (slider challenge) ----
// In offline/mock mode the slider always succeeds; images are simple placeholder PNGs.
const PLACEHOLDER_BG = 'iVBORw0KGgoAAAANSUhEUgAAAUAAAACgCAYAAABRoiIQAAAABmJLR0QA/wD/AP+gvaeTAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=='
const PLACEHOLDER_PIECE = 'iVBORw0KGgoAAAANSUhEUgAAAEYAAABGCAYAAABxLuKEAAAABmJLR0QA/wD/AP+gvaeTAAAADUlEQVR42mNkYGBg+A8AAQQAAd4TKCQAAAAASUVORK5CYII='
let captchaCounter = 0
function mockCaptchaChallenge() {
  captchaCounter++
  return {
    originalImageBase64: PLACEHOLDER_BG,
    jigsawImageBase64: PLACEHOLDER_PIECE,
    token: 'mock-captcha-token-' + captchaCounter,
    secretKey: 'abcdefghij123456', // 16-char AES key
  }
}

// ---- mock online users (5 records) ----
const MOCK_ONLINE_USERS = (() => {
  const now = Date.now()
  const entries = [
    { userId: '1', username: 'admin', realName: 'Admin' },
    { userId: '2', username: 'operator', realName: '运营主管' },
    { userId: '4', username: 'user001', realName: '张三' },
    { userId: '5', username: 'user002', realName: '李四' },
    { userId: '6', username: 'user003', realName: '王五' },
  ]
  return entries.map((u, i) => {
    const loginAgo = (10 + i * 35) * 60_000
    const accessAgo = (i * 4) * 60_000
    return {
      tokenValue: `satoken-mock-${1000 + i}`,
      userId: u.userId,
      username: u.username,
      realName: u.realName,
      loginTime: now - loginAgo,
      lastAccessTime: now - accessAgo,
    }
  })
})()

// ---- route → mock response map ----

type MockHandler = (config: { url?: string; method?: string; data?: string }) => Result<unknown> | null

const handlers: MockHandler[] = [
  // Captcha-plus: GET challenge
  (c) => {
    if (c.url?.includes('/auth/captcha/get') && c.method === 'get') return ok(mockCaptchaChallenge())
    return null
  },
  // Captcha-plus: POST check — in mock mode always succeeds
  (c) => {
    if (c.url?.includes('/auth/captcha/check') && c.method === 'post') {
      return ok({ captchaVerification: 'mock-captcha-verification-' + Date.now() })
    }
    return null
  },

  // Password login
  (c) => {
    if (c.url?.includes('/auth/login') && c.method === 'post') {
      try {
        const body = typeof c.data === 'string' ? JSON.parse(c.data) : c.data
        if ((body.account === 'admin' || body.account === '13800138000') && body.password === 'admin123') {
          return ok(LOGIN_RESULT)
        }
        return fail('USRB001', 'Invalid account or password')
      } catch {
        return fail('USRB001', 'Invalid request')
      }
    }
    return null
  },

  // SMS send
  (c) => {
    if (c.url?.includes('/auth/sms/send') && c.method === 'post') return ok(null)
    return null
  },

  // SMS login
  (c) => {
    if (c.url?.includes('/auth/sms/login') && c.method === 'post') {
      try {
        const body = typeof c.data === 'string' ? JSON.parse(c.data) : c.data
        if (body.mobile === '13800138000' && body.code === '888888') {
          return ok(LOGIN_RESULT)
        }
        return fail('USRB002', 'Invalid SMS code')
      } catch {
        return fail('USRB002', 'Invalid request')
      }
    }
    return null
  },

  // Register
  (c) => {
    if (c.url?.includes('/auth/register') && c.method === 'post') {
      try {
        const body = typeof c.data === 'string' ? JSON.parse(c.data) : c.data
        return ok({
          ...LOGIN_RESULT,
          username: body.username,
          realName: body.realName || body.username,
        })
      } catch {
        return fail('USRB003', 'Invalid request')
      }
    }
    return null
  },

  // User info
  (c) => {
    if (c.url?.includes('/auth/info') && c.method === 'get') return ok(ADMIN_USER)
    return null
  },

  // Logout
  (c) => {
    if (c.url?.includes('/auth/logout') && c.method === 'post') return ok(null)
    return null
  },

  // Menu tree
  (c) => {
    if (c.url?.includes('/admin/menus/tree') && c.method === 'get') return ok(MENU_TREE)
    return null
  },

  // ---- Admin RBAC mock ----

  // Admin list
  (c) => {
    if (c.url?.match(/\/admin\/admins(\?|$)/) && c.method === 'get') {
      return ok([
        { id: '1', username: 'admin', nickName: '超级管理员', avatar: '', status: 'ACTIVE', roleIds: ['1'], createdAt: '2025-01-01 00:00:00' },
        { id: '2', username: 'operator', nickName: '运营主管', avatar: '', status: 'ACTIVE', roleIds: ['2'], createdAt: '2025-02-15 10:00:00' },
        { id: '3', username: 'auditor', nickName: '审计员', avatar: '', status: 'DISABLED', roleIds: ['3'], createdAt: '2025-03-20 14:30:00' },
      ])
    }
    return null
  },
  // Admin get by ID
  (c) => {
    if (c.url?.match(/\/admin\/admins\/\w+$/) && c.method === 'get') {
      return ok({ id: '1', username: 'admin', nickName: '超级管理员', avatar: '', status: 'ACTIVE', roleIds: ['1'], createdAt: '2025-01-01 00:00:00' })
    }
    return null
  },
  // Admin create/update/delete/assign/enable/disable
  (c) => {
    if (c.url?.includes('/admin/admins') && (c.method === 'post' || c.method === 'put' || c.method === 'delete')) {
      return ok(c.method === 'post' ? '99' : null)
    }
    return null
  },

  // Roles list
  (c) => {
    if (c.url?.match(/\/admin\/roles(\?|$)/) && c.method === 'get') {
      return ok([
        { id: '1', roleKey: 'super_admin', roleName: 'Super Admin', sort: 0, status: 'ACTIVE', menuIds: ['1', '11', '12', '13', '14', '15', '2', '21', '22', '23', '231', '232', '233', '3', '31', '32', '33', '331', '332'] },
        { id: '2', roleKey: 'admin', roleName: 'Admin', sort: 1, status: 'ACTIVE', menuIds: ['1', '11', '12'] },
        { id: '3', roleKey: 'user', roleName: 'User', sort: 2, status: 'ACTIVE', menuIds: [] },
      ])
    }
    return null
  },
  // Role get by ID
  (c) => {
    if (c.url?.match(/\/admin\/roles\/\w+$/) && c.method === 'get') {
      return ok({ id: '1', roleKey: 'super_admin', roleName: 'Super Admin', sort: 0, status: 'ACTIVE', menuIds: ['1', '11', '12', '13', '14', '15'] })
    }
    return null
  },
  // Role create/update/delete/assign menus
  (c) => {
    if (c.url?.includes('/admin/roles') && (c.method === 'post' || c.method === 'put' || c.method === 'delete')) {
      return ok(c.method === 'post' ? '99' : null)
    }
    return null
  },

  // Menu create/update/delete (tree is already handled above)
  (c) => {
    if (c.url?.includes('/admin/menus') && (c.method === 'post' || c.method === 'put' || c.method === 'delete')) {
      return ok(c.method === 'post' ? '99' : null)
    }
    return null
  },

  // Dict
  (c) => {
    if (c.url?.match(/\/admin\/dict\/type\//) && c.method === 'get') {
      return ok([
        { dictType: 'sys_status', dictLabel: 'Active', dictValue: 'ACTIVE', sort: 1 },
        { dictType: 'sys_status', dictLabel: 'Disabled', dictValue: 'DISABLED', sort: 2 },
      ])
    }
    return null
  },

  // ---- User CRUD ----
  // List with pagination
  (c) => {
    if (c.url?.match(/\/users(\?|$)/) && c.method === 'get') {
      const url = new URL(c.url!, 'http://localhost')
      const pageNum = parseInt(url.searchParams.get('pageNum') || '1')
      const pageSize = parseInt(url.searchParams.get('pageSize') || '10')
      const keyword = url.searchParams.get('keyword') || ''
      let list = MOCK_USERS
      if (keyword) {
        const kw = keyword.toLowerCase()
        list = list.filter(u =>
          u.username.toLowerCase().includes(kw) ||
          u.realName.includes(kw) ||
          u.mobile.includes(kw)
        )
      }
      const start = (pageNum - 1) * pageSize
      return ok({ list: list.slice(start, start + pageSize), total: list.length })
    }
    return null
  },

  // Get by ID
  (c) => {
    const m = c.url?.match(/\/users\/(\d+)$/)
    if (m && c.method === 'get') {
      const user = MOCK_USERS.find(u => u.userId === m[1])
      return user ? ok(user) : fail('404', 'User not found')
    }
    return null
  },

  // Create
  (c) => {
    if (c.url?.match(/\/users$/) && c.method === 'post') {
      return ok(String(MOCK_USERS.length + 1))
    }
    return null
  },

  // Change password (must come BEFORE the generic /users/:id PUT handler)
  (c) => {
    if (c.url?.match(/\/users\/password$/) && c.method === 'put') {
      try {
        const body = typeof c.data === 'string' ? JSON.parse(c.data) : c.data
        if (!body.oldPassword || !body.newPassword) return fail('USRA001', 'Missing password fields')
        if (body.oldPassword !== 'admin123') return fail('USRB004', 'Old password incorrect')
        return ok(null)
      } catch {
        return fail('USRA001', 'Invalid request')
      }
    }
    return null
  },

  // Update / freeze / unfreeze / delete
  (c) => {
    const m = c.url?.match(/\/users\/(\d+)/)
    if (m && (c.method === 'put' || c.method === 'delete')) {
      return ok(null)
    }
    return null
  },

  // ---- Configs CRUD ----
  (c) => {
    if (c.url?.match(/\/admin\/configs(\?|$)/) && c.method === 'get') {
      const u = new URL(c.url!, 'http://localhost')
      const keyword = (u.searchParams.get('keyword') || '').toLowerCase()
      const builtInParam = u.searchParams.get('builtIn')
      let list = MOCK_CONFIGS.slice()
      if (keyword) {
        list = list.filter(cfg =>
          cfg.configKey.toLowerCase().includes(keyword) ||
          cfg.configName.toLowerCase().includes(keyword),
        )
      }
      if (builtInParam === 'true') list = list.filter(cfg => cfg.builtIn)
      else if (builtInParam === 'false') list = list.filter(cfg => !cfg.builtIn)
      return ok(paginate(list, c.url!))
    }
    return null
  },
  (c) => {
    if (c.url?.match(/\/admin\/configs$/) && c.method === 'post') return ok(String(Date.now()))
    if (c.url?.match(/\/admin\/configs\/[\w-]+$/) && (c.method === 'put' || c.method === 'delete')) return ok(null)
    return null
  },

  // ---- Dict Types CRUD ----
  (c) => {
    if (c.url?.match(/\/admin\/dict\/types(\?|$)/) && c.method === 'get') {
      const u = new URL(c.url!, 'http://localhost')
      const keyword = (u.searchParams.get('keyword') || '').toLowerCase()
      let list = MOCK_DICT_TYPES.slice()
      if (keyword) {
        list = list.filter(t =>
          t.dictName.toLowerCase().includes(keyword) ||
          t.dictType.toLowerCase().includes(keyword),
        )
      }
      return ok(paginate(list, c.url!))
    }
    return null
  },
  (c) => {
    if (c.url?.match(/\/admin\/dict\/types$/) && c.method === 'post') return ok(String(Date.now()))
    if (c.url?.match(/\/admin\/dict\/types\/[\w-]+$/) && (c.method === 'put' || c.method === 'delete')) return ok(null)
    return null
  },

  // ---- Dict Data CRUD ----
  (c) => {
    if (c.url?.match(/\/admin\/dict\/data(\?|$)/) && c.method === 'get') {
      const u = new URL(c.url!, 'http://localhost')
      const type = u.searchParams.get('dictType') || ''
      const keyword = (u.searchParams.get('keyword') || '').toLowerCase()
      let list = MOCK_DICT_DATA[type] ?? []
      if (keyword) {
        list = list.filter(d =>
          d.dictLabel.toLowerCase().includes(keyword) ||
          d.dictValue.toLowerCase().includes(keyword),
        )
      }
      return ok(paginate(list, c.url!))
    }
    return null
  },
  (c) => {
    if (c.url?.match(/\/admin\/dict\/data$/) && c.method === 'post') return ok(String(Date.now()))
    if (c.url?.match(/\/admin\/dict\/data\/[\w-]+$/) && (c.method === 'put' || c.method === 'delete')) return ok(null)
    return null
  },

  // ---- Operation logs (list + detail) ----
  (c) => {
    if (c.url?.match(/\/admin\/operation-logs\/[\w-]+$/) && c.method === 'get') {
      const id = c.url!.split('/').pop()!
      const log = MOCK_OPERATION_LOGS.find(l => l.id === id)
      return log ? ok(log) : fail('404', 'Log not found')
    }
    if (c.url?.match(/\/admin\/operation-logs(\?|$)/) && c.method === 'get') {
      const u = new URL(c.url!, 'http://localhost')
      const mod = (u.searchParams.get('module') || '').toLowerCase()
      const username = (u.searchParams.get('username') || '').toLowerCase()
      const startTime = u.searchParams.get('startTime') || ''
      const endTime = u.searchParams.get('endTime') || ''
      let list = MOCK_OPERATION_LOGS.slice()
      if (mod) list = list.filter(l => l.module.toLowerCase().includes(mod))
      if (username) list = list.filter(l => l.username.toLowerCase().includes(username))
      if (startTime) list = list.filter(l => l.createdAt >= startTime)
      if (endTime) list = list.filter(l => l.createdAt <= endTime)
      return ok(paginate(list, c.url!))
    }
    return null
  },

  // ---- Login logs (list + detail) ----
  (c) => {
    if (c.url?.match(/\/admin\/login-logs\/[\w-]+$/) && c.method === 'get') {
      const id = c.url!.split('/').pop()!
      const log = MOCK_LOGIN_LOGS.find(l => l.id === id)
      return log ? ok(log) : fail('404', 'Log not found')
    }
    if (c.url?.match(/\/admin\/login-logs(\?|$)/) && c.method === 'get') {
      const u = new URL(c.url!, 'http://localhost')
      const username = (u.searchParams.get('username') || '').toLowerCase()
      const statusParam = u.searchParams.get('status')
      const startTime = u.searchParams.get('startTime') || ''
      const endTime = u.searchParams.get('endTime') || ''
      let list = MOCK_LOGIN_LOGS.slice()
      if (username) list = list.filter(l => l.username.toLowerCase().includes(username))
      if (statusParam === '0' || statusParam === '1') list = list.filter(l => l.status === parseInt(statusParam))
      if (startTime) list = list.filter(l => l.createdAt >= startTime)
      if (endTime) list = list.filter(l => l.createdAt <= endTime)
      return ok(paginate(list, c.url!))
    }
    return null
  },

  // ---- Admin: reset password ----
  (c) => {
    if (c.url?.match(/\/admin\/admins\/[\w-]+\/password\/reset$/) && c.method === 'put') {
      return ok(null)
    }
    return null
  },

  // ---- Server info (monitor/server) ----
  (c) => {
    if (c.url?.match(/\/admin\/monitor\/server$/) && c.method === 'get') {
      return ok({
        jvm: {
          maxMemory: '1024 MB',
          totalMemory: '512 MB',
          freeMemory: `${Math.round(80 + Math.random() * 40)} MB`,
          usedMemory: `${Math.round(420 + Math.random() * 80)} MB`,
          javaVersion: '21.0.2',
          jvmName: 'OpenJDK 64-Bit Server VM',
        },
        os: {
          name: 'Windows 10',
          arch: 'amd64',
          availableProcessors: 8,
        },
        disk: {
          total: '512 GB',
          free: `${Math.round(340 + Math.random() * 20)} GB`,
          used: `${Math.round(156 + Math.random() * 10)} GB`,
        },
      })
    }
    return null
  },

  // ---- Cache info (monitor/cache) ----
  (c) => {
    if (c.url?.match(/\/admin\/monitor\/cache$/) && c.method === 'get') {
      return ok({
        redis: {
          version: '7.2.4',
          usedMemory: '4.27M',
          maxMemory: '256.00M',
          connectedClients: '12',
          uptimeDays: '17',
          dbSize: 368,
        },
      })
    }
    return null
  },

  // ---- Online Users ----
  (c) => {
    if (c.url?.match(/\/admin\/online-users\/[\w-]+\/kick$/) && c.method === 'post') {
      return ok(null)
    }
    if (c.url?.match(/\/admin\/online-users(\?|$)/) && c.method === 'get') {
      const u = new URL(c.url!, 'http://localhost')
      const keyword = (u.searchParams.get('keyword') || '').toLowerCase()
      let list = MOCK_ONLINE_USERS.slice()
      if (keyword) {
        list = list.filter(o =>
          o.username.toLowerCase().includes(keyword) ||
          (o.realName && o.realName.toLowerCase().includes(keyword)),
        )
      }
      return ok(paginate(list, c.url!))
    }
    return null
  },

  // ---- Dashboard (RFC-047 G4) ----
  (c) => {
    if (c.url?.match(/\/admin\/monitor\/dashboard$/) && c.method === 'get') {
      // Build last-7-days series with deterministic-looking variance so the
      // chart looks realistic across reloads but doesn't change wildly.
      const today = new Date()
      const last7Days = []
      for (let i = 6; i >= 0; i--) {
        const d = new Date(today)
        d.setDate(d.getDate() - i)
        const iso = d.toISOString().slice(0, 10)
        // Mix in i so each day is slightly different; seed for repeatability.
        const apiCalls = 40 + ((i * 13 + 7) % 35) + Math.round(Math.random() * 8)
        const logins = 6 + ((i * 5 + 2) % 9) + Math.round(Math.random() * 3)
        last7Days.push({ date: iso, apiCalls, logins })
      }
      return ok({
        userCount: 12,
        todayLoginCount: last7Days[6].logins,
        todayOpCount: last7Days[6].apiCalls,
        onlineCount: 5,
        last7Days,
        services: [
          { name: 'gateway', url: 'http://localhost:9010', state: 'UP',   latencyMs: 18 },
          { name: 'auth',    url: 'http://localhost:9020', state: 'UP',   latencyMs: 24 },
          { name: 'system',  url: 'http://localhost:9030', state: 'UP',   latencyMs: 31 },
          { name: 'admin',   url: 'http://localhost:9040', state: 'UP',   latencyMs: 22 },
          { name: 'notice',  url: 'http://localhost:9050', state: 'DOWN', latencyMs: null },
        ],
      })
    }
    return null
  },

  // ---- Excel export endpoints (RFC-047 G2) ----
  // Mock cannot actually return a Blob through this layer (the layer returns
  // a Result<T> shape); the export composable uses raw axios anyway, so when
  // it hits these mock routes we return a tiny CSV-ish payload that the Blob
  // download path in useExport will save as a stub file. Good enough for the
  // offline demo.
  (c) => {
    const exportPaths = [
      '/users/export', '/users/import/template',
      '/admin/admins/export', '/admin/admins/import/template',
    ]
    if (c.url && exportPaths.some(p => c.url!.includes(p)) && c.method === 'get') {
      return ok('(mock xlsx — backend not running)' as any)
    }
    return null
  },

  // ---- Excel import (RFC-047 G3) ----
  (c) => {
    if ((c.url?.match(/\/users\/import$/) || c.url?.match(/\/admin\/admins\/import$/))
        && c.method === 'post') {
      // Pretend the server processed 8 rows: 7 ok, 1 dupe.
      return ok({
        total: 8,
        success: 7,
        fail: 1,
        errors: ['Row 5: Username already exists: demo01'],
      })
    }
    return null
  },

  // ---- Batch ops (RFC-047 G1) ----
  // Matches /users/batch-{freeze,unfreeze,delete} and
  // /admin/admins/batch-{enable,disable,delete}. Returns a BatchResult.
  (c) => {
    const batchRe = /\/(users|admin\/admins)\/batch-(freeze|unfreeze|delete|enable|disable)$/
    if (c.url?.match(batchRe) && c.method === 'post') {
      let ids: string[] = []
      try { ids = typeof c.data === 'string' ? JSON.parse(c.data) : (c.data as any) } catch { /* */ }
      if (!Array.isArray(ids)) ids = []
      // Pretend the last id always fails so the UI's partial-failure path
      // gets exercised in the offline demo.
      const failures = ids.length > 1
        ? [{ id: ids[ids.length - 1], message: 'Mock failure for the last id' }]
        : []
      return ok({
        successCount: ids.length - failures.length,
        failCount: failures.length,
        failures,
      })
    }
    return null
  },
]

/**
 * Try to match an axios config to a mock handler.
 * Returns a Result if matched, null if no mock available.
 */
export function tryMock(config: { url?: string; method?: string; data?: string }): Result<unknown> | null {
  for (const handler of handlers) {
    const result = handler(config)
    if (result !== null) return result
  }
  return null
}
