<script setup lang="ts">
import { ref } from 'vue'

const username = ref('')
const password = ref('')
const loading = ref(false)

const baseUrl = (import.meta.env.VITE_API_BASE_URL as string) || '/api/v1'

function login() {
  if (!username.value || !password.value) {
    uni.showToast({ title: 'Please enter username and password', icon: 'none' })
    return
  }
  loading.value = true
  uni.request({
    url: `${baseUrl}/auth/login`,
    method: 'POST',
    data: { username: username.value, password: password.value, type: 'password' },
    success: (res) => {
      const body = res.data as { code?: string; data?: { tokenValue?: string }; msg?: string }
      if (body && body.code === '200' && body.data?.tokenValue) {
        uni.setStorageSync('mate.token', body.data.tokenValue)
        uni.switchTab({ url: '/pages/home/index' })
      } else {
        uni.showToast({ title: body?.msg || 'Login failed', icon: 'none' })
      }
    },
    fail: (err) => {
      uni.showToast({ title: err.errMsg || 'Network error', icon: 'none' })
    },
    complete: () => {
      loading.value = false
    },
  })
}
</script>

<template>
  <view class="page">
    <view class="card">
      <text class="title">MateCloud</text>
      <input v-model="username" class="input" placeholder="Username" />
      <input v-model="password" class="input" placeholder="Password" password />
      <button class="primary" :disabled="loading" @tap="login">
        {{ loading ? 'Signing in...' : 'Sign in' }}
      </button>
    </view>
  </view>
</template>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32rpx;
}
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 48rpx;
  width: 100%;
  max-width: 600rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
}
.title {
  font-size: 44rpx;
  font-weight: 600;
  color: #1976d2;
  text-align: center;
  margin-bottom: 16rpx;
}
.input {
  border: 1rpx solid #e0e0e0;
  padding: 20rpx 24rpx;
  border-radius: 8rpx;
  font-size: 28rpx;
}
.primary {
  background: #1976d2;
  color: #fff;
  border: none;
  padding: 24rpx;
  border-radius: 8rpx;
  font-size: 30rpx;
  margin-top: 16rpx;
}
.primary[disabled] {
  background: #b0bec5;
}
</style>
