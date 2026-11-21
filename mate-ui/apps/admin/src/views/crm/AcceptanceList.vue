<template>
  <MatePageCard title="回款管理" description="回款计划分期维护 + 回款到账登记">
    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <MateInput v-model="query.keyword" placeholder="合同编号" clearable style="width: 200px" @enter="handleSearch">
        <template #prefix><Search :size="14" /></template>
      </MateInput>
      <el-select v-model="query.status" placeholder="计划状态" clearable style="width: 140px">
        <el-option label="待回款" :value="0" /><el-option label="部分回款" :value="1" />
        <el-option label="已结清" :value="2" /><el-option label="已逾期" :value="3" />
      </el-select>
    </MateSearchBar>

    <el-tabs v-model="tab" @tab-change="onTabChange">
      <!-- 回款计划 -->
      <el-tab-pane label="回款计划" name="plans">
        <div class="toolbar">
          <MateButton v-permission="'crm:repayment:plan'" variant="primary" @click="openPlanDialog()">
            <template #icon><Plus :size="14" /></template>新增计划
          </MateButton>
        </div>
        <MateTable :columns="planColumns" :data="plans" :loading="loading" row-key="id" :action-width="220" action-label="操作" empty-text="暂无回款计划">
          <template #col-status="{ row }">
            <MateTag :variant="planStatusVariant(row.status)" dot>{{ planStatusLabel(row.status) }}</MateTag>
          </template>
          <template #actions="{ row }">
            <button v-permission="'crm:repayment:add'" class="mc-action-btn mc-action-btn--success" @click="openRepayDialog(row)">登记回款</button>
            <button v-if="row.status === 0 || row.status === 3" v-permission="'crm:repayment:plan'" class="mc-action-btn" @click="openPlanDialog(row)">编辑</button>
            <MateInlineConfirm title="确认删除该计划?" @confirm="deletePlan(row)">
              <button v-permission="'crm:repayment:plan'" class="mc-action-btn mc-action-btn--danger">删除</button>
            </MateInlineConfirm>
          </template>
        </MateTable>
      </el-tab-pane>

      <!-- 回款记录 -->
      <el-tab-pane label="回款记录" name="records">
        <MateTable :columns="recordColumns" :data="records" :loading="loading" row-key="id" :action-width="90" action-label="操作" empty-text="暂无回款记录">
          <template #actions="{ row }">
            <MateInlineConfirm title="确认删除该回款记录?" @confirm="deleteRepayment(row)">
              <button v-permission="'crm:repayment:delete'" class="mc-action-btn mc-action-btn--danger">删除</button>
            </MateInlineConfirm>
          </template>
        </MateTable>
      </el-tab-pane>
    </el-tabs>

    <MatePagination v-model:page-num="query.pageNum" v-model:page-size="query.pageSize" :total="total" @change="loadData" />

    <MateDialog v-model="planVisible" :title="editingPlan ? '编辑回款计划' : '新增回款计划'" :submitting="submitting" @submit="savePlan">
      <MateForm ref="planFormRef" :schema="planSchema" :model-value="planForm" label-position="top" @update:model-value="(v: any) => Object.assign(planForm, v)" />
    </MateDialog>

    <MateDialog v-model="repayVisible" title="登记回款" :submitting="submitting" @submit="saveRepayment">
      <MateForm ref="repayFormRef" :schema="repaySchema" :model-value="repayForm" label-position="top" @update:model-value="(v: any) => Object.assign(repayForm, v)" />
    </MateDialog>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Plus } from 'lucide-vue-next'
import {
  repaymentApi, type PaymentPlan, type PaymentPlanCommand, type Repayment, type RepaymentCommand,
} from '@matecloud/core'
import {
  MatePageCard, MateTable, MateButton, MateInput, MateTag, MateSearchBar, MatePagination,
  MateDialog, MateForm, MateMessage, MateInlineConfirm, type MateColumn, type FormSchema, type TagVariantProps,
} from '@matecloud/ui'

defineOptions({ name: 'CrmAcceptanceList' })

const tab = ref<'plans' | 'records'>('plans')

const planColumns: MateColumn[] = [
  { prop: 'contractNum', label: '合同编号', minWidth: 150 },
  { prop: 'termNo', label: '期次', width: 80 },
  { prop: 'money', label: '计划金额', width: 120 },
  { prop: 'receivedMoney', label: '已回金额', width: 120 },
  { prop: 'checkingDate', label: '计划回款日', width: 130 },
  { prop: 'status', label: '状态', width: 110 },
  { prop: 'ownerName', label: '负责人', width: 100 },
]
const recordColumns: MateColumn[] = [
  { prop: 'number', label: '回款单号', width: 160 },
  { prop: 'contractNum', label: '合同编号', minWidth: 150 },
  { prop: 'money', label: '回款金额', width: 120 },
  { prop: 'paymentMethod', label: '方式', width: 110 },
  { prop: 'returnTime', label: '回款日期', width: 130 },
  { prop: 'creatorName', label: '登记人', width: 110 },
  { prop: 'createdAt', label: '登记时间', width: 160, type: 'datetime' },
]

function planStatusLabel(s?: number) { return ({ 0: '待回款', 1: '部分回款', 2: '已结清', 3: '已逾期' } as any)[s ?? -1] || '—' }
function planStatusVariant(s?: number): NonNullable<TagVariantProps['variant']> {
  if (s === 2) return 'success'; if (s === 3) return 'danger'; if (s === 1) return 'warning'; return 'info'
}

