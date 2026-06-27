<script setup lang="ts">
import { computed } from 'vue'
import MateEmpty from '../MateEmpty/MateEmpty.vue'
import MateSkeleton from '../MateSkeleton/MateSkeleton.vue'

/**
 * 异步状态声明式容器 —— 配合 `useAsync` 把 加载/出错/空/有数据 四态统一渲染,
 * 替代每个视图各自手写的 v-if loading / v-else error / v-else empty 分支。
 *
 * 加载 → 骨架屏; 出错 → 错误态 + 重试按钮; 空 → MateEmpty; 成功 → 默认插槽。
 * 首次加载才显示骨架; 已有数据的刷新(re-fetch)保持旧内容不闪屏 (keepContentOnRefresh)。
 */
type AsyncState = 'idle' | 'loading' | 'error' | 'success'

const props = withDefaults(defineProps<{
  state: AsyncState
  /** 数据是否为空 (由调用方判定: !list.length 等)。 */
  empty?: boolean
  /** 空态文案。 */
  emptyText?: string
  /** 骨架行数。 */
  skeletonRows?: number
  /** 已有内容时刷新不显示骨架 (默认 true)。 */
  keepContentOnRefresh?: boolean
  /** 出错时的可读消息 (默认通用文案)。 */
  errorText?: string
}>(), {
  empty: false,
  emptyText: '暂无数据',
  skeletonRows: 4,
  keepContentOnRefresh: true,
  errorText: '加载失败',
})

const emit = defineEmits<{ (e: 'retry'): void }>()

/** 是否已有可展示内容 (用于刷新不闪屏判定)。 */
const hasContent = computed(() => props.state === 'success' && !props.empty)

const showSkeleton = computed(() =>
  props.state === 'loading' && !(props.keepContentOnRefresh && hasContent.value))
const showError = computed(() => props.state === 'error')
const showEmpty = computed(() => props.state === 'success' && props.empty)
const showContent = computed(() =>
  hasContent.value || (props.state === 'loading' && props.keepContentOnRefresh && hasContent.value))
</script>

<template>
  <div class="mc-async">
    <div v-if="showSkeleton" class="mc-async__skeleton">
      <MateSkeleton :count="skeletonRows" height="16" style="margin-bottom: 12px" />
    </div>

    <div v-else-if="showError" class="mc-async__error">
      <slot name="error" :retry="() => emit('retry')">
        <div class="mc-async__error-icon">!</div>
        <p class="mc-async__error-text">{{ errorText }}</p>
        <button type="button" class="mc-async__retry" @click="emit('retry')">重试</button>
      </slot>
    </div>

    <slot v-else-if="showEmpty" name="empty">
      <MateEmpty :text="emptyText" />
    </slot>

    <slot v-else-if="showContent || state === 'success'" />
  </div>
</template>

<style scoped>
.mc-async__skeleton { padding: 8px 0; }
.mc-async__error {
  display: flex; flex-direction: column; align-items: center;
  gap: 10px; padding: 48px 0; color: var(--mc-text-muted, var(--el-text-color-secondary));
}
.mc-async__error-icon {
  width: 36px; height: 36px; border-radius: 50%;
  display: grid; place-items: center; font-weight: 700; font-size: 18px;
  background: var(--el-color-danger-light-9); color: var(--el-color-danger);
}
.mc-async__error-text { margin: 0; font-size: 13px; }
.mc-async__retry {
  border: 1px solid var(--el-border-color); background: var(--el-bg-color);
  color: var(--el-text-color-primary); border-radius: 6px;
  padding: 4px 16px; font-size: 13px; cursor: pointer; transition: border-color .15s, color .15s;
}
.mc-async__retry:hover { border-color: var(--el-color-primary); color: var(--el-color-primary); }
</style>
