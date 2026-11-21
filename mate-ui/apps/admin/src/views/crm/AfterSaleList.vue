<template>
  <MatePageCard title="售后工单" description="工单创建 → 受理 → 处理 → 完成 → 评价">
    <template #actions>
      <MateButton v-permission="'crm:aftersale:add'" variant="primary" @click="openDialog()">
        <template #icon><Plus :size="14" /></template>新建工单
      </MateButton>
    </template>

    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <MateInput v-model="query.keyword" placeholder="标题 / 工单号" clearable style="width: 200px" @enter="handleSearch">
        <template #prefix><Search :size="14" /></template>
      </MateInput>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
        <el-option v-for="s in STATUS" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <el-select v-model="query.type" placeholder="类型" clearable style="width: 120px">
        <el-option label="投诉" value="COMPLAINT" /><el-option label="报修" value="REPAIR" />
        <el-option label="咨询" value="CONSULT" /><el-option label="服务" value="SERVICE" />
      </el-select>
    </MateSearchBar>

    <MateTable :columns="columns" :data="rows" :loading="loading" row-key="id" :action-width="120" action-label="操作" empty-text="暂无工单">
      <template #col-status="{ row }">
        <MateTag :variant="statusVariant(row.status)" dot>{{ statusLabel(row.status) }}</MateTag>
      </template>
      <template #actions="{ row }">
        <button class="mc-action-btn" @click="openDrawer(row)">处理</button>
      </template>
    </MateTable>

    <MatePagination v-model:page-num="query.pageNum" v-model:page-size="query.pageSize" :total="total" @change="loadData" />

    <MateDialog v-model="dialogVisible" title="新建工单" :submitting="submitting" @submit="handleSubmit">
      <MateForm ref="formRef" :schema="formSchema" :model-value="form" label-position="top" @update:model-value="(v: any) => Object.assign(form, v)" />
    </MateDialog>

    <MateDrawer v-model="drawerVisible" title="工单处理" size="600px">
      <template v-if="detail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="工单号">{{ detail.afterSale.ticketNo }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ detail.afterSale.title }}</el-descriptions-item>
          <el-descriptions-item label="客户">{{ detail.afterSale.customerName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="类型 / 优先级">{{ typeLabel(detail.afterSale.type) }} / {{ priorityLabel(detail.afterSale.priority) }}</el-descriptions-item>
          <el-descriptions-item label="状态"><MateTag :variant="statusVariant(detail.afterSale.status)" dot>{{ statusLabel(detail.afterSale.status) }}</MateTag></el-descriptions-item>
          <el-descriptions-item label="描述">{{ detail.afterSale.description || '—' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.afterSale.rating" label="评价">{{ detail.afterSale.rating }} 星 {{ detail.afterSale.ratingRemark }}</el-descriptions-item>
        </el-descriptions>

        <div class="handle-bar">
          <MateButton v-if="detail.afterSale.status === 'OPEN'" v-permission="'crm:aftersale:handle'" variant="primary" size="sm" @click="doAccept">受理</MateButton>
          <MateButton v-if="detail.afterSale.status === 'PROCESSING'" v-permission="'crm:aftersale:handle'" size="sm" @click="promptProcess">添加处理记录</MateButton>
          <MateButton v-if="detail.afterSale.status === 'PROCESSING'" v-permission="'crm:aftersale:handle'" variant="primary" size="sm" @click="promptResolve">标记完成</MateButton>
          <MateButton v-if="detail.afterSale.status === 'RESOLVED'" v-permission="'crm:aftersale:close'" size="sm" @click="promptClose">关闭工单</MateButton>
          <el-input v-if="detail.afterSale.status === 'RESOLVED' || detail.afterSale.status === 'CLOSED'" v-model="rateValue" placeholder="评价 1-5" style="width: 120px" size="small" />
          <MateButton v-if="detail.afterSale.status === 'RESOLVED' || detail.afterSale.status === 'CLOSED'" v-permission="'crm:aftersale:handle'" size="sm" @click="doRate">提交评价</MateButton>
        </div>

        <h4 class="log-title">处理记录</h4>
        <el-timeline>
          <el-timeline-item v-for="log in detail.logs" :key="log.id" :timestamp="log.createdAt" placement="top">
            <div class="log-action">{{ actionLabel(log.action) }} · {{ log.operatorName }}</div>
            <div v-if="log.content" class="log-content">{{ log.content }}</div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </MateDrawer>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Plus } from 'lucide-vue-next'
import { afterSaleApi, type AfterSale, type AfterSaleCommand, type AfterSaleDetail } from '@matecloud/core'
import {
  MatePageCard, MateTable, MateButton, MateInput, MateTag, MateSearchBar, MatePagination,
  MateDialog, MateDrawer, MateForm, MateMessage, MateMessageBox, type MateColumn, type FormSchema, type TagVariantProps,
} from '@matecloud/ui'

defineOptions({ name: 'CrmAfterSaleList' })

const STATUS = [
  { value: 'OPEN', label: '待受理' }, { value: 'PROCESSING', label: '处理中' },
  { value: 'RESOLVED', label: '已完成' }, { value: 'CLOSED', label: '已关闭' },
]
function statusLabel(s?: string) { return STATUS.find((x) => x.value === s)?.label || s || '—' }
function statusVariant(s?: string): NonNullable<TagVariantProps['variant']> {
  if (s === 'RESOLVED') return 'success'; if (s === 'CLOSED') return 'info'
  if (s === 'PROCESSING') return 'warning'; return 'danger'
}
function typeLabel(s?: string) { return ({ COMPLAINT: '投诉', REPAIR: '报修', CONSULT: '咨询', SERVICE: '服务' } as any)[s || ''] || s || '—' }
function priorityLabel(s?: string) { return ({ HIGH: '高', MEDIUM: '中', LOW: '低' } as any)[s || ''] || s || '—' }
function actionLabel(a?: string) { return ({ CREATE: '创建', ACCEPT: '受理', PROCESS: '处理', FINISH: '完成', CLOSE: '关闭', RATE: '评价' } as any)[a || ''] || a || '' }

const columns: MateColumn[] = [
  { prop: 'ticketNo', label: '工单号', width: 160 },
  { prop: 'title', label: '标题', minWidth: 160 },
  { prop: 'customerName', label: '客户', width: 140 },
  { prop: 'type', label: '类型', width: 90 },
  { prop: 'priority', label: '优先级', width: 90 },
  { prop: 'status', label: '状态', width: 110 },
  { prop: 'handlerName', label: '处理人', width: 110 },
]

const loading = ref(false)
const rows = ref<AfterSale[]>([])
const total = ref(0)
const query = reactive<{ keyword: string; status?: string; type?: string; pageNum: number; pageSize: number }>({ keyword: '', status: undefined, type: undefined, pageNum: 1, pageSize: 20 })

async function loadData() {
  loading.value = true
  try { const res = await afterSaleApi.page({ ...query }); rows.value = res.data.list; total.value = res.data.total }
  catch (e: any) { MateMessage.error(e?.msg || '加载失败') }
  finally { loading.value = false }
}
function handleSearch() { query.pageNum = 1; loadData() }
function handleReset() { query.keyword = ''; query.status = undefined; query.type = undefined; handleSearch() }

const submitting = ref(false)
const dialogVisible = ref(false)
const formRef = ref<InstanceType<typeof MateForm>>()
const form = reactive<Record<string, any>>({})
const formSchema = computed<FormSchema[]>(() => [
  { field: 'title', label: '工单标题', type: 'input', rules: [{ required: true, message: '请输入标题', trigger: 'blur' }] },
  { field: 'customerId', label: '客户 ID', type: 'input', rules: [{ required: true, message: '请输入客户 ID', trigger: 'blur' }] },
  { field: 'customerName', label: '客户名称', type: 'input' },
  { field: 'contractId', label: '关联合同 ID', type: 'input' },
  { field: 'type', label: '类型', type: 'select', options: [
    { label: '投诉', value: 'COMPLAINT' }, { label: '报修', value: 'REPAIR' }, { label: '咨询', value: 'CONSULT' }, { label: '服务', value: 'SERVICE' }] },
  { field: 'priority', label: '优先级', type: 'select', options: [
    { label: '高', value: 'HIGH' }, { label: '中', value: 'MEDIUM' }, { label: '低', value: 'LOW' }] },
  { field: 'description', label: '问题描述', type: 'textarea' },
])
function openDialog() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { title: '', customerId: '', customerName: '', contractId: '', type: 'CONSULT', priority: 'MEDIUM', description: '' })
  dialogVisible.value = true
}
async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return
  submitting.value = true
  try { await afterSaleApi.create({ ...form } as AfterSaleCommand); MateMessage.success('创建成功'); dialogVisible.value = false; loadData() }
  catch (e: any) { MateMessage.error(e?.msg || '创建失败') }
  finally { submitting.value = false }
}

