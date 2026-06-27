<template>
  <span :class="tagVariants({ variant, size })">
    <span v-if="dot" class="mc-tag__dot" :class="{ 'mc-tag__dot--pulse': pulse }" aria-hidden="true" />
    <slot>{{ label }}</slot>
  </span>
</template>

<script setup lang="ts">
import { tagVariants, type TagVariantProps } from './tagVariants'

/**
 * MateTag — base atom (RFC-053 #1). Pill label with semantic color variants.
 *   <MateTag variant="success" dot>启用</MateTag>
 *   <MateTag variant="warning" dot pulse>未保存</MateTag>   <!-- 闪烁圆点提醒 -->
 */
withDefaults(defineProps<{
  variant?: NonNullable<TagVariantProps['variant']>
  size?: NonNullable<TagVariantProps['size']>
  label?: string
  /** 左侧圆点 (与文字同色). */
  dot?: boolean
  /** 圆点闪烁动画 (用于状态吸睛, 如 "未保存"). 仅在 dot=true 时生效. */
  pulse?: boolean
}>(), {
  variant: 'default',
  size: 'md',
  dot: false,
  pulse: false,
})
</script>

<style scoped>
.mc-tag__dot--pulse { animation: mc-tag-pulse 1.4s ease-in-out infinite; }
@keyframes mc-tag-pulse {
  0%, 100% { opacity: 1; }
  50%      { opacity: 0.35; }
}
</style>
