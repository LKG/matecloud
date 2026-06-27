<template>
  <button
    :type="nativeType"
    :class="buttonVariants({ variant, size, block })"
    :disabled="disabled || loading"
    @click="onClick"
  >
    <span v-if="loading" class="mc-btn__spinner" aria-hidden="true" />
    <slot v-else name="icon" />
    <span v-if="$slots.default" class="mc-btn__label"><slot /></span>
  </button>
</template>

<script setup lang="ts">
import { buttonVariants, type ButtonVariantProps } from './buttonVariants'

/**
 * MateButton — base atom (RFC-053 #1).
 *
 * Token-styled, framework-light button. Variants are declared in
 * `buttonVariants` (CVA). Existing `<el-button>` usage is untouched — this
 * is an additive base atom, not a replacement.
 *
 *   <MateButton variant="ghost" size="sm" @click="...">取消</MateButton>
 *   <MateButton :loading="saving">保存</MateButton>
 */
const props = withDefaults(defineProps<{
  variant?: NonNullable<ButtonVariantProps['variant']>
  size?: NonNullable<ButtonVariantProps['size']>
  block?: boolean
  disabled?: boolean
  loading?: boolean
  nativeType?: 'button' | 'submit' | 'reset'
}>(), {
  variant: 'primary',
  size: 'md',
  block: false,
  disabled: false,
  loading: false,
  nativeType: 'button',
})

const emit = defineEmits<{ (e: 'click', ev: MouseEvent): void }>()

function onClick(ev: MouseEvent) {
  if (props.disabled || props.loading) return
  emit('click', ev)
}
</script>
