<template>
  <el-popover
    ref="popRef"
    :placement="placement"
    :width="width"
    trigger="click"
    popper-class="mate-inline-confirm"
  >
    <template #reference>
      <span class="mic-trigger"><slot /></span>
    </template>

    <div class="mic">
      <div class="mic-q">
        <svg v-if="variant === 'delete'" class="mic-ic del" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6" /></svg>
        <svg v-else-if="variant === 'warning'" class="mic-ic warn" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M10.3 3.7 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.7a2 2 0 0 0-3.4 0zM12 9v4M12 17h.01" /></svg>
        <svg v-else class="mic-ic info" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="9" /><path d="M12 16v-4M12 8h.01" /></svg>
        <span>{{ titleLabel }}</span>
      </div>
      <div class="mic-row">
        <button class="mic-btn cancel" @click="onCancel">{{ cancelLabel }}</button>
        <button class="mic-btn confirm" :class="variant" @click="onConfirm">{{ confirmLabel }}</button>
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { PopoverInstance } from 'element-plus'

type Placement =
  | 'top' | 'top-start' | 'top-end'
  | 'bottom' | 'bottom-start' | 'bottom-end'
  | 'left' | 'left-start' | 'left-end'
  | 'right' | 'right-start' | 'right-end'

/**
 * MateInlineConfirm — lightweight popover confirmation anchored to its trigger.
 * A non-blocking alternative to {@code ElMessageBox.confirm} for per-row /
 * per-card destructive actions. Wrap the trigger in the default slot:
 *
 *   <MateInlineConfirm title="删除该素材?" @confirm="remove(id)">
 *     <button>删除</button>
 *   </MateInlineConfirm>
 */
const props = withDefaults(defineProps<{
  title?: string
  confirmText?: string
  cancelText?: string
  variant?: 'delete' | 'warning' | 'info'
  placement?: Placement
  width?: number | string
}>(), {
  variant: 'delete',
  placement: 'top',
  width: 168,
})

const emit = defineEmits<{ (e: 'confirm'): void; (e: 'cancel'): void }>()

const { t } = useI18n()
const titleLabel = computed(() => props.title ?? t('uikit.confirmTitle'))
const confirmLabel = computed(() => props.confirmText ?? t('uikit.confirm'))
const cancelLabel = computed(() => props.cancelText ?? t('uikit.cancel'))

const popRef = ref<PopoverInstance>()
function onConfirm() { popRef.value?.hide(); emit('confirm') }
function onCancel() { popRef.value?.hide(); emit('cancel') }
</script>

<style scoped>
.mic-trigger { display: inline-flex; }
.mic { display: flex; flex-direction: column; gap: 10px; }
.mic-q { display: flex; align-items: center; gap: 7px; font-size: 13px; font-weight: 600; color: var(--el-text-color-primary); }
.mic-ic.del { color: var(--el-color-danger); }
.mic-ic.warn { color: var(--el-color-warning); }
.mic-ic.info { color: var(--el-color-primary); }
.mic-row { display: flex; gap: 8px; }
.mic-btn { flex: 1; border: 1px solid var(--el-border-color); background: #fff; border-radius: 7px; padding: 5px 8px; font-size: 12px; font-weight: 600; cursor: pointer; color: var(--el-text-color-regular); transition: .15s; }
.mic-btn.cancel:hover { background: var(--el-fill-color-light); }
.mic-btn.confirm { color: #fff; border: none; }
.mic-btn.confirm.delete { background: var(--el-color-danger); }
.mic-btn.confirm.warning { background: var(--el-color-warning); }
.mic-btn.confirm.info { background: var(--el-color-primary); }
.mic-btn.confirm:hover { filter: brightness(.94); }
</style>
