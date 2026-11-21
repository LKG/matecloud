<template>
  <MatePageCard title="客户管理" description="私海 / 公海客户流转, 领取、释放与转移">
    <template #actions>
      <MateButton v-permission="'crm:customer:add'" variant="primary" @click="openDialog()">
        <template #icon><Plus :size="14" /></template>新建客户
      </MateButton>
    </template>

    <el-tabs v-model="poolTab" @tab-change="handleTabChange">
      <el-tab-pane label="私海客户" name="private" />
      <el-tab-pane label="公海客户" name="public" />
    </el-tabs>

    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <MateInput v-model="query.keyword" placeholder="客户名称 / 电话" clearable style="width: 220px" @enter="handleSearch">
        <template #prefix><Search :size="14" /></template>
      </MateInput>
      <el-select v-model="query.level" placeholder="等级" clearable style="width: 110px">
        <el-option label="A" value="A" /><el-option label="B" value="B" /><el-option label="C" value="C" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
        <el-option label="初始" value="INITIAL" /><el-option label="跟进中" value="NEGOTIATING" />
        <el-option label="成交" value="OPPORTUNITY" /><el-option label="沉睡" value="DORMANT" /><el-option label="流失" value="CHURNED" />
      </el-select>
      <el-select v-model="query.mineOnly" placeholder="范围" clearable style="width: 120px">
        <el-option label="仅我的" :value="true" />
      </el-select>
      <CrmAdvancedFilter v-model="advGroups" module="customer" @search="handleSearch" />
    </MateSearchBar>

    <MateTable :columns="columns" :data="rows" :loading="loading" row-key="id" :action-width="260" action-label="操作" empty-text="暂无客户">
      <template #col-customerName="{ row }">
        <MateEntityCell :name="row.customerName" :sub="row.industry || '—'" avatar />
      </template>
      <template #col-level="{ row }">
        <MateTag v-if="row.level" :variant="row.level === 'A' ? 'danger' : row.level === 'B' ? 'warning' : 'info'">{{ row.level }} 类</MateTag>
      </template>
      <template #col-status="{ row }">
        <MateTag :variant="custStatusVariant(row.status)" dot>{{ custStatusLabel(row.status) }}</MateTag>
      </template>
      <template #actions="{ row }">
        <button class="mc-action-btn" @click="goDetail(row)">详情</button>
        <template v-if="poolTab === 'public'">
          <button v-permission="'crm:customer:claim'" class="mc-action-btn mc-action-btn--success" @click="handleClaim(row)">领取</button>
        </template>
        <template v-else>
          <button v-permission="'crm:customer:edit'" class="mc-action-btn" @click="openDialog(row)">编辑</button>
          <button v-permission="'crm:customer:transfer'" class="mc-action-btn" @click="openTransfer(row)">转移</button>
          <button v-permission="'crm:customer:release'" class="mc-action-btn mc-action-btn--warn" @click="handleRelease(row)">释放</button>
        </template>
      </template>
    </MateTable>

    <MatePagination v-model:page-num="query.pageNum" v-model:page-size="query.pageSize" :total="total" @change="loadData" />

    <MateDialog v-model="dialogVisible" :title="editing ? '编辑客户' : '新建客户'" :submitting="submitting" @submit="handleSubmit">
      <MateForm ref="formRef" :schema="formSchema" :model-value="form" label-position="top" @update:model-value="(v: any) => Object.assign(form, v)" />
    </MateDialog>

    <MateDialog v-model="transferVisible" title="转移负责人" :submitting="submitting" @submit="handleTransfer">
      <el-form label-position="top">
        <el-form-item label="新负责人 ID" required><el-input v-model="transferForm.ownerId" placeholder="请输入用户 ID" /></el-form-item>
        <el-form-item label="新负责人姓名"><el-input v-model="transferForm.ownerName" placeholder="请输入姓名" /></el-form-item>
      </el-form>
    </MateDialog>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Plus } from 'lucide-vue-next'
import { customerApi, type Customer, type CustomerCommand, type ConditionGroup } from '@matecloud/core'
import {
  MatePageCard, MateTable, MateButton, MateInput, MateTag, MateSearchBar, MatePagination,
  MateDialog, MateForm, MateEntityCell, MateMessage, MateMessageBox,
  type MateColumn, type FormSchema, type TagVariantProps,
} from '@matecloud/ui'
import CrmAdvancedFilter from '../../components/crm/CrmAdvancedFilter.vue'
import { buildAdvancedQuery, hasValidAdvanced } from '../../composables/useCrmAdvancedQuery'

defineOptions({ name: 'CrmCustomerList' })

const router = useRouter()
const poolTab = ref<'private' | 'public'>('private')

const columns = computed<MateColumn[]>(() => {
  const base: MateColumn[] = [
    { prop: 'customerName', label: '客户名称', minWidth: 180 },
    { prop: 'level', label: '等级', width: 90 },
    { prop: 'phone', label: '电话', width: 140 },
    { prop: 'status', label: '状态', width: 110 },
  ]
  if (poolTab.value === 'private') {
    base.push({ prop: 'ownerName', label: '负责人', width: 110 })
    base.push({ prop: 'recentFollowTime', label: '最近跟进', width: 160, type: 'datetime' })
  } else {
    base.push({ prop: 'toPoolReason', label: '入公海原因', minWidth: 160 })
  }
  return base
})

function custStatusLabel(s?: string) {
  return ({ INITIAL: '初始', NEGOTIATING: '跟进中', OPPORTUNITY: '成交', DORMANT: '沉睡', CHURNED: '流失' } as any)[s || ''] || s || '—'
}
function custStatusVariant(s?: string): NonNullable<TagVariantProps['variant']> {
  if (s === 'OPPORTUNITY') return 'success'
  if (s === 'CHURNED') return 'danger'
  if (s === 'DORMANT') return 'info'
  return 'primary'
}

