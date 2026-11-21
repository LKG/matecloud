<template>
  <MatePageCard title="产品管理" description="标准产品与价格表">
    <template #actions>
      <MateButton v-permission="'crm:product:add'" variant="primary" @click="openDialog()">
        <template #icon><Plus :size="14" /></template>新建产品
      </MateButton>
    </template>

    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <MateInput v-model="query.keyword" placeholder="产品名称 / 编码" clearable style="width: 220px" @enter="handleSearch">
        <template #prefix><Search :size="14" /></template>
      </MateInput>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
        <el-option label="上架" :value="1" /><el-option label="下架" :value="0" />
      </el-select>
      <CrmAdvancedFilter v-model="advGroups" module="product" @search="handleSearch" />
    </MateSearchBar>

    <MateTable :columns="columns" :data="rows" :loading="loading" row-key="id" :action-width="220" action-label="操作" empty-text="暂无产品">
      <template #col-status="{ row }">
        <MateTag :variant="row.status === 1 ? 'success' : 'info'" dot>{{ row.status === 1 ? '上架' : '下架' }}</MateTag>
      </template>
      <template #actions="{ row }">
        <button v-permission="'crm:product:edit'" class="mc-action-btn" @click="openDialog(row)">编辑</button>
        <button v-permission="'crm:product:edit'" class="mc-action-btn" @click="handleToggle(row)">{{ row.status === 1 ? '下架' : '上架' }}</button>
        <MateInlineConfirm title="确认删除该产品?" @confirm="handleDelete(row)">
          <button v-permission="'crm:product:delete'" class="mc-action-btn mc-action-btn--danger">删除</button>
        </MateInlineConfirm>
      </template>
    </MateTable>

    <MatePagination v-model:page-num="query.pageNum" v-model:page-size="query.pageSize" :total="total" @change="loadData" />

    <MateDialog v-model="dialogVisible" :title="editing ? '编辑产品' : '新建产品'" :submitting="submitting" @submit="handleSubmit">
      <MateForm ref="formRef" :schema="formSchema" :model-value="form" label-position="top" @update:model-value="(v: any) => Object.assign(form, v)" />
    </MateDialog>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Plus } from 'lucide-vue-next'
import { productApi, type Product, type ProductCommand, type ConditionGroup } from '@matecloud/core'
import {
  MatePageCard, MateTable, MateButton, MateInput, MateTag, MateSearchBar, MatePagination,
  MateDialog, MateForm, MateMessage, MateInlineConfirm, type MateColumn, type FormSchema,
} from '@matecloud/ui'
import CrmAdvancedFilter from '../../components/crm/CrmAdvancedFilter.vue'
import { buildAdvancedQuery, hasValidAdvanced } from '../../composables/useCrmAdvancedQuery'

defineOptions({ name: 'CrmProductList' })

const columns: MateColumn[] = [
  { prop: 'productName', label: '产品名称', minWidth: 160 },
  { prop: 'productCode', label: '编码', width: 140 },
  { prop: 'category', label: '分类', width: 120 },
  { prop: 'price', label: '价格', width: 120 },
  { prop: 'unit', label: '单位', width: 90 },
  { prop: 'status', label: '状态', width: 100 },
]

const loading = ref(false)
const rows = ref<Product[]>([])
const total = ref(0)
const query = reactive<{ keyword: string; status?: number; pageNum: number; pageSize: number }>({ keyword: '', status: undefined, pageNum: 1, pageSize: 20 })
const advGroups = ref<ConditionGroup[]>([])

async function loadData() {
  loading.value = true
  try {
    const res = hasValidAdvanced(advGroups.value)
      ? await productApi.search(buildAdvancedQuery({
          pageNum: query.pageNum, pageSize: query.pageSize,
          kw: query.keyword, kwFields: ['productName', 'productCode'],
          base: [{ fieldKey: 'status', operator: 'EQ', values: query.status != null ? [String(query.status)] : [] }],
          groups: advGroups.value,
        }))
      : await productApi.page({ ...query })
    rows.value = res.data.list
    total.value = res.data.total
  } catch (e: any) { MateMessage.error(e?.msg || '加载失败') }
  finally { loading.value = false }
}
function handleSearch() { query.pageNum = 1; loadData() }
function handleReset() { query.keyword = ''; query.status = undefined; advGroups.value = []; handleSearch() }

async function handleToggle(row: Product) {
  try { await productApi.switchStatus(row.id, row.status === 1 ? 0 : 1); MateMessage.success('操作成功'); loadData() }
  catch (e: any) { MateMessage.error(e?.msg || '操作失败') }
}
async function handleDelete(row: Product) {
  try { await productApi.delete(row.id); MateMessage.success('删除成功'); loadData() }
  catch (e: any) { MateMessage.error(e?.msg || '删除失败') }
}

const dialogVisible = ref(false)
const editing = ref<Product | null>(null)
const submitting = ref(false)
const formRef = ref<InstanceType<typeof MateForm>>()
const form = reactive<Record<string, any>>({})

const formSchema = computed<FormSchema[]>(() => [
  { field: 'productName', label: '产品名称', type: 'input', rules: [{ required: true, message: '请输入产品名称', trigger: 'blur' }] },
  { field: 'productCode', label: '产品编码', type: 'input' },
  { field: 'category', label: '分类', type: 'input' },
  { field: 'price', label: '价格', type: 'number', props: { min: 0, precision: 2 } },
  { field: 'unit', label: '单位', type: 'input' },
  { field: 'sort', label: '排序', type: 'number' },
  { field: 'description', label: '描述', type: 'textarea' },
])

function openDialog(row?: Product) {
  editing.value = row || null
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, {
    productName: row?.productName || '', productCode: row?.productCode || '', category: row?.category || '',
    price: row?.price ?? 0, unit: row?.unit || '', sort: row?.sort ?? 0, description: row?.description || '',
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return
  submitting.value = true
  try {
    const payload = { ...form } as ProductCommand
    if (editing.value) await productApi.update(editing.value.id, payload)
    else await productApi.create(payload)
    MateMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch (e: any) { MateMessage.error(e?.msg || '保存失败') }
  finally { submitting.value = false }
}

onMounted(loadData)
</script>
