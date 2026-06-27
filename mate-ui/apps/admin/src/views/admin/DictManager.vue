<template>
  <MatePageCard :title="t('dict.title')" :description="t('dict.description')">
    <div class="dict-layout">
      <!-- ========== LEFT: Dict Types ========== -->
      <div class="pane pane--types">
        <div class="pane-header">
          <span class="pane-title">{{ t('dict.types') }}</span>
          <el-button v-permission="'sys:dict:add'" size="small" type="primary" @click="openTypeDialog()">
            <Plus :size="13" class="mr-1" />{{ t('dict.createType') }}
          </el-button>
        </div>

        <div class="pane-search">
          <el-input
v-model="typeKeyword" size="small" :placeholder="t('dict.searchTypes')" clearable
                    @keyup.enter="loadTypes" @clear="loadTypes">
            <template #prefix><Search :size="13" /></template>
          </el-input>
        </div>

        <div v-loading="typesLoading" class="pane-body">
          <MateEmpty v-if="!typeList.length && !typesLoading" :text="t('common.noData')" :padding="32" />
          <div
            v-for="t0 in typeList"
            :key="t0.id"
            class="type-row"
            :class="{ 'type-row--active': currentType?.id === t0.id }"
            @click="selectType(t0)"
          >
            <div class="type-info">
              <span class="type-name">{{ t0.dictName }}</span>
              <span class="type-code">{{ t0.dictType }}</span>
            </div>
            <div class="type-actions">
              <button
                v-permission="'sys:dict:edit'"
                class="icon-btn" :title="t('common.edit')"
                @click.stop="openTypeDialog(t0)"
              ><Pencil :size="13" /></button>
              <MateInlineConfirm
                :title="t('common.deleteConfirm')"
                @confirm="handleDeleteType(t0)"
              >
                                  <button
                    v-permission="'sys:dict:delete'"
                    class="icon-btn icon-btn--danger" :title="t('common.delete')"
                    @click.stop
                  ><Trash2 :size="13" /></button>
                
              </MateInlineConfirm>
            </div>
          </div>
        </div>

        <div class="pane-pager">
          <MatePagination
            v-model:page-num="typePageNum"
            v-model:page-size="typePageSize"
            :total="typeTotal"
            align="center"
            layout="prev, pager, next"
            small
            @change="loadTypes"
          />
        </div>
      </div>

      <!-- ========== RIGHT: Dict Data ========== -->
      <div class="pane pane--data">
        <div class="pane-header">
          <span class="pane-title">
            {{ t('dict.data') }}
            <span v-if="currentType" class="pane-subtitle">/ {{ currentType.dictName }}</span>
          </span>
          <el-button
            v-permission="'sys:dict:add'"
            size="small"
            type="primary"
            :disabled="!currentType"
            @click="openDataDialog()"
          >
            <Plus :size="13" class="mr-1" />{{ t('dict.createData') }}
          </el-button>
        </div>

        <div class="pane-search">
          <el-input
