<script setup lang="ts">
import { MateMessage } from '@matecloud/ui'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const username = ref('admin')
const password = ref('admin123')
const loading = ref(false)

const baseUrl = (import.meta.env.VITE_API_BASE_URL as string) || 'http://127.0.0.1:9010/api/v1'

async function login() {
  if (!username.value || !password.value) return
  loading.value = true
  try {
    const response = await fetch(`${baseUrl}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: username.value,
        password: password.value,
        type: 'password',
      }),
    })
    const body = await response.json() as { code?: string; msg?: string; data?: { tokenValue?: string } }
    if (body.code === '200' && body.data?.tokenValue) {
      localStorage.setItem('mate.token', body.data.tokenValue)
      localStorage.setItem('mate.username', username.value)
      router.push('/home')
    } else {
      MateMessage.error(body.msg || 'Login failed')
    }
  } catch (e: any) {
    MateMessage.error(e?.message || 'Network error')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="card">
      <h1>MateCloud</h1>
      <el-input v-model="username" placeholder="Username" size="large" />
      <el-input v-model="password" type="password" placeholder="Password" size="large" />
      <el-button type="primary" size="large" :loading="loading" @click="login">
        Sign in
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
.card {
  background: #fff;
  padding: 48px;
  border-radius: 12px;
  width: 360px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}
h1 {
  text-align: center;
  color: #1976d2;
  margin: 0 0 16px;
}
</style>
