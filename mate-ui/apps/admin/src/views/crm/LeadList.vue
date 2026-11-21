<template>
  <MatePageCard title="线索管理" description="录入潜在线索, 跟进后一键转化为客户">
    <template #actions>
      <MateButton v-permission="'crm:lead:add'" variant="primary" @click="openDialog()">
        <template #icon><Plus :size="14" /></template>新建线索
      </MateButton>
    </template>

    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <MateInput v-model="query.keyword" placeholder="线索名称 / 联系人 / 手机号" clearable style="width: 240px" @enter="handleSearch">
        <template #prefix><Search :size="14" /></template>
      </MateInput>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
        <el-option label="跟进中" value="FOLLOWING" />
        <el-option label="已转化" value="CONVERTED" />
        <el-option label="已作废" value="INVALID" />
      </el-select>
      <el-select v-model="query.mineOnly" placeholder="范围" clearable style="width: 120px">
        <el-option label="仅我的" :value="true" />
      </el-select>
      <CrmAdvancedFilter v-model="advGroups" module="lead" @search="handleSearch" />
    </MateSearchBar>

    <MateTable :columns="columns" :data="rows" :loading="loading" row-key="id" :action-width="240" action-label="操作" empty-text="暂无线索">
      <template #col-status="{ row }">
        <MateTag :variant="statusVariant(row.status)" dot>{{ statusLabel(row.status) }}</MateTag>
      </template>
      <template #actions="{ row }">
        <button v-permission="'crm:lead:edit'" class="mc-action-btn" @click="openDialog(row)">编辑</button>
        <button v-if="row.status === 'FOLLOWING'" v-permission="'crm:lead:convert'" class="mc-action-btn mc-action-btn--success" @click="handleConvert(row)">转化</button>
        <button v-if="row.status === 'FOLLOWING'" v-permission="'crm:lead:edit'" class="mc-action-btn" @click="handleFollow(row)">跟进</button>
        <MateInlineConfirm title="确认删除该线索?" @confirm="handleDelete(row)">
          <button v-permission="'crm:lead:delete'" class="mc-action-btn mc-action-btn--danger">删除</button>
        </MateInlineConfirm>
      </template>
    </MateTable>

    <MatePagination v-model:page-num="query.pageNum" v-model:page-size="query.pageSize" :total="total" @change="loadData" />

    <MateDialog v-model="dialogVisible" :title="editing ? '编辑线索' : '新建线索'" :submitting="submitting" @submit="handleSubmit">
      <MateForm ref="formRef" :schema="formSchema" :model-value="form" label-position="top" @update:model-value="(v: any) => Object.assign(form, v)" />
    </MateDialog>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Plus } from 'lucide-vue-next'
import { leadApi, type Lead, type LeadCommand, type ConditionGroup } from '@matecloud/core'
import {
  MatePageCard, MateTable, MateButton, MateInput, MateTag, MateSearchBar,
  MatePagination, MateDialog, MateForm, MateMessage, MateInlineConfirm,
  type MateColumn, type FormSchema, type TagVariantProps,
} from '@matecloud/ui'
import CrmAdvancedFilter from '../../components/crm/CrmAdvancedFilter.vue'
import { buildAdvancedQuery, hasValidAdvanced } from '../../composables/useCrmAdvancedQuery'

defineOptions({ name: 'CrmLeadList' })

const columns: MateColumn[] = [
  { prop: 'leadName', label: '线索名称', minWidth: 160 },
  { prop: 'contactName', label: '联系人', width: 110 },
  { prop: 'mobile', label: '手机号', width: 140 },
  { prop: 'industry', label: '行业', width: 120 },
  { prop: 'source', label: '来源', width: 110 },
  { prop: 'ownerName', label: '负责人', width: 110 },
  { prop: 'status', label: '状态', width: 110 },
]