const loading = ref(false)
const rows = ref<Customer[]>([])
const total = ref(0)
const query = reactive<{ keyword: string; level?: string; status?: string; mineOnly?: boolean; poolStatus: number; pageNum: number; pageSize: number }>({
  keyword: '', level: undefined, status: undefined, mineOnly: undefined, poolStatus: 0, pageNum: 1, pageSize: 20,
})
const advGroups = ref<ConditionGroup[]>([])

async function loadData() {
  loading.value = true
  query.poolStatus = poolTab.value === 'public' ? 1 : 0
  try {
    const res = hasValidAdvanced(advGroups.value)
      ? await customerApi.search(buildAdvancedQuery({
          pageNum: query.pageNum, pageSize: query.pageSize,
          kw: query.keyword, kwFields: ['customerName', 'phone'],
          base: [
            { fieldKey: 'level', operator: 'EQ', values: query.level ? [query.level] : [] },
            { fieldKey: 'status', operator: 'EQ', values: query.status ? [query.status] : [] },
            { fieldKey: 'poolStatus', operator: 'EQ', values: [String(query.poolStatus)] },
          ],
          mineOnly: query.mineOnly, groups: advGroups.value,
        }))
      : await customerApi.page({ ...query })
    rows.value = res.data.list
    total.value = res.data.total
  } catch (e: any) {
    MateMessage.error(e?.msg || '加载失败')
  } finally {
    loading.value = false
  }
}
function handleTabChange() { query.pageNum = 1; loadData() }
function handleSearch() { query.pageNum = 1; loadData() }
function handleReset() { query.keyword = ''; query.level = undefined; query.status = undefined; query.mineOnly = undefined; advGroups.value = []; handleSearch() }

function goDetail(row: Customer) { router.push({ path: '/crm/customer-detail', query: { id: row.id } }) }

async function handleClaim(row: Customer) {
  try { await customerApi.claim(row.id); MateMessage.success('领取成功'); loadData() }
  catch (e: any) { MateMessage.error(e?.msg || '领取失败') }
}
async function handleRelease(row: Customer) {
  try {
    const { value } = await MateMessageBox.prompt('请输入释放原因', '释放到公海', { inputPlaceholder: '释放原因' })
    await customerApi.release(row.id, value)
    MateMessage.success('已释放到公海'); loadData()
  } catch (e: any) { if (e !== 'cancel' && e?.message !== 'cancel') MateMessage.error(e?.msg || '操作失败') }
}

// ── Transfer ──
const transferVisible = ref(false)
const transferTarget = ref<Customer | null>(null)
const transferForm = reactive<{ ownerId: string; ownerName: string }>({ ownerId: '', ownerName: '' })
function openTransfer(row: Customer) {
  transferTarget.value = row
  transferForm.ownerId = ''; transferForm.ownerName = ''
  transferVisible.value = true
}
async function handleTransfer() {
  if (!transferTarget.value || !transferForm.ownerId) { MateMessage.warning('请输入负责人 ID'); return }
  submitting.value = true
  try {
    await customerApi.transfer(transferTarget.value.id, { ownerId: transferForm.ownerId, ownerName: transferForm.ownerName })
    MateMessage.success('转移成功'); transferVisible.value = false; loadData()
  } catch (e: any) { MateMessage.error(e?.msg || '转移失败') }
  finally { submitting.value = false }
}

// ── Create / Edit ──
const dialogVisible = ref(false)
const editing = ref<Customer | null>(null)
const submitting = ref(false)
const formRef = ref<InstanceType<typeof MateForm>>()
const form = reactive<Record<string, any>>({})

const formSchema = computed<FormSchema[]>(() => [
  { field: 'customerName', label: '客户名称', type: 'input', rules: [{ required: true, message: '请输入客户名称', trigger: 'blur' }] },
  { field: 'industry', label: '行业', type: 'input' },
  { field: 'level', label: '等级', type: 'select', options: [{ label: 'A', value: 'A' }, { label: 'B', value: 'B' }, { label: 'C', value: 'C' }] },
  { field: 'source', label: '来源', type: 'input' },
  { field: 'status', label: '状态', type: 'select', options: [
    { label: '初始', value: 'INITIAL' }, { label: '跟进中', value: 'NEGOTIATING' }, { label: '成交', value: 'OPPORTUNITY' },
    { label: '沉睡', value: 'DORMANT' }, { label: '流失', value: 'CHURNED' }] },
  { field: 'phone', label: '电话', type: 'input' },
  { field: 'website', label: '官网', type: 'input' },
  { field: 'address', label: '地址', type: 'input' },
  { field: 'remark', label: '备注', type: 'textarea' },
  ...(editing.value ? [] : [{ field: 'toPublicPool', label: '直接放入公海', type: 'switch' as const, defaultValue: false }]),
])

function openDialog(row?: Customer) {
  editing.value = row || null
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, {
    customerName: row?.customerName || '', industry: row?.industry || '', level: row?.level || '',
    source: row?.source || '', status: row?.status || 'INITIAL', phone: row?.phone || '',
    website: row?.website || '', address: row?.address || '', remark: row?.remark || '', toPublicPool: false,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return
  submitting.value = true
  try {
    const payload = { ...form } as CustomerCommand
    if (editing.value) await customerApi.update(editing.value.id, payload)
    else await customerApi.create(payload)
    MateMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch (e: any) { MateMessage.error(e?.msg || '保存失败') }
  finally { submitting.value = false }
}

onMounted(loadData)
</script>
