<template>
  <div class="sm-wrap">
    <div v-for="row in rows" :key="row.modelType" class="smrow">
      <div class="smt">
        <span class="modal" :class="modalityClass(row.modelType)">
          {{ t(`modelConfig.modality.${row.modelType}`) }}
        </span>
        <span class="smt-name">{{ t(`modelConfig.modalityName.${row.modelType}`) }}</span>
      </div>

      <div class="msel" :class="{ unset: !row.providerId }" @click="openEdit(row)">
        <template v-if="row.providerId">
          <img v-if="vendorIcon(row.vendor || '')" class="mlogo mlogo-img" :src="vendorIcon(row.vendor || '')!" :alt="row.vendor || ''" />
          <span v-else class="mlogo" :style="{ background: vendorColor(row.vendor || '') }">
            {{ vendorInitials(row.providerName || row.vendor || '?') }}
          </span>
          <span class="mnm">{{ row.model || t('modelConfig.noModelName') }}</span>
          <span class="mvia">· {{ row.providerName }}</span>
        </template>
        <span v-else class="mempty">{{ t('modelConfig.notConfigured') }}</span>
        <ChevronDown :size="14" class="mchev" />
      </div>
    </div>

    <MateEmpty v-if="!rows.length" :description="t('modelConfig.empty')" />

    <!-- capability legend (inline SVG icons, mirrors the prototype) -->
    <div class="cap-legend">
      <span class="cap-legend-tt">{{ t('modelConfig.capLegend') }}</span>
      <span v-for="k in CAP_KEYS" :key="k" class="cap-item">
        <span class="cap-ic" v-html="capSvg(k)" />
        <span class="cap-lb">{{ t(`modelConfig.cap.${k}`) }}</span>
      </span>
    </div>

    <!-- Edit default for a model type -->
    <MateDialog
      v-model="editVisible"
      :title="editTitle"
      width="460px"
      :submitting="submitting"
      :confirm-text="t('modelConfig.save')"
      :cancel-text="t('modelConfig.cancel')"
      @submit="onSubmit"
    >
      <el-form label-position="top">
        <el-form-item :label="t('modelConfig.providerField')" required>
          <el-select v-model="formProviderId" filterable style="width: 100%" :placeholder="t('modelConfig.providerPlaceholder')">
            <el-option
              v-for="p in eligibleProviders"
              :key="p.id"
              :label="p.name"
              :value="p.id"
            />
          </el-select>
          <div v-if="!eligibleProviders.length" class="tip warn">{{ t('modelConfig.noEligibleProvider') }}</div>
        </el-form-item>
        <el-form-item :label="t('modelConfig.modelNameField')" required>
          <el-select
            v-model="formModel"
            filterable
            allow-create
            default-first-option
            clearable
            style="width: 100%"
            :placeholder="modelPlaceholder"
          >
            <el-option
              v-for="m in providerModels"
              :key="m"
              :label="m"
              :value="m"
            />
          </el-select>
          <div class="tip">{{ t('modelConfig.pickModelTip') }}</div>
        </el-form-item>
      </el-form>
    </MateDialog>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ChevronDown } from 'lucide-vue-next'
import { MateDialog, MateEmpty, MateMessage } from '@matecloud/ui'
import type { ModelProviderView, SystemModelView } from '@matecloud/core'
import { modalityClass, vendorColor, vendorIcon, vendorInitials } from './theme'
import { CAP_KEYS, capSvg } from './capIcon'

const props = defineProps<{
  rows: SystemModelView[]
  providers: ModelProviderView[]
}>()
const emit = defineEmits<{
  (e: 'save', payload: { type: string; providerId: string; model: string }): void
}>()

const { t } = useI18n()

const editVisible = ref(false)
const submitting = ref(false)
const editType = ref('')
const formProviderId = ref('')
const formModel = ref('')

const editTitle = computed(() =>
  editType.value ? t('modelConfig.setDefaultFor', { type: t(`modelConfig.modalityName.${editType.value}`) }) : '')

// Only enabled providers that serve this modality are eligible defaults.
const eligibleProviders = computed(() =>
  props.providers.filter((p) => p.enabled && p.modalities.includes(editType.value)))

// Model ids configured on the selected provider (free-add still allowed).
const providerModels = computed(() => {
  const p = props.providers.find((x) => x.id === formProviderId.value)
  return p?.models || []
})

const modelPlaceholder = computed(() => {
  const p = props.providers.find((x) => x.id === formProviderId.value)
  return p ? t('modelConfig.modelNamePlaceholder', { vendor: p.name }) : t('modelConfig.modelNamePlaceholderDefault')
})

function openEdit(row: SystemModelView) {
  editType.value = row.modelType
  formProviderId.value = row.providerId || ''
  formModel.value = row.model || ''
  editVisible.value = true
}

async function onSubmit() {
  if (!formProviderId.value) { MateMessage.warning(t('modelConfig.providerRequired')); return }
  if (!formModel.value.trim()) { MateMessage.warning(t('modelConfig.modelNameRequired')); return }
  submitting.value = true
  try {
    emit('save', { type: editType.value, providerId: formProviderId.value, model: formModel.value.trim() })
    editVisible.value = false
  } finally {
    submitting.value = false
  }
}
</script>

<style src="./modality.css"></style>
<style scoped>
.smrow {
  display: flex; align-items: center; gap: 12px;
  padding: 11px 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 10px;
  background: var(--el-bg-color);
  margin-bottom: 8px;
}
.smt { width: 200px; flex: none; display: flex; align-items: center; gap: 8px; }
.smt-name { font-weight: 600; font-size: 13px; }
.msel {
  flex: 1; display: flex; align-items: center; gap: 9px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px; padding: 7px 10px;
  background: var(--el-fill-color-lighter); cursor: pointer; transition: 0.15s;
}
.msel:hover { border-color: var(--el-color-primary); }
.msel.unset { border-style: dashed; }
.mlogo {
  width: 20px; height: 20px; border-radius: 6px;
  display: grid; place-items: center;
  color: #fff; font-weight: 800; font-size: 10px; flex: none;
}
.mlogo-img { background: var(--mc-logo-plate); object-fit: contain; padding: 2px; box-shadow: inset 0 0 0 1px var(--el-border-color-lighter); }
.mnm { font-weight: 600; }
.mvia { font-size: 11px; color: var(--el-text-color-secondary); }
.mempty { color: var(--el-text-color-secondary); }
.mchev { margin-left: auto; color: var(--el-text-color-disabled); flex: none; }
.tip { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 4px; }
.tip.warn { color: var(--el-color-warning); }
.cap-legend {
  display: flex; align-items: center; flex-wrap: wrap; gap: 14px;
  margin-top: 12px; padding: 11px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px; background: var(--el-fill-color-lighter);
}
.cap-legend-tt { font-size: 12px; font-weight: 700; color: var(--el-text-color-regular); }
.cap-item { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; color: var(--el-text-color-secondary); }
.cap-ic { display: inline-grid; place-items: center; color: var(--el-text-color-regular); }
.cap-ic :deep(svg) { display: block; }
.cap-lb { line-height: 1; }
</style>
