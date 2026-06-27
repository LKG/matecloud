<script setup lang="ts">
import { ref } from 'vue'

const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const submitting = ref(false)

const baseUrl = (import.meta.env.VITE_API_BASE_URL as string) || '/api/v1'

function submit() {
  if (!oldPassword.value || !newPassword.value) {
    uni.showToast({ title: 'Please fill in both passwords', icon: 'none' })
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    uni.showToast({ title: 'Passwords do not match', icon: 'none' })
    return
  }
  if (newPassword.value.length < 6) {
    uni.showToast({ title: 'New password too short (min 6)', icon: 'none' })
    return
  }
  submitting.value = true
  uni.request({
    url: `${baseUrl}/users/password`,
    method: 'POST',
    header: {
      Authorization: `Bearer ${uni.getStorageSync('mate.token') || ''}`,
      'Content-Type': 'application/json',
    },
    data: {
      oldPassword: oldPassword.value,
      newPassword: newPassword.value,
    },
    success: (res) => {
      const body = res.data as { code?: string; msg?: string }
      if (body && body.code === '200') {
        uni.showToast({ title: 'Password changed', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 600)
      } else {
        uni.showToast({ title: body?.msg || 'Failed', icon: 'none' })
      }
    },
    fail: (err) => {
      uni.showToast({ title: err.errMsg || 'Network error', icon: 'none' })
    },
    complete: () => {
      submitting.value = false
    },
  })
}
</script>

<template>
  <view class="page">
    <view class="card">
      <text class="label">Old password</text>
      <input v-model="oldPassword" class="input" password placeholder="Current password" />

      <text class="label">New password</text>
      <input v-model="newPassword" class="input" password placeholder="At least 6 characters" />

      <text class="label">Confirm new password</text>
      <input v-model="confirmPassword" class="input" password placeholder="Repeat the new password" />

      <button class="primary" :disabled="submitting" @tap="submit">
        {{ submitting ? 'Saving...' : 'Save' }}
      </button>
    </view>
  </view>
</template>

<style scoped>
.page { padding: 32rpx; }
.card {
  background: #fff;
  padding: 48rpx;
  border-radius: 12rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}
.label { font-size: 26rpx; color: #4b5563; margin-top: 8rpx; }
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
.primary[disabled] { background: #b0bec5; }
</style>
