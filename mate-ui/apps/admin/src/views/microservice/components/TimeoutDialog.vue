<template>
  <MateDialog
    v-model="open"
    :title="`编辑超时 · ${service?.service ?? ''}`"
    width="460px"
    :submitting="saving"
    confirm-text="保存并生效"
    @submit="save"
  >
    <MateForm
      ref="formRef"
      :schema="schema"
      :model-value="form"
      label-width="100px"
      @update:model-value="(v) => Object.assign(form, v)"
    />
    <div class="hint">启用后写入该服务的路由级 metadata,覆盖网关全局默认超时;保存即触发路由重建。</div>
  </MateDialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { MateDialog, MateForm, type FormSchema, MateMessage } from '@matecloud/ui'
import { gatewayApi, type ServiceView, type TimeoutRule } from '@matecloud/core'

const props = defineProps<{ modelValue: boolean; service: ServiceView | null }>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; saved: [] }>()

const open = computed({ get: () => props.modelValue, set: (v) => emit('update:modelValue', v) })

const schema: FormSchema[] = [
  { field: 'enabled', label: '启用覆盖', type: 'switch' },
  { field: 'connectTimeoutMs', label: '连接超时(ms)', type: 'number', props: { min: 100, max: 60000, step: 100 } },
  { field: 'responseTimeoutMs', label: '响应超时(ms)', type: 'number', props: { min: 100, max: 600000, step: 100 } },
]

const formRef = ref<InstanceType<typeof MateForm>>()
const saving = ref(false)
// reactive(而非 ref):配合 :model-value + Object.assign 原地改,保持引用稳定,
// 避免 MateForm reseed→emit→reseed 反馈环(OOM)。与已验证的 JobList 写法一致。
const form = reactive<TimeoutRule>({ service: '', enabled: false, connectTimeoutMs: 1000, responseTimeoutMs: 5000 })

watch(() => props.modelValue, (v) => {
  if (v && props.service) Object.assign(form, props.service.timeout, { service: props.service.service })
})

async function save() {
  if (!(await formRef.value?.validate())) return
  saving.value = true
  try {
    const res = await gatewayApi.saveTimeout({ ...form })
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
.hint { font-size: 12px; color: var(--mc-text-disabled); margin-top: 4px; }
</style>
