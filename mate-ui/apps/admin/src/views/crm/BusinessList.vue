<template>
  <MatePageCard title="商机管理" description="销售漏斗: 初次接触 → 需求确认 → 方案报价 → 商务谈判 → 赢单/输单">
    <template #actions>
      <el-radio-group v-model="view" size="small" style="margin-right: 12px">
        <el-radio-button value="list">列表</el-radio-button>
        <el-radio-button value="board">看板</el-radio-button>
      </el-radio-group>
      <MateButton v-permission="'crm:business:add'" variant="primary" @click="openDialog()">
        <template #icon><Plus :size="14" /></template>新建商机
      </MateButton>
    </template>

    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <MateInput v-model="query.keyword" placeholder="商机名称 / 客户" clearable style="width: 220px" @enter="handleSearch">
        <template #prefix><Search :size="14" /></template>
      </MateInput>
      <el-select v-model="query.stage" placeholder="阶段" clearable style="width: 140px">
        <el-option v-for="s in STAGES" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
    </MateSearchBar>

    <!-- 列表视图 -->
    <template v-if="view === 'list'">
      <MateTable :columns="columns" :data="rows" :loading="loading" row-key="id" :action-width="260" action-label="操作" empty-text="暂无商机">
        <template #col-stage="{ row }">
          <MateTag :variant="stageVariant(row.stage)" dot>{{ stageLabel(row.stage) }}</MateTag>
        </template>
        <template #actions="{ row }">
          <button v-permission="'crm:business:edit'" class="mc-action-btn" @click="openDialog(row)">编辑</button>
          <button v-if="!isTerminal(row.stage)" v-permission="'crm:business:stage'" class="mc-action-btn" @click="handleAdvance(row)">推进</button>
          <button v-if="!isTerminal(row.stage)" v-permission="'crm:business:stage'" class="mc-action-btn mc-action-btn--success" @click="handleWin(row)">赢单</button>
          <button v-if="!isTerminal(row.stage)" v-permission="'crm:business:stage'" class="mc-action-btn mc-action-btn--warn" @click="handleLose(row)">输单</button>
          <button v-if="row.stage === 'WIN'" v-permission="'crm:contract:add'" class="mc-action-btn mc-action-btn--success" @click="handleToContract(row)">转合同</button>
        </template>
      </MateTable>
      <MatePagination v-model:page-num="query.pageNum" v-model:page-size="query.pageSize" :total="total" @change="loadData" />
    </template>

    <!-- 看板视图 -->
    <div v-else v-loading="loading" class="board">
      <div v-for="s in STAGES" :key="s.value" class="board-col">
        <div class="board-col__head">{{ s.label }} <span class="board-col__count">{{ grouped[s.value]?.length || 0 }}</span></div>
        <div class="board-col__body">
          <div v-for="b in grouped[s.value] || []" :key="b.id" class="biz-card">
            <div class="biz-card__name">{{ b.businessName }}</div>
            <div class="biz-card__cust">{{ b.customerName || '—' }}</div>
            <div class="biz-card__foot">
              <span class="biz-card__money">{{ b.money ?? 0 }}</span>
              <button v-if="!isTerminal(b.stage)" v-permission="'crm:business:stage'" class="mc-action-btn" @click="handleAdvance(b)">推进 ›</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <MateDialog v-model="dialogVisible" :title="editing ? '编辑商机' : '新建商机'" :submitting="submitting" width="720px" @submit="handleSubmit">
      <MateForm ref="formRef" :schema="formSchema" :model-value="form" label-position="top" @update:model-value="(v: any) => Object.assign(form, v)" />
      <div class="lines-head">
        <span>报价产品</span>
        <MateButton size="sm" @click="addLine"><template #icon><Plus :size="12" /></template>添加行</MateButton>
      </div>
      <el-table :data="products" size="small" empty-text="未添加产品">
        <el-table-column label="产品名称"><template #default="{ row }"><el-input v-model="row.productName" size="small" /></template></el-table-column>
        <el-table-column label="单价" width="130"><template #default="{ row }"><el-input-number v-model="row.unitPrice" :min="0" size="small" controls-position="right" style="width:100%" /></template></el-table-column>
        <el-table-column label="数量" width="130"><template #default="{ row }"><el-input-number v-model="row.quantity" :min="0" size="small" controls-position="right" style="width:100%" /></template></el-table-column>
        <el-table-column label="操作" width="70"><template #default="{ $index }"><button class="mc-action-btn mc-action-btn--danger" @click="products.splice($index, 1)">删</button></template></el-table-column>
      </el-table>
    </MateDialog>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Plus } from 'lucide-vue-next'
import { businessApi, contractApi, type Business, type BusinessCommand, type BusinessProductLine, type BusinessStage } from '@matecloud/core'
import {
  MatePageCard, MateTable, MateButton, MateInput, MateTag, MateSearchBar, MatePagination,
  MateDialog, MateForm, MateMessage, type MateColumn, type FormSchema, type TagVariantProps,
} from '@matecloud/ui'

defineOptions({ name: 'CrmBusinessList' })

const STAGES: { value: BusinessStage; label: string }[] = [
  { value: 'INITIAL', label: '初次接触' }, { value: 'DEMAND', label: '需求确认' },
  { value: 'PLAN', label: '方案报价' }, { value: 'NEGOTIATION', label: '商务谈判' },
  { value: 'WIN', label: '赢单' }, { value: 'LOSE', label: '输单' },
]
function stageLabel(s?: string) { return STAGES.find((x) => x.value === s)?.label || s || '—' }
function isTerminal(s?: string) { return s === 'WIN' || s === 'LOSE' }
function stageVariant(s?: string): NonNullable<TagVariantProps['variant']> {
  if (s === 'WIN') return 'success'; if (s === 'LOSE') return 'danger'
  if (s === 'NEGOTIATION') return 'warning'; return 'primary'
}

