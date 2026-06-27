<template>
  <div class="login-container">
    <!-- ===== Login Card ===== -->
    <div class="login-card">

      <!-- Logo & Title -->
      <div class="text-center mb-8 relative z-10">
        <img src="@/assets/logo.svg" alt="MateCloud" class="login-logo" />
        <h1 class="login-title">{{ t('login.title') }}</h1>
        <p class="login-subtitle">{{ t('login.subtitle') }}</p>
      </div>

      <!-- Login Tabs -->
      <el-tabs v-model="activeTab" class="login-tabs" stretch>
        <!-- Password Login -->
        <el-tab-pane :label="t('login.passwordTab')" name="password">
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" size="large" @submit.prevent="handlePasswordLogin">
            <el-form-item prop="account">
              <el-input v-model="pwdForm.account" :placeholder="t('login.account')" :prefix-icon="IconUser" clearable />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="pwdForm.password" type="password" :placeholder="t('login.password')" :prefix-icon="IconLock" show-password @keyup.enter="handlePasswordLogin" />
            </el-form-item>

            <div class="flex items-center justify-between mb-4">
              <el-checkbox v-model="rememberMe">{{ t('login.rememberMe') }}</el-checkbox>
              <el-link type="primary" :underline="false" class="text-sm">{{ t('login.forgotPassword') }}</el-link>
            </div>

            <el-button type="primary" class="w-full login-btn" size="large" :loading="loading" @click="handlePasswordLogin">
              {{ t('login.loginBtn') }}
            </el-button>
          </el-form>
        </el-tab-pane>

        <!-- SMS Login -->
        <el-tab-pane :label="t('login.smsTab')" name="sms">
          <el-form ref="smsFormRef" :model="smsForm" :rules="smsRules" size="large" @submit.prevent="handleSmsLogin">
            <el-form-item prop="mobile">
              <el-input v-model="smsForm.mobile" :placeholder="t('login.mobile')" :prefix-icon="IconPhone" clearable />
            </el-form-item>
            <el-form-item prop="code">
              <div class="captcha-row">
                <el-input v-model="smsForm.code" :placeholder="t('login.smsCode')" :prefix-icon="IconMessage" class="flex-1" @keyup.enter="handleSmsLogin" />
                <el-button :disabled="countdown > 0" class="sms-btn" size="large" @click="handleSendSms">
                  {{ countdown > 0 ? t('login.resend', { seconds: countdown }) : t('login.sendCode') }}
                </el-button>
              </div>
            </el-form-item>

            <el-button type="primary" class="w-full login-btn mt-4" size="large" :loading="loading" @click="handleSmsLogin">
              {{ t('login.loginBtn') }}
            </el-button>
          </el-form>
        </el-tab-pane>

        <!-- 企业微信扫码登录 (only when a wechat_work provider is configured) -->
        <el-tab-pane v-if="wechatProvider" :label="t('login.wechatTab')" name="wechat">
          <div class="sso-pane">
            <p class="sso-tip">{{ t('login.wechatTip') }}</p>
            <el-button type="primary" class="w-full login-btn" size="large" :loading="ssoLoading" @click="startWechatLogin">
              <IconQr :size="16" style="margin-right:6px" /> {{ t('login.wechatBtn') }}
            </el-button>
          </div>
        </el-tab-pane>
      </el-tabs>

      <!-- Public self-registration is disabled: back-office accounts (mate_admin)
           are created by an administrator in 管理员管理, not self-served. -->
      <!--
      <div class="login-footer">
        {{ t('login.noAccount') }}
        <router-link to="/register" class="login-link">{{ t('login.createAccount') }}</router-link>
      </div>
      -->
      <div class="login-spacer"></div>
      <div class="login-copyright">Powered by MateCloud &copy; {{ new Date().getFullYear() }}</div>
    </div>

    <!-- Theme & Language (top-right) -->
    <div class="toolbar">
      <button class="toolbar-btn" @click="toggleDark">
        <component :is="isDark ? IconSun : IconMoon" :size="18" />
      </button>
      <MateDropdown @command="changeLocale">
        <button class="toolbar-btn"><IconLanguages :size="18" /></button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="zh-CN">中文</el-dropdown-item>
            <el-dropdown-item command="en-US">English</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </MateDropdown>
    </div>

    <!-- Slider Captcha Dialog (shared by both login methods) -->
    <SliderCaptcha ref="captchaRef" @verified="onCaptchaVerified" @cancel="onCaptchaCancel" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { MateMessage, MateDropdown } from '@matecloud/ui'
