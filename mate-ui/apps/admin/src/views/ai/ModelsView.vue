<template>
  <MatePageCard :title="t('ai.models.title')" :description="t('ai.models.description')">
    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <el-input v-model="search.keyword" :placeholder="t('ai.models.searchPlaceholder')"
                style="width: 200px" clearable @keyup.enter="handleSearch" />
      <el-select v-model="search.vendor" :placeholder="t('ai.models.vendorPlaceholder')" clearable style="width: 140px">
        <el-option label="OpenAI"    value="OPENAI" />
        <el-option label="Anthropic" value="ANTHROPIC" />
        <el-option label="Google"    value="GOOGLE" />
        <el-option label="DeepSeek"  value="DEEPSEEK" />
        <el-option label="Custom"    value="CUSTOM" />
      </el-select>
      <el-button type="primary" :icon="Plus" @click="openCreate">{{ t('ai.models.createBtn') }}</el-button>
    </MateSearchBar>

    <div v-if="!loading && !rows.length">
      <MateEmpty :description="t('ai.models.empty')" />
    </div>
    <div v-else class="ai-models__grid" v-loading="loading">
      <AiProviderCard
        v-for="p in rows"
        :key="p.id"
        :name="p.name"
        :vendor="p.vendor"
        :model="p.defaultModel || ''"
        :api-key-masked="p.apiKeyMasked || ''"
        :base-url="p.baseUrl || ''"
        :is-default="p.isDefault === 1"
      >
        <template #actions>
          <el-button text size="small" :icon="EditPen" @click="openEdit(p)">{{ t('ai.models.edit') }}</el-button>
          <el-button text size="small" :icon="Connection" @click="testProvider(p)">{{ t('ai.models.test') }}</el-button>
          <el-button text size="small" :icon="Switch"
                     :type="p.enabled === 1 ? 'warning' : 'success'"
                     @click="toggle(p)">{{ p.enabled === 1 ? t('ai.models.disable') : t('ai.models.enable') }}</el-button>
          <el-button text size="small" :icon="Delete" type="danger" @click="remove(p)">{{ t('ai.models.delete') }}</el-button>
        </template>
      </AiProviderCard>
    </div>

    <MatePagination v-model:page-num="pageNum" v-model:page-size="pageSize"
                    :total="total" @change="loadData" />

    <MateDialog v-model="formVisible" :title="isEdit ? t('ai.models.editTitle') : t('ai.models.createTitle')" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item :label="t('ai.common.code')" prop="code">
          <el-input v-model="form.code" :disabled="isEdit" :placeholder="t('ai.models.codePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('ai.models.nameLabel')" prop="name">
          <el-input v-model="form.name" :placeholder="t('ai.models.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('ai.models.vendorLabel')" prop="vendor">
          <el-select v-model="form.vendor" style="width: 100%">
            <el-option label="OpenAI"    value="OPENAI" />
            <el-option label="Anthropic" value="ANTHROPIC" />
            <el-option label="Google"    value="GOOGLE" />
            <el-option label="DeepSeek"  value="DEEPSEEK" />
            <el-option label="Custom"    value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('ai.models.baseUrlLabel')">
          <el-input v-model="form.baseUrl" :placeholder="t('ai.models.baseUrlPlaceholder')" />
        </el-form-item>
        <el-form-item :label="isEdit ? t('ai.models.apiKeyEditLabel') : t('ai.models.apiKeyLabel')"
                      :prop="isEdit ? '' : 'apiKey'">
          <el-input v-model="form.apiKey" show-password :placeholder="t('ai.models.apiKeyPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('ai.models.defaultModelLabel')">
          <el-input v-model="form.defaultModel" :placeholder="t('ai.models.defaultModelPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('ai.models.availableModelsLabel')">
          <el-input v-model="form.availableModels" type="textarea" :rows="2"
                    :placeholder="t('ai.models.availableModelsPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('ai.models.isDefaultLabel')">
          <el-switch v-model="defaultSwitch" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">{{ t('ai.common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ t('ai.common.save') }}</el-button>
      </template>
    </MateDialog>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance } from 'element-plus'
import { Plus, EditPen, Delete, Connection, Switch } from '@element-plus/icons-vue'
import { aiApi, type ProviderView, type ProviderRequest } from '@matecloud/core'
import { MatePageCard, MateSearchBar, MatePagination, MateDialog, MateEmpty, AiProviderCard, MateMessage, MateMessageBox } from '@matecloud/ui'

defineOptions({ name: 'AiModelsView' })

const { t } = useI18n()

const loading = ref(false)
const rows = ref<ProviderView[]>([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const search = reactive({ keyword: '', vendor: '' as string })

async function loadData() {
  loading.value = true
  try {
    const res: any = await aiApi.providerPage({
      pageNum: pageNum.value, pageSize: pageSize.value,
      keyword: search.keyword || undefined, vendor: search.vendor || undefined,
    })
    rows.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() { search.keyword = ''; search.vendor = ''; handleSearch() }

const formVisible = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<ProviderRequest>({
  code: '', name: '', vendor: 'OPENAI', baseUrl: '', apiKey: '',
  defaultModel: '', availableModels: '', isDefault: 0,
})
const defaultSwitch = computed({
  get: () => form.isDefault === 1,
  set: (v: boolean) => { form.isDefault = v ? 1 : 0 },
})
const rules = computed(() => ({
  code: [{ required: true, message: t('ai.models.codeRequired'), trigger: 'blur' }],
  name: [{ required: true, message: t('ai.models.nameRequired'), trigger: 'blur' }],
  vendor: [{ required: true, message: t('ai.models.vendorRequired'), trigger: 'change' }],
  apiKey: [{ required: true, message: t('ai.models.apiKeyRequired'), trigger: 'blur' }],
}))

function openCreate() {
  isEdit.value = false; editingId.value = null
  Object.assign(form, { code: '', name: '', vendor: 'OPENAI', baseUrl: '', apiKey: '',
    defaultModel: '', availableModels: '', isDefault: 0 })
  formVisible.value = true
}

function openEdit(row: ProviderView) {
  isEdit.value = true; editingId.value = row.id
  Object.assign(form, {
    code: row.code, name: row.name, vendor: row.vendor, baseUrl: row.baseUrl,
    apiKey: '', defaultModel: row.defaultModel, availableModels: row.availableModels,
    isDefault: row.isDefault,
  })
  formVisible.value = true
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value && editingId.value) {
      await aiApi.providerUpdate(editingId.value, form)
    } else {
      await aiApi.providerCreate(form)
    }
    MateMessage.success(t('ai.common.saveSuccess'))
    formVisible.value = false
    loadData()
  } finally { saving.value = false }
}

async function testProvider(p: ProviderView) {
  try {
    const res: any = await aiApi.providerTest(p.id)
    if (res.data) MateMessage.success(t('ai.models.testSuccess', { name: p.name }))
    else MateMessage.error(t('ai.models.testFailed', { name: p.name }))
    loadData()
  } catch { /* interceptor */ }
}

async function toggle(p: ProviderView) {
  await aiApi.providerToggle(p.id, p.enabled !== 1)
  loadData()
}

async function remove(p: ProviderView) {
  await MateMessageBox.confirm(t('ai.models.deleteConfirm', { name: p.name }), t('ai.common.confirmTitle'), { type: 'warning' })
  await aiApi.providerDelete(p.id)
  MateMessage.success(t('ai.common.deleted'))
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.ai-models__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
  padding: 8px 0 16px;
}
</style>
