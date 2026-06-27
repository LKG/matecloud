<script setup lang="ts">
import { ref, onMounted } from 'vue'

const username = ref('')

onMounted(() => {
  username.value = (uni.getStorageSync('mate.username') as string) || 'Guest'
})

function changePassword() {
  uni.navigateTo({ url: '/pages/profile/change-password' })
}

function logout() {
  uni.removeStorageSync('mate.token')
  uni.removeStorageSync('mate.username')
  uni.reLaunch({ url: '/pages/login/index' })
}
</script>

<template>
  <view class="page">
    <view class="user-card">
      <view class="avatar">{{ username.charAt(0).toUpperCase() }}</view>
      <view class="info">
        <text class="name">{{ username }}</text>
        <text class="hint">Signed in</text>
      </view>
    </view>

    <view class="menu">
      <view class="menu-item" @tap="changePassword">
        <text>Change password</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <button class="danger" @tap="logout">Sign out</button>
  </view>
</template>

<style scoped>
.page { padding: 32rpx; }
.user-card {
  background: #fff;
  padding: 32rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  gap: 24rpx;
  margin-bottom: 24rpx;
}
.avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 96rpx;
  background: #1976d2;
  color: #fff;
  font-size: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.info { display: flex; flex-direction: column; gap: 4rpx; }
.name { font-size: 32rpx; font-weight: 600; }
.hint { font-size: 24rpx; color: #6b7280; }

.menu {
  background: #fff;
  border-radius: 12rpx;
  margin-bottom: 24rpx;
}
.menu-item {
  padding: 28rpx 32rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 28rpx;
}
.arrow { color: #9ca3af; }

.danger {
  background: #ef4444;
  color: #fff;
  border: none;
  padding: 24rpx;
  border-radius: 8rpx;
}
</style>
