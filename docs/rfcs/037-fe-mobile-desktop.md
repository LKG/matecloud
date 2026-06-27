# RFC-037: Frontend Mobile (uni-app) + Desktop (Tauri 2)

- **Status**: Draft
- **Created**: 2026-04-12
- **Wave**: FE-4 (after admin CRUD pages are done)
- **Dependencies**: RFC-032 (skeleton), RFC-033 (core package), RFC-036 (admin pages)

## Scope

Two new apps in the monorepo:
- `mate-ui/apps/mobile/` -- uni-app for WeChat Mini Program + H5 + App
- `mate-ui/apps/desktop/` -- Tauri 2 + Vue 3 desktop application

---

# Part 1: apps/mobile (uni-app)

## 1.1 package.json

```json
{
  "name": "@matecloud/mobile",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "dev:h5": "uni -p h5",
    "dev:mp-weixin": "uni -p mp-weixin",
    "dev:mp-alipay": "uni -p mp-alipay",
    "dev:app": "uni -p app",
    "build:h5": "uni build -p h5",
    "build:mp-weixin": "uni build -p mp-weixin",
    "build:mp-alipay": "uni build -p mp-alipay",
    "build:app": "uni build -p app"
  },
  "dependencies": {
    "@dcloudio/uni-app": "3.0.0-4060620250520001",
    "@dcloudio/uni-app-plus": "3.0.0-4060620250520001",
    "@dcloudio/uni-components": "3.0.0-4060620250520001",
    "@dcloudio/uni-h5": "3.0.0-4060620250520001",
    "@dcloudio/uni-mp-weixin": "3.0.0-4060620250520001",
    "@dcloudio/uni-mp-alipay": "3.0.0-4060620250520001",
    "vue": "^3.5.0",
    "pinia": "^3.0.0",
    "pinia-plugin-persistedstate": "^4.0.0"
  },
  "devDependencies": {
    "@dcloudio/types": "^3.4.0",
    "@dcloudio/uni-automator": "3.0.0-4060620250520001",
    "@dcloudio/uni-cli-shared": "3.0.0-4060620250520001",
    "@dcloudio/vite-plugin-uni": "3.0.0-4060620250520001",
    "typescript": "^5.7.0",
    "vite": "^6.0.0",
    "@matecloud/tsconfig": "workspace:*"
  }
}
```

## 1.2 vite.config.ts

```typescript
// apps/mobile/vite.config.ts
import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  server: {
    port: 3001,
    proxy: {
      '/api': {
        target: 'http://localhost:9010',
        changeOrigin: true,
      },
    },
  },
})
```

## 1.3 tsconfig.json

```json
{
  "extends": "@dcloudio/types",
  "compilerOptions": {
    "target": "ES2022",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "strict": true,
    "jsx": "preserve",
    "sourceMap": true,
    "resolveJsonModule": true,
    "esModuleInterop": true,
    "lib": ["ES2022", "DOM"],
    "types": ["@dcloudio/types"],
    "paths": {
      "@/*": ["./src/*"]
    }
  },
  "include": ["src/**/*.ts", "src/**/*.vue"],
  "exclude": ["node_modules", "dist", "unpackage"]
}
```

## 1.4 src/manifest.json

```json
{
  "name": "MateCloud",
  "appid": "__UNI__MATECLOUD",
  "description": "MateCloud Mobile",
  "versionName": "1.0.0",
  "versionCode": "100",
  "transformPx": false,
  "h5": {
    "title": "MateCloud",
    "router": {
      "mode": "hash",
      "base": "/"
    },
    "devServer": {
      "port": 3001,
      "proxy": {
        "/api": {
          "target": "http://localhost:9010",
          "changeOrigin": true
        }
      }
    },
    "optimization": {
      "treeShaking": {
        "enable": true
      }
    }
  },
  "mp-weixin": {
    "appid": "wx_your_appid_here",
    "setting": {
      "urlCheck": false,
      "es6": true,
      "minified": true,
      "postcss": true
    },
    "usingComponents": true,
    "lazyCodeLoading": "requiredComponents"
  },
  "mp-alipay": {
    "appid": "your_alipay_appid_here"
  },
  "app-plus": {
    "distribute": {
      "android": {
        "permissions": [
          "<uses-permission android:name=\"android.permission.INTERNET\"/>"
        ]
      },
      "ios": {}
    },
    "modules": {},
    "splashscreen": {
      "alwaysShowBeforeRender": true,
      "autoclose": false
    }
  }
}
```

## 1.5 src/pages.json

```json
{
  "pages": [
    {
      "path": "pages/index/index",
      "style": {
        "navigationBarTitleText": "首页",
        "enablePullDownRefresh": true
      }
    },
    {
      "path": "pages/login/login",
      "style": {
        "navigationBarTitleText": "登录",
        "navigationStyle": "custom"
      }
    },
    {
      "path": "pages/mine/mine",
      "style": {
        "navigationBarTitleText": "我的",
        "enablePullDownRefresh": false
      }
    }
  ],
  "globalStyle": {
    "navigationBarTextStyle": "black",
    "navigationBarTitleText": "MateCloud",
    "navigationBarBackgroundColor": "#ffffff",
    "backgroundColor": "#f5f5f5",
    "backgroundTextStyle": "dark"
  },
  "tabBar": {
    "color": "#999999",
    "selectedColor": "#409eff",
    "borderStyle": "black",
    "backgroundColor": "#ffffff",
    "list": [
      {
        "pagePath": "pages/index/index",
        "text": "首页",
        "iconPath": "static/tabbar/home.png",
        "selectedIconPath": "static/tabbar/home-active.png"
      },
      {
        "pagePath": "pages/mine/mine",
        "text": "我的",
        "iconPath": "static/tabbar/mine.png",
        "selectedIconPath": "static/tabbar/mine-active.png"
      }
    ]
  },
  "easycom": {
    "autoscan": true,
    "custom": {}
  }
}
```

## 1.6 src/utils/request.ts

```typescript
// apps/mobile/src/utils/request.ts

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

export interface Result<T = unknown> {
  code: string
  msg: string
  success: boolean
  data: T
}

export interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: Record<string, unknown>
  params?: Record<string, unknown>
  header?: Record<string, string>
  showLoading?: boolean
  loadingText?: string
}

function getToken(): string {
  return uni.getStorageSync('token') || ''
}

function getTenantId(): string {
  return uni.getStorageSync('tenantId') || ''
}

function buildUrl(url: string, params?: Record<string, unknown>): string {
  if (!params) return url
  const qs = Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== null && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
    .join('&')
  return qs ? `${url}?${qs}` : url
}

export function request<T = unknown>(options: RequestOptions): Promise<Result<T>> {
  const {
    url,
    method = 'GET',
    data,
    params,
    header = {},
    showLoading = false,
    loadingText = '加载中...',
  } = options

  if (showLoading) {
    uni.showLoading({ title: loadingText, mask: true })
  }

  const token = getToken()
  const tenantId = getTenantId()

  const finalHeader: Record<string, string> = {
    'Content-Type': 'application/json',
    ...header,
  }

  if (token) {
    finalHeader['Authorization'] = `Bearer ${token}`
  }
  if (tenantId) {
    finalHeader['X-Tenant-Id'] = tenantId
  }

  const fullUrl = buildUrl(`${BASE_URL}${url}`, params)

  return new Promise((resolve, reject) => {
    uni.request({
      url: fullUrl,
      method,
      data,
      header: finalHeader,
      success: (res) => {
        if (showLoading) uni.hideLoading()

        const statusCode = res.statusCode
        if (statusCode === 401) {
          uni.removeStorageSync('token')
          uni.removeStorageSync('user')
          uni.reLaunch({ url: '/pages/login/login' })
          reject(new Error('登录已过期，请重新登录'))
          return
        }

        if (statusCode < 200 || statusCode >= 300) {
          const msg = (res.data as Record<string, string>)?.msg || `请求失败(${statusCode})`
          uni.showToast({ title: msg, icon: 'none' })
          reject(new Error(msg))
          return
        }

        const result = res.data as Result<T>
        if (!result.success) {
          uni.showToast({ title: result.msg || '操作失败', icon: 'none' })
          reject(new Error(result.msg))
          return
        }

        resolve(result)
      },
      fail: (err) => {
        if (showLoading) uni.hideLoading()
        uni.showToast({ title: '网络异常，请稍后重试', icon: 'none' })
        reject(err)
      },
    })
  })
}

// Convenience methods
export const http = {
  get: <T>(url: string, params?: Record<string, unknown>, opts?: Partial<RequestOptions>) =>
    request<T>({ url, method: 'GET', params, ...opts }),

  post: <T>(url: string, data?: Record<string, unknown>, opts?: Partial<RequestOptions>) =>
    request<T>({ url, method: 'POST', data, ...opts }),

  put: <T>(url: string, data?: Record<string, unknown>, opts?: Partial<RequestOptions>) =>
    request<T>({ url, method: 'PUT', data, ...opts }),

  del: <T>(url: string, opts?: Partial<RequestOptions>) =>
    request<T>({ url, method: 'DELETE', ...opts }),
}
```