const loading = ref(false)
const plans = ref<PaymentPlan[]>([])
const records = ref<Repayment[]>([])
const total = ref(0)
const query = reactive<{ keyword: string; status?: number; pageNum: number; pageSize: number }>({ keyword: '', status: undefined, pageNum: 1, pageSize: 20 })

async function loadData() {
  loading.value = true
  try {
    if (tab.value === 'plans') {
      const res = await repaymentApi.pagePlans({ ...query })
      plans.value = res.data.list; total.value = res.data.total
    } else {
      const res = await repaymentApi.pageRepayments({ keyword: query.keyword, pageNum: query.pageNum, pageSize: query.pageSize })
      records.value = res.data.list; total.value = res.data.total
    }
  } catch (e: any) { MateMessage.error(e?.msg || '加载失败') }
  finally { loading.value = false }
}
function onTabChange() { query.pageNum = 1; query.status = undefined; loadData() }
function handleSearch() { query.pageNum = 1; loadData() }
function handleReset() { query.keyword = ''; query.status = undefined; handleSearch() }

const submitting = ref(false)

// ── Plan dialog ──
const planVisible = ref(false)
const editingPlan = ref<PaymentPlan | null>(null)
const planFormRef = ref<InstanceType<typeof MateForm>>()
const planForm = reactive<Record<string, any>>({})
const planSchema = computed<FormSchema[]>(() => [
  { field: 'contractId', label: '合同 ID', type: 'input', rules: [{ required: true, message: '请输入合同 ID', trigger: 'blur' }] },
  { field: 'contractNum', label: '合同编号', type: 'input' },
  { field: 'customerId', label: '客户 ID', type: 'input' },
  { field: 'termNo', label: '期次', type: 'number', defaultValue: 1 },
  { field: 'money', label: '计划金额', type: 'number', props: { min: 0, precision: 2 }, rules: [{ required: true, message: '请输入金额', trigger: 'blur' }] },
  { field: 'checkingDate', label: '计划回款日', type: 'date' },
  { field: 'returnType', label: '回款方式', type: 'input' },
  { field: 'remark', label: '备注', type: 'textarea' },
])
function openPlanDialog(row?: PaymentPlan) {
  editingPlan.value = row || null
  Object.keys(planForm).forEach((k) => delete planForm[k])
  Object.assign(planForm, {
    contractId: row?.contractId || '', contractNum: row?.contractNum || '', customerId: row?.customerId || '',
    termNo: row?.termNo ?? 1, money: row?.money ?? 0, checkingDate: row?.checkingDate || '', returnType: row?.returnType || '', remark: row?.remark || '',
  })
  planVisible.value = true
}
async function savePlan() {
  const valid = await planFormRef.value?.validate()
  if (!valid) return
  submitting.value = true
  try {
    const payload = { ...planForm } as PaymentPlanCommand
    if (editingPlan.value) await repaymentApi.updatePlan(editingPlan.value.id, payload)
    else await repaymentApi.createPlan(payload)
    MateMessage.success('保存成功'); planVisible.value = false; loadData()
  } catch (e: any) { MateMessage.error(e?.msg || '保存失败') }
  finally { submitting.value = false }
}
async function deletePlan(row: PaymentPlan) {
  try { await repaymentApi.deletePlan(row.id); MateMessage.success('删除成功'); loadData() }
  catch (e: any) { MateMessage.error(e?.msg || '删除失败') }
}

// ── Repayment dialog ──
const repayVisible = ref(false)
const repayFormRef = ref<InstanceType<typeof MateForm>>()
const repayForm = reactive<Record<string, any>>({})
const repaySchema = computed<FormSchema[]>(() => [
  { field: 'contractId', label: '合同 ID', type: 'input', rules: [{ required: true, message: '请输入合同 ID', trigger: 'blur' }] },
  { field: 'planId', label: '关联计划 ID', type: 'input' },
  { field: 'money', label: '回款金额', type: 'number', props: { min: 0, precision: 2 }, rules: [{ required: true, message: '请输入金额', trigger: 'blur' }] },
  { field: 'paymentMethod', label: '回款方式', type: 'select', options: [
    { label: '现金', value: 'CASH' }, { label: '转账', value: 'TRANSFER' }, { label: '支票', value: 'CHECK' }, { label: '其他', value: 'OTHER' }] },
  { field: 'returnTime', label: '回款日期', type: 'date' },
  { field: 'remark', label: '备注', type: 'textarea' },
])
function openRepayDialog(row: PaymentPlan) {
  Object.keys(repayForm).forEach((k) => delete repayForm[k])
  Object.assign(repayForm, {
    contractId: row.contractId, planId: row.id, money: Number(row.money || 0) - Number(row.receivedMoney || 0),
    paymentMethod: 'TRANSFER', returnTime: '', remark: '',
  })
  repayVisible.value = true
}
async function saveRepayment() {
  const valid = await repayFormRef.value?.validate()
  if (!valid) return
  submitting.value = true
  try {
    await repaymentApi.register({ ...repayForm } as RepaymentCommand)
    MateMessage.success('登记成功'); repayVisible.value = false; loadData()
  } catch (e: any) { MateMessage.error(e?.msg || '登记失败') }
  finally { submitting.value = false }
}
async function deleteRepayment(row: Repayment) {
  try { await repaymentApi.deleteRepayment(row.id); MateMessage.success('删除成功'); loadData() }
  catch (e: any) { MateMessage.error(e?.msg || '删除失败') }
}

onMounted(loadData)
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
</style>
