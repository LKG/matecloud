import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import enUs from 'element-plus/es/locale/lang/en'
import App from './App.vue'
import router from './router'
import i18n from './i18n'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import '@/styles/tokens.css'
import 'virtual:uno.css'

const app = createApp(App)

// Pinia
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)
app.use(pinia)

// i18n
app.use(i18n)

// Router
app.use(router)

// Element Plus — seed the initial locale to match the stored language so the
// very first paint is correct; App.vue's ElConfigProvider then keeps it in sync
// reactively when the user switches language.
app.use(ElementPlus, {
  locale: localStorage.getItem('mate_locale') === 'en-US' ? enUs : zhCn,
})

// 全局错误网 (与 useAsync 逐请求处理互补的兜底): 捕获未处理的渲染异常与 unhandledrejection
import { installErrorNet } from '@/support/errorNet'
installErrorNet(app)

// Custom directives
import { vPermission, vRole } from '@/directives/permission'
app.directive('permission', vPermission)
app.directive('role', vRole)

// Apply dark class BEFORE first render so there's no flash-of-light.
// useDark() stores 'dark' or 'auto' in localStorage key 'mate_dark'.
const storedDark = localStorage.getItem('mate_dark')
// Migrate old '1'/'0' values from previous convention
if (storedDark === '1') {
  localStorage.setItem('mate_dark', 'dark')
  document.documentElement.classList.add('dark')
} else if (storedDark === '0') {
  localStorage.setItem('mate_dark', 'auto')
} else if (storedDark === 'dark') {
  document.documentElement.classList.add('dark')
} else if (!storedDark || storedDark === 'auto') {
  if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
    document.documentElement.classList.add('dark')
  }
}

// Listen for 401 from API client (core package dispatches CustomEvent to avoid circular deps)
window.addEventListener('mate:unauthorized', () => {
  if (router.currentRoute.value.name !== 'Login') {
    router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
  }
})

// Listen for 403 from API client — navigate to the forbidden page
window.addEventListener('mate:forbidden', () => {
  if (router.currentRoute.value.name !== 'Forbidden') {
    router.push({ name: 'Forbidden' })
  }
})

app.mount('#app')
