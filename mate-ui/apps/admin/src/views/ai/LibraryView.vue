<template>
  <MatePageCard :title="t('ai.library.title')" :description="t('ai.library.description')">
    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <el-input v-model="search.keyword" :placeholder="t('ai.library.searchPlaceholder')"
                style="width: 220px" clearable @keyup.enter="handleSearch" />
      <el-select v-model="search.status" :placeholder="t('ai.library.statusPlaceholder')" clearable style="width: 120px">
        <el-option :label="t('ai.library.statusActive')" :value="0" />
        <el-option :label="t('ai.library.statusArchived')" :value="1" />
      </el-select>
    </MateSearchBar>

    <MateTable :columns="columns" :data="rows" :loading="loading"
               row-key="id" :action-width="160" :action-label="t('common.action')">
      <template #col-title="{ row }">
        <a class="ai-library__title" @click="open(row)">{{ row.title }}</a>
      </template>
      <template #col-agentCode="{ row }">
        <AiAgentBadge v-if="row.agentCode" :code="row.agentCode" :label="row.agentCode" size="sm" />
        <span v-else>-</span>
      </template>
      <template #col-status="{ row }">
        <MateBadge :status="row.status">{{ row.statusLabel }}</MateBadge>
      </template>
      <template #col-messageCount="{ row }">
        <span class="mc-mono">{{ row.messageCount || 0 }}</span>
      </template>
      <template #actions="{ row }">
        <button class="mc-action-btn mc-action-btn--primary" @click="open(row)">{{ t('ai.library.open') }}</button>
        <button class="mc-action-btn mc-action-btn--danger" @click="remove(row)">{{ t('ai.library.delete') }}</button>
      </template>
    </MateTable>

    <MatePagination v-model:page-num="pageNum" v-model:page-size="pageSize"
                    :total="total" @change="loadData" />
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { aiApi, type ConversationView } from '@matecloud/core'
import { MatePageCard, MateSearchBar, MateTable, MateBadge, MatePagination, AiAgentBadge, type MateColumn, MateMessage, MateMessageBox } from '@matecloud/ui'

defineOptions({ name: 'AiLibraryView' })

const { t } = useI18n()
const router = useRouter()

const columns = computed<MateColumn[]>(() => [
  { prop: 'title', label: t('ai.library.colTitle') },
  { prop: 'agentCode', label: t('ai.library.colAgent'), width: 160 },
  { prop: 'model', label: t('ai.library.colModel'), width: 180 },
  { prop: 'messageCount', label: t('ai.library.colMessages'), width: 90, align: 'center' },
  { prop: 'status', label: t('ai.library.colStatus'), width: 90, align: 'center' },
  { prop: 'lastActiveAt', label: t('ai.library.colLastActive'), width: 170 },
  { prop: 'createdAt', label: t('ai.library.colCreatedAt'), width: 170 },
])

const loading = ref(false)
const rows = ref<ConversationView[]>([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const search = reactive({ keyword: '', status: undefined as number | undefined })

async function loadData() {
  loading.value = true
  try {
    const res: any = await aiApi.conversationPage({
      pageNum: pageNum.value, pageSize: pageSize.value,
      keyword: search.keyword || undefined, status: search.status,
    })
    rows.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() {
  search.keyword = ''; search.status = undefined; handleSearch()
}

function open(row: ConversationView) {
  router.push({ path: '/ai/chat', query: { id: row.id } })
}

async function remove(row: ConversationView) {
  await MateMessageBox.confirm(t('ai.library.deleteConfirm', { title: row.title }), t('ai.common.confirmTitle'), { type: 'warning' })
  await aiApi.conversationDelete(row.id)
  MateMessage.success(t('ai.common.deleted'))
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.ai-library__title {
  color: var(--mc-primary);
  cursor: pointer;
  font-weight: 500;
}
.ai-library__title:hover { text-decoration: underline; }
.mc-mono { font-family: var(--mc-font-mono, monospace); font-size: 12px; }
</style>
