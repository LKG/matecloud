<template>
  <MatePageCard title="联系人" description="客户联系人维护">
    <template #actions>
      <MateButton v-permission="'crm:contact:add'" variant="primary" @click="openDialog()">
        <template #icon><Plus :size="14" /></template>新建联系人
      </MateButton>
    </template>

    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <MateInput v-model="query.keyword" placeholder="姓名 / 手机 / 邮箱" clearable style="width: 240px" @enter="handleSearch">
        <template #prefix><Search :size="14" /></template>
      </MateInput>
    </MateSearchBar>

    <MateTable :columns="columns" :data="rows" :loading="loading" row-key="id" :action-width="200" action-label="操作" empty-text="暂无联系人">
      <template #col-isPrimary="{ row }">
        <MateTag v-if="row.isPrimary === 1" variant="success">主联系人</MateTag>
        <button v-else v-permission="'crm:contact:edit'" class="mc-action-btn" @click="handleSetPrimary(row)">设为主</button>
      </template>
      <template #actions="{ row }">
        <button v-permission="'crm:contact:edit'" class="mc-action-btn" @click="openDialog(row)">编辑</button>
        <MateInlineConfirm title="确认删除该联系人?" @confirm="handleDelete(row)">
          <button v-permission="'crm:contact:delete'" class="mc-action-btn mc-action-btn--danger">删除</button>
        </MateInlineConfirm>
      </template>
    </MateTable>

    <MatePagination v-model:page-num="query.pageNum" v-model:page-size="query.pageSize" :total="total" @change="loadData" />

    <MateDialog v-model="dialogVisible" :title="editing ? '编辑联系人' : '新建联系人'" :submitting="submitting" @submit="handleSubmit">
      <MateForm ref="formRef" :schema="formSchema" :model-value="form" label-position="top" @update:model-value="(v: any) => Object.assign(form, v)" />
    </MateDialog>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Plus } from 'lucide-vue-next'
import { contactApi, type Contact, type ContactCommand } from '@matecloud/core'
import {
  MatePageCard, MateTable, MateButton, MateInput, MateTag, MateSearchBar, MatePagination,
  MateDialog, MateForm, MateMessage, MateInlineConfirm,
  type MateColumn, type FormSchema,
} from '@matecloud/ui'

defineOptions({ name: 'CrmContactList' })

const columns: MateColumn[] = [
  { prop: 'contactName', label: '姓名', minWidth: 120 },
  { prop: 'customerName', label: '所属客户', minWidth: 160 },
  { prop: 'position', label: '职位', width: 130 },
  { prop: 'mobile', label: '手机', width: 140 },
  { prop: 'email', label: '邮箱', minWidth: 160 },
  { prop: 'isPrimary', label: '主联系人', width: 110 },
]

const loading = ref(false)
const rows = ref<Contact[]>([])
const total = ref(0)
const query = reactive<{ keyword: string; pageNum: number; pageSize: number }>({ keyword: '', pageNum: 1, pageSize: 20 })

async function loadData() {
  loading.value = true
  try {
    const res = await contactApi.page({ ...query })
    rows.value = res.data.list
    total.value = res.data.total
  } catch (e: any) { MateMessage.error(e?.msg || '加载失败') }
  finally { loading.value = false }
}
function handleSearch() { query.pageNum = 1; loadData() }
function handleReset() { query.keyword = ''; handleSearch() }

async function handleSetPrimary(row: Contact) {
  try { await contactApi.setPrimary(row.id); MateMessage.success('已设为主联系人'); loadData() }
  catch (e: any) { MateMessage.error(e?.msg || '操作失败') }
}
async function handleDelete(row: Contact) {
  try { await contactApi.delete(row.id); MateMessage.success('删除成功'); loadData() }
  catch (e: any) { MateMessage.error(e?.msg || '删除失败') }
}

const dialogVisible = ref(false)
const editing = ref<Contact | null>(null)
const submitting = ref(false)
const formRef = ref<InstanceType<typeof MateForm>>()
const form = reactive<Record<string, any>>({})

const formSchema = computed<FormSchema[]>(() => [
  { field: 'customerId', label: '客户 ID', type: 'input', rules: [{ required: true, message: '请输入所属客户 ID', trigger: 'blur' }] },
  { field: 'customerName', label: '客户名称', type: 'input' },
  { field: 'contactName', label: '姓名', type: 'input', rules: [{ required: true, message: '请输入姓名', trigger: 'blur' }] },
  { field: 'position', label: '职位', type: 'input' },
  { field: 'mobile', label: '手机', type: 'input' },
  { field: 'telephone', label: '电话', type: 'input' },
  { field: 'email', label: '邮箱', type: 'input' },
  { field: 'wechat', label: '微信', type: 'input' },
  { field: 'gender', label: '性别', type: 'select', options: [{ label: '男', value: 'MALE' }, { label: '女', value: 'FEMALE' }] },
  { field: 'remark', label: '备注', type: 'textarea' },
])

function openDialog(row?: Contact) {
  editing.value = row || null
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, {
    customerId: row?.customerId || '', customerName: row?.customerName || '', contactName: row?.contactName || '',
    position: row?.position || '', mobile: row?.mobile || '', telephone: row?.telephone || '', email: row?.email || '',
    wechat: row?.wechat || '', gender: row?.gender || '', remark: row?.remark || '',
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return
  submitting.value = true
  try {
    const payload = { ...form } as ContactCommand
    if (editing.value) await contactApi.update(editing.value.id, payload)
    else await contactApi.create(payload)
    MateMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch (e: any) { MateMessage.error(e?.msg || '保存失败') }
  finally { submitting.value = false }
}

onMounted(loadData)
</script>
