# RFC-030: Frontend Architecture — One Codebase, Every Screen

- **Status**: Draft
- **Created**: 2026-04-12
- **Author**: MateCloud Team

> "Design is not just what it looks like and feels like. Design is how it works."

## 核心决策

**一句话**: 一个 Monorepo，五个端，共享 80% 代码。

```
mate-ui/                          # pnpm workspace + Turborepo
├── apps/
│   ├── admin/                    # 管理后台 (Vue 3 + Vite)
│   ├── mobile/                   # 小程序 + H5 + App (uni-app)
│   └── desktop/                  # PC 桌面端 (Tauri 2 + Vue 3)
├── packages/
│   ├── core/                     # 核心: API 客户端 + 状态管理 + 类型
│   ├── ui/                       # 共享组件库 (headless + styled)
│   ├── hooks/                    # 共享 composables
│   ├── utils/                    # 工具函数
│   └── config/                   # ESLint + TSConfig + Tailwind 共享配置
└── tooling/
    ├── eslint-config/            # 统一 lint 规则
    ├── tsconfig/                 # 统一 TS 配置
    └── tailwind-config/          # 统一 Tailwind 主题
```

## 为什么这样选

### 管理后台: Vue 3 + Vite + Element Plus + Tailwind CSS 4

不用 Nuxt。管理后台是 SPA，不需要 SSR。Nuxt 在这里是 overhead。

| 选项 | 判定 |
|------|------|
| Vue 3 + Vite | ✓ 极速 HMR，原生 ESM |
| Element Plus | ✓ Vue 3 生态最成熟的管理后台组件库 |
| Tailwind CSS 4 | ✓ 原子化 CSS，与 Element Plus 互补 |
| Pinia | ✓ Vue 官方推荐状态管理 |
| VueRouter 4 | ✓ 动态路由支持 RBAC 菜单 |

### 小程序 + H5 + App: uni-app

不用 Taro。原因很简单——uni-app 对 Vue 开发者的迁移成本比 Taro 低 58%。写 Vue 组件就是写小程序，不需要学新 API。

| 选项 | 判定 |
|------|------|
| uni-app | ✓ Vue 语法直出，一套代码 → 微信/支付宝/抖音/H5/App |
| uni-ui | ✓ 官方组件，跨端一致 |
| HBuilderX/Vite | ✓ 一键真机调试 |

### PC 桌面端: Tauri 2 + Vue 3

不用 Electron。Tauri 体积小 96%，内存少 75%，启动快 4 倍。2026 年还用 Electron 做桌面端，就像 2007 年还在做翻盖手机。

| 选项 | 判定 |
|------|------|
| Tauri 2 | ✓ Rust 后端，系统 WebView，< 10MB 安装包 |
| Vue 3 前端 | ✓ 复用 admin 的组件和逻辑 |
| Tauri 2 Mobile | ✓ 2.x 支持 iOS/Android (备选) |

---

## Part 1: Monorepo 工程化

### 技术栈版本

```json
{
  "vue": "^3.5",
  "vite": "^6.x",
  "typescript": "^5.7",
  "pnpm": "^9.x",
  "turbo": "^2.x",
  "tailwindcss": "^4.x",
  "element-plus": "^2.9",
  "pinia": "^3.x",
  "vue-router": "^4.5",
  "axios": "^1.7",
  "vueuse": "^12.x",
  "@tauri-apps/cli": "^2.x",
  "@dcloudio/uni-app": "^3.x"
}
```

### pnpm-workspace.yaml

```yaml
packages:
  - 'apps/*'
  - 'packages/*'
  - 'tooling/*'
```

### turbo.json

```json
{
  "$schema": "https://turbo.build/schema.json",
  "tasks": {
    "build": {
      "dependsOn": ["^build"],
      "outputs": ["dist/**"]
    },
    "dev": {
      "cache": false,
      "persistent": true
    },
    "lint": {},
    "typecheck": {
      "dependsOn": ["^build"]
    }
  }
}
```

### 开发命令

```bash
# 安装依赖
pnpm install

# 启动管理后台
pnpm dev --filter admin

# 启动小程序 (微信)
pnpm dev:mp-weixin --filter mobile

# 启动桌面端
pnpm dev --filter desktop

# 全部构建
pnpm build

# 全部 lint
pnpm lint

# 类型检查
pnpm typecheck
```

---

## Part 2: packages/core — 共享核心

这是前端的 mate-base。所有端共享同一套 API 客户端、类型定义、状态管理。

### API 客户端