v-model="dataKeyword" size="small" :placeholder="t('dict.searchData')"
                    clearable :disabled="!currentType"
                    @keyup.enter="loadData" @clear="loadData">
            <template #prefix><Search :size="13" /></template>
          </el-input>
        </div>

        <div v-loading="dataLoading" class="pane-body pane-body--data">
          <MateEmpty v-if="!currentType" :text="t('dict.selectTypeFirst')" :padding="48" />
          <MateEmpty v-else-if="!dataList.length && !dataLoading" :text="t('common.noData')" :padding="48" />
          <MateTable
            v-else
            :columns="dataColumns"
            :data="dataList"
            :action-width="190"
            :action-label="t('common.action')"
            :empty-text="t('common.noData')"
          >
            <template #col-dictValue="{ row }">
              <span class="mc-mono">{{ row.dictValue }}</span>
            </template>
            <template #col-status="{ row }">
              <MateBadge :status="row.status === 1 ? 'ACTIVE' : 'DISABLED'">
                {{ row.status === 1 ? t('common.enabled') : t('common.disabled') }}
              </MateBadge>
            </template>
            <template #actions="{ row }">
              <button v-permission="'sys:dict:edit'" class="mc-action-btn" @click="openDataDialog(row)">
                {{ t('common.edit') }}
              </button>
              <button
                v-permission="'sys:dict:edit'"
                class="mc-action-btn"
                @click="toggleDataStatus(row)"
              >
                {{ row.status === 1 ? t('common.disable') : t('common.enable') }}
              </button>
              <MateInlineConfirm :title="t('common.deleteConfirm')" @confirm="handleDeleteData(row)">
                                  <button v-permission="'sys:dict:delete'" class="mc-action-btn mc-action-btn--danger">
                    {{ t('common.delete') }}
                  </button>
                
              </MateInlineConfirm>
            </template>
          </MateTable>
        </div>

        <MatePagination
          v-model:page-num="dataPageNum"
          v-model:page-size="dataPageSize"
          :total="dataTotal"
          small
          @change="loadData"
        />
      </div>
    </div>

    <!-- ========== Dict Type Dialog ========== -->
    <MateDialog
      v-model="typeDialog.visible"
      :title="typeDialog.editing ? t('dict.editType') : t('dict.createType')"
      width="460px"
      :submitting="submitting"
      @submit="submitType"
    >
      <MateForm
        ref="typeFormRef"
        :schema="typeSchema"
        :model-value="typeDialog.form"
        label-position="top"
        @update:model-value="(v: any) => Object.assign(typeDialog.form, v)"
      />
    </MateDialog>

    <!-- ========== Dict Data Dialog ========== -->
    <MateDialog
      v-model="dataDialog.visible"
      :title="dataDialog.editing ? t('dict.editData') : t('dict.createData')"
      width="460px"
      :submitting="submitting"
      @submit="submitData"
    >
      <MateForm
        ref="dataFormRef"
        :schema="dataSchema"
        :model-value="dataDialog.form"
        label-position="top"
        @update:model-value="(v: any) => Object.assign(dataDialog.form, v)"
      />
    </MateDialog>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Plus, Search, Pencil, Trash2 } from 'lucide-vue-next'
import { adminApi, type DictType, type DictDataItem } from '@matecloud/core'

defineOptions({ name: 'DictManagerView' })
import { MatePageCard, MateBadge, MateTable, MatePagination, MateDialog, MateForm, MateEmpty, type MateColumn, type FormSchema, MateMessage, MateInlineConfirm } from '@matecloud/ui'

const { t } = useI18n()

// ---- types pane ----
const typeList = ref<DictType[]>([])
const typeTotal = ref(0)
const typePageNum = ref(1)
const typePageSize = ref(20)
const typeKeyword = ref('')
const typesLoading = ref(false)
const currentType = ref<DictType | null>(null)

async function loadTypes() {
  typesLoading.value = true
  try {
    const res = await adminApi.dictTypeList({
      pageNum: typePageNum.value,
      pageSize: typePageSize.value,
      keyword: typeKeyword.value || undefined,
    })
    const data: any = res.data ?? res
    typeList.value = data.list ?? data ?? []
    typeTotal.value = data.total ?? typeList.value.length
    if (!currentType.value && typeList.value.length) {
      selectType(typeList.value[0])
    }
  } catch {
    typeList.value = []
    typeTotal.value = 0
  } finally {
    typesLoading.value = false
  }
}

function selectType(type: DictType) {
  currentType.value = type
  dataPageNum.value = 1
  dataKeyword.value = ''
  loadData()
}

// ---- data pane ----
const dataColumns: MateColumn[] = [
  { prop: 'dictLabel', label: t('dict.label'), minWidth: 140 },
  { prop: 'dictValue', label: t('dict.value'), minWidth: 140 },
  { prop: 'sort', label: t('dict.sort'), width: 80, align: 'center' },
  { prop: 'status', label: t('common.status'), width: 90, align: 'center' },
]

const dataList = ref<DictDataItem[]>([])
const dataTotal = ref(0)
const dataPageNum = ref(1)
const dataPageSize = ref(10)
const dataKeyword = ref('')
const dataLoading = ref(false)

