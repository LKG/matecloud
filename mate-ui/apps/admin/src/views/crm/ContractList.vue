<template>
  <MatePageCard title="合同管理" description="草稿 → 提交审批 → 通过归档的签约闭环">
    <template #actions>
      <MateButton v-permission="'crm:contract:add'" variant="primary" @click="openDialog()">
        <template #icon><Plus :size="14" /></template>新建合同
      </MateButton>
    </template>

    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <MateInput v-model="query.keyword" placeholder="合同名称 / 编号" clearable style="width: 220px" @enter="handleSearch">
        <template #prefix><Search :size="14" /></template>
      </MateInput>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
        <el-option v-for="s in STATUS" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <el-select v-model="query.mineOnly" placeholder="范围" clearable style="width: 120px">
        <el-option label="仅我的" :value="true" />
      </el-select>
    </MateSearchBar>

    <MateTable :columns="columns" :data="rows" :loading="loading" row-key="id" :action-width="280" action-label="操作" empty-text="暂无合同">
      <template #col-status="{ row }">
        <MateTag :variant="statusVariant(row.status)" dot>{{ statusLabel(row.status) }}</MateTag>
      </template>
      <template #actions="{ row }">
        <button class="mc-action-btn" @click="openDetail(row)">详情</button>
        <template v-if="row.status === 'DRAFT' || row.status === 'REJECTED'">
          <button v-permission="'crm:contract:edit'" class="mc-action-btn" @click="openDialog(row)">编辑</button>
          <button v-permission="'crm:contract:submit'" class="mc-action-btn mc-action-btn--success" @click="handleSubmitApproval(row)">提交审批</button>
        </template>
        <button v-if="row.status === 'APPROVED'" v-permission="'crm:contract:archive'" class="mc-action-btn" @click="handleArchive(row)">归档</button>
        <MateInlineConfirm v-if="row.status === 'DRAFT'" title="确认删除该合同?" @confirm="handleDelete(row)">
          <button v-permission="'crm:contract:delete'" class="mc-action-btn mc-action-btn--danger">删除</button>
        </MateInlineConfirm>
      </template>
    </MateTable>

    <MatePagination v-model:page-num="query.pageNum" v-model:page-size="query.pageSize" :total="total" @change="loadData" />

    <MateDialog v-model="dialogVisible" :title="editing ? '编辑合同' : '新建合同'" :submitting="submitting" width="720px" @submit="onSave">
      <MateForm ref="formRef" :schema="formSchema" :model-value="form" label-position="top" @update:model-value="(v: any) => Object.assign(form, v)" />
      <div class="lines-head">
        <span>签单产品</span>
        <MateButton size="sm" @click="addLine"><template #icon><Plus :size="12" /></template>添加行</MateButton>
      </div>
      <el-table :data="products" size="small" empty-text="未添加产品">
        <el-table-column label="产品名称"><template #default="{ row }"><el-input v-model="row.productName" size="small" /></template></el-table-column>
        <el-table-column label="单价" width="120"><template #default="{ row }"><el-input-number v-model="row.unitPrice" :min="0" size="small" controls-position="right" style="width:100%" /></template></el-table-column>
        <el-table-column label="数量" width="110"><template #default="{ row }"><el-input-number v-model="row.quantity" :min="0" size="small" controls-position="right" style="width:100%" /></template></el-table-column>
        <el-table-column label="折扣" width="110"><template #default="{ row }"><el-input-number v-model="row.discount" :min="0" :max="1" :step="0.1" size="small" controls-position="right" style="width:100%" /></template></el-table-column>
        <el-table-column label="操作" width="60"><template #default="{ $index }"><button class="mc-action-btn mc-action-btn--danger" @click="products.splice($index, 1)">删</button></template></el-table-column>
      </el-table>
    </MateDialog>

    <MateDrawer v-model="detailVisible" title="合同详情" size="560px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="合同名称">{{ detail.contract.contractName }}</el-descriptions-item>
        <el-descriptions-item label="合同编号">{{ detail.contract.contractNum || '—' }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ detail.contract.customerName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ detail.contract.money }}</el-descriptions-item>
        <el-descriptions-item label="已回款">{{ detail.contract.receivedMoney ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(detail.contract.status) }}</el-descriptions-item>
        <el-descriptions-item label="起止">{{ detail.contract.startTime || '—' }} ~ {{ detail.contract.endTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.contract.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
      <h4 class="detail-sub">签单产品</h4>
      <el-table :data="detail?.products || []" size="small" empty-text="无产品">
        <el-table-column prop="productName" label="名称" />
        <el-table-column prop="unitPrice" label="单价" width="90" />
        <el-table-column prop="quantity" label="数量" width="70" />
        <el-table-column prop="subtotal" label="小计" width="100" />
      </el-table>
    </MateDrawer>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Plus } from 'lucide-vue-next'
import { contractApi, approvalApi, type Contract, type ContractCommand, type ContractDetail, type ContractStatus, type ContractProductLine } from '@matecloud/core'
import {
  MatePageCard, MateTable, MateButton, MateInput, MateTag, MateSearchBar, MatePagination,
  MateDialog, MateDrawer, MateForm, MateMessage, MateInlineConfirm, type MateColumn, type FormSchema, type TagVariantProps,
} from '@matecloud/ui'

defineOptions({ name: 'CrmContractList' })

const STATUS: { value: ContractStatus; label: string }[] = [
  { value: 'DRAFT', label: '草稿' }, { value: 'PENDING', label: '审批中' }, { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已驳回' }, { value: 'ARCHIVED', label: '已归档' },
]
function statusLabel(s?: string) { return STATUS.find((x) => x.value === s)?.label || s || '—' }
function statusVariant(s?: string): NonNullable<TagVariantProps['variant']> {
  if (s === 'APPROVED' || s === 'ARCHIVED') return 'success'
  if (s === 'REJECTED') return 'danger'
  if (s === 'PENDING') return 'warning'
  return 'info'
}

const columns: MateColumn[] = [
  { prop: 'contractName', label: '合同名称', minWidth: 160 },
  { prop: 'contractNum', label: '编号', width: 150 },
  { prop: 'customerName', label: '客户', minWidth: 130 },
  { prop: 'money', label: '金额', width: 120 },
  { prop: 'receivedMoney', label: '已回款', width: 110 },
  { prop: 'status', label: '状态', width: 110 },
  { prop: 'ownerName', label: '负责人', width: 100 },
]

const loading = ref(false)
const rows = ref<Contract[]>([])
const total = ref(0)
const query = reactive<{ keyword: string; status?: string; mineOnly?: boolean; pageNum: number; pageSize: number }>({ keyword: '', status: undefined, mineOnly: undefined, pageNum: 1, pageSize: 20 })

async function loadData() {
  loading.value = true
  try {
    const res = await contractApi.page({ ...query })
    rows.value = res.data.list
    total.value = res.data.total
  } catch (e: any) { MateMessage.error(e?.msg || '加载失败') }
  finally { loading.value = false }
}
function handleSearch() { query.pageNum = 1; loadData() }
function handleReset() { query.keyword = ''; query.status = undefined; query.mineOnly = undefined; handleSearch() }

async function handleSubmitApproval(row: Contract) {
  // 编排入口在 approval 上下文: 建审批实例 + 合同转 PENDING (contractApi.submit 仅状态迁移)
  try { await approvalApi.submitContract(row.id); MateMessage.success('已提交审批'); loadData() }
  catch (e: any) { MateMessage.error(e?.msg || '提交失败') }
}
async function handleArchive(row: Contract) {
  try { await contractApi.archive(row.id); MateMessage.success('已归档'); loadData() }
  catch (e: any) { MateMessage.error(e?.msg || '归档失败') }
}
async function handleDelete(row: Contract) {
  try { await contractApi.delete(row.id); MateMessage.success('删除成功'); loadData() }
  catch (e: any) { MateMessage.error(e?.msg || '删除失败') }
}

// ── Detail drawer ──
const detailVisible = ref(false)
const detail = ref<ContractDetail | null>(null)
async function openDetail(row: Contract) {
  detail.value = null
  detailVisible.value = true
  try { const res = await contractApi.detail(row.id); detail.value = res.data }
  catch (e: any) { MateMessage.error(e?.msg || '加载详情失败') }
}

// ── Create / Edit ──
const dialogVisible = ref(false)
const editing = ref<Contract | null>(null)
const submitting = ref(false)
const formRef = ref<InstanceType<typeof MateForm>>()
const form = reactive<Record<string, any>>({})
const products = ref<ContractProductLine[]>([])

const formSchema = computed<FormSchema[]>(() => [
  { field: 'contractName', label: '合同名称', type: 'input', rules: [{ required: true, message: '请输入合同名称', trigger: 'blur' }] },
  { field: 'contractNum', label: '合同编号', type: 'input' },
  { field: 'customerId', label: '客户 ID', type: 'input', rules: [{ required: true, message: '请输入客户 ID', trigger: 'blur' }] },
  { field: 'customerName', label: '客户名称', type: 'input' },
  { field: 'contactName', label: '联系人', type: 'input' },
  { field: 'signTime', label: '签单日期', type: 'date' },
  { field: 'startTime', label: '开始日期', type: 'date' },
  { field: 'endTime', label: '结束日期', type: 'date' },
  { field: 'remark', label: '备注', type: 'textarea' },
])

function addLine() { products.value.push({ productName: '', unitPrice: 0, quantity: 1, discount: 1 }) }

async function openDialog(row?: Contract) {
  editing.value = row || null
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, {
    contractName: row?.contractName || '', contractNum: row?.contractNum || '', customerId: row?.customerId || '',
    customerName: row?.customerName || '', contactName: row?.contactName || '', signTime: row?.signTime || '',
    startTime: row?.startTime || '', endTime: row?.endTime || '', remark: row?.remark || '',
  })
  products.value = []
  if (row) {
    try { const d = await contractApi.detail(row.id); products.value = (d.data.products || []).map((p) => ({ ...p })) }
    catch { /* ignore */ }
  }
  dialogVisible.value = true
}

async function onSave() {
  const valid = await formRef.value?.validate()
  if (!valid) return
  submitting.value = true
  try {
    const payload = { ...form, products: products.value.filter((p) => p.productName) } as ContractCommand
    if (editing.value) await contractApi.update(editing.value.id, payload)
    else await contractApi.create(payload)
    MateMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch (e: any) { MateMessage.error(e?.msg || '保存失败') }
  finally { submitting.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.lines-head { display: flex; justify-content: space-between; align-items: center; margin: 16px 0 8px; font-weight: 500; }
.detail-sub { margin: 16px 0 8px; }
</style>
