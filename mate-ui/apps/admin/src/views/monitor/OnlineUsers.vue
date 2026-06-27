<template>
  <MatePageCard :title="t('online.title')" :description="t('online.description')">
    <template #actions>
      <el-button @click="loadData">
        <RefreshCw :size="14" class="mr-1" />{{ t('common.refresh') }}
      </el-button>
    </template>

    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <el-input
        v-model="keyword"
        :placeholder="t('online.searchPlaceholder')"
        clearable
        style="width: 240px"
        @keyup.enter="handleSearch"
      >
        <template #prefix><Search :size="14" /></template>
      </el-input>
    </MateSearchBar>

    <MateTable
      :columns="columns"
      :data="rows"
      :loading="loading"
      row-key="userId"
      :action-width="130"
    >
      <template #col-username="{ row }">
        <MateEntityCell :name="row.realName || '-'" :sub="row.username" avatar />
      </template>
      <template #col-tokenCount="{ row }">
        <el-tag size="small" :type="row.tokenCount > 1 ? 'warning' : 'info'">
          {{ row.tokenCount }} {{ t('online.devices') }}
        </el-tag>
      </template>
      <template #col-loginTime="{ value }">
        <span class="mc-mono">{{ value || '-' }}</span>
      </template>
      <template #col-timeout="{ row }">
        <span v-if="row.timeout > 0" class="mc-mono">{{ formatTimeout(row.timeout) }}</span>
        <el-tag v-else size="small" type="danger">{{ t('online.expired') }}</el-tag>
      </template>
      <template #actions="{ row }">
        <MateInlineConfirm :title="t('online.kickConfirm')" @confirm="handleKick(row)">
                      <button v-permission="'sys:log:list'" class="mc-action-btn mc-action-btn--danger">
              {{ t('online.kick') }}
            </button>
          
        </MateInlineConfirm>
      </template>
    </MateTable>

    <MatePagination
      v-model:page-num="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      @change="loadData"
    />
  </MatePageCard>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search, RefreshCw } from 'lucide-vue-next'
import { MatePageCard, MateTable, MateSearchBar, MatePagination, MateEntityCell, type MateColumn, MateMessage, MateInlineConfirm } from '@matecloud/ui'
import { adminApi } from '@matecloud/core'

defineOptions({ name: 'OnlineUsersView' })

const { t } = useI18n()

const columns = computed<MateColumn[]>(() => [
  { prop: 'username', label: t('online.user') },
  { prop: 'tokenCount', label: t('online.tokenCount'), width: 100 },
  { prop: 'loginTime', label: t('online.loginAt'), width: 180 },
  { prop: 'timeout', label: t('online.timeout'), width: 140 },
])

const loading = ref(false)
const rows = ref<any[]>([])
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

async function loadData() {
  loading.value = true
  try {
    const res = await adminApi.onlineUsers({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
    })
    const data: any = res.data ?? res
    rows.value = data.list ?? []
    total.value = data.total ?? 0
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadData()
}

function handleReset() {
  keyword.value = ''
  handleSearch()
}

async function handleKick(row: any) {
  try {
    await adminApi.kickOnlineUser(row.userId)
    MateMessage.success(t('online.kickSuccess', { name: row.username || row.userId }))
    loadData()
  } catch (e: any) {
    MateMessage.error(e?.msg || t('common.failed'))
  }
}

function formatTimeout(seconds: number): string {
  if (seconds <= 0) return t('online.expired')
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  if (h > 0) return `${h}h ${m}m`
  return `${m}m`
}

onMounted(loadData)
</script>

<style scoped>
</style>