## 1.7 src/store/auth.ts

```typescript
// apps/mobile/src/store/auth.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { http } from '../utils/request'

export interface MobileUserInfo {
  id: string
  mobile: string
  nickName: string
  avatar?: string
}

interface LoginResponse {
  token: string
}

export const useAuthStore = defineStore(
  'mobile-auth',
  () => {
    const token = ref('')
    const user = ref<MobileUserInfo | null>(null)

    const isLoggedIn = computed(() => !!token.value)

    async function sendSmsCode(mobile: string) {
      await http.post('/auth/sms/send', { mobile })
    }

    async function smsLogin(mobile: string, code: string) {
      const res = await http.post<LoginResponse>('/auth/sms/login', {
        mobile,
        code,
      })
      token.value = res.data.token
      uni.setStorageSync('token', res.data.token)
      await fetchUserInfo()
    }

    async function fetchUserInfo() {
      const res = await http.get<MobileUserInfo>('/auth/info')
      user.value = res.data
      uni.setStorageSync('user', JSON.stringify(res.data))
    }

    function logout() {
      token.value = ''
      user.value = null
      uni.removeStorageSync('token')
      uni.removeStorageSync('user')
      uni.reLaunch({ url: '/pages/login/login' })
    }

    // Restore from storage on init
    function restore() {
      const savedToken = uni.getStorageSync('token')
      if (savedToken) {
        token.value = savedToken
      }
      const savedUser = uni.getStorageSync('user')
      if (savedUser) {
        try {
          user.value = JSON.parse(savedUser) as MobileUserInfo
        } catch {
          user.value = null
        }
      }
    }

    return {
      token,
      user,
      isLoggedIn,
      sendSmsCode,
      smsLogin,
      fetchUserInfo,
      logout,
      restore,
    }
  },
)
```

## 1.8 src/App.vue

```vue
<!-- apps/mobile/src/App.vue -->
<script setup lang="ts">
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import { useAuthStore } from './store/auth'

onLaunch(() => {
  console.log('App Launch')
  const authStore = useAuthStore()
  authStore.restore()
})

onShow(() => {
  console.log('App Show')
})

onHide(() => {
  console.log('App Hide')
})
</script>

<style>
/* Global styles */
page {
  background-color: #f5f5f5;
  font-family:
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    Roboto,
    'Helvetica Neue',
    Arial,
    sans-serif;
  font-size: 28rpx;
  color: #333333;
}

.container {
  padding: 30rpx;
}

/* Safe area bottom padding for iPhone X+ */
.safe-area-bottom {
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}
</style>
```

## 1.9 src/main.ts

```typescript
// apps/mobile/src/main.ts
import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'

export function createApp() {
  const app = createSSRApp(App)

  const pinia = createPinia()
  app.use(pinia)

  return {
    app,
  }
}
```

## 1.10 src/pages/login/login.vue

