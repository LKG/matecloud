<template>
  <span
    v-for="i in count"
    :key="i"
    class="mc-skel"
    :class="{ circle }"
    :style="blockStyle"
  ></span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

/**
 * MateSkeleton — shimmer placeholder block. Compose multiples for list/grid
 * loading states instead of a full {@code v-loading} overlay.
 *
 *   <MateSkeleton :width="'100%'" :height="118" :radius="0" />
 *   <MateSkeleton :width="'60%'" :height="10" />           // text line
 *   <MateSkeleton circle :width="32" :height="32" />        // avatar
 */
const props = withDefaults(defineProps<{
  width?: string | number
  height?: string | number
  radius?: string | number
  circle?: boolean
  /** Render N identical blocks (e.g. several text lines). */
  count?: number
}>(), {
  width: '100%',
  height: 12,
  radius: 6,
  circle: false,
  count: 1,
})

const px = (v: string | number) => (typeof v === 'number' ? `${v}px` : v)
const blockStyle = computed(() => ({
  width: px(props.width),
  height: px(props.height),
  borderRadius: props.circle ? '50%' : px(props.radius),
}))
</script>

<style scoped>
.mc-skel { display: block; background: linear-gradient(100deg, var(--el-fill-color, #eef1f5) 30%, var(--el-fill-color-lighter, #f7f9fb) 50%, var(--el-fill-color, #eef1f5) 70%); background-size: 200% 100%; animation: mc-skel-sh 1.2s infinite; }
.mc-skel.circle { flex: none; }
@keyframes mc-skel-sh { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
</style>
