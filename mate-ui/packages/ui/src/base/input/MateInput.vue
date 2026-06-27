<template>
  <div :class="inputVariants({ size, destructive, disabled })">
    <span v-if="$slots.prefix" class="mc-field__affix mc-field__prefix"><slot name="prefix" /></span>
    <input
      ref="inputRef"
      class="mc-field__input"
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :readonly="readonly"
      @input="onInput"
      @change="onChange"
      @keyup.enter="emit('enter', String(modelValue ?? ''))"
    />
    <button
      v-if="clearable && !disabled && !readonly && String(modelValue ?? '').length"
      class="mc-field__clear"
      type="button"
      tabindex="-1"
      aria-label="clear"
      @click="onClear"
    >✕</button>
    <span v-if="$slots.suffix" class="mc-field__affix mc-field__suffix"><slot name="suffix" /></span>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { inputVariants, type InputVariantProps } from './inputVariants'

/**
 * MateInput — base atom (RFC-053 #1).
 *   <MateInput v-model="kw" placeholder="搜索…" clearable>
 *     <template #prefix>🔍</template>
 *   </MateInput>
 */
const props = withDefaults(defineProps<{
  modelValue?: string | number | null
  type?: string
  placeholder?: string
  size?: NonNullable<InputVariantProps['size']>
  destructive?: boolean
  disabled?: boolean
  readonly?: boolean
  clearable?: boolean
}>(), {
  modelValue: '',
  type: 'text',
  size: 'md',
  destructive: false,
  disabled: false,
  readonly: false,
  clearable: false,
})

const emit = defineEmits<{
  (e: 'update:modelValue', v: string): void
  (e: 'input', v: string): void
  (e: 'change', v: string): void
  (e: 'enter', v: string): void
  (e: 'clear'): void
}>()

const inputRef = ref<HTMLInputElement>()

function onInput(ev: Event) {
  const v = (ev.target as HTMLInputElement).value
  emit('update:modelValue', v)
  emit('input', v)
}
function onChange(ev: Event) {
  emit('change', (ev.target as HTMLInputElement).value)
}
function onClear() {
  emit('update:modelValue', '')
  emit('clear')
  inputRef.value?.focus()
}

defineExpose({ focus: () => inputRef.value?.focus() })
</script>
