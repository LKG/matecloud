<template>
  <MateDialog
    v-model="open"
    :title="`编辑限流 · ${service?.service ?? ''}`"
    width="460px"
    :submitting="saving"
    confirm-text="保存并生效"
    @submit="save"
  >
    <MateForm
      ref="formRef"
      :schema="schema"
      :model-value="form"
      label-width="92px"
      @update:model-value="(v) => Object.assign(form, v)"
    />
    <div class="hint">每 {{ form.interval }} 秒最多放行 {{ form.rate }} 个请求,超出返回 429。{{ keyHint }}</div>
  </MateDialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { MateDialog, MateForm, type FormSchema, MateMessage } from '@matecloud/ui'
import { gatewayApi, type ServiceView, type RateLimitRule, type RateLimitKeyType } from '@matecloud/core'

const props = defineProps<{ modelValue: boolean; service: ServiceView | null }>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; saved: [] }>()

const open = computed({ get: () => props.modelValue, set: (v) => emit('update:modelValue', v) })

const KEY_HINTS: Record<RateLimitKeyType, string> = {
  IP: '按客户端 IP 计数。',
  USER: '按登录用户计数(取 token,缺失回退 IP)。',
  PATH: '按请求路径计数。',
  SERVICE: '整服务一个总闸。',
}

const schema: FormSchema[] = [
  { field: 'enabled', label: '启用限流', type: 'switch' },
  { field: 'key', label: '限流维度', type: 'radio', options: [
    { label: '按 IP', value: 'IP' },
    { label: '按用户', value: 'USER' },
    { label: '按路径', value: 'PATH' },
    { label: '按服务', value: 'SERVICE' },
  ] },
  { field: 'rate', label: '速率(令牌)', type: 'number', props: { min: 1, max: 100000 } },
  { field: 'interval', label: '时间窗(秒)', type: 'number', props: { min: 1, max: 3600 } },
]

const formRef = ref<InstanceType<typeof MateForm>>()
const saving = ref(false)
// reactive(而非 ref)::model-value 传稳定引用、@update:model-value 里 Object.assign 原地改,
// 避免 MateForm 的 props.modelValue 引用变更触发 reseed→emit→reseed 反馈环(会撑爆内存)。
// 与已验证的 JobList 写法一致。
const form = reactive<RateLimitRule>({ service: '', enabled: false, key: 'IP', rate: 100, interval: 1 })

const keyHint = computed(() => KEY_HINTS[form.key])

// 打开时原地回填当前服务的规则;MateDialog destroy-on-close 会重挂 MateForm 据 form 重新 seed。
watch(() => props.modelValue, (v) => {
  if (v && props.service) Object.assign(form, props.service.rateLimit, { service: props.service.service })
})

async function save() {
  if (!(await formRef.value?.validate())) return
  saving.value = true
  try {
    const res = await gatewayApi.saveRateLimit({ ...form })
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