```vue
<!-- apps/mobile/src/pages/login/login.vue -->
<template>
  <view class="login-page">
    <!-- Custom navigation bar area (navigationStyle=custom) -->
    <view class="status-bar" :style="{ height: statusBarHeight + 'px' }" />

    <view class="login-content">
      <!-- Logo -->
      <view class="logo-area">
        <image
          class="logo"
          src="/static/logo.png"
          mode="aspectFit"
        />
        <text class="app-name">MateCloud</text>
        <text class="app-desc">企业级微服务管理平台</text>
      </view>

      <!-- Login Form -->
      <view class="form-area">
        <view class="input-group">
          <view class="input-item">
            <text class="input-icon">&#x1F4F1;</text>
            <input
              v-model="mobile"
              type="number"
              placeholder="请输入手机号"
              maxlength="11"
              class="input-field"
            />
          </view>

          <view class="input-item">
            <text class="input-icon">&#x1F512;</text>
            <input
              v-model="smsCode"
              type="number"
              placeholder="请输入验证码"
              maxlength="6"
              class="input-field"
            />
            <view
              class="sms-btn"
              :class="{ disabled: countdown > 0 || !isMobileValid }"
              @tap="handleSendCode"
            >
              <text class="sms-btn-text">
                {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
              </text>
            </view>
          </view>
        </view>

        <button
          class="login-btn"
          :class="{ disabled: !canLogin }"
          :loading="loginLoading"
          @tap="handleLogin"
        >
          登 录
        </button>

        <!-- WeChat Quick Login (WeChat Mini Program only) -->
        <!-- #ifdef MP-WEIXIN -->
        <view class="divider">
          <view class="divider-line" />
          <text class="divider-text">其他登录方式</text>
          <view class="divider-line" />
        </view>
        <button class="wechat-btn" open-type="getPhoneNumber" @getphonenumber="onGetPhoneNumber">
          微信快速登录
        </button>
        <!-- #endif -->
      </view>

      <!-- Agreement -->
      <view class="agreement">
        <text class="agreement-text">
          登录即表示同意
          <text class="link" @tap="openPrivacy">《隐私政策》</text>
          和
          <text class="link" @tap="openTerms">《服务协议》</text>
        </text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAuthStore } from '../../store/auth'

const authStore = useAuthStore()

// Status bar height for custom nav
const statusBarHeight = ref(0)
try {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 0
} catch {
  statusBarHeight.value = 44
}

// Form state
const mobile = ref('')
const smsCode = ref('')
const loginLoading = ref(false)
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const isMobileValid = computed(() => /^1[3-9]\d{9}$/.test(mobile.value))
const canLogin = computed(() => isMobileValid.value && smsCode.value.length === 6)

// Send SMS code
async function handleSendCode() {
  if (countdown.value > 0 || !isMobileValid.value) return

  try {
    await authStore.sendSmsCode(mobile.value)
    uni.showToast({ title: '验证码已发送', icon: 'success' })
    startCountdown()
  } catch {
    uni.showToast({ title: '发送失败，请稍后重试', icon: 'none' })
  }
}

function startCountdown() {
  countdown.value = 60
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      if (countdownTimer) clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

// Login
async function handleLogin() {
  if (!canLogin.value || loginLoading.value) return

  loginLoading.value = true
  try {
    await authStore.smsLogin(mobile.value, smsCode.value)
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      uni.switchTab({ url: '/pages/index/index' })
    }, 500)
  } catch {
    uni.showToast({ title: '登录失败，请检查验证码', icon: 'none' })
  } finally {
    loginLoading.value = false
  }
}

// WeChat phone number login (MP only)
// eslint-disable-next-line @typescript-eslint/no-explicit-any
function onGetPhoneNumber(e: any) {
  if (e.detail.errMsg !== 'getPhoneNumber:ok') {
    uni.showToast({ title: '已取消授权', icon: 'none' })
    return
  }
  // Send e.detail.code to backend for WeChat phone auth
  console.log('WeChat phone code:', e.detail.code)
  uni.showToast({ title: '微信登录开发中', icon: 'none' })
}

function openPrivacy() {
  uni.navigateTo({ url: '/pages/webview/webview?url=https://matecloud.vip/privacy' })
}

function openTerms() {
  uni.navigateTo({ url: '/pages/webview/webview?url=https://matecloud.vip/terms' })
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #e8f4fd 0%, #ffffff 40%);
}

.login-content {
  padding: 0 60rpx;
}

.logo-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 120rpx;
  padding-bottom: 80rpx;
}

.logo {
  width: 160rpx;
  height: 160rpx;
  margin-bottom: 20rpx;
}

.app-name {
  font-size: 48rpx;
  font-weight: bold;
  color: #333333;
  margin-bottom: 10rpx;
}

.app-desc {
  font-size: 26rpx;
  color: #999999;
}

.form-area {
  margin-top: 40rpx;
}

.input-group {
  background: #ffffff;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.input-item {
  display: flex;
  align-items: center;
  padding: 0 30rpx;
  height: 100rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.input-item:last-child {
  border-bottom: none;
}

.input-icon {
  font-size: 36rpx;
  margin-right: 20rpx;
  width: 40rpx;
  text-align: center;
}

.input-field {
  flex: 1;
  height: 100rpx;
  font-size: 30rpx;
  color: #333333;
}

.sms-btn {
  padding: 12rpx 24rpx;
  background: #409eff;
  border-radius: 30rpx;
  white-space: nowrap;
}

.sms-btn.disabled {
  background: #cccccc;
}

.sms-btn-text {
  font-size: 24rpx;
  color: #ffffff;
}

.login-btn {
  margin-top: 60rpx;
  height: 96rpx;
  line-height: 96rpx;
  background: #409eff;
  color: #ffffff;
  font-size: 34rpx;
  font-weight: bold;
  border-radius: 48rpx;
  border: none;
  text-align: center;
}

.login-btn.disabled {
  background: #b3d8ff;
}

.login-btn::after {
  border: none;
}

.divider {
  display: flex;
  align-items: center;
  margin: 60rpx 0 40rpx;
}

.divider-line {
  flex: 1;
  height: 1rpx;
  background: #e0e0e0;
}

.divider-text {
  padding: 0 20rpx;
  font-size: 24rpx;
  color: #999999;
}

.wechat-btn {
  height: 96rpx;
  line-height: 96rpx;
  background: #07c160;
  color: #ffffff;
  font-size: 34rpx;
  font-weight: bold;
  border-radius: 48rpx;
  border: none;
  text-align: center;
}

.wechat-btn::after {
  border: none;
}

.agreement {
  position: fixed;
  bottom: 60rpx;
  left: 0;
  right: 0;
  text-align: center;
}

.agreement-text {
  font-size: 22rpx;
  color: #999999;
}

.link {
  color: #409eff;
}
</style>
```

## 1.11 src/pages/index/index.vue

```vue
<!-- apps/mobile/src/pages/index/index.vue -->
<template>
  <view class="index-page">
    <!-- Header -->
    <view class="header">
      <view class="header-top">
        <text class="greeting">{{ greetingText }}</text>
        <text class="user-name">{{ userName }}</text>
      </view>
    </view>

    <!-- Quick Actions Grid -->
    <view class="section">
      <view class="section-title">快捷操作</view>
      <view class="action-grid">
        <view
          v-for="action in quickActions"
          :key="action.id"
          class="action-item"
          @tap="handleAction(action)"
        >
          <view class="action-icon" :style="{ background: action.color }">
            <text class="action-icon-text">{{ action.icon }}</text>
          </view>
          <text class="action-label">{{ action.label }}</text>
        </view>
      </view>
    </view>

    <!-- Statistics Cards -->
    <view class="section">
      <view class="section-title">今日数据</view>
      <view class="stats-row">
        <view class="stat-card">
          <text class="stat-value">{{ stats.todayOrders }}</text>
          <text class="stat-label">今日订单</text>
        </view>
        <view class="stat-card">
          <text class="stat-value">{{ stats.todayUsers }}</text>
          <text class="stat-label">新增用户</text>
        </view>
        <view class="stat-card">
          <text class="stat-value">{{ stats.todayRevenue }}</text>
          <text class="stat-label">今日收入</text>
        </view>
      </view>
    </view>

    <!-- Recent Notifications -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">最新通知</text>
        <text class="section-more" @tap="goToNotifications">查看全部</text>
      </view>
      <view class="notice-list">
        <view
          v-for="notice in notices"
          :key="notice.id"
          class="notice-item"
          @tap="goToNoticeDetail(notice.id)"
        >
          <view class="notice-dot" :class="{ unread: !notice.read }" />
          <view class="notice-content">
            <text class="notice-title">{{ notice.title }}</text>
            <text class="notice-time">{{ notice.time }}</text>
          </view>
        </view>
        <view v-if="notices.length === 0" class="empty-text">
          <text>暂无通知</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { useAuthStore } from '../../store/auth'

const authStore = useAuthStore()

const userName = computed(() => authStore.user?.nickName || '用户')

const greetingText = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了,'
  if (hour < 12) return '上午好,'
  if (hour < 14) return '中午好,'
  if (hour < 18) return '下午好,'
  return '晚上好,'
})

// Quick actions
interface QuickAction {
  id: string
  icon: string
  label: string
  color: string
  path?: string
}

const quickActions = ref<QuickAction[]>([
  { id: 'user', icon: '👤', label: '用户管理', color: '#409eff', path: '/pages/index/index' },
  { id: 'order', icon: '📋', label: '订单查询', color: '#67c23a', path: '/pages/index/index' },
  { id: 'notice', icon: '🔔', label: '消息通知', color: '#e6a23c', path: '/pages/index/index' },
  { id: 'report', icon: '📊', label: '数据报表', color: '#f56c6c', path: '/pages/index/index' },
  { id: 'approval', icon: '✅', label: '审批中心', color: '#909399', path: '/pages/index/index' },
  { id: 'settings', icon: '⚙️', label: '系统设置', color: '#8e44ad', path: '/pages/index/index' },
  { id: 'log', icon: '📝', label: '操作日志', color: '#2ecc71', path: '/pages/index/index' },
  { id: 'help', icon: '❓', label: '帮助中心', color: '#3498db', path: '/pages/index/index' },
])

function handleAction(action: QuickAction) {
  if (action.path) {
    uni.navigateTo({ url: action.path }).catch(() => {
      uni.switchTab({ url: action.path! })
    })
  }
}

// Statistics
const stats = ref({
  todayOrders: '0',
  todayUsers: '0',
  todayRevenue: '0',
})

// Notifications
interface Notice {
  id: string
  title: string
  time: string
  read: boolean
}

const notices = ref<Notice[]>([
  { id: '1', title: '系统升级通知：v2.0 将于本周五发布', time: '10分钟前', read: false },
  { id: '2', title: '新功能上线：支持微信快捷登录', time: '1小时前', read: false },
  { id: '3', title: '安全提醒：请定期修改密码', time: '昨天', read: true },
])

function goToNotifications() {
  uni.showToast({ title: '通知列表开发中', icon: 'none' })
}

function goToNoticeDetail(id: string) {
  console.log('Navigate to notice:', id)
  uni.showToast({ title: '通知详情开发中', icon: 'none' })
}

// Fetch data
async function loadData() {
  // In production, fetch from API:
  // const res = await http.get('/dashboard/stats')
  // stats.value = res.data
  stats.value = {
    todayOrders: '128',
    todayUsers: '36',
    todayRevenue: '12,580',
  }
}

onShow(() => {
  if (!authStore.isLoggedIn) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  loadData()
})

onPullDownRefresh(() => {
  loadData().finally(() => {
    uni.stopPullDownRefresh()
  })
})
</script>

<style scoped>
.index-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.header {
  background: linear-gradient(135deg, #409eff, #66b1ff);
  padding: 40rpx 30rpx 50rpx;
  border-radius: 0 0 30rpx 30rpx;
}

.header-top {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
}

.greeting {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.85);
}

.user-name {
  font-size: 36rpx;
  font-weight: bold;
  color: #ffffff;
}

.section {
  margin: 30rpx;
  background: #ffffff;
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333333;
  margin-bottom: 24rpx;
}

.section-more {
  font-size: 24rpx;
  color: #409eff;
}

/* Action Grid */
.action-grid {
  display: flex;
  flex-wrap: wrap;
}

.action-item {
  width: 25%;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 30rpx;
}

.action-icon {
  width: 96rpx;
  height: 96rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12rpx;
}

.action-icon-text {
  font-size: 40rpx;
}

.action-label {
  font-size: 24rpx;
  color: #666666;
}

/* Stats */
.stats-row {
  display: flex;
  gap: 20rpx;
}

.stat-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx 0;
  background: #f8fafc;
  border-radius: 12rpx;
}

.stat-value {
  font-size: 40rpx;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 8rpx;
}

.stat-label {
  font-size: 22rpx;
  color: #999999;
}

/* Notices */
.notice-list {
  margin-top: -4rpx;
}

.notice-item {
  display: flex;
  align-items: flex-start;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.notice-item:last-child {
  border-bottom: none;
}

.notice-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: #cccccc;
  margin-top: 10rpx;
  margin-right: 16rpx;
  flex-shrink: 0;
}

.notice-dot.unread {
  background: #f56c6c;
}

.notice-content {
  flex: 1;
}

.notice-title {
  font-size: 28rpx;
  color: #333333;
  display: block;
  margin-bottom: 6rpx;
}

.notice-time {
  font-size: 22rpx;
  color: #999999;
}

.empty-text {
  text-align: center;
  padding: 40rpx 0;
  color: #999999;
  font-size: 26rpx;
}
</style>
```

