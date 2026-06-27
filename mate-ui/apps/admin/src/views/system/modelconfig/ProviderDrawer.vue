<template>
  <MateDialog
    :model-value="visible"
    :title="isEdit ? t('modelConfig.editProvider') : t('modelConfig.addProvider')"
    width="560px"
    :submitting="saving"
    :confirm-text="t('modelConfig.save')"
    :cancel-text="t('modelConfig.cancel')"
    @update:model-value="(v: boolean) => !v && $emit('close')"
    @submit="onSubmit"
  >
    <el-form label-position="top" class="md-form">
      <!-- vendor: locked on edit, descriptor picker on create -->
      <el-form-item :label="t('modelConfig.vendorField')" required>
        <el-select
          v-model="vendor"
          :disabled="isEdit"
          filterable
          style="width: 100%"
          :placeholder="t('modelConfig.vendorPlaceholder')"
          @change="onVendorChange"
        >
          <el-option
            v-for="d in descriptors"
            :key="d.vendor"
            :label="descriptorLabel(d)"
            :value="d.vendor"
          />
        </el-select>
        <div v-if="activeDescriptor" class="tip">{{ descriptorDesc(activeDescriptor) }}</div>
      </el-form-item>

      <el-form-item :label="t('modelConfig.nameField')" required>
        <el-input v-model="name" :placeholder="t('modelConfig.namePlaceholder')" clearable />
      </el-form-item>

      <el-form-item :label="t('modelConfig.modalitiesField')" required>
        <el-checkbox-group v-model="modalities">
          <el-checkbox
            v-for="m in availableModalities"
            :key="m"
            :value="m"
            border
            class="md-modality"
          >{{ t(`modelConfig.modality.${m}`) }}</el-checkbox>
        </el-checkbox-group>
        <div class="tip">{{ t('modelConfig.modalitiesTip') }}</div>
      </el-form-item>

      <!-- editable list of supported model ids (preset + free add/remove) -->
      <el-form-item>
        <template #label>
          <div class="md-models-head">
            <span>{{ t('modelConfig.models') }}</span>
            <MateTooltip
              v-if="!isEdit"
              :content="t('modelConfig.fetchSaveFirst')"
              placement="top"
            >
              <span>
                <el-button text type="primary" size="small" disabled>
                  ⟳ {{ t('modelConfig.fetchModels') }}
                </el-button>
              </span>
            </MateTooltip>
            <el-button
              v-else
              text
              type="primary"
              size="small"
              :loading="fetching"
              @click="onFetchModels"
            >
              ⟳ {{ t('modelConfig.fetchModels') }}
            </el-button>
          </div>
        </template>
        <el-select
          v-model="models"
          multiple
          filterable
          allow-create
          default-first-option
          style="width: 100%"
          :placeholder="t('modelConfig.modelsPlaceholder')"
        >
          <el-option
            v-for="m in suggestedModels"
            :key="m"
            :label="m"
            :value="m"
          />
        </el-select>
        <div class="tip">{{ isEdit ? t('modelConfig.fetchModelsTip') : t('modelConfig.modelsTip') }}</div>
      </el-form-item>

      <!-- descriptor-driven credential fields -->
      <template v-for="f in fields" :key="f.key">
        <el-form-item :label="fieldLabel(f)" :required="f.required">
          <el-input
            v-if="f.type === 'SECRET'"
            v-model="values[f.key]"
            type="password"
            show-password
            :placeholder="secretConfigured[f.key] ? t('modelConfig.secretConfigured') : t('modelConfig.secretPlaceholder')"
          />
          <el-input
            v-else-if="f.type === 'MAP' || f.type === 'TEXTAREA'"
            v-model="values[f.key]"
            type="textarea"
            :rows="3"
            :placeholder="textareaPlaceholder(f)"
          />
          <el-input
            v-else
            v-model="values[f.key]"
            :placeholder="f.placeholder"
            clearable
          />
          <div v-if="f.type === 'SECRET'" class="tip">{{ t('modelConfig.secretTipKeep') }}</div>
          <div v-else-if="f.tip" class="tip">{{ f.tip }}</div>
        </el-form-item>
      </template>

      <el-form-item :label="t('modelConfig.enabledField')">
        <el-switch v-model="enabled" :active-text="t('modelConfig.enabledOn')" />
      </el-form-item>
    </el-form>
  </MateDialog>
</template>

<script lang="ts" setup>
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { MateDialog, MateMessage, MateTooltip } from '@matecloud/ui'
import { modelApi } from '@matecloud/core'
import type {
  ModelDescriptorView,
  ModelFieldSpec,
  ModelProviderRequest,
  ModelProviderView,
} from '@matecloud/core'

const props = defineProps<{
  visible: boolean
  /** Provider being edited, or null for create. */
  provider: ModelProviderView | null
  /** Vendor pre-selected when opening in create mode (from "addable" card). */
  presetVendor?: string | null
  descriptors: ModelDescriptorView[]
  saving: boolean
}>()
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'submit', payload: { id: string | null; req: ModelProviderRequest }): void
}>()

const { t, te } = useI18n()