```typescript
// packages/core/src/api/client.ts
import axios from 'axios'
import type { Result } from '../types'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000,
})

// 请求拦截: 注入 Token + Tenant
client.interceptors.request.use((config) => {
  const token = useAuthStore().token
  if (token) config.headers.Authorization = `Bearer ${token}`
  
  const tenantId = useTenantStore().tenantId
  if (tenantId) config.headers['X-Tenant-Id'] = tenantId
  
  return config
})

// 响应拦截: 统一错误处理
client.interceptors.response.use(
  (res) => {
    const data = res.data as Result<unknown>
    if (!data.success) {
      throw new BizError(data.code, data.msg)
    }
    return data
  },
  (err) => {
    if (err.response?.status === 401) {
      useAuthStore().logout()
      // 跳转登录
    }
    throw err
  }
)

export { client }
```

### 类型定义 (与后端 mate-api 对齐)

```typescript
// packages/core/src/types/result.ts
export interface Result<T = unknown> {
  code: string
  msg: string
  success: boolean
  data: T
}

// packages/core/src/types/user.ts (对应 UserInfoResponse.java)
export interface UserInfo {
  id: string
  mobile: string
  nickName: string
  avatar?: string
  gender?: number
  status: UserStatus
}

export enum UserStatus {
  ACTIVE = 0,
  FROZEN = 1,
  DELETED = 2,
}
```

### API 模块 (按后端服务划分)

```typescript
// packages/core/src/api/modules/auth.ts
export const authApi = {
  login: (data: LoginCommand) =>
    client.post<Result<LoginResponse>>('/auth/login', data),
  logout: () =>
    client.post<Result<void>>('/auth/logout'),
  info: () =>
    client.get<Result<UserInfo>>('/auth/info'),
  sendSms: (mobile: string) =>
    client.post<Result<void>>('/auth/sms/send', { mobile }),
}

// packages/core/src/api/modules/system.ts
export const userApi = {
  list: (params?: PageQuery) =>
    client.get<Result<UserInfo[]>>('/users', { params }),
  getById: (id: string) =>
    client.get<Result<UserInfo>>(`/users/${id}`),
  create: (data: CreateUserCommand) =>
    client.post<Result<string>>('/users', data),
}

// packages/core/src/api/modules/admin.ts
export const adminApi = {
  menuTree: () => client.get<Result<MenuItem[]>>('/admin/menus/tree'),
  roleList: () => client.get<Result<Role[]>>('/admin/roles'),
  dictByType: (type: string) =>
    client.get<Result<DictData[]>>(`/admin/dict/${type}`),
}
```

### 共享状态 (Pinia Store)

```typescript
// packages/core/src/stores/auth.ts
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>('')
  const user = ref<UserInfo | null>(null)
  const permissions = ref<string[]>([])

  const isLoggedIn = computed(() => !!token.value)

  async function login(command: LoginCommand) {
    const res = await authApi.login(command)
    token.value = res.data.token
    await fetchUserInfo()
  }

  async function fetchUserInfo() {
    const res = await authApi.info()
    user.value = res.data
  }

  function logout() {
    token.value = ''
    user.value = null
    permissions.value = []
  }

  // 权限检查
  function hasPermission(perm: string): boolean {
    return permissions.value.includes(perm) || permissions.value.includes('*')
  }

  return { token, user, permissions, isLoggedIn, login, fetchUserInfo, logout, hasPermission }
}, {
  persist: true, // pinia-plugin-persistedstate
})
```

---

## Part 3: apps/admin — 管理后台

### 目录结构

```
apps/admin/
├── src/
│   ├── layouts/
│   │   ├── DefaultLayout.vue       # 侧边栏 + 顶栏 + 内容区
│   │   ├── Sidebar.vue             # 动态菜单 (RBAC)
│   │   ├── Header.vue              # 面包屑 + 用户头像 + 租户切换
│   │   └── TabBar.vue              # 多标签页
│   ├── views/
│   │   ├── login/Login.vue
│   │   ├── dashboard/Dashboard.vue
│   │   ├── system/                  # 系统管理 (对应 mate-system)
│   │   │   ├── user/UserList.vue
│   │   │   └── user/UserForm.vue
│   │   ├── admin/                   # 后台管理 (对应 mate-admin)
│   │   │   ├── role/RoleList.vue
│   │   │   ├── menu/MenuTree.vue
│   │   │   └── dict/DictManager.vue
│   │   └── [app]/                   # 子系统页面 (CRM/Mall 等按需加)
│   ├── router/
│   │   ├── index.ts                 # 静态路由
│   │   ├── guard.ts                 # 路由守卫 (auth + permission)
│   │   └── dynamic.ts              # 动态路由 (从菜单 API 生成)
│   ├── directives/
│   │   └── permission.ts           # v-permission="'system:user:create'"
│   ├── composables/                # 页面级 hooks
│   ├── App.vue
│   └── main.ts
├── index.html
├── vite.config.ts
├── tailwind.config.ts
└── package.json
```

### 动态路由 (RBAC 菜单驱动)