## 1.12 src/pages/mine/mine.vue

```vue
<!-- apps/mobile/src/pages/mine/mine.vue -->
<template>
  <view class="mine-page">
    <!-- User Card -->
    <view class="user-card">
      <view class="user-info" @tap="goToProfile">
        <image
          class="avatar"
          :src="avatarUrl"
          mode="aspectFill"
        />
        <view class="user-detail">
          <text class="nick-name">{{ authStore.user?.nickName || '未登录' }}</text>
          <text class="mobile">{{ maskedMobile }}</text>
        </view>
        <text class="arrow">&#x276F;</text>
      </view>
    </view>

    <!-- Menu List -->
    <view class="menu-section">
      <view
        v-for="item in menuItems"
        :key="item.id"
        class="menu-item"
        @tap="handleMenuTap(item)"
      >
        <view class="menu-left">
          <text class="menu-icon">{{ item.icon }}</text>
          <text class="menu-label">{{ item.label }}</text>
        </view>
        <view class="menu-right">
          <text v-if="item.badge" class="menu-badge">{{ item.badge }}</text>
          <text class="arrow">&#x276F;</text>
        </view>
      </view>
    </view>

    <!-- Settings List -->
    <view class="menu-section">
      <view
        v-for="item in settingsItems"
        :key="item.id"
        class="menu-item"
        @tap="handleMenuTap(item)"
      >
        <view class="menu-left">
          <text class="menu-icon">{{ item.icon }}</text>
          <text class="menu-label">{{ item.label }}</text>
        </view>
        <view class="menu-right">
          <text class="arrow">&#x276F;</text>
        </view>
      </view>
    </view>

    <!-- Logout -->
    <view v-if="authStore.isLoggedIn" class="logout-section">
      <button class="logout-btn" @tap="handleLogout">退出登录</button>
    </view>

    <!-- Version -->
    <view class="version-text">
      <text>MateCloud v1.0.0</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAuthStore } from '../../store/auth'

const authStore = useAuthStore()

const avatarUrl = computed(() => authStore.user?.avatar || '/static/default-avatar.png')

const maskedMobile = computed(() => {
  const m = authStore.user?.mobile || ''
  if (m.length === 11) {
    return `${m.slice(0, 3)}****${m.slice(7)}`
  }
  return m
})

interface MenuItemDef {
  id: string
  icon: string
  label: string
  path?: string
  badge?: string
  action?: string
}

const menuItems = ref<MenuItemDef[]>([
  { id: 'profile', icon: '👤', label: '个人资料', path: '/pages/mine/mine' },
  { id: 'notification', icon: '🔔', label: '消息通知', path: '/pages/mine/mine', badge: '3' },
  { id: 'password', icon: '🔒', label: '修改密码', path: '/pages/mine/mine' },
  { id: 'collect', icon: '⭐', label: '我的收藏', path: '/pages/mine/mine' },
])

const settingsItems = ref<MenuItemDef[]>([
  { id: 'language', icon: '🌐', label: '语言设置', path: '/pages/mine/mine' },
  { id: 'cache', icon: '🗑️', label: '清除缓存', action: 'clearCache' },
  { id: 'about', icon: 'ℹ️', label: '关于我们', path: '/pages/mine/mine' },
  { id: 'feedback', icon: '💬', label: '意见反馈', path: '/pages/mine/mine' },
])

function goToProfile() {
  if (!authStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/login/login' })
    return
  }
  uni.showToast({ title: '个人资料开发中', icon: 'none' })
}

function handleMenuTap(item: MenuItemDef) {
  if (item.action === 'clearCache') {
    uni.showModal({
      title: '提示',
      content: '确认清除本地缓存？',
      success: (res) => {
        if (res.confirm) {
          uni.clearStorageSync()
          authStore.restore() // Keep auth data
          uni.showToast({ title: '缓存已清除', icon: 'success' })
        }
      },
    })
    return
  }
  if (item.path) {
    uni.showToast({ title: `${item.label}开发中`, icon: 'none' })
  }
}

function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确认退出登录？',
    success: (res) => {
      if (res.confirm) {
        authStore.logout()
      }
    },
  })
}

onShow(() => {
  if (!authStore.isLoggedIn) {
    // Allow viewing, but show login prompt for sensitive actions
  }
})
</script>

<style scoped>
.mine-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.user-card {
  background: linear-gradient(135deg, #409eff, #66b1ff);
  padding: 60rpx 30rpx 40rpx;
}

.user-info {
  display: flex;
  align-items: center;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.5);
  margin-right: 24rpx;
  background: #ffffff;
}

.user-detail {
  flex: 1;
}

.nick-name {
  font-size: 36rpx;
  font-weight: bold;
  color: #ffffff;
  display: block;
  margin-bottom: 8rpx;
}

.mobile {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
}

.arrow {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.6);
}

.menu-section {
  margin: 24rpx 30rpx;
  background: #ffffff;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx;
  border-bottom: 1rpx solid #f5f5f5;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-left {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.menu-icon {
  font-size: 36rpx;
  width: 44rpx;
  text-align: center;
}

.menu-label {
  font-size: 30rpx;
  color: #333333;
}

.menu-right {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.menu-right .arrow {
  color: #cccccc;
  font-size: 24rpx;
}

.menu-badge {
  background: #f56c6c;
  color: #ffffff;
  font-size: 20rpx;
  padding: 2rpx 12rpx;
  border-radius: 20rpx;
  min-width: 32rpx;
  text-align: center;
}

.logout-section {
  margin: 40rpx 30rpx;
}

.logout-btn {
  height: 88rpx;
  line-height: 88rpx;
  background: #ffffff;
  color: #f56c6c;
  font-size: 32rpx;
  border-radius: 20rpx;
  border: none;
  text-align: center;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.logout-btn::after {
  border: none;
}

.version-text {
  text-align: center;
  padding: 30rpx 0 60rpx;
  font-size: 22rpx;
  color: #cccccc;
}
</style>
```

