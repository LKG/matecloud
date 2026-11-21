<template>
  <MatePageCard title="我的待办" description="跟进 / 回访 / 会议等待办任务, AI 亦可自动生成">
    <template #actions>
      <el-checkbox v-model="onlyOpen" style="margin-right: 12px" @change="handleSearch">只看未完成</el-checkbox>
      <MateButton v-permission="'crm:task:add'" variant="primary" @click="openDialog()">
        <template #icon><Plus :size="14" /></template>新建待办
      </MateButton>
    </template>

    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <MateInput v-model="query.keyword" placeholder="标题" clearable style="width: 220px" @enter="handleSearch">
        <template #prefix><Search :size="14" /></template>
      </MateInput>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
        <el-option label="待办" value="OPEN" /><el-option label="已完成" value="DONE" /><el-option label="已取消" value="CANCELED" />
      </el-select>
      <el-select v-model="query.type" placeholder="类型" clearable style="width: 120px">
        <el-option label="跟进" value="FOLLOW" /><el-option label="回访" value="RETURN_VISIT" />
        <el-option label="会议" value="MEETING" /><el-option label="其他" value="OTHER" />
      </el-select>
    </MateSearchBar>

    <MateTable :columns="columns" :data="rows" :loading="loading" row-key="id" :action-width="200" action-label="操作" empty-text="暂无待办">
      <template #col-source="{ row }">
        <MateTag :variant="row.source === 'AI' ? 'primary' : 'info'">{{ row.source === 'AI' ? 'AI' : '手动' }}</MateTag>
      </template>
      <template #col-status="{ row }">
        <MateTag :variant="statusVariant(row.status)" dot>{{ statusLabel(row.status) }}</MateTag>
      </template>
      <template #actions="{ row }">
        <template v-if="row.status === 'OPEN'">
          <button v-permission="'crm:task:add'" class="mc-action-btn" @click="openDialog(row)">编辑</button>
          <button v-permission="'crm:task:done'" class="mc-action-btn mc-action-btn--success" @click="handleDone(row)">完成</button>
          <button v-permission="'crm:task:done'" class="mc-action-btn mc-action-btn--warn" @click="handleCancel(row)">取消</button>
        </template>
        <MateInlineConfirm title="确认删除?" @confirm="handleDelete(row)">
          <button v-permission="'crm:task:delete'" class="mc-action-btn mc-action-btn--danger">删除</button>
        </MateInlineConfirm>
      </template>
    </MateTable>

    <MatePagination v-model:page-num="query.pageNum" v-model:page-size="query.pageSize" :total="total" @change="loadData" />

    <MateDialog v-model="dialogVisible" :title="editing ? '编辑待办' : '新建待办'" :submitting="submitting" @submit="handleSubmit">
      <MateForm ref="formRef" :schema="formSchema" :model-value="form" label-position="top" @update:model-value="(v: any) => Object.assign(form, v)" />
    </MateDialog>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Plus } from 'lucide-vue-next'
import { taskApi, type CrmTask, type TaskCommand } from '@matecloud/core'
import {
  MatePageCard, MateTable, MateButton, MateInput, MateTag, MateSearchBar, MatePagination,
  MateDialog, MateForm, MateMessage, MateInlineConfirm, type MateColumn, type FormSchema, type TagVariantProps,
} from '@matecloud/ui'

defineOptions({ name: 'CrmTaskList' })

const columns: MateColumn[] = [
  { prop: 'title', label: '标题', minWidth: 180 },
  { prop: 'type', label: '类型', width: 100 },
  { prop: 'bizName', label: '关联业务', width: 140 },
  { prop: 'dueTime', label: '截止时间', width: 160, type: 'datetime' },
  { prop: 'priority', label: '优先级', width: 90 },
  { prop: 'source', label: '来源', width: 90 },
  { prop: 'status', label: '状态', width: 100 },
]

function statusLabel(s?: string) { return ({ OPEN: '待办', DONE: '已完成', CANCELED: '已取消' } as any)[s || ''] || s || '—' }
function statusVariant(s?: string): NonNullable<TagVariantProps['variant']> {
  if (s === 'DONE') return 'success'; if (s === 'CANCELED') return 'info'; return 'warning'
}

