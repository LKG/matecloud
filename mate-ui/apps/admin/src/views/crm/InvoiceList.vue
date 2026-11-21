<template>
  <MatePageCard title="发票管理" description="开票申请 → 开票 / 驳回 → 作废">
    <template #actions>
      <MateButton v-permission="'crm:invoice:add'" variant="primary" @click="openDialog()">
        <template #icon><Plus :size="14" /></template>开票申请
      </MateButton>
    </template>

    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <MateInput v-model="query.keyword" placeholder="抬头 / 合同编号" clearable style="width: 220px" @enter="handleSearch">
        <template #prefix><Search :size="14" /></template>
      </MateInput>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
        <el-option label="已申请" :value="0" /><el-option label="已开票" :value="1" />
        <el-option label="已驳回" :value="2" /><el-option label="已作废" :value="3" />
      </el-select>
      <CrmAdvancedFilter v-model="advGroups" module="invoice" @search="handleSearch" />
    </MateSearchBar>

    <MateTable :columns="columns" :data="rows" :loading="loading" row-key="id" :action-width="240" action-label="操作" empty-text="暂无发票">
      <template #col-status="{ row }">
        <MateTag :variant="statusVariant(row.status)" dot>{{ statusLabel(row.status) }}</MateTag>
      </template>
      <template #actions="{ row }">
        <template v-if="row.status === 0">
          <button v-permission="'crm:invoice:issue'" class="mc-action-btn mc-action-btn--success" @click="openIssue(row)">开票</button>
          <button v-permission="'crm:invoice:issue'" class="mc-action-btn mc-action-btn--warn" @click="handleReject(row)">驳回</button>
        </template>
        <button v-if="row.status === 1" v-permission="'crm:invoice:void'" class="mc-action-btn mc-action-btn--warn" @click="handleVoid(row)">作废</button>
        <MateInlineConfirm title="确认删除?" @confirm="handleDelete(row)">
          <button v-permission="'crm:invoice:void'" class="mc-action-btn mc-action-btn--danger">删除</button>
        </MateInlineConfirm>
      </template>
    </MateTable>

    <MatePagination v-model:page-num="query.pageNum" v-model:page-size="query.pageSize" :total="total" @change="loadData" />

    <MateDialog v-model="dialogVisible" title="开票申请" :submitting="submitting" @submit="handleSubmit">
      <MateForm ref="formRef" :schema="formSchema" :model-value="form" label-position="top" @update:model-value="(v: any) => Object.assign(form, v)" />
    </MateDialog>

    <MateDialog v-model="issueVisible" title="开票" :submitting="submitting" @submit="handleIssue">
      <el-form label-position="top">
        <el-form-item label="发票号"><el-input v-model="issueForm.invoiceNo" placeholder="请输入发票号" /></el-form-item>
        <el-form-item label="开票日期"><el-date-picker v-model="issueForm.invoiceDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
      </el-form>
    </MateDialog>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Plus } from 'lucide-vue-next'
import { invoiceApi, type Invoice, type InvoiceCommand, type ConditionGroup } from '@matecloud/core'
import {
  MatePageCard, MateTable, MateButton, MateInput, MateTag, MateSearchBar, MatePagination,
  MateDialog, MateForm, MateMessage, MateInlineConfirm, type MateColumn, type FormSchema, type TagVariantProps,
} from '@matecloud/ui'
import CrmAdvancedFilter from '../../components/crm/CrmAdvancedFilter.vue'
import { buildAdvancedQuery, hasValidAdvanced } from '../../composables/useCrmAdvancedQuery'

defineOptions({ name: 'CrmInvoiceList' })

const columns: MateColumn[] = [
  { prop: 'title', label: '发票抬头', minWidth: 160 },
  { prop: 'contractNum', label: '合同编号', width: 150 },
  { prop: 'invoiceType', label: '类型', width: 110 },
  { prop: 'money', label: '金额', width: 120 },
  { prop: 'taxMoney', label: '税额', width: 110 },
  { prop: 'invoiceNo', label: '发票号', width: 140 },
  { prop: 'status', label: '状态', width: 100 },
]

function statusLabel(s?: number) { return ({ 0: '已申请', 1: '已开票', 2: '已驳回', 3: '已作废' } as any)[s ?? -1] || '—' }
function statusVariant(s?: number): NonNullable<TagVariantProps['variant']> {
  if (s === 1) return 'success'; if (s === 2 || s === 3) return 'danger'; return 'warning'
}