```typescript
// src/router/dynamic.ts
export async function generateRoutes(): Promise<RouteRecordRaw[]> {
  const menus = await adminApi.menuTree()
  return transformMenusToRoutes(menus.data)
}

function transformMenusToRoutes(menus: MenuItem[]): RouteRecordRaw[] {
  return menus
    .filter(m => m.type !== 'BUTTON') // 按钮不是路由
    .map(menu => ({
      path: menu.path,
      name: menu.name,
      component: loadView(menu.component), // () => import(`@/views/${component}.vue`)
      meta: {
        title: menu.title,
        icon: menu.icon,
        permissions: menu.perms ? menu.perms.split(',') : [],
      },
      children: menu.children ? transformMenusToRoutes(menu.children) : [],
    }))
}
```

### 权限指令

```typescript
// src/directives/permission.ts
export const vPermission: Directive = {
  mounted(el, binding) {
    const { hasPermission } = useAuthStore()
    if (!hasPermission(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  },
}

// 使用: <el-button v-permission="'system:user:delete'">删除</el-button>
```

---

## Part 4: apps/mobile — uni-app 跨端

### 目录结构

```
apps/mobile/
├── src/
│   ├── pages/
│   │   ├── index/index.vue           # 首页
│   │   ├── login/login.vue           # 登录 (短信验证码)
│   │   ├── mine/mine.vue             # 我的
│   │   ├── crm/                      # CRM 页面 (子系统按需)
│   │   │   ├── customer-list.vue
│   │   │   └── customer-detail.vue
│   │   └── mall/                     # 商城页面 (子系统按需)
│   │       ├── product-list.vue
│   │       └── order-detail.vue
│   ├── components/                   # 移动端专用组件
│   ├── store/                        # 复用 packages/core 的 Pinia store
│   ├── App.vue
│   ├── main.ts
│   ├── pages.json                    # 页面路由配置
│   ├── manifest.json                 # 多端配置
│   └── uni.scss                      # 全局样式变量
├── package.json
└── vite.config.ts
```

### 条件编译 (一套代码多端运行)

```vue
<!-- 微信小程序专属 -->
<!-- #ifdef MP-WEIXIN -->
<button open-type="getPhoneNumber" @getphonenumber="onGetPhone">
  微信快速登录
</button>
<!-- #endif -->

<!-- H5 专属 -->
<!-- #ifdef H5 -->
<button @click="smsLogin">短信验证码登录</button>
<!-- #endif -->

<!-- App 专属 -->
<!-- #ifdef APP-PLUS -->
<button @click="biometricLogin">指纹/面容登录</button>
<!-- #endif -->
```

### 构建命令

```bash
# 微信小程序
pnpm --filter mobile dev:mp-weixin
pnpm --filter mobile build:mp-weixin

# H5
pnpm --filter mobile dev:h5
pnpm --filter mobile build:h5

# App (需要 HBuilderX 打包)
pnpm --filter mobile build:app
```

---

## Part 5: apps/desktop — Tauri 2 桌面端

### 目录结构

```
apps/desktop/
├── src/                              # Vue 3 前端 (复用 admin 大部分代码)
│   ├── App.vue
│   ├── main.ts
│   └── views/                        # 桌面端特有视图
│       ├── Tray.vue                  # 系统托盘面板
│       └── Settings.vue             # 桌面端设置
├── src-tauri/                        # Rust 后端
│   ├── src/
│   │   ├── main.rs                   # Tauri 入口
│   │   ├── commands.rs               # Rust ↔ Vue 桥接命令
│   │   └── tray.rs                   # 系统托盘
│   ├── Cargo.toml
│   ├── tauri.conf.json               # 窗口/权限/打包配置
│   └── icons/                        # 应用图标
├── package.json
└── vite.config.ts
```

### Tauri 独有能力

```typescript
// 系统通知 (比浏览器 Notification API 更原生)
import { sendNotification } from '@tauri-apps/plugin-notification'
await sendNotification({ title: '新订单', body: '客户张三下了一笔 ¥5,000 的订单' })

// 全局快捷键
import { register } from '@tauri-apps/plugin-global-shortcut'
await register('CmdOrCtrl+Shift+M', () => { /* 唤出 MateCloud */ })

// 系统托盘
import { TrayIcon } from '@tauri-apps/api/tray'
const tray = await TrayIcon.new({ icon: 'icons/tray.png', tooltip: 'MateCloud' })

// 本地数据库 (SQLite, 离线缓存)
import Database from '@tauri-apps/plugin-sql'
const db = await Database.load('sqlite:local.db')
```

---

## Part 6: packages/ui — 共享组件库

不重复造轮子。基于 Element Plus 做业务封装。