function statusLabel(s: string) { return ({ FOLLOWING: '跟进中', CONVERTED: '已转化', INVALID: '已作废' } as any)[s] || s }
function statusVariant(s: string): NonNullable<TagVariantProps['variant']> {
  return s === 'CONVERTED' ? 'success' : s === 'INVALID' ? 'danger' : 'warning'
}

const loading = ref(false)
const rows = ref<Lead[]>([])
const total = ref(0)
const query = reactive<{ keyword: string; status?: string; mineOnly?: boolean; pageNum: number; pageSize: number }>({
  keyword: '', status: undefined, mineOnly: undefined, pageNum: 1, pageSize: 20,
})
const advGroups = ref<ConditionGroup[]>([])

async function loadData() {
  loading.value = true
  try {
    const res = hasValidAdvanced(advGroups.value)
      ? await leadApi.search(buildAdvancedQuery({
          pageNum: query.pageNum, pageSize: query.pageSize,
          kw: query.keyword, kwFields: ['leadName', 'contactName', 'mobile'],
          base: [{ fieldKey: 'status', operator: 'EQ', values: query.status ? [query.status] : [] }],
          mineOnly: query.mineOnly, groups: advGroups.value,
        }))
      : await leadApi.page({ ...query })
    rows.value = res.data.list
    total.value = res.data.total
  } catch (e: any) {
    MateMessage.error(e?.msg || '加载失败')
  } finally {
    loading.value = false
  }
}
function handleSearch() { query.pageNum = 1; loadData() }
function handleReset() { query.keyword = ''; query.status = undefined; query.mineOnly = undefined; advGroups.value = []; handleSearch() }

async function handleConvert(row: Lead) {
  try {
    await leadApi.convert(row.id)
    MateMessage.success('转化成功, 已生成客户')
    loadData()
  } catch (e: any) { MateMessage.error(e?.msg || '转化失败') }
}
async function handleFollow(row: Lead) {
  try {
    await leadApi.follow(row.id)
    MateMessage.success('已记录跟进')
    loadData()
  } catch (e: any) { MateMessage.error(e?.msg || '操作失败') }
}
async function handleDelete(row: Lead) {
  try {
    await leadApi.delete(row.id)
    MateMessage.success('删除成功')
    loadData()
  } catch (e: any) { MateMessage.error(e?.msg || '删除失败') }
}

const dialogVisible = ref(false)
const editing = ref<Lead | null>(null)
const submitting = ref(false)
const formRef = ref<InstanceType<typeof MateForm>>()
const form = reactive<Record<string, any>>({})

const formSchema = computed<FormSchema[]>(() => [
  { field: 'leadName', label: '线索名称', type: 'input', rules: [{ required: true, message: '请输入线索名称', trigger: 'blur' }] },
  { field: 'contactName', label: '联系人', type: 'input' },
  { field: 'mobile', label: '手机号', type: 'input' },
  { field: 'telephone', label: '电话', type: 'input' },
  { field: 'email', label: '邮箱', type: 'input' },
  { field: 'industry', label: '行业', type: 'input' },
  { field: 'source', label: '来源', type: 'input' },
  { field: 'address', label: '地址', type: 'input' },
  { field: 'nextTime', label: '下次跟进', type: 'datetime' },
  { field: 'remark', label: '备注', type: 'textarea' },
])

function openDialog(row?: Lead) {
  editing.value = row || null
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, {
    leadName: row?.leadName || '', contactName: row?.contactName || '', mobile: row?.mobile || '',
    telephone: row?.telephone || '', email: row?.email || '', industry: row?.industry || '',
    source: row?.source || '', address: row?.address || '', nextTime: row?.nextTime || '', remark: row?.remark || '',
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return
  submitting.value = true
  try {
    const payload = { ...form } as LeadCommand
    if (editing.value) await leadApi.update(editing.value.id, payload)
    else await leadApi.create(payload)
    MateMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch (e: any) {
    MateMessage.error(e?.msg || '保存失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>
