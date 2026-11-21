<template>
  <MatePageCard title="审批中心" description="合同签约的两级审批: 我发起的 / 待我审批">
    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <MateInput v-model="query.keyword" placeholder="标题 / 单号" clearable style="width: 220px" @enter="handleSearch">
        <template #prefix><Search :size="14" /></template>
      </MateInput>
    </MateSearchBar>

    <el-tabs v-model="tab" @tab-change="onTabChange">
      <el-tab-pane label="我发起的" name="mine">
        <MateTable :columns="columns" :data="rows" :loading="loading" row-key="id" :action-width="160" action-label="操作" empty-text="暂无发起的审批">
          <template #actions="{ row }">
            <button class="mc-action-btn" @click="openDetail(row)">查看</button>
            <button v-if="row.status === 'PENDING'" class="mc-action-btn mc-action-btn--warn" @click="handleCancel(row)">撤回</button>
          </template>
        </MateTable>
      </el-tab-pane>

      <el-tab-pane label="待我审批" name="todo">
        <MateTable :columns="columns" :data="rows" :loading="loading" row-key="id" :action-width="220" action-label="操作" empty-text="暂无待办审批">
          <template #actions="{ row }">
            <button class="mc-action-btn" @click="openDetail(row)">查看</button>
            <button v-permission="'crm:approval:audit'" class="mc-action-btn mc-action-btn--success" @click="handleApprove(row)">通过</button>
            <button v-permission="'crm:approval:audit'" class="mc-action-btn mc-action-btn--danger" @click="handleReject(row)">驳回</button>
          </template>
        </MateTable>
      </el-tab-pane>
    </el-tabs>

    <MatePagination v-model:page-num="query.pageNum" v-model:page-size="query.pageSize" :total="total" @change="loadData" />

    <MateDrawer v-model="detailVisible" title="审批详情" size="560px">
      <template v-if="detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="标题">{{ detail.approval.title }}</el-descriptions-item>
          <el-descriptions-item label="业务类型">{{ bizTypeLabel(detail.approval.bizType) }}</el-descriptions-item>
          <el-descriptions-item label="业务单号">{{ detail.approval.bizNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ detail.approval.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="状态"><MateTag :variant="statusVariant(detail.approval.status)" dot>{{ statusLabel(detail.approval.status) }}</MateTag></el-descriptions-item>
        </el-descriptions>

        <h4 class="sec">审批链</h4>
        <el-steps :active="activeStep" finish-status="success" align-center>
          <el-step v-for="(n, i) in detail.nodes" :key="i" :title="n.nodeName" :description="n.approverName" />
        </el-steps>

        <h4 class="sec">流转记录</h4>
        <el-timeline>
          <el-timeline-item v-for="r in detail.records" :key="r.id" :timestamp="r.actedAt" placement="top">
            <div class="rec">{{ actionLabel(r.action) }} · {{ r.approverName }}<span v-if="r.remark"> — {{ r.remark }}</span></div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </MateDrawer>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Search } from 'lucide-vue-next'
import { approvalApi, type Approval, type ApprovalDetail } from '@matecloud/core'
import {
  MatePageCard, MateTable, MateInput, MateTag, MateSearchBar, MatePagination, MateDrawer,
  MateMessage, MateMessageBox, type MateColumn, type TagVariantProps,
} from '@matecloud/ui'

defineOptions({ name: 'CrmApprovalCenter' })

const tab = ref<'mine' | 'todo'>('mine')

const columns: MateColumn[] = [
  { prop: 'title', label: '标题', minWidth: 180 },
  { prop: 'bizType', label: '业务类型', width: 120 },
  { prop: 'bizNo', label: '业务单号', width: 160 },
  { prop: 'applicantName', label: '申请人', width: 120 },
  { prop: 'status', label: '状态', width: 110 },
  { prop: 'createdAt', label: '发起时间', width: 160, type: 'datetime' },
]

function bizTypeLabel(s?: string) { return ({ CONTRACT: '合同' } as any)[s || ''] || s || '—' }
function statusLabel(s?: string) { return ({ PENDING: '审批中', APPROVED: '已通过', REJECTED: '已驳回', CANCELED: '已撤回' } as any)[s || ''] || s || '—' }
function statusVariant(s?: string): NonNullable<TagVariantProps['variant']> {
  if (s === 'APPROVED') return 'success'; if (s === 'REJECTED') return 'danger'
  if (s === 'CANCELED') return 'info'; return 'warning'
}
function actionLabel(a?: string) { return ({ SUBMIT: '提交', APPROVE: '通过', REJECT: '驳回', CANCEL: '撤回' } as any)[a || ''] || a || '' }

const loading = ref(false)
const rows = ref<Approval[]>([])
const total = ref(0)
const query = reactive<{ keyword: string; pageNum: number; pageSize: number }>({ keyword: '', pageNum: 1, pageSize: 20 })

async function loadData() {
  loading.value = true
  try {
    const res = tab.value === 'mine'
      ? await approvalApi.pageMine({ ...query })
      : await approvalApi.pageTodo({ keyword: query.keyword, pageNum: query.pageNum, pageSize: query.pageSize })
    rows.value = res.data.list; total.value = res.data.total
  } catch (e: any) { MateMessage.error(e?.msg || '加载失败') }
  finally { loading.value = false }
}
function onTabChange() { query.pageNum = 1; loadData() }
function handleSearch() { query.pageNum = 1; loadData() }
function handleReset() { query.keyword = ''; handleSearch() }

// ── Detail ──
const detailVisible = ref(false)
const detail = ref<ApprovalDetail | null>(null)
const activeStep = computed(() => {
  if (!detail.value) return 0
  const cur = detail.value.approval.currentNode
  return cur == null || cur < 0 ? detail.value.nodes.length : cur
})
async function openDetail(row: Approval) {
  detail.value = null; detailVisible.value = true
  try { const res = await approvalApi.detail(row.id); detail.value = res.data }
  catch (e: any) { MateMessage.error(e?.msg || '加载失败') }
}

async function handleApprove(row: Approval) {
  try { const { value } = await promptBox('审批意见'); await approvalApi.approve(row.id, value); MateMessage.success('已通过'); loadData() }
  catch (e: any) { if (e !== 'cancel') MateMessage.error(e?.msg || '操作失败') }
}
async function handleReject(row: Approval) {
  try { const { value } = await promptBox('驳回意见'); await approvalApi.reject(row.id, value); MateMessage.success('已驳回'); loadData() }
  catch (e: any) { if (e !== 'cancel') MateMessage.error(e?.msg || '操作失败') }
}
async function handleCancel(row: Approval) {
  try { await approvalApi.cancel(row.id); MateMessage.success('已撤回'); loadData() }
  catch (e: any) { MateMessage.error(e?.msg || '操作失败') }
}

function promptBox(title: string) { return MateMessageBox.prompt(title, title, { inputValue: '' }) }

onMounted(loadData)
</script>

<style scoped>
.sec { margin: 16px 0 8px; }
.rec { font-size: 13px; }
</style>