import {
  User as IconUser, Lock as IconLock,
  Smartphone as IconPhone, MessageSquare as IconMessage,
  Languages as IconLanguages, Moon as IconMoon, Sun as IconSun,
  QrCode as IconQr,
} from 'lucide-vue-next'
import { useI18n } from 'vue-i18n'
import { setLocale } from '@/i18n'
import { authApi, useAuthStore, SliderCaptcha } from '@matecloud/core'
import type { FormInstance } from 'element-plus'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

// Dark mode
const isDark = ref(document.documentElement.classList.contains('dark'))
function toggleDark() {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem('mate_dark', isDark.value ? 'dark' : 'auto')
}

// Language
function changeLocale(locale: string) { setLocale(locale as 'zh-CN' | 'en-US') }

// Tab
const activeTab = ref('password')
const loading = ref(false)
const rememberMe = ref(false)

// ── Captcha Dialog ────────────────────────────────────────────────────────
const captchaRef = ref<InstanceType<typeof SliderCaptcha> | null>(null)

/** What action is pending behind the captcha: 'pwd-login' | 'sms-login' | 'sms-send' */
type PendingAction = 'pwd-login' | 'sms-login' | 'sms-send'
let pendingAction: PendingAction | null = null

function showCaptcha(action: PendingAction) {
  pendingAction = action
  captchaRef.value?.open()
}

function onCaptchaVerified(token: string) {
  const action = pendingAction
  pendingAction = null
  switch (action) {
    case 'pwd-login': doPasswordLogin(token); break
    case 'sms-login': doSmsLogin(token); break
    case 'sms-send': doSendSms(token); break
  }
}

function onCaptchaCancel() {
  pendingAction = null
  loading.value = false
}

// === Password Login ===
const pwdFormRef = ref<FormInstance>()
const pwdForm = reactive({ account: '', password: '' })
const pwdRules = {
  account: [{ required: true, message: () => t('login.accountRequired'), trigger: 'blur' }],
  password: [{ required: true, message: () => t('login.passwordRequired'), trigger: 'blur' }],
}

async function handlePasswordLogin() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  showCaptcha('pwd-login')
}

async function doPasswordLogin(captchaVerification: string) {
  loading.value = true
  try {
    await auth.loginWithPassword({
      account: pwdForm.account,
      password: pwdForm.password,
      captchaVerification,
    })
    MateMessage.success({ message: t('login.loginSuccess'), placement: 'center' })
    router.push((route.query.redirect as string) || '/')
  } catch (e: any) {
    MateMessage.error(e?.msg || e?.message || t('common.loginFailed'))
  } finally { loading.value = false }
}

// === SMS Login ===
const smsFormRef = ref<FormInstance>()
const smsForm = reactive({ mobile: '', code: '' })
const smsRules = {
  mobile: [
    { required: true, message: () => t('login.mobileRequired'), trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: () => t('login.mobileInvalid'), trigger: 'blur' },
  ],
  code: [
    { required: true, message: () => t('login.smsCodeRequired'), trigger: 'blur' },
    { pattern: /^\d{6}$/, message: () => t('login.smsCodeInvalid'), trigger: 'blur' },
  ],
}

const countdown = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

/** Click "Send Code" → validate mobile → show captcha → then send */
async function handleSendSms() {
  const valid = await smsFormRef.value?.validateField('mobile').catch(() => false)
  if (!valid) return
  showCaptcha('sms-send')
}

