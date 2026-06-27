<template>
  <MateTooltip :content="copied ? copiedLabel : copyLabel" placement="top" :show-after="200">
    <button type="button" class="mcb" :class="{ copied }" :aria-label="copyLabel" @click.stop="onCopy">
      <svg v-if="!copied" viewBox="0 0 24 24" :width="size" :height="size" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="9" y="9" width="13" height="13" rx="2" /><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" /></svg>
      <svg v-else viewBox="0 0 24 24" :width="size" :height="size" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 6 9 17l-5-5" /></svg>
    </button>
  </MateTooltip>
</template>

<script setup lang="ts">
import MateTooltip from '../MateTooltip/MateTooltip.vue'
import { computed, onBeforeUnmount, ref } from 'vue'
import { useI18n } from 'vue-i18n'

/**
 * MateCopyButton — copy-to-clipboard icon button with inline ✓ feedback.
 * The icon swaps to a check for {@code timeout} ms after a successful copy and
 * the tooltip flips to "copied", giving clearer feedback than a transient toast.
 * Labels are bilingual by default (uikit.copy / uikit.copied).
 */
const props = withDefaults(defineProps<{
  content: string
  copyText?: string
  copiedText?: string
  size?: number
  timeout?: number
}>(), {
  size: 16,
  timeout: 2000,
})

const emit = defineEmits<{ (e: 'copied'): void }>()

const { t } = useI18n()
const copyLabel = computed(() => props.copyText ?? t('uikit.copy'))
const copiedLabel = computed(() => props.copiedText ?? t('uikit.copied'))

const copied = ref(false)
let timer: ReturnType<typeof setTimeout> | null = null

async function onCopy() {
  try {
    await navigator.clipboard.writeText(props.content)
  } catch {
    // Fallback for non-secure contexts / older browsers
    const ta = document.createElement('textarea')
    ta.value = props.content
    ta.style.position = 'fixed'
    ta.style.opacity = '0'
    document.body.appendChild(ta)
    ta.select()
    try { document.execCommand('copy') } catch { /* ignore */ }
    document.body.removeChild(ta)
  }
  copied.value = true
  emit('copied')
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => { copied.value = false }, props.timeout)
}

onBeforeUnmount(() => { if (timer) clearTimeout(timer) })
</script>

<style scoped>
.mcb { display: inline-grid; place-items: center; width: 30px; height: 30px; border: 1px solid var(--el-border-color); background: #fff; border-radius: 7px; cursor: pointer; color: var(--el-text-color-regular); transition: color .15s, background .15s, border-color .15s; }
.mcb:hover { color: var(--el-color-primary); background: var(--el-fill-color-light); }
.mcb.copied { color: var(--el-color-success); border-color: var(--el-color-success); }
</style>
