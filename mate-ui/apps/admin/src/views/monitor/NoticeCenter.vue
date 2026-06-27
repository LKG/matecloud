<template>
  <MatePageCard :title="t('notice.title')" :description="t('notice.desc')">
    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <el-input
        v-model="search.keyword"
        :placeholder="t('notice.targetPlaceholder')"
        style="width: 180px"
        clearable
        @keyup.enter="handleSearch"
      />
      <el-select v-model="search.channel" :placeholder="t('notice.channel')" clearable style="width: 130px">
        <el-option v-for="c in channels" :key="c" :label="c" :value="c" />
      </el-select>
      <el-select v-model="search.status" :placeholder="t('notice.status')" clearable style="width: 130px">
        <el-option :label="t('notice.statusPending')" :value="0" />
        <el-option :label="t('notice.statusSuccess')" :value="1" />
        <el-option :label="t('notice.statusFailed')" :value="2" />
      </el-select>
      <el-button type="primary" @click="openSend">{{ t('notice.send') }}</el-button>
    </MateSearchBar>

    <MateTable
      :columns="columns"
      :data="rows"
      :loading="loading"
      row-key="id"
      :action-width="90"
      :action-label="t('common.action')"
      :empty-text="t('common.noData')"
    >
      <template #col-target="{ row }">
        <span class="mc-mono">{{ row.target }}</span>
      </template>
      <template #col-status="{ row }">
        <MateBadge :status="row.status">{{ row.statusLabel }}</MateBadge>
      </template>
      <template #actions="{ row }">
        <button
          v-if="row.status === 2"
          class="mc-action-btn mc-action-btn--success"
          @click="handleRetry(row)"
        >{{ t('notice.retry') }}</button>
      </template>
    </MateTable>

    <MatePagination
      v-model:page-num="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      @change="loadData"
    />

    <!-- Send dialog -->
    <MateDialog v-model="sendVisible" :title="t('notice.send')" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item :label="t('notice.channel')" prop="channel">
          <el-select v-model="form.channel" style="width: 100%">
            <el-option v-for="c in channels" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('notice.businessType')" prop="businessType">
          <el-select v-model="form.businessType" style="width: 100%">
            <el-option v-for="b in businessTypes" :key="b" :label="b" :value="b" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('notice.target')" prop="target">
          <el-input v-model="form.target" :placeholder="t('notice.targetPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('notice.content')" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sendVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="sending" @click="handleSend">{{ t('notice.send') }}</el-button>
      </template>
    </MateDialog>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance } from 'element-plus'

defineOptions({ name: 'NoticeCenterView' })
import { MatePageCard, MateTable, MateBadge, MateSearchBar, MatePagination, MateDialog, type MateColumn, MateMessage } from '@matecloud/ui'
import { noticeApi, type NoticeRecord } from '@matecloud/core'

const { t } = useI18n()

const channels = ['SMS', 'EMAIL', 'WECHAT', 'PUSH']
const businessTypes = ['VERIFY_CODE', 'ORDER_NOTIFY', 'SYSTEM_ALERT', 'MARKETING']

const columns: MateColumn[] = [
  { prop: 'channel', label: t('notice.channel'), width: 100 },
  { prop: 'target', label: t('notice.target'), width: 180 },
  { prop: 'businessType', label: t('notice.businessType'), width: 140 },
  { prop: 'status', label: t('notice.status'), width: 100, align: 'center' },
  { prop: 'content', label: t('notice.content') },
  { prop: 'retryCount', label: t('notice.retryCount'), width: 90, align: 'center' },
  { prop: 'createdAt', label: t('notice.createdAt'), width: 160 },
]

const loading = ref(false)
const rows = ref<NoticeRecord[]>([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

const search = reactive({
  keyword: '',
  channel: undefined as string | undefined,
  status: undefined as number | undefined,
})

async function loadData() {
  loading.value = true
  try {
    const res = await noticeApi.page({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: search.keyword || undefined,
      channel: search.channel,
      status: search.status,
    })
    const data: any = (res as any).data ?? res
    rows.value = data.list ?? []
    total.value = data.total ?? 0
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() {
  search.keyword = ''
  search.channel = undefined
  search.status = undefined
  handleSearch()
}

async function handleRetry(row: NoticeRecord) {
  try {
    await noticeApi.retry(row.id)
    MateMessage.success(t('common.success'))
    loadData()
  } catch { /* interceptor */ }
}

// ---- send dialog ----
const sendVisible = ref(false)
const sending = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  channel: 'SMS' as any,
  businessType: 'VERIFY_CODE' as any,
  target: '',
  content: '',
})
const rules = {
  channel: [{ required: true, message: t('notice.channel'), trigger: 'change' }],
  businessType: [{ required: true, message: t('notice.businessType'), trigger: 'change' }],
  target: [{ required: true, message: t('notice.target'), trigger: 'blur' }],
  content: [{ required: true, message: t('notice.content'), trigger: 'blur' }],
}

function openSend() {
  Object.assign(form, { channel: 'SMS', businessType: 'VERIFY_CODE', target: '', content: '' })
  sendVisible.value = true
}

async function handleSend() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  sending.value = true
  try {
    await noticeApi.send({ ...form })
    MateMessage.success(t('common.success'))
    sendVisible.value = false
    handleSearch()
  } finally {
    sending.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.mc-mono { font-family: var(--mc-font-mono, monospace); }
</style>