// ── Drawer handling ──
const drawerVisible = ref(false)
const detail = ref<AfterSaleDetail | null>(null)
const rateValue = ref('')
async function loadDetail(id: string) {
  try { const res = await afterSaleApi.detail(id); detail.value = res.data }
  catch (e: any) { MateMessage.error(e?.msg || '加载失败') }
}
function openDrawer(row: AfterSale) { detail.value = null; rateValue.value = ''; drawerVisible.value = true; loadDetail(row.id) }

async function doAccept() { if (!detail.value) return; await run(() => afterSaleApi.accept(detail.value!.afterSale.id), '已受理') }
async function promptProcess() { const c = await askContent('添加处理记录'); if (c !== null && detail.value) await run(() => afterSaleApi.process(detail.value!.afterSale.id, c), '已记录') }
async function promptResolve() { const c = await askContent('完成说明'); if (c !== null && detail.value) await run(() => afterSaleApi.resolve(detail.value!.afterSale.id, c || '处理完成'), '工单已完成') }
async function promptClose() { const c = await askContent('关闭说明'); if (c !== null && detail.value) await run(() => afterSaleApi.close(detail.value!.afterSale.id, c || '关闭'), '工单已关闭') }
async function doRate() {
  if (!detail.value) return
  const r = Number(rateValue.value)
  if (!r || r < 1 || r > 5) { MateMessage.warning('请输入 1-5 的评分'); return }
  await run(() => afterSaleApi.rate(detail.value!.afterSale.id, r), '评价已提交')
}

async function run(fn: () => Promise<any>, okMsg: string) {
  try { await fn(); MateMessage.success(okMsg); if (detail.value) await loadDetail(detail.value.afterSale.id); loadData() }
  catch (e: any) { MateMessage.error(e?.msg || '操作失败') }
}
async function askContent(title: string): Promise<string | null> {
  try { const { value } = await MateMessageBox.prompt(title, title, { inputType: 'textarea', inputValue: '' }); return value }
  catch { return null }
}

onMounted(loadData)
</script>

<style scoped>
.handle-bar { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin: 16px 0; }
.log-title { margin: 8px 0; }
.log-action { font-size: 13px; font-weight: 500; }
.log-content { font-size: 13px; color: var(--el-text-color-regular); margin-top: 2px; }
</style>
