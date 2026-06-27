<template>
  <div class="register-container">
    <div class="register-bg" />

    <div class="register-card">
      <div class="text-center mb-6">
        <div class="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-primary/10 mb-4">
          <span class="text-3xl font-black text-primary">M</span>
        </div>
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
          {{ t('register.title') }}
        </h1>
        <p class="mt-2 text-sm text-gray-500 dark:text-gray-400">
          {{ t('register.subtitle') }}
        </p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        @submit.prevent="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            :placeholder="t('register.username')"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            :placeholder="t('register.password')"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item prop="realName">
          <el-input
            v-model="form.realName"
            :placeholder="t('register.realName')"
            :prefix-icon="UserFilled"
          />
        </el-form-item>
        <el-form-item prop="mobile">
          <el-input
            v-model="form.mobile"
            :placeholder="t('register.mobile')"
            :prefix-icon="Iphone"
            clearable
          />
        </el-form-item>
        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            :placeholder="t('register.email')"
            :prefix-icon="Message"
            clearable
          />
        </el-form-item>

        <el-button
          type="primary"
          class="w-full mt-2"
          size="large"
          :loading="loading"
          @click="handleRegister"
        >
          {{ t('register.submit') }}
        </el-button>
      </el-form>

      <div class="mt-6 text-center text-sm text-gray-500 dark:text-gray-400">
        {{ t('register.haveAccount') }}
        <router-link to="/login" class="text-primary ml-1 hover:underline">
          {{ t('register.signIn') }}
        </router-link>
      </div>

      <div class="mt-4 text-center text-xs text-gray-400 dark:text-gray-500">
        <span>Powered by MateCloud &copy; {{ new Date().getFullYear() }}</span>
      </div>
    </div>

    <!-- Slider Captcha Dialog -->
    <SliderCaptcha ref="captchaRef" @verified="onCaptchaVerified" @cancel="onCaptchaCancel" />
  </div>
</template>

<script setup lang="ts">
import { MateMessage } from '@matecloud/ui'
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, UserFilled, Lock, Iphone, Message } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore, SliderCaptcha } from '@matecloud/core'
import type { FormInstance } from 'element-plus'

const { t } = useI18n()
const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const captchaRef = ref<InstanceType<typeof SliderCaptcha> | null>(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  realName: '',
  mobile: '',
  email: '',
})

const rules = {
  username: [
    { required: true, message: () => t('register.usernameRequired'), trigger: 'blur' },
    {
      pattern: /^[A-Za-z0-9_]{4,32}$/,
      message: () => t('register.usernameInvalid'),
      trigger: 'blur',
    },
  ],
  password: [
    { required: true, message: () => t('register.passwordRequired'), trigger: 'blur' },
    { min: 6, max: 64, message: () => t('register.passwordLength'), trigger: 'blur' },
  ],
  mobile: [
    { pattern: /^1[3-9]\d{9}$/, message: () => t('login.mobileInvalid'), trigger: 'blur' },
  ],
}

/** Click register → validate form → show captcha dialog */
async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  captchaRef.value?.open()
}

/** Captcha verified → proceed with registration */
async function onCaptchaVerified(captchaVerification: string) {
  loading.value = true
  try {
    await auth.register({
      username: form.username,
      password: form.password,
      realName: form.realName || undefined,
      mobile: form.mobile || undefined,
      email: form.email || undefined,
      captchaVerification,
    })
    MateMessage.success(t('register.success'))
    router.replace('/')
  } catch (e: any) {
    MateMessage.error(e?.msg || e?.message || t('common.registerFailed'))
  } finally {
    loading.value = false
  }
}

function onCaptchaCancel() {
  loading.value = false
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 40px 0;
}

.register-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  z-index: 0;
}

:global(.dark) .register-bg {
  background: linear-gradient(135deg, #1e1b4b 0%, #312e81 100%);
}

.register-card {
  position: relative;
  z-index: 1;
  width: 420px;
  max-width: calc(100vw - 32px);
  padding: 40px 32px 28px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

:global(.dark) .register-card {
  background: rgba(30, 27, 75, 0.85);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
}
</style>
