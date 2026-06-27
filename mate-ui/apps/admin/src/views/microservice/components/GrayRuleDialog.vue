<template>
  <MateDialog
    v-model="open"
    :title="`编辑路由 · ${service?.service ?? ''}`"
    width="480px"
    :submitting="saving"
    confirm-text="保存并生效"
    @submit="save"
  >
    <div v-if="service" class="gray-form">
      <div class="form-label">路由模式</div>
      <el-radio-group v-model="form.mode" class="mb16">
        <el-radio-button value="OFF">轮询</el-radio-button>
        <el-radio-button value="HEADER">按请求头</el-radio-button>
        <el-radio-button value="WEIGHTED">按权重灰度</el-radio-button>
      </el-radio-group>

      <template v-if="form.mode === 'HEADER'">
        <div class="form-label">版本请求头</div>
        <el-input v-model="form.headerName" class="mb8" placeholder="X-Service-Version" />
        <div class="hint">仅命中 metadata.version 与该头完全相等的实例;无匹配则尝试降级兜底。</div>
      </template>

      <template v-if="form.mode === 'WEIGHTED'">
        <el-alert
          v-if="(service.versions?.length || 0) < 2"
          type="warning"
          :closable="false"
          show-icon
          title="该服务仅 1 个版本,权重灰度无实际分流意义,建议多版本部署后再启用"
          class="mb12"
        />
        <div class="form-label">灰度权重</div>
        <div v-for="ver in service.versions || []" :key="ver.version" class="weight-row">
          <span class="weight-ver mc-mono">{{ ver.version }}</span>
          <el-slider v-model="form.weights[ver.version]" :max="100" :step="5" style="flex: 1" />
          <span class="weight-val">{{ form.weights[ver.version] || 0 }}%</span>
        </div>
        <DistributionBar :segments="dialogSegments" class="mt8" />
        <div class="hint">{{ weightHint }}</div>
      </template>

      <div class="form-label mt8">降级兜底版本</div>
      <el-select v-model="form.fallbackVersion" clearable placeholder="无" style="width: 100%" class="mb8">
        <el-option v-for="ver in service.versions || []" :key="ver.version" :label="ver.version" :value="ver.version" />
      </el-select>
      <div class="hint">目标版本无存活实例时自动降级,避免 503。</div>
    </div>
  </MateDialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { MateDialog, MateMessage } from '@matecloud/ui'
import { gatewayApi, type ServiceView, type GrayRule } from '@matecloud/core'
import DistributionBar from './DistributionBar.vue'

const props = defineProps<{ modelValue: boolean; service: ServiceView | null }>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; saved: [] }>()

const open = computed({ get: () => props.modelValue, set: (v) => emit('update:modelValue', v) })

const saving = ref(false)
const form = ref<GrayRule>({ service: '', mode: 'OFF', headerName: 'X-Service-Version', weights: {}, fallbackVersion: '', enabled: false })

// 打开时按所选服务回填表单(各版本权重缺省补 0)。
watch(() => props.modelValue, (v) => {
  if (v && props.service) {
    const s = props.service
    const weights: Record<string, number> = {}
    ;(s.versions || []).forEach((ver) => { weights[ver.version] = (s.gray.weights || {})[ver.version] ?? 0 })
    form.value = {
      service: s.service,
      mode: s.gray.mode,
      headerName: s.gray.headerName || 'X-Service-Version',
      weights,
      fallbackVersion: s.gray.fallbackVersion || '',
      enabled: s.gray.mode !== 'OFF',
    }
  }
})

const dialogSegments = computed(() =>
  (props.service?.versions ?? []).map((v) => ({ label: v.version, value: form.value.weights[v.version] || 0 })),
)
const weightHint = computed(() => {
  const vers = props.service?.versions ?? []
  const sum = vers.reduce((a, v) => a + (form.value.weights[v.version] || 0), 0) || 1
  return '每 100 个请求 → ' + vers.map((v) => `${v.version} ${Math.round((form.value.weights[v.version] || 0) / sum * 100)} 个`).join('，')
})

async function save() {
  saving.value = true
  try {
    const rule: GrayRule = { ...form.value, enabled: form.value.mode !== 'OFF' }
    const res = await gatewayApi.saveGray(rule)
    if (res.success) {
      MateMessage.success('已保存并生效')
      open.value = false
      emit('saved')
    } else {
      MateMessage.error(res.msg || '保存失败')
    }
  } catch (e: any) {
    MateMessage.error(e?.msg || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.form-label { font-size: 12px; color: var(--mc-text-muted); margin-bottom: 6px; }
.mb16 { margin-bottom: 16px; }
.mb12 { margin-bottom: 12px; }
.mb8 { margin-bottom: 8px; }
.mt8 { margin-top: 8px; }
.hint { font-size: 12px; color: var(--mc-text-disabled); margin-bottom: 8px; }
.weight-row { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.weight-ver { width: 64px; font-size: 12px; }
.weight-val { width: 42px; text-align: right; font-size: 13px; font-weight: 600; }
.mc-mono { font-family: 'SF Mono', 'JetBrains Mono', monospace; }
</style>