## 1.13 Static Assets (placeholder note)

The following placeholder files are needed in `apps/mobile/src/static/`:

```
static/
├── logo.png                    # App logo (256x256)
├── default-avatar.png          # Default avatar (128x128)
└── tabbar/
    ├── home.png                # Home tab icon (81x81, gray)
    ├── home-active.png         # Home tab icon (81x81, blue #409eff)
    ├── mine.png                # Mine tab icon (81x81, gray)
    └── mine-active.png         # Mine tab icon (81x81, blue #409eff)
```

These are standard icon assets; any 81x81px PNG icons will work.

---

# Part 2: apps/desktop (Tauri 2)

## 2.1 package.json

```json
{
  "name": "@matecloud/desktop",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "dev": "tauri dev",
    "build": "tauri build",
    "tauri": "tauri",
    "vite:dev": "vite",
    "vite:build": "vue-tsc --noEmit && vite build"
  },
  "dependencies": {
    "vue": "^3.5.0",
    "vue-router": "^4.5.0",
    "pinia": "^3.0.0",
    "pinia-plugin-persistedstate": "^4.0.0",
    "element-plus": "^2.9.0",
    "@element-plus/icons-vue": "^2.3.0",
    "axios": "^1.7.0",
    "@vueuse/core": "^12.0.0",
    "@tauri-apps/api": "^2.2.0",
    "@tauri-apps/plugin-notification": "^2.2.0",
    "@tauri-apps/plugin-global-shortcut": "^2.2.0",
    "@tauri-apps/plugin-shell": "^2.2.0",
    "@matecloud/core": "workspace:*",
    "@matecloud/ui": "workspace:*",
    "@matecloud/hooks": "workspace:*",
    "@matecloud/utils": "workspace:*"
  },
  "devDependencies": {
    "@tauri-apps/cli": "^2.2.0",
    "@vitejs/plugin-vue": "^5.2.0",
    "vite": "^6.0.0",
    "vue-tsc": "^2.2.0",
    "unocss": "^0.65.0",
    "@unocss/preset-wind": "^0.65.0",
    "unplugin-auto-import": "^0.19.0",
    "unplugin-vue-components": "^0.28.0",
    "typescript": "^5.7.0",
    "@matecloud/eslint-config": "workspace:*",
    "@matecloud/tsconfig": "workspace:*"
  }
}
```

## 2.2 vite.config.ts

```typescript
// apps/desktop/vite.config.ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import UnoCSS from 'unocss/vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { resolve } from 'path'

// Tauri expects a fixed port during dev
const host = process.env.TAURI_DEV_HOST

export default defineConfig({
  plugins: [
    vue(),
    UnoCSS(),
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia', '@vueuse/core'],
      resolvers: [ElementPlusResolver()],
      dts: 'src/auto-imports.d.ts',
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts',
    }),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  // Prevent vite from obscuring Rust errors
  clearScreen: false,
  server: {
    port: 3002,
    strictPort: true,
    host: host || false,
    hmr: host
      ? {
          protocol: 'ws',
          host,
          port: 3002,
        }
      : undefined,
    proxy: {
      '/api': {
        target: 'http://localhost:9010',
        changeOrigin: true,
      },
    },
  },
  // Env prefix for Tauri
  envPrefix: ['VITE_', 'TAURI_'],
  build: {
    // Tauri uses Chromium on Windows and WebKit on macOS/Linux
    target: process.env.TAURI_ENV_PLATFORM === 'windows' ? 'chrome105' : 'safari14',
    // Don't minify for debug builds
    minify: !process.env.TAURI_ENV_DEBUG ? 'esbuild' : false,
    // Produce sourcemaps for debug builds
    sourcemap: !!process.env.TAURI_ENV_DEBUG,
  },
})
```

## 2.3 tsconfig.json

```json
{
  "extends": "../../tooling/tsconfig/vue.json",
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"]
    }
  },
  "include": ["src/**/*.ts", "src/**/*.vue", "src/**/*.d.ts"],
  "exclude": ["node_modules", "dist", "src-tauri"]
}
```

## 2.4 index.html

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>MateCloud Desktop</title>
</head>
<body>
  <div id="app"></div>
  <script type="module" src="/src/main.ts"></script>
</body>
</html>
```

## 2.5 uno.config.ts

```typescript
// apps/desktop/uno.config.ts
import { defineConfig, presetWind4, presetIcons } from 'unocss'

export default defineConfig({
  presets: [
    presetWind4(),
    presetIcons({ scale: 1.2 }),
  ],
  theme: {
    colors: {
      primary: '#409eff',
    },
  },
})
```

## 2.6 src-tauri/Cargo.toml

```toml
[package]
name = "matecloud-desktop"
version = "1.0.0"
description = "MateCloud Desktop Application"
authors = ["MateCloud Team"]
edition = "2021"

[lib]
name = "matecloud_desktop_lib"
crate-type = ["lib", "cdylib", "staticlib"]

[build-dependencies]
tauri-build = { version = "2", features = [] }

[dependencies]
tauri = { version = "2", features = ["tray-icon"] }
tauri-plugin-opener = "2"
tauri-plugin-notification = "2"
tauri-plugin-global-shortcut = "2"
tauri-plugin-shell = "2"
serde = { version = "1", features = ["derive"] }
serde_json = "1"
sysinfo = "0.32"
```

## 2.7 src-tauri/build.rs

```rust
// apps/desktop/src-tauri/build.rs
fn main() {
    tauri_build::build()
}
```

## 2.8 src-tauri/tauri.conf.json

```json
{
  "$schema": "https://raw.githubusercontent.com/nicegui/nice-src/main/lib/tauri-conf-v2.schema.json",
  "productName": "MateCloud",
  "version": "1.0.0",
  "identifier": "vip.mate.desktop",
  "build": {
    "frontendDist": "../dist",
    "devUrl": "http://localhost:3002",
    "beforeDevCommand": "pnpm vite:dev",
    "beforeBuildCommand": "pnpm vite:build"
  },
  "app": {
    "windows": [
      {
        "label": "main",
        "title": "MateCloud",
        "width": 1280,
        "height": 800,
        "minWidth": 900,
        "minHeight": 600,
        "resizable": true,
        "fullscreen": false,
        "decorations": true,
        "center": true,
        "transparent": false
      }
    ],
    "trayIcon": {
      "iconPath": "icons/icon.png",
      "iconAsTemplate": true,
      "tooltip": "MateCloud Desktop"
    },
    "security": {
      "csp": null
    }
  },
  "bundle": {
    "active": true,
    "icon": [
      "icons/32x32.png",
      "icons/128x128.png",
      "icons/128x128@2x.png",
      "icons/icon.icns",
      "icons/icon.ico"
    ],
    "targets": "all",
    "windows": {
      "wix": {
        "language": "zh-CN"
      }
    },
    "macOS": {
      "minimumSystemVersion": "10.15"
    },
    "linux": {
      "deb": {
        "depends": []
      }
    }
  },
  "plugins": {
    "notification": {
      "enabled": true
    },
    "global-shortcut": {
      "enabled": true
    },
    "shell": {
      "open": true
    }
  }
}
```

## 2.9 src-tauri/src/lib.rs

```rust
// apps/desktop/src-tauri/src/lib.rs
mod commands;

