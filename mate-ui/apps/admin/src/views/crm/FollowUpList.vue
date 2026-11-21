<template>
  <MatePageCard title="跟进记录" description="线索 / 客户 / 联系人 / 商机的跟进动态">
    <template #actions>
      <MateButton v-permission="'crm:followup:add'" variant="primary" @click="openDialog()">
        <template #icon><Plus :size="14" /></template>写跟进
      </MateButton>
    </template>

    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <el-select v-model="query.bizType" placeholder="业务类型" clearable style="width: 140px">
        <el-option label="线索" value="LEAD" /><el-option label="客户" value="CUSTOMER" />
        <el-option label="联系人" value="CONTACT" /><el-option label="商机" value="BUSINESS" />
      </el-select>
      <MateInput v-model="query.bizId" placeholder="业务 ID" clearable style="width: 200px" @enter="handleSearch" />
    </MateSearchBar>

    <MateTable :columns="columns" :data="rows" :loading="loading" row-key="id" :action-width="90" action-label="操作" empty-text="暂无跟进记录">
      <template #col-bizType="{ row }">
        <MateTag variant="info">{{ bizTypeLabel(row.bizType) }}</MateTag>
      </template>
      <template #col-content="{ row }">
        <span class="fu-text">{{ row.content }}</span>
      </template>
      <template #actions="{ row }">
        <MateInlineConfirm title="确认删除该跟进?" @confirm="handleDelete(row)">
          <button v-permission="'crm:followup:delete'" class="mc-action-btn mc-action-btn--danger">删除</button>
        </MateInlineConfirm>
      </template>
    </MateTable>

    <MatePagination v-model:page-num="query.pageNum" v-model:page-size="query.pageSize" :total="total" @change="loadData" />

    <MateDialog v-model="dialogVisible" title="写跟进" :submitting="submitting" @submit="handleSubmit">
      <MateForm ref="formRef" :schema="formSchema" :model-value="form" label-position="top" @update:model-value="(v: any) => Object.assign(form, v)" />
    </MateDialog>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus } from 'lucide-vue-next'
import { followUpApi, type FollowUp, type FollowUpCommand } from '@matecloud/core'
import {
  MatePageCard, MateTable, MateButton, MateInput, MateTag, MateSearchBar, MatePagination,
  MateDialog, MateForm, MateMessage, MateInlineConfirm,
  type MateColumn, type FormSchema,
} from '@matecloud/ui'

defineOptions({ name: 'CrmFollowUpList' })

const columns: MateColumn[] = [
  { prop: 'bizType', label: '类型', width: 100 },
  { prop: 'bizName', label: '业务名称', minWidth: 160 },
  { prop: 'content', label: '跟进内容', minWidth: 240 },
  { prop: 'followType', label: '方式', width: 100 },
  { prop: 'userName', label: '跟进人', width: 110 },
  { prop: 'createdAt', label: '时间', width: 160, type: 'datetime' },
]

function bizTypeLabel(s?: string) {
  return ({ LEAD: '线索', CUSTOMER: '客户', CONTACT: '联系人', BUSINESS: '商机' } as any)[s || ''] || s || '—'
}

const loading = ref(false)
const rows = ref<FollowUp[]>([])
const total = ref(0)
const query = reactive<{ bizType?: string; bizId: string; pageNum: number; pageSize: number }>({ bizType: undefined, bizId: '', pageNum: 1, pageSize: 20 })

async function loadData() {
  loading.value = true
  try {
    const res = await followUpApi.page({ ...query })
    rows.value = res.data.list
    total.value = res.data.total
  } catch (e: any) { MateMessage.error(e?.msg || '加载失败') }
  finally { loading.value = false }
}
function handleSearch() { query.pageNum = 1; loadData() }
function handleReset() { query.bizType = undefined; query.bizId = ''; handleSearch() }

async function handleDelete(row: FollowUp) {
  try { await followUpApi.delete(row.id); MateMessage.success('删除成功'); loadData() }
  catch (e: any) { MateMessage.error(e?.msg || '删除失败') }
}

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<InstanceType<typeof MateForm>>()
const form = reactive<Record<string, any>>({})

const formSchema = computed<FormSchema[]>(() => [
  { field: 'bizType', label: '业务类型', type: 'select', rules: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
    options: [{ label: '线索', value: 'LEAD' }, { label: '客户', value: 'CUSTOMER' }, { label: '联系人', value: 'CONTACT' }, { label: '商机', value: 'BUSINESS' }] },
  { field: 'bizId', label: '业务 ID', type: 'input', rules: [{ required: true, message: '请输入业务 ID', trigger: 'blur' }] },
  { field: 'bizName', label: '业务名称', type: 'input' },
  { field: 'customerId', label: '关联客户 ID', type: 'input' },
  { field: 'followType', label: '跟进方式', type: 'select', options: [
    { label: '电话', value: 'PHONE' }, { label: '拜访', value: 'VISIT' }, { label: '微信', value: 'WECHAT' },
    { label: '邮件', value: 'EMAIL' }, { label: '会议', value: 'MEETING' }, { label: '其他', value: 'OTHER' }] },
  { field: 'content', label: '跟进内容', type: 'textarea', rules: [{ required: true, message: '请输入跟进内容', trigger: 'blur' }] },
  { field: 'nextContent', label: '下次计划', type: 'input' },
  { field: 'nextTime', label: '下次时间', type: 'datetime' },
])

function openDialog() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { bizType: 'CUSTOMER', bizId: '', bizName: '', customerId: '', followType: 'PHONE', content: '', nextContent: '', nextTime: '' })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return
  submitting.value = true
  try {
    await followUpApi.save({ ...form } as FollowUpCommand)
    MateMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch (e: any) { MateMessage.error(e?.msg || '保存失败') }
  finally { submitting.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.fu-text { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
</style>