const isEdit = computed(() => !!props.provider)
const vendor = ref('')
const name = ref('')
const modalities = ref<string[]>([])
const models = ref<string[]>([])
const enabled = ref(true)
const fetching = ref(false)
const values = reactive<Record<string, any>>({})
const secretConfigured = reactive<Record<string, boolean>>({})

const activeDescriptor = computed(() =>
  props.descriptors.find((d) => d.vendor === vendor.value) || null)
const fields = computed<ModelFieldSpec[]>(() => activeDescriptor.value?.fields || [])
const availableModalities = computed(() => activeDescriptor.value?.modalities || [])
const suggestedModels = computed(() => activeDescriptor.value?.suggestedModels || [])

function descriptorLabel(d: ModelDescriptorView): string {
  const k = `modelConfig.vendor.${d.vendor}`
  return te(k) ? t(k) : d.name
}
function descriptorDesc(d: ModelDescriptorView): string {
  const k = `modelConfig.vendorDesc.${d.vendor}`
  return te(k) ? t(k) : d.describe
}
function fieldLabel(f: ModelFieldSpec): string {
  const k = `modelConfig.field.${f.key}`
  return te(k) ? t(k) : f.label
}
function textareaPlaceholder(f: ModelFieldSpec): string {
  if (f.placeholder) return f.placeholder
  return f.type === 'MAP' ? '{"key":"value"}' : ''
}

function resetValues() {
  Object.keys(values).forEach((k) => delete values[k])
  Object.keys(secretConfigured).forEach((k) => delete secretConfigured[k])
}

/** Hydrate the form when the dialog opens (edit vs create). */
function hydrate() {
  resetValues()
  const p = props.provider
  if (p) {
    vendor.value = p.vendor
    name.value = p.name
    modalities.value = [...(p.modalities || [])]
    models.value = [...(p.models || [])]
    enabled.value = p.enabled
    for (const f of p.fields) {
      if (f.type === 'SECRET') {
        values[f.key] = ''
        secretConfigured[f.key] = p.values?.[f.key] === '******'
      } else if (f.type === 'MAP') {
        const v = p.values?.[f.key]
        values[f.key] = v && typeof v === 'object' ? JSON.stringify(v, null, 2) : (v ?? '')
      } else {
        values[f.key] = p.values?.[f.key] ?? ''
      }
    }
  } else {
    vendor.value = props.presetVendor || (props.descriptors[0]?.vendor ?? '')
    name.value = activeDescriptor.value ? descriptorLabel(activeDescriptor.value) : ''
    modalities.value = [...availableModalities.value]
    models.value = [...suggestedModels.value]
    enabled.value = true
    for (const f of fields.value) values[f.key] = ''
  }
}

watch(() => props.visible, (v) => { if (v) hydrate() })

function onVendorChange() {
  if (isEdit.value) return
  name.value = activeDescriptor.value ? descriptorLabel(activeDescriptor.value) : ''
  modalities.value = [...availableModalities.value]
  models.value = [...suggestedModels.value]
  resetValues()
  for (const f of fields.value) values[f.key] = ''
}

/**
 * Live-fetch the model list from the provider (edit mode only) and merge the result
 * into the current selection, preserving any ids the operator added by hand.
 */
async function onFetchModels() {
  const id = props.provider?.id
  if (!id) {
    MateMessage.warning(t('modelConfig.fetchSaveFirst'))
    return
  }
  fetching.value = true
  try {
    const res = await modelApi.fetchModels(id)
    const fetched = res.data || []
    const merged = Array.from(new Set([...models.value, ...fetched]))
    models.value = merged
    MateMessage.success(t('modelConfig.fetched', { n: fetched.length }))
  } catch (e: any) {
    MateMessage.error(e?.message || e?.msg || String(e))
  } finally {
    fetching.value = false
  }
}

/** Build the request; MAP fields are parsed from textarea JSON. */
function assembleValues(): Record<string, any> | null {
  const out: Record<string, any> = {}
  for (const f of fields.value) {
    const raw = values[f.key]
    if (f.type === 'MAP') {
      const s = (raw ?? '').toString().trim()
      if (!s) continue
      try {
        out[f.key] = JSON.parse(s)
      } catch {
        MateMessage.error(t('modelConfig.invalidJson', { field: fieldLabel(f) }))
        return null
      }
    } else {
      out[f.key] = raw
    }
  }
  return out
}

function onSubmit() {
  if (!vendor.value) { MateMessage.warning(t('modelConfig.vendorRequired')); return }
  if (!name.value.trim()) { MateMessage.warning(t('modelConfig.nameRequired')); return }
  if (!modalities.value.length) { MateMessage.warning(t('modelConfig.modalitiesRequired')); return }
  const assembled = assembleValues()
  if (assembled === null) return
  const req: ModelProviderRequest = {
    vendor: vendor.value,
    name: name.value.trim(),
    modalities: modalities.value,
    enabled: enabled.value,
    sort: props.provider?.sort ?? 0,
    values: assembled,
    models: [...models.value],
  }
  emit('submit', { id: props.provider?.id ?? null, req })
}
</script>

<style scoped>
.md-form { padding: 4px 2px; }
.tip { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 4px; line-height: 1.5; }
.md-modality { margin: 0 8px 8px 0; }
.md-models-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  line-height: normal;
}
</style>