async function loadData() {
  if (!currentType.value) {
    dataList.value = []
    dataTotal.value = 0
    return
  }
  dataLoading.value = true
  try {
    const res = await adminApi.dictDataList({
      pageNum: dataPageNum.value,
      pageSize: dataPageSize.value,
      dictType: currentType.value.dictType,
      keyword: dataKeyword.value || undefined,
    })
    const data: any = res.data ?? res
    dataList.value = data.list ?? data ?? []
    dataTotal.value = data.total ?? dataList.value.length
  } catch {
    dataList.value = []
    dataTotal.value = 0
  } finally {
    dataLoading.value = false
  }
}

// ---- type dialog ----
const submitting = ref(false)
const typeFormRef = ref<InstanceType<typeof MateForm>>()
const typeDialog = reactive({
  visible: false,
  editing: null as DictType | null,
  form: { dictName: '', dictType: '', status: 1, remark: '' } as Record<string, any>,
})

const typeSchema = computed<FormSchema[]>(() => [
  {
    field: 'dictName',
    label: t('dict.dictName'),
    type: 'input',
    rules: [{ required: true, message: t('dict.dictNameRequired'), trigger: 'blur' }],
  },
  {
    field: 'dictType',
    label: t('dict.dictType'),
    type: 'input',
    placeholder: 'sys_xxx',
    disabled: () => !!typeDialog.editing,
    rules: [{ required: true, message: t('dict.dictTypeRequired'), trigger: 'blur' }],
  },
  {
    field: 'status',
    label: t('common.status'),
    type: 'switch',
    props: { activeValue: 1, inactiveValue: 0 },
  },
  { field: 'remark', label: t('dict.remark'), type: 'textarea' },
])

function openTypeDialog(row?: DictType) {
  typeDialog.editing = row ?? null
  typeDialog.form = {
    dictName: row?.dictName ?? '',
    dictType: row?.dictType ?? '',
    status: row?.status ?? 1,
    remark: row?.remark ?? '',
  }
  typeDialog.visible = true
}

async function submitType() {
  const valid = await typeFormRef.value?.validate()
  if (!valid) return
  submitting.value = true
  try {
    if (typeDialog.editing) {
      await adminApi.dictTypeUpdate(typeDialog.editing.id, typeDialog.form as any)
      MateMessage.success(t('common.updateSuccess'))
    } else {
      await adminApi.dictTypeCreate(typeDialog.form as any)
      MateMessage.success(t('common.createSuccess'))
    }
    typeDialog.visible = false
    loadTypes()
  } finally {
    submitting.value = false
  }
}

async function handleDeleteType(row: DictType) {
  await adminApi.dictTypeDelete(row.id)
  MateMessage.success(t('common.deleteSuccess'))
  if (currentType.value?.id === row.id) currentType.value = null
  loadTypes()
}

// ---- data dialog ----
const dataFormRef = ref<InstanceType<typeof MateForm>>()
const dataDialog = reactive({
  visible: false,
  editing: null as DictDataItem | null,
  form: { dictLabel: '', dictValue: '', sort: 0, status: 1, remark: '' } as Record<string, any>,
})

const dataSchema = computed<FormSchema[]>(() => [
  {
    field: 'dictLabel',
    label: t('dict.label'),
    type: 'input',
    rules: [{ required: true, message: t('dict.dictLabelRequired'), trigger: 'blur' }],
  },
  {
    field: 'dictValue',
    label: t('dict.value'),
    type: 'input',
    rules: [{ required: true, message: t('dict.dictValueRequired'), trigger: 'blur' }],
  },
  { field: 'sort', label: t('dict.sort'), type: 'number' },
  {
    field: 'status',
    label: t('common.status'),
    type: 'switch',
    props: { activeValue: 1, inactiveValue: 0 },
  },
  { field: 'remark', label: t('dict.remark'), type: 'textarea' },
])

function openDataDialog(row?: DictDataItem) {
  if (!currentType.value) {
    MateMessage.warning(t('dict.selectTypeFirst'))
    return
  }
  dataDialog.editing = row ?? null
  dataDialog.form = {
    dictLabel: row?.dictLabel ?? '',
    dictValue: row?.dictValue ?? '',
    sort: row?.sort ?? 0,
    status: row?.status ?? 1,
    remark: row?.remark ?? '',
  }
  dataDialog.visible = true
}