async function doSendSms(captchaVerification: string) {
  try {
    await authApi.sendSms({ mobile: smsForm.mobile, captchaVerification })
    MateMessage.success(t('common.success'))
    countdown.value = 60
    if (timer) clearInterval(timer)
    timer = setInterval(() => { countdown.value--; if (countdown.value <= 0 && timer) { clearInterval(timer); timer = null } }, 1000)
  } catch (e: any) {
    MateMessage.error(e?.msg || e?.message || t('common.failed'))
  }
}

async function handleSmsLogin() {
  const valid = await smsFormRef.value?.validate().catch(() => false)
  if (!valid) return
  doSmsLogin()
}

async function doSmsLogin(_captchaVerification?: string) {
  loading.value = true
  try {
    await auth.loginWithSms({ mobile: smsForm.mobile, code: smsForm.code })
    MateMessage.success({ message: t('login.loginSuccess'), placement: 'center' })
    router.push((route.query.redirect as string) || '/')
  } catch (e: any) { MateMessage.error(e?.msg || e?.message || t('common.loginFailed')) }
  finally { loading.value = false }
}

// === 企业微信扫码登录 ===
const SSO_STATE_KEY = 'mate_sso_state'
const wechatProvider = ref<{ code: string; config: Record<string, string> } | null>(null)
const ssoLoading = ref(false)

/** Jump to WeChat Work's QR connect page; it redirects back here with ?code&state. */
function startWechatLogin() {
  const p = wechatProvider.value
  if (!p) return
  const corpId = p.config.corpId
  const agentId = p.config.agentId
  if (!corpId || !agentId) { MateMessage.error(t('login.wechatNotConfigured')); return }
  const state = Math.random().toString(36).slice(2, 12)
  sessionStorage.setItem(SSO_STATE_KEY, JSON.stringify({ provider: p.code, state }))
  const redirectUri = encodeURIComponent(window.location.origin + window.location.pathname)
  window.location.href =
    'https://login.work.weixin.qq.com/wwlogin/sso/login?login_type=CorpApp'
    + `&appid=${corpId}&agentid=${agentId}&redirect_uri=${redirectUri}&state=${state}`
}

/** Handle the OAuth callback (?code&state) after WeChat redirects back. */
async function handleSsoCallback() {
  const code = route.query.code as string | undefined
  const state = route.query.state as string | undefined
  if (!code) return
  const saved = sessionStorage.getItem(SSO_STATE_KEY)
  sessionStorage.removeItem(SSO_STATE_KEY)
  let providerCode = 'wechat_work'
  if (saved) {
    const parsed = JSON.parse(saved)
    if (parsed.state && state && parsed.state !== state) return // CSRF guard
    providerCode = parsed.provider || providerCode
  }
  ssoLoading.value = true
  try {
    await auth.loginWithSso({ providerCode, code, state })
    MateMessage.success({ message: t('login.loginSuccess'), placement: 'center' })
    router.replace((route.query.redirect as string) || '/')
  } catch (e: any) {
    MateMessage.error(e?.msg || e?.message || t('common.loginFailed'))
    router.replace({ path: '/login' }) // strip ?code so it isn't replayed
  } finally { ssoLoading.value = false }
}

onMounted(async () => {
  isDark.value = document.documentElement.classList.contains('dark')
  // Discover configured SSO providers (non-secret config) for the scan panel
  try {
    const res = await authApi.ssoProviders()
    const list = res.data || []
    const ww = list.find((x) => x.code === 'wechat_work')
    if (ww) wechatProvider.value = { code: ww.code, config: ww.config }
  } catch { /* SSO off or unreachable — keep password/SMS only */ }
  await handleSsoCallback()
})
onBeforeUnmount(() => { if (timer) clearInterval(timer) })
</script>

<!-- ============================================================
     All styles use var(--mc-*) design tokens from tokens.css.
     Dark mode is handled entirely through those tokens + html.dark.
============================================================ -->
<style scoped>
/* ---- Stage ---- */
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background: var(--mc-bg);
  background-image:
    radial-gradient(circle at top left, rgba(var(--mc-primary-rgb), 0.1), transparent 26%),
    radial-gradient(circle at bottom right, rgba(var(--mc-primary-rgb), 0.06), transparent 24%);
}