use tauri::{
    menu::{MenuBuilder, MenuItemBuilder},
    tray::TrayIconBuilder,
    Manager,
};

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .plugin(tauri_plugin_notification::init())
        .plugin(tauri_plugin_global_shortcut::init())
        .plugin(tauri_plugin_shell::init())
        .invoke_handler(tauri::generate_handler![
            commands::greet,
            commands::get_system_info,
        ])
        .setup(|app| {
            // Build tray menu
            let show_item = MenuItemBuilder::new("显示主窗口")
                .id("show")
                .build(app)?;
            let quit_item = MenuItemBuilder::new("退出")
                .id("quit")
                .build(app)?;

            let menu = MenuBuilder::new(app)
                .item(&show_item)
                .separator()
                .item(&quit_item)
                .build()?;

            // Create tray icon
            let _tray = TrayIconBuilder::new()
                .menu(&menu)
                .tooltip("MateCloud Desktop")
                .on_menu_event(move |app, event| {
                    match event.id().as_ref() {
                        "show" => {
                            if let Some(window) = app.get_webview_window("main") {
                                let _ = window.show();
                                let _ = window.set_focus();
                            }
                        }
                        "quit" => {
                            app.exit(0);
                        }
                        _ => {}
                    }
                })
                .on_tray_icon_event(|tray, event| {
                    if let tauri::tray::TrayIconEvent::DoubleClick { .. } = event {
                        let app = tray.app_handle();
                        if let Some(window) = app.get_webview_window("main") {
                            let _ = window.show();
                            let _ = window.set_focus();
                        }
                    }
                })
                .build(app)?;

            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running MateCloud Desktop");
}
```

## 2.10 src-tauri/src/main.rs

```rust
// apps/desktop/src-tauri/src/main.rs
// Prevents additional console window on Windows in release
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

fn main() {
    matecloud_desktop_lib::run()
}
```

## 2.11 src-tauri/src/commands.rs

```rust
// apps/desktop/src-tauri/src/commands.rs
use serde::Serialize;
use sysinfo::System;

/// Greet command: demonstrates Rust <-> JS bridge
#[tauri::command]
pub fn greet(name: &str) -> String {
    format!("Hello, {}! Welcome to MateCloud Desktop.", name)
}

/// System info: returns machine info for display in TrayPanel
#[derive(Serialize)]
pub struct SystemInfo {
    pub os_name: String,
    pub os_version: String,
    pub host_name: String,
    pub cpu_count: usize,
    pub total_memory_mb: u64,
    pub used_memory_mb: u64,
    pub memory_usage_percent: f64,
}

#[tauri::command]
pub fn get_system_info() -> SystemInfo {
    let mut sys = System::new_all();
    sys.refresh_all();

    let total_memory = sys.total_memory(); // bytes
    let used_memory = sys.used_memory();   // bytes
    let total_mb = total_memory / (1024 * 1024);
    let used_mb = used_memory / (1024 * 1024);
    let usage_pct = if total_memory > 0 {
        (used_memory as f64 / total_memory as f64) * 100.0
    } else {
        0.0
    };

    SystemInfo {
        os_name: System::name().unwrap_or_else(|| "Unknown".to_string()),
        os_version: System::os_version().unwrap_or_else(|| "Unknown".to_string()),
        host_name: System::host_name().unwrap_or_else(|| "Unknown".to_string()),
        cpu_count: sys.cpus().len(),
        total_memory_mb: total_mb,
        used_memory_mb: used_mb,
        memory_usage_percent: (usage_pct * 100.0).round() / 100.0,
    }
}
```

## 2.12 src/main.ts

```typescript
// apps/desktop/src/main.ts
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import 'virtual:uno.css'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('./views/Home.vue'),
    },
    {
      path: '/tray',
      component: () => import('./views/TrayPanel.vue'),
    },
  ],
})

const app = createApp(App)
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
app.use(router)
app.mount('#app')
```

## 2.13 src/App.vue

```vue
<!-- apps/desktop/src/App.vue -->
<template>
  <div class="desktop-app h-screen">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'

onMounted(() => {
  console.log('MateCloud Desktop loaded')
})
</script>

<style>
/* Global reset for desktop */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  overflow: hidden;
  font-family:
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    Roboto,
    'PingFang SC',
    'Microsoft YaHei',
    sans-serif;
}
</style>
```

## 2.14 src/views/Home.vue

```vue
<!-- apps/desktop/src/views/Home.vue -->
<template>
  <div class="home flex flex-col h-screen">
    <!-- Titlebar area (for custom title bar if decorations: false) -->
    <header class="h-12 flex items-center justify-between px-4 bg-white border-b border-gray-200 select-none" data-tauri-drag-region>
      <div class="flex items-center gap-2">
        <img src="/logo.png" alt="logo" class="w-6 h-6" />
        <span class="text-sm font-bold text-gray-700">MateCloud Desktop</span>
      </div>
      <div class="flex items-center gap-2 text-xs text-gray-500">
        <span>v1.0.0</span>
      </div>
    </header>

    <!-- Main content -->
    <main class="flex-1 overflow-auto bg-gray-50 p-6">
      <div class="max-w-4xl mx-auto">
        <!-- Welcome card -->
        <el-card shadow="never" class="mb-6">
          <div class="flex items-center gap-4">
            <div class="w-16 h-16 bg-blue-100 rounded-2xl flex items-center justify-center">
              <el-icon :size="32" color="#409eff"><Monitor /></el-icon>
            </div>
            <div>
              <h1 class="text-xl font-bold text-gray-800">MateCloud Desktop</h1>
              <p class="text-sm text-gray-500 mt-1">企业级微服务管理平台 - 桌面客户端</p>
            </div>
          </div>
        </el-card>

        <!-- Quick actions -->
        <div class="grid grid-cols-3 gap-4 mb-6">
          <el-card
            v-for="card in actionCards"
            :key="card.title"
            shadow="hover"
            class="cursor-pointer"
            @click="card.action"
          >
            <div class="text-center py-2">
              <div
                class="w-12 h-12 mx-auto mb-3 rounded-xl flex items-center justify-center"
                :style="{ background: card.bg }"
              >
                <el-icon :size="24" :color="card.color">
                  <component :is="card.icon" />
                </el-icon>
              </div>
              <p class="text-sm font-medium text-gray-700">{{ card.title }}</p>
              <p class="text-xs text-gray-400 mt-1">{{ card.desc }}</p>
            </div>
          </el-card>
        </div>

        <!-- Greet demo -->
        <el-card shadow="never" class="mb-6">
          <template #header>
            <span class="font-bold">Tauri Command Demo</span>
          </template>
          <div class="flex items-center gap-3">
            <el-input
              v-model="greetName"
              placeholder="输入你的名字"
              class="w-64"
            />
            <el-button type="primary" @click="handleGreet">调用 Rust</el-button>
          </div>
          <p v-if="greetMessage" class="mt-3 text-sm text-green-600">{{ greetMessage }}</p>
        </el-card>

        <!-- System info -->
        <el-card shadow="never">
          <template #header>
            <div class="flex items-center justify-between">
              <span class="font-bold">系统信息</span>
              <el-button size="small" @click="loadSystemInfo">刷新</el-button>
            </div>
          </template>
          <el-descriptions v-if="sysInfo" :column="2" border size="small">
            <el-descriptions-item label="操作系统">{{ sysInfo.os_name }}</el-descriptions-item>
            <el-descriptions-item label="系统版本">{{ sysInfo.os_version }}</el-descriptions-item>
            <el-descriptions-item label="主机名">{{ sysInfo.host_name }}</el-descriptions-item>
            <el-descriptions-item label="CPU 核心">{{ sysInfo.cpu_count }}</el-descriptions-item>
            <el-descriptions-item label="总内存">{{ sysInfo.total_memory_mb }} MB</el-descriptions-item>
            <el-descriptions-item label="已用内存">
              {{ sysInfo.used_memory_mb }} MB ({{ sysInfo.memory_usage_percent }}%)
            </el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="点击刷新加载系统信息" />
        </el-card>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { invoke } from '@tauri-apps/api/core'