const columns: MateColumn[] = [
  { prop: 'businessName', label: '商机名称', minWidth: 160 },
  { prop: 'customerName', label: '客户', minWidth: 140 },
  { prop: 'stage', label: '阶段', width: 120 },
  { prop: 'money', label: '金额', width: 120 },
  { prop: 'dealProb', label: '概率%', width: 90 },
  { prop: 'ownerName', label: '负责人', width: 110 },
]

const view = ref<'list' | 'board'>('list')
const loading = ref(false)
const rows = ref<Business[]>([])
const total = ref(0)
const query = reactive<{ keyword: string; stage?: string; pageNum: number; pageSize: number }>({ keyword: '', stage: undefined, pageNum: 1, pageSize: 20 })

const grouped = computed<Record<string, Business[]>>(() => {
  const g: Record<string, Business[]> = {}
  for (const b of rows.value) (g[b.stage] ||= []).push(b)
  return g
})

async function loadData() {
  loading.value = true
  try {
    const params = view.value === 'board' ? { ...query, pageNum: 1, pageSize: 200 } : { ...query }
    const res = await businessApi.page(params)
    rows.value = res.data.list
    total.value = res.data.total
  } catch (e: any) { MateMessage.error(e?.msg || '加载失败') }
  finally { loading.value = false }
}
function handleSearch() { query.pageNum = 1; loadData() }
function handleReset() { query.keyword = ''; query.stage = undefined; handleSearch() }

async function handleAdvance(row: Business) {
  try { await businessApi.advance(row.id); MateMessage.success('已推进到下一阶段'); loadData() }
  catch (e: any) { MateMessage.error(e?.msg || '推进失败') }
}
async function handleWin(row: Business) {
  try { await businessApi.win(row.id); MateMessage.success('赢单成功'); loadData() }
  catch (e: any) { MateMessage.error(e?.msg || '操作失败') }
}
async function handleLose(row: Business) {
  try { await businessApi.lose(row.id); MateMessage.success('已标记输单'); loadData() }
  catch (e: any) { MateMessage.error(e?.msg || '操作失败') }
}
async function handleToContract(row: Business) {
  try {
    const res = await contractApi.fromBusiness(row.id)
    MateMessage.success('已生成合同草稿: ' + (res.data || ''))
  } catch (e: any) { MateMessage.error(e?.msg || '转合同失败') }
}

const dialogVisible = ref(false)
const editing = ref<Business | null>(null)
const submitting = ref(false)
const formRef = ref<InstanceType<typeof MateForm>>()
const form = reactive<Record<string, any>>({})
const products = ref<BusinessProductLine[]>([])

const formSchema = computed<FormSchema[]>(() => [
  { field: 'businessName', label: '商机名称', type: 'input', rules: [{ required: true, message: '请输入商机名称', trigger: 'blur' }] },
  { field: 'customerId', label: '客户 ID', type: 'input', rules: [{ required: true, message: '请输入客户 ID', trigger: 'blur' }] },
  { field: 'customerName', label: '客户名称', type: 'input' },
  { field: 'contactName', label: '联系人', type: 'input' },
  { field: 'source', label: '来源', type: 'input' },
  { field: 'dealProb', label: '成交概率(%)', type: 'number', props: { max: 100 } },
  { field: 'dealDate', label: '预计成交日', type: 'date' },
  { field: 'nextStep', label: '下一步计划', type: 'input' },
  { field: 'remark', label: '备注', type: 'textarea' },
])

function addLine() { products.value.push({ productName: '', unitPrice: 0, quantity: 1 }) }

async function openDialog(row?: Business) {
  editing.value = row || null
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, {
    businessName: row?.businessName || '', customerId: row?.customerId || '', customerName: row?.customerName || '',
    contactName: row?.contactName || '', source: row?.source || '', dealProb: row?.dealProb ?? 50,
    dealDate: row?.dealDate || '', nextStep: row?.nextStep || '', remark: row?.remark || '',
  })
  products.value = []
  if (row) {
    try { const d = await businessApi.detail(row.id); products.value = (d.data.products || []).map((p) => ({ ...p })) }
    catch { /* ignore */ }
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return
  submitting.value = true
  try {
    const payload = { ...form, products: products.value.filter((p) => p.productName) } as BusinessCommand
    if (editing.value) await businessApi.update(editing.value.id, payload)
    else await businessApi.create(payload)
    MateMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch (e: any) { MateMessage.error(e?.msg || '保存失败') }
  finally { submitting.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.board { display: flex; gap: 12px; overflow-x: auto; align-items: flex-start; }
.board-col { flex: 0 0 220px; background: var(--el-fill-color-light); border-radius: 8px; padding: 8px; }
.board-col__head { font-weight: 600; font-size: 13px; padding: 4px 6px 8px; display: flex; justify-content: space-between; }
.board-col__count { color: var(--el-text-color-secondary); }
.board-col__body { display: flex; flex-direction: column; gap: 8px; min-height: 40px; }
.biz-card { background: var(--el-bg-color); border: 1px solid var(--el-border-color-lighter); border-radius: 6px; padding: 8px 10px; }
.biz-card__name { font-size: 13px; font-weight: 500; }
.biz-card__cust { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 2px; }
.biz-card__foot { display: flex; justify-content: space-between; align-items: center; margin-top: 6px; }
.biz-card__money { font-size: 13px; color: var(--el-color-primary); }
.lines-head { display: flex; justify-content: space-between; align-items: center; margin: 16px 0 8px; font-weight: 500; }
</style>
