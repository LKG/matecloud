<script setup lang="ts">
import { ref, onMounted } from 'vue'

interface DashboardVO {
  userCount: number
  todayLoginCount: number
  todayOpCount: number
  onlineCount: number
}

const username = ref('')
const data = ref<DashboardVO | null>(null)
const loading = ref(false)

const baseUrl = (import.meta.env.VITE_API_BASE_URL as string) || '/api/v1'

function load() {
  loading.value = true
  uni.request({
    url: `${baseUrl}/admin/monitor/dashboard`,
    method: 'GET',
    header: {
      Authorization: `Bearer ${uni.getStorageSync('mate.token') || ''}`,
    },
    success: (res) => {
      const body = res.data as { code?: string; data?: DashboardVO }
      if (body && body.code === '200') {
        data.value = body.data!
      }
    },
    complete: () => {
      loading.value = false
    },
  })
}

onMounted(() => {
  username.value = (uni.getStorageSync('mate.username') as string) || 'Guest'
  load()
})

function goNotifications() {
  uni.navigateTo({ url: '/pages/notifications/index' })
}
</script>

<template>
  <view class="page">
    <view class="hero">
      <text class="title">Hello, {{ username }}</text>
      <text class="sub">Welcome to MateCloud</text>
    </view>

    <view class="grid">
      <view class="tile">
        <text class="tile-title">Users</text>
        <text class="tile-num">{{ data?.userCount ?? '—' }}</text>
      </view>
      <view class="tile">
        <text class="tile-title">Online</text>
        <text class="tile-num">{{ data?.onlineCount ?? '—' }}</text>
      </view>
      <view class="tile">
        <text class="tile-title">Today logins</text>
        <text class="tile-num">{{ data?.todayLoginCount ?? '—' }}</text>
      </view>
      <view class="tile">
        <text class="tile-title">Today ops</text>
        <text class="tile-num">{{ data?.todayOpCount ?? '—' }}</text>
      </view>
    </view>

    <view class="actions">
      <button class="action" @tap="goNotifications">Notifications</button>
      <button class="action" @tap="load" :disabled="loading">
        {{ loading ? 'Refreshing...' : 'Refresh' }}
      </button>
    </view>
  </view>
</template>

<style scoped>
.page { padding: 32rpx; }
.hero { padding: 32rpx 0; }
.title { font-size: 48rpx; font-weight: 600; display: block; }
.sub { display: block; color: #6b7280; margin-top: 12rpx; font-size: 28rpx; }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 24rpx; }
.tile {
  background: #fff;
  padding: 32rpx;
  border-radius: 12rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}
.tile-title { font-size: 26rpx; color: #6b7280; display: block; }
.tile-num { font-size: 56rpx; font-weight: 600; margin-top: 8rpx; display: block; color: #1976d2; }
.actions { display: flex; gap: 16rpx; margin-top: 32rpx; }
.action {
  flex: 1;
  background: #fff;
  border: 1rpx solid #e0e0e0;
  border-radius: 8rpx;
  padding: 24rpx;
  font-size: 28rpx;
}
</style>