import { Monitor, Connection, Setting } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

// Greet demo
const greetName = ref('')
const greetMessage = ref('')

async function handleGreet() {
  if (!greetName.value.trim()) {
    ElMessage.warning('请输入名字')
    return
  }
  try {
    greetMessage.value = await invoke<string>('greet', { name: greetName.value })
  } catch (err) {
    ElMessage.error('调用 Rust 命令失败')
    console.error(err)
  }
}

// System info
interface SystemInfo {
  os_name: string
  os_version: string
  host_name: string
  cpu_count: number
  total_memory_mb: number
  used_memory_mb: number
  memory_usage_percent: number
}

const sysInfo = ref<SystemInfo | null>(null)

async function loadSystemInfo() {
  try {
    sysInfo.value = await invoke<SystemInfo>('get_system_info')
  } catch (err) {
    ElMessage.error('获取系统信息失败')
    console.error(err)
  }
}

// Action cards
const actionCards = [
  {
    title: '打开管理后台',
    desc: '在内嵌浏览器中打开',
    icon: Monitor,
    color: '#409eff',
    bg: '#ecf5ff',
    action: () => {
      window.open('http://localhost:3000', '_blank')
    },
  },
  {
    title: '服务状态',
    desc: '查看微服务运行状态',
    icon: Connection,
    color: '#67c23a',
    bg: '#f0f9eb',
    action: () => {
      ElMessage.info('服务状态面板开发中')
    },
  },
  {
    title: '系统设置',
    desc: '桌面端偏好配置',
    icon: Setting,
    color: '#e6a23c',
    bg: '#fdf6ec',
    action: () => {
      ElMessage.info('设置面板开发中')
    },
  },
]

onMounted(() => {
  loadSystemInfo()
})
</script>

<style scoped>
/* UnoCSS handles layout; no extra styles needed */
</style>
```

## 2.15 src/views/TrayPanel.vue

```vue
<!-- apps/desktop/src/views/TrayPanel.vue -->
<template>
  <div class="tray-panel">
    <!-- Header -->
    <div class="tray-header">
      <div class="flex items-center gap-2">
        <div class="w-6 h-6 bg-blue-500 rounded-md flex items-center justify-center">
          <span class="text-white text-xs font-bold">M</span>
        </div>
        <span class="text-sm font-bold text-gray-800">MateCloud</span>
      </div>
      <span class="status-badge online">运行中</span>
    </div>

    <!-- Quick Status -->
    <div class="tray-section">
      <div class="status-grid">
        <div class="status-item">
          <span class="status-label">Gateway</span>
          <span class="status-dot online" />
        </div>
        <div class="status-item">
          <span class="status-label">Auth</span>
          <span class="status-dot online" />
        </div>
        <div class="status-item">
          <span class="status-label">System</span>
          <span class="status-dot online" />
        </div>
        <div class="status-item">
          <span class="status-label">Admin</span>
          <span class="status-dot online" />
        </div>
        <div class="status-item">
          <span class="status-label">Notice</span>
          <span class="status-dot" :class="noticeStatus" />
        </div>
      </div>
    </div>

    <!-- System Stats -->
    <div class="tray-section">
      <div class="stat-row">
        <span class="stat-label">CPU</span>
        <div class="stat-bar">
          <div class="stat-fill" :style="{ width: cpuUsage + '%' }" />
        </div>
        <span class="stat-value">{{ cpuUsage }}%</span>
      </div>
      <div class="stat-row">
        <span class="stat-label">内存</span>
        <div class="stat-bar">
          <div
            class="stat-fill"
            :class="{ warning: memoryUsage > 80 }"
            :style="{ width: memoryUsage + '%' }"
          />
        </div>
        <span class="stat-value">{{ memoryUsage }}%</span>
      </div>
    </div>

    <!-- Recent Notifications -->
    <div class="tray-section">
      <div class="section-title">最新通知</div>
      <div v-for="n in notifications" :key="n.id" class="notification-item">
        <div class="notif-dot" :class="n.type" />
        <div class="notif-content">
          <span class="notif-text">{{ n.text }}</span>
          <span class="notif-time">{{ n.time }}</span>
        </div>
      </div>
      <div v-if="notifications.length === 0" class="empty-text">暂无通知</div>
    </div>

    <!-- Footer Actions -->
    <div class="tray-footer">
      <button class="tray-btn" @click="openMainWindow">打开主窗口</button>
      <button class="tray-btn secondary" @click="quitApp">退出</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { invoke } from '@tauri-apps/api/core'
import { getCurrentWindow } from '@tauri-apps/api/window'

// Service statuses
const noticeStatus = ref('online')

// System stats
const cpuUsage = ref(0)
const memoryUsage = ref(0)

interface SystemInfo {
  memory_usage_percent: number
  cpu_count: number
}

async function refreshStats() {
  try {
    const info = await invoke<SystemInfo>('get_system_info')
    memoryUsage.value = Math.round(info.memory_usage_percent)
    // CPU usage approximation (actual CPU usage requires periodic sampling)
    cpuUsage.value = Math.round(Math.random() * 30 + 10) // placeholder
  } catch {
    // silently fail in tray
  }
}

let statsInterval: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  refreshStats()
  statsInterval = setInterval(refreshStats, 5000)
})

onUnmounted(() => {
  if (statsInterval) clearInterval(statsInterval)
})

// Notifications
interface TrayNotification {
  id: string
  text: string
  time: string
  type: 'info' | 'warning' | 'error'
}

const notifications = ref<TrayNotification[]>([
  { id: '1', text: '系统升级已完成 v2.0', time: '5分钟前', type: 'info' },
  { id: '2', text: 'Notice 服务内存使用率偏高', time: '15分钟前', type: 'warning' },
])

// Actions
async function openMainWindow() {
  try {
    const mainWindow = getCurrentWindow()
    await mainWindow.show()
    await mainWindow.setFocus()
  } catch {
    // If tray panel is a separate window, use app handle
    console.log('Opening main window...')
  }
}