const loading = ref(false)
const onlyOpen = ref(true)
const rows = ref<CrmTask[]>([])
const total = ref(0)
const query = reactive<{ keyword: string; status?: string; type?: string; onlyMine: boolean; onlyOpen?: boolean; pageNum: number; pageSize: number }>({
  keyword: '', status: undefined, type: undefined, onlyMine: true, onlyOpen: true, pageNum: 1, pageSize: 20,
})

async function loadData() {
  loading.value = true
  try {
    const res = await taskApi.page({ ...query })
    rows.value = res.data.list.map((r) => ({ ...r }))
    total.value = res.data.total
  } catch (e: any) { MateMessage.error(e?.msg || '加载失败') }
  finally { loading.value = false }
}
function handleSearch() { query.pageNum = 1; query.onlyOpen = onlyOpen.value ? true : undefined; if (onlyOpen.value) query.status = 'OPEN'; loadData() }
function handleReset() { query.keyword = ''; query.status = undefined; query.type = undefined; onlyOpen.value = true; handleSearch() }

async function handleDone(row: CrmTask) {
  try { await taskApi.complete(row.id); MateMessage.success('已完成'); loadData() }
  catch (e: any) { MateMessage.error(e?.msg || '操作失败') }
}
async function handleCancel(row: CrmTask) {
  try { await taskApi.cancel(row.id); MateMessage.success('已取消'); loadData() }
  catch (e: any) { MateMessage.error(e?.msg || '操作失败') }
}
async function handleDelete(row: CrmTask) {
  try { await taskApi.delete(row.id); MateMessage.success('删除成功'); loadData() }
  catch (e: any) { MateMessage.error(e?.msg || '删除失败') }
}

const dialogVisible = ref(false)
const editing = ref<CrmTask | null>(null)
const submitting = ref(false)
const formRef = ref<InstanceType<typeof MateForm>>()
const form = reactive<Record<string, any>>({})
const formSchema = computed<FormSchema[]>(() => [
  { field: 'title', label: '标题', type: 'input', rules: [{ required: true, message: '请输入标题', trigger: 'blur' }] },
  { field: 'type', label: '类型', type: 'select', options: [
    { label: '跟进', value: 'FOLLOW' }, { label: '回访', value: 'RETURN_VISIT' }, { label: '会议', value: 'MEETING' }, { label: '其他', value: 'OTHER' }] },
  { field: 'bizType', label: '关联业务类型', type: 'select', options: [
    { label: '线索', value: 'LEAD' }, { label: '客户', value: 'CUSTOMER' }, { label: '联系人', value: 'CONTACT' }, { label: '商机', value: 'BUSINESS' }] },
  { field: 'bizId', label: '关联业务 ID', type: 'input' },
  { field: 'bizName', label: '关联业务名称', type: 'input' },
  { field: 'priority', label: '优先级', type: 'select', options: [
    { label: '高', value: 'HIGH' }, { label: '中', value: 'MEDIUM' }, { label: '低', value: 'LOW' }] },
  { field: 'dueTime', label: '截止时间', type: 'datetime' },
  { field: 'content', label: '详情', type: 'textarea' },
])

function openDialog(row?: CrmTask) {
  editing.value = row || null
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, {
    title: row?.title || '', type: row?.type || 'FOLLOW', bizType: row?.bizType || '', bizId: row?.bizId || '',
    bizName: row?.bizName || '', priority: row?.priority || 'MEDIUM', dueTime: row?.dueTime || '', content: row?.content || '',
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return
  submitting.value = true
  try {
    const payload = { ...form, source: 'MANUAL' } as TaskCommand
    if (editing.value) await taskApi.update(editing.value.id, payload)
    else await taskApi.create(payload)
    MateMessage.success('保存成功'); dialogVisible.value = false; loadData()
  } catch (e: any) { MateMessage.error(e?.msg || '保存失败') }
  finally { submitting.value = false }
}

onMounted(loadData)
</script>