const loading = ref(false)
const rows = ref<Invoice[]>([])
const total = ref(0)
const query = reactive<{ keyword: string; status?: number; pageNum: number; pageSize: number }>({ keyword: '', status: undefined, pageNum: 1, pageSize: 20 })
const advGroups = ref<ConditionGroup[]>([])

async function loadData() {
  loading.value = true
  try {
    const res = hasValidAdvanced(advGroups.value)
      ? await invoiceApi.search(buildAdvancedQuery({
          pageNum: query.pageNum, pageSize: query.pageSize,
          kw: query.keyword, kwFields: ['title', 'contractNum', 'invoiceNo'],
          base: [{ fieldKey: 'status', operator: 'EQ', values: query.status != null ? [String(query.status)] : [] }],
          groups: advGroups.value,
        }))
      : await invoiceApi.page({ ...query })
    rows.value = res.data.list; total.value = res.data.total
  } catch (e: any) { MateMessage.error(e?.msg || '加载失败') }
  finally { loading.value = false }
}
function handleSearch() { query.pageNum = 1; loadData() }
function handleReset() { query.keyword = ''; query.status = undefined; advGroups.value = []; handleSearch() }

const submitting = ref(false)

// ── Apply ──
const dialogVisible = ref(false)
const formRef = ref<InstanceType<typeof MateForm>>()
const form = reactive<Record<string, any>>({})
const formSchema = computed<FormSchema[]>(() => [
  { field: 'contractId', label: '合同 ID', type: 'input', rules: [{ required: true, message: '请输入合同 ID', trigger: 'blur' }] },
  { field: 'invoiceType', label: '发票类型', type: 'select', options: [
    { label: '普票', value: 'GENERAL' }, { label: '专票', value: 'SPECIAL' }, { label: '电子票', value: 'ELECTRONIC' }] },
  { field: 'title', label: '发票抬头', type: 'input', rules: [{ required: true, message: '请输入抬头', trigger: 'blur' }] },
  { field: 'taxNo', label: '税号', type: 'input' },
  { field: 'money', label: '开票金额', type: 'number', props: { min: 0, precision: 2 }, rules: [{ required: true, message: '请输入金额', trigger: 'blur' }] },
  { field: 'taxRate', label: '税率', type: 'number', props: { min: 0, max: 1, step: 0.01, precision: 2 } },
  { field: 'bankName', label: '开户行', type: 'input' },
  { field: 'bankAccount', label: '银行账号', type: 'input' },
  { field: 'addressInfo', label: '地址', type: 'input' },
  { field: 'remark', label: '备注', type: 'textarea' },
])
function openDialog() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { contractId: '', invoiceType: 'GENERAL', title: '', taxNo: '', money: 0, taxRate: 0, bankName: '', bankAccount: '', addressInfo: '', remark: '' })
  dialogVisible.value = true
}
async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return
  submitting.value = true
  try {
    await invoiceApi.apply({ ...form } as InvoiceCommand)
    MateMessage.success('申请成功'); dialogVisible.value = false; loadData()
  } catch (e: any) { MateMessage.error(e?.msg || '申请失败') }
  finally { submitting.value = false }
}

// ── Issue ──
const issueVisible = ref(false)
const issueTarget = ref<Invoice | null>(null)
const issueForm = reactive<{ invoiceNo: string; invoiceDate: string }>({ invoiceNo: '', invoiceDate: '' })
function openIssue(row: Invoice) {
  issueTarget.value = row; issueForm.invoiceNo = ''; issueForm.invoiceDate = ''
  issueVisible.value = true
}
async function handleIssue() {
  if (!issueTarget.value) return
  submitting.value = true
  try {
    await invoiceApi.issue(issueTarget.value.id, { invoiceNo: issueForm.invoiceNo, invoiceDate: issueForm.invoiceDate })
    MateMessage.success('已开票'); issueVisible.value = false; loadData()
  } catch (e: any) { MateMessage.error(e?.msg || '开票失败') }
  finally { submitting.value = false }
}

async function handleReject(row: Invoice) {
  try { await invoiceApi.reject(row.id); MateMessage.success('已驳回'); loadData() }
  catch (e: any) { MateMessage.error(e?.msg || '操作失败') }
}
async function handleVoid(row: Invoice) {
  try { await invoiceApi.voidInvoice(row.id); MateMessage.success('已作废'); loadData() }
  catch (e: any) { MateMessage.error(e?.msg || '操作失败') }
}
async function handleDelete(row: Invoice) {
  try { await invoiceApi.delete(row.id); MateMessage.success('删除成功'); loadData() }
  catch (e: any) { MateMessage.error(e?.msg || '删除失败') }
}

onMounted(loadData)
</script>