async function quitApp() {
  try {
    const { exit } = await import('@tauri-apps/api/process')
    await exit(0)
  } catch {
    window.close()
  }
}
</script>

<style scoped>
.tray-panel {
  width: 320px;
  background: #ffffff;
  border-radius: 12px;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.tray-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.status-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.status-badge.online {
  background: #f0f9eb;
  color: #67c23a;
}

.tray-section {
  padding: 12px 16px;
  border-bottom: 1px solid #f5f5f5;
}

.section-title {
  font-size: 11px;
  font-weight: 600;
  color: #999;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

/* Status Grid */
.status-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: #f8f9fa;
  border-radius: 6px;
  font-size: 12px;
}

.status-label {
  color: #666;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ddd;
}

.status-dot.online {
  background: #67c23a;
}

.status-dot.warning {
  background: #e6a23c;
}

.status-dot.error {
  background: #f56c6c;
}

/* Stats */
.stat-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.stat-row:last-child {
  margin-bottom: 0;
}

.stat-label {
  font-size: 12px;
  color: #666;
  width: 32px;
  flex-shrink: 0;
}

.stat-bar {
  flex: 1;
  height: 6px;
  background: #f0f0f0;
  border-radius: 3px;
  overflow: hidden;
}

.stat-fill {
  height: 100%;
  background: #409eff;
  border-radius: 3px;
  transition: width 0.3s ease;
}

.stat-fill.warning {
  background: #e6a23c;
}

.stat-value {
  font-size: 11px;
  color: #999;
  width: 36px;
  text-align: right;
  flex-shrink: 0;
}

/* Notifications */
.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 6px 0;
}

.notif-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
}

.notif-dot.info {
  background: #409eff;
}

.notif-dot.warning {
  background: #e6a23c;
}

.notif-dot.error {
  background: #f56c6c;
}

.notif-content {
  flex: 1;
  min-width: 0;
}

.notif-text {
  display: block;
  font-size: 12px;
  color: #333;
  line-height: 1.4;
}

.notif-time {
  font-size: 10px;
  color: #bbb;
}

.empty-text {
  text-align: center;
  font-size: 12px;
  color: #ccc;
  padding: 8px 0;
}

/* Footer */
.tray-footer {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
}

.tray-btn {
  flex: 1;
  height: 32px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
  background: #409eff;
  color: #ffffff;
}

.tray-btn:hover {
  opacity: 0.9;
}

.tray-btn.secondary {
  background: #f5f5f5;
  color: #666;
}

.tray-btn.secondary:hover {
  background: #e8e8e8;
}
</style>
```

## 2.16 Static Assets (placeholder note)

The following files are needed in `apps/desktop/src-tauri/icons/`:

```
icons/
├── 32x32.png
├── 128x128.png
├── 128x128@2x.png
├── icon.icns          # macOS
├── icon.ico           # Windows
└── icon.png           # Tray icon
```

And in `apps/desktop/public/`:

```
public/
└── logo.png           # App logo for the header (32x32 or SVG)
```

Use `tauri icon` CLI to generate all sizes from a single 1024x1024 source PNG:

```bash
cd apps/desktop
pnpm tauri icon path/to/icon-1024x1024.png
```

---

## 3. Build & Run Commands

### Mobile

```bash
# Install deps
cd mate-ui && pnpm install

# H5 development
pnpm --filter @matecloud/mobile dev:h5
# -> http://localhost:3001

# WeChat Mini Program development
pnpm --filter @matecloud/mobile dev:mp-weixin
# -> output to dist/dev/mp-weixin, import in WeChat DevTools

# Production builds
pnpm --filter @matecloud/mobile build:h5
pnpm --filter @matecloud/mobile build:mp-weixin
```

### Desktop

```bash
# Prerequisites: Rust toolchain (rustup), system WebView2 (Windows)

# Development (starts Vite + Tauri window)
pnpm --filter @matecloud/desktop dev

# Production build
pnpm --filter @matecloud/desktop build
# -> produces:
#    Windows: src-tauri/target/release/bundle/msi/MateCloud_1.0.0_x64_en-US.msi
#    macOS:   src-tauri/target/release/bundle/dmg/MateCloud_1.0.0_aarch64.dmg
#    Linux:   src-tauri/target/release/bundle/appimage/MateCloud_1.0.0_amd64.AppImage
```

---

## 4. File Tree Summary

```
mate-ui/apps/
├── mobile/
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   └── src/
│       ├── manifest.json
│       ├── pages.json
│       ├── App.vue
│       ├── main.ts
│       ├── utils/
│       │   └── request.ts
│       ├── store/
│       │   └── auth.ts
│       ├── pages/
│       │   ├── login/
│       │   │   └── login.vue
│       │   ├── index/
│       │   │   └── index.vue
│       │   └── mine/
│       │       └── mine.vue
│       └── static/
│           ├── logo.png
│           ├── default-avatar.png
│           └── tabbar/
│               ├── home.png
│               ├── home-active.png
│               ├── mine.png
│               └── mine-active.png
│
└── desktop/
    ├── package.json
    ├── vite.config.ts
    ├── tsconfig.json
    ├── index.html
    ├── uno.config.ts
    ├── src/
    │   ├── main.ts
    │   ├── App.vue
    │   └── views/
    │       ├── Home.vue
    │       └── TrayPanel.vue
    ├── public/
    │   └── logo.png
    └── src-tauri/
        ├── Cargo.toml
        ├── build.rs
        ├── tauri.conf.json
        ├── icons/
        │   ├── 32x32.png
        │   ├── 128x128.png
        │   ├── 128x128@2x.png
        │   ├── icon.icns
        │   ├── icon.ico
        │   └── icon.png
        └── src/
            ├── main.rs
            ├── lib.rs
            └── commands.rs
```

## 5. Integration with Monorepo

### pnpm-workspace.yaml (already includes apps/*)

No change needed. Both `apps/mobile` and `apps/desktop` are automatically included by the `'apps/*'` glob in `pnpm-workspace.yaml`.

### turbo.json additions

The existing turbo.json `dev` and `build` tasks work as-is. Mobile and desktop apps will participate in `pnpm build` and `pnpm dev` alongside admin.

For platform-specific scripts, run directly:

```bash
# These bypass turbo (platform-specific)
pnpm --filter @matecloud/mobile dev:mp-weixin
pnpm --filter @matecloud/desktop dev
```

## 6. Key Architecture Decisions

1. **Mobile does NOT import from @matecloud/core directly** -- uni-app has its own HTTP (`uni.request`), so `request.ts` is a standalone wrapper. Type definitions can be duplicated or shared via a types-only package. This avoids bundling `axios` into the mobile build.

2. **Desktop DOES import from @matecloud/core** -- Tauri 2 uses a standard Chromium/WebKit webview, so Axios and all admin packages work unchanged.

3. **Pinia stores** -- Mobile has its own minimal auth store with `uni.setStorageSync` for persistence (no `pinia-plugin-persistedstate` needed, uni-app has its own storage). Desktop reuses the same Pinia setup as admin.

4. **Tauri 2 lib.rs pattern** -- Using the `lib.rs` + `main.rs` split pattern recommended by Tauri 2 for both desktop and mobile targets. The `#[cfg_attr(mobile, tauri::mobile_entry_point)]` attribute is ready for future iOS/Android Tauri builds.

5. **System tray** -- Double-click tray icon shows main window. Right-click shows menu with "Show" and "Quit" options. This is standard desktop app behavior.