/* Subtle grid texture overlay */
.login-container::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(var(--mc-primary-rgb), 0.012) 1px, transparent 1px),
    linear-gradient(90deg, rgba(var(--mc-primary-rgb), 0.012) 1px, transparent 1px);
  background-size: 24px 24px;
  opacity: 0.4;
  mix-blend-mode: soft-light;
}

/* ---- Login Card (Glass morphism) ---- */
.login-card {
  position: relative; z-index: 1;
  width: 380px;
  max-width: calc(100vw - 32px);
  padding: 40px 32px 32px;
  border-radius: var(--mc-radius-xl);
  background: var(--mc-surface-overlay);
  backdrop-filter: blur(16px) saturate(1.2);
  -webkit-backdrop-filter: blur(16px) saturate(1.2);
  border: 1px solid var(--mc-border);
  box-shadow: var(--mc-shadow-strong);
  animation: fadeUp 0.6s ease-out both;
}

/* ---- Logo ---- */
.login-logo {
  width: 64px; height: 64px;
  margin: 0 auto 14px;
  border-radius: 14px;
  animation: breathe 3.5s ease-in-out infinite;
  filter: drop-shadow(0 6px 20px rgba(var(--mc-primary-rgb), 0.25));
}

.login-title {
  font-size: 24px; font-weight: 700;
  color: var(--mc-text-primary);
  letter-spacing: -0.01em; margin: 0;
}
.login-subtitle {
  margin-top: 6px; font-size: 13px;
  color: var(--mc-text-muted);
}

/* ---- Tabs ---- */
.login-tabs :deep(.el-tabs__header) { margin-bottom: 22px; }
.login-tabs :deep(.el-tabs__active-bar) {
  height: 3px; border-radius: 2px;
  background: var(--mc-primary);
}

/* ---- SMS code row ---- */
.captcha-row { display: flex; gap: 12px; width: 100%; }

/* ---- Buttons ---- */
.login-btn {
  height: 44px !important;
  border-radius: var(--mc-radius) !important;
  font-weight: 600;
  font-size: 14px !important;
  background: var(--mc-primary) !important;
  border: none !important;
  transition: all 0.2s;
}
.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(var(--mc-primary-rgb), 0.3);
}

.sms-btn {
  width: 128px; flex-shrink: 0;
  background: var(--mc-panel-raised) !important;
  border: 1px solid var(--mc-border) !important;
  border-radius: var(--mc-radius-lg) !important;
  color: var(--mc-text-secondary) !important;
}

/* ---- Footer ---- */
.login-footer {
  margin-top: 24px; text-align: center;
  font-size: 13px; color: var(--mc-text-muted);
}
.login-link { color: var(--mc-primary); margin-left: 4px; }
.login-link:hover { text-decoration: underline; }
.login-copyright {
  margin-top: 12px; text-align: center;
  font-size: 11px; color: var(--mc-text-disabled);
}

/* ---- Toolbar (top-right) ---- */
.toolbar {
  position: fixed; top: 16px; right: 16px;
  display: flex; gap: 8px; z-index: 20;
}
.toolbar-btn {
  display: inline-flex; align-items: center; justify-content: center;
  width: 36px; height: 36px; border: none;
  background: var(--mc-surface-overlay);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: var(--mc-radius);
  border: 1px solid var(--mc-border-light);
  color: var(--mc-text-secondary);
  cursor: pointer;
  transition: color 0.2s, background 0.2s;
}
.toolbar-btn:hover { color: var(--mc-primary); background: var(--mc-fill); }

/* ---- Animations ---- */
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes breathe {
  0%, 100% {
    transform: scale(1);
    filter: drop-shadow(0 6px 20px rgba(var(--mc-primary-rgb), 0.3));
  }
  50% {
    transform: scale(1.05);
    filter: drop-shadow(0 8px 28px rgba(var(--mc-primary-rgb), 0.45));
  }
}

/* ---- Responsive ---- */
@media (max-width: 480px) {
  .login-card {
    padding: 32px 24px 28px;
    border-radius: var(--mc-radius-lg);
  }
}
</style>
