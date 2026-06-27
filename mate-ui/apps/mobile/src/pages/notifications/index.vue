<script setup lang="ts">
import { ref, onMounted } from 'vue'

interface NoticeItem {
  id: string
  title: string
  content: string
  createdAt: string
}

const items = ref<NoticeItem[]>([])
const loading = ref(false)

onMounted(() => {
  loading.value = true
  // Stub: there's no per-user inbox endpoint yet — we surface the latest
  // operation log entries as a stand-in until that API is added.
  uni.request({
    url: ((import.meta.env.VITE_API_BASE_URL as string) || '/api/v1')
      + '/admin/operation-logs?pageNum=1&pageSize=20',
    method: 'GET',
    header: { Authorization: `Bearer ${uni.getStorageSync('mate.token') || ''}` },
    success: (res) => {
      const body = res.data as { code?: string; data?: { list?: any[] } }
      if (body && body.code === '200' && body.data?.list) {
        items.value = body.data.list.map((row) => ({
          id: String(row.id),
          title: row.module || row.operationDesc || 'Notice',
          content: row.operationDesc || '',
          createdAt: row.createdAt || '',
        }))
      }
    },
    complete: () => { loading.value = false },
  })
})
</script>

<template>
  <view class="page">
    <view v-if="loading" class="empty">Loading...</view>
    <view v-else-if="items.length === 0" class="empty">No notifications</view>
    <view v-else>
      <view v-for="item in items" :key="item.id" class="card">
        <text class="title">{{ item.title }}</text>
        <text class="body">{{ item.content }}</text>
        <text class="time">{{ item.createdAt }}</text>
      </view>
    </view>
  </view>
</template>

<style scoped>
.page { padding: 32rpx; }
.empty {
  text-align: center;
  color: #9ca3af;
  padding: 96rpx 0;
}
.card {
  background: #fff;
  padding: 32rpx;
  border-radius: 12rpx;
  margin-bottom: 16rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.title { font-size: 30rpx; font-weight: 600; }
.body { font-size: 26rpx; color: #4b5563; }
.time { font-size: 22rpx; color: #9ca3af; margin-top: 8rpx; }
</style>