```
packages/ui/
├── src/
│   ├── MateTable/                    # 增强表格 (分页+搜索+CRUD)
│   │   ├── MateTable.vue
│   │   └── types.ts
│   ├── MateForm/                     # 增强表单 (JSON Schema 驱动)
│   │   ├── MateForm.vue
│   │   └── types.ts
│   ├── MateSearch/                   # 搜索栏 (条件折叠)
│   ├── MateDialog/                   # 增强对话框
│   ├── MateUpload/                   # 文件上传 (对接 mate-file-starter)
│   ├── MateChart/                    # 图表封装 (ECharts)
│   ├── MateEditor/                   # 富文本 (Tiptap)
│   └── index.ts                      # 统一导出
└── package.json
```

### MateTable 示例 (一句话出表格)

```vue
<template>
  <MateTable
    :api="userApi.list"
    :columns="columns"
    :searchFields="searchFields"
    row-key="id"
    @create="handleCreate"
    @edit="handleEdit"
    @delete="handleDelete"
  />
</template>

<script setup lang="ts">
const columns = [
  { prop: 'mobile', label: '手机号', width: 140 },
  { prop: 'nickName', label: '昵称' },
  { prop: 'status', label: '状态', dict: 'user_status' }, // 自动字典翻译
  { prop: 'createdAt', label: '创建时间', type: 'datetime' },
]

const searchFields = [
  { field: 'mobile', label: '手机号', type: 'input' },
  { field: 'status', label: '状态', type: 'select', dict: 'user_status' },
]
</script>
```

---

## Part 7: 子系统前端如何扩展

每个子系统 (CRM/Mall/OA) 的前端作为 **独立 package** 或 **独立仓库** 注入:

### 方式 1: Monorepo 内部 package (推荐初期)

```
mate-ui/
├── apps/admin/
├── packages/
│   ├── core/
│   ├── ui/
│   ├── app-crm/                    # CRM 前端模块
│   │   ├── views/
│   │   ├── api/
│   │   ├── routes.ts               # 导出路由配置
│   │   └── package.json
│   └── app-mall/                   # 商城前端模块
```

Admin 动态加载子系统路由:

```typescript
// apps/admin/src/router/apps.ts
const appModules = import.meta.glob('../../packages/app-*/routes.ts')

export async function loadAppRoutes(): Promise<RouteRecordRaw[]> {
  const routes: RouteRecordRaw[] = []
  for (const [path, loader] of Object.entries(appModules)) {
    const mod = await loader() as { default: RouteRecordRaw[] }
    routes.push(...mod.default)
  }
  return routes
}
```

### 方式 2: 独立仓库 + npm 包 (规模化后)

```bash
# 子系统前端独立仓库
npm install @matecloud/app-crm-ui
npm install @matecloud/app-mall-ui

# 自动注册路由和菜单
```

---

## Part 8: 先交付哪些

遵循 RFC-029 的聚焦原则——**先把这 3 个做好**:

### Wave 1: 管理后台 (admin)

```
交付物:
├── 登录页 (账号密码 + 短信验证码)
├── Dashboard (服务状态 + 今日数据)
├── 用户管理 (CRUD + 搜索 + 分页)
├── 角色管理 (CRUD + 菜单分配)
├── 菜单管理 (树形 CRUD)
├── 字典管理 (类型 + 数据)
└── 个人中心 (修改密码 + 头像)
```

### Wave 2: 共享基础 (core + ui)

```
交付物:
├── API 客户端 (axios 封装 + 拦截器)
├── Auth Store (登录/登出/Token/权限)
├── MateTable (一句话出表格)
├── MateForm (JSON Schema 表单)
├── MateSearch (搜索栏)
└── 权限指令 (v-permission)
```

### Wave 3: 移动端 (mobile) — 按业务需求

```
首发 H5 + 微信小程序。App 和桌面端后续。
```

---

## 技术决策速查

| 决策 | 选择 | 砍掉什么 |
|------|------|---------|
| 渲染模式 | SPA (admin) | 砍 SSR/Nuxt (管理后台不需要 SEO) |
| 跨端框架 | uni-app | 砍 Taro (Vue 迁移成本低 58%) |
| 桌面端 | Tauri 2 | 砍 Electron (体积大 25 倍) |
| CSS 方案 | Tailwind 4 + Element Plus | 砍自建设计系统 (不值得) |
| 状态管理 | Pinia | 砍 Vuex (已废弃) |
| 构建工具 | Vite 6 | 砍 Webpack (慢) |
| Monorepo | pnpm + Turborepo | 砍 Lerna/Nx (pnpm 最轻量) |
| 图表 | ECharts | 砍 Chart.js (功能弱) |
| 富文本 | Tiptap | 砍 CKEditor/Quill (Tiptap headless 最灵活) |
| HTTP | Axios | 砍 Fetch (拦截器生态成熟) |