async function submitData() {
  const valid = await dataFormRef.value?.validate()
  if (!valid || !currentType.value) return
  submitting.value = true
  try {
    if (dataDialog.editing) {
      await adminApi.dictDataUpdate(dataDialog.editing.id, dataDialog.form as any)
      MateMessage.success(t('common.updateSuccess'))
    } else {
      await adminApi.dictDataCreate({
        ...(dataDialog.form as any),
        dictType: currentType.value.dictType,
      })
      MateMessage.success(t('common.createSuccess'))
    }
    dataDialog.visible = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function toggleDataStatus(row: DictDataItem) {
  const next = row.status === 1 ? 0 : 1
  try {
    // Send the full row with the flipped status (PUT does a full update).
    await adminApi.dictDataUpdate(row.id, {
      dictType: currentType.value?.dictType,
      dictLabel: row.dictLabel,
      dictValue: row.dictValue,
      sort: row.sort,
      status: next,
      remark: (row as any).remark,
    } as any)
    MateMessage.success(t('common.updateSuccess'))
    loadData()
  } catch (e: any) {
    MateMessage.error(e?.msg || t('common.failed'))
  }
}

async function handleDeleteData(row: DictDataItem) {
  await adminApi.dictDataDelete(row.id)
  MateMessage.success(t('common.deleteSuccess'))
  loadData()
}

onMounted(loadTypes)
</script>

<style scoped>
.dict-layout {
  display: flex;
  gap: 16px;
  height: calc(100vh - var(--mc-header-height, 56px) - var(--mc-tab-height, 40px) - 160px);
  min-height: 420px;
}

.pane {
  display: flex;
  flex-direction: column;
  min-height: 0;
  border: 1px solid var(--mc-divider-regular, rgb(16 24 40 / 0.08));
  border-radius: var(--mc-radius);
  overflow: hidden;
  background: var(--mc-bg-elevated);
}
.pane--types { width: 320px; flex-shrink: 0; }
.pane--data { flex: 1; min-width: 0; }

/* Phone: the two side-by-side panes don't fit — stack them. The type list gets
   a capped, scrollable height; the data pane sits below at a usable height. */
@media (max-width: 768px) {
  .dict-layout {
    flex-direction: column;
    height: auto;
    min-height: 0;
    gap: 12px;
  }
  .pane--types { width: auto; }
  .pane--types .pane-body { max-height: 38vh; }
  .pane--data { min-height: 60vh; }
}

.pane-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 14px;
  border-bottom: 1px solid var(--mc-divider-subtle, rgb(16 24 40 / 0.04));
}
.pane-title {
  font-size: 13px; font-weight: 600;
  color: var(--mc-text-primary);
}
.pane-subtitle {
  font-weight: 400; color: var(--mc-text-muted);
  margin-left: 4px;
}

.pane-search { padding: 10px 14px 0; }

.pane-body {
  flex: 1; min-height: 0; overflow: auto;
  padding: 4px 0;
}
.pane-body--data { padding: 10px 14px; }

.pane-pager {
  padding: 8px 14px;
  border-top: 1px solid var(--mc-divider-subtle, rgb(16 24 40 / 0.04));
  display: flex; justify-content: center;
}

/* type row */
.type-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 14px;
  cursor: pointer;
  border-left: 2px solid transparent;
  transition: background 0.1s;
}
.type-row:hover { background: var(--mc-state-hover, rgb(200 206 218 / 0.2)); }
.type-row--active {
  background: var(--mc-menu-bg-active, rgb(21 90 239 / 0.08));
  border-left-color: var(--mc-primary);
}

.type-info { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.type-name {
  font-size: 13px; font-weight: 500;
  color: var(--mc-text-primary);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.type-code {
  font-size: 11px;
  color: var(--mc-text-muted);
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
}

.type-actions { display: flex; gap: 4px; opacity: 0; transition: opacity 0.1s; }
.type-row:hover .type-actions,
.type-row--active .type-actions { opacity: 1; }

.icon-btn {
  border: none; background: none; cursor: pointer; padding: 4px;
  border-radius: 4px;
  color: var(--mc-text-muted);
  display: inline-flex; align-items: center; justify-content: center;
}
.icon-btn:hover { color: var(--mc-primary); background: var(--mc-state-accent-hover, #EFF4FF); }
.icon-btn--danger:hover { color: var(--mc-danger); background: rgb(240 68 56 / 0.08); }
</style>
