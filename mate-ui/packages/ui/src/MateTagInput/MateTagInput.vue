<template>
  <div class="mti" :class="{ disabled }" @click="focusInput">
    <span v-for="(tag, i) in modelValue" :key="`${tag}-${i}`" class="mti-tag">
      {{ tag }}
      <i v-if="!disabled" class="mti-x" @click.stop="removeAt(i)">
        <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M18 6 6 18M6 6l12 12" /></svg>
      </i>
    </span>
    <input
      ref="inputRef"
      v-model="draft"
      class="mti-input"
      :placeholder="modelValue.length ? '' : ph"
      :disabled="disabled || (max > 0 && modelValue.length >= max)"
      @keydown.enter.prevent="commit"
      @keydown.delete="onBackspace"
      @blur="commit"
      @paste="onPaste"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'

/**
 * MateTagInput — chip-style multi-value text input (v-model: string[]).
 * Enter or a separator commits a tag; Backspace on an empty draft removes the
 * last; paste splits on the separator. Use for material tags / keyword search.
 * The placeholder is bilingual by default.
 */
const props = withDefaults(defineProps<{
  modelValue: string[]
  placeholder?: string
  /** Max tags (0 = unlimited). */
  max?: number
  /** Characters that also commit a tag when typed/pasted. */
  separator?: string
  disabled?: boolean
  /** Reject duplicate tags. */
  unique?: boolean
}>(), {
  max: 0,
  separator: ',',
  disabled: false,
  unique: true,
})

const emit = defineEmits<{ (e: 'update:modelValue', v: string[]): void }>()

const { t } = useI18n()
const ph = computed(() => props.placeholder ?? t('uikit.tagPlaceholder'))
const draft = ref('')
const inputRef = ref<HTMLInputElement>()

function focusInput() { inputRef.value?.focus() }

function add(raw: string) {
  const v = raw.trim()
  if (!v) return
  if (props.max > 0 && props.modelValue.length >= props.max) return
  if (props.unique && props.modelValue.includes(v)) return
  emit('update:modelValue', [...props.modelValue, v])
}

function commit() {
  if (!draft.value.trim()) return
  add(draft.value)
  draft.value = ''
}

function removeAt(i: number) {
  const next = props.modelValue.slice()
  next.splice(i, 1)
  emit('update:modelValue', next)
}

function onBackspace() {
  if (draft.value === '' && props.modelValue.length) removeAt(props.modelValue.length - 1)
}

function onPaste(e: ClipboardEvent) {
  const text = e.clipboardData?.getData('text') ?? ''
  if (props.separator && text.includes(props.separator)) {
    e.preventDefault()
    text.split(props.separator).forEach(add)
    draft.value = ''
  }
}
</script>

<style scoped>
.mti { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; min-height: 40px; padding: 5px 8px; border: 1px solid var(--el-border-color); border-radius: 8px; background: #fff; cursor: text; transition: border-color .15s, box-shadow .15s; }
.mti:focus-within { border-color: var(--el-color-primary); box-shadow: 0 0 0 3px var(--el-color-primary-light-9); }
.mti.disabled { background: var(--el-fill-color-light); cursor: not-allowed; }
.mti-tag { display: inline-flex; align-items: center; gap: 5px; background: var(--el-color-primary-light-9); color: var(--el-color-primary); border: 1px solid var(--el-color-primary-light-7); border-radius: 6px; padding: 3px 8px; font-size: 12px; font-weight: 600; line-height: 1.4; }
.mti-x { display: inline-flex; cursor: pointer; opacity: .7; }
.mti-x:hover { opacity: 1; }
.mti-input { flex: 1; min-width: 90px; border: 0; outline: 0; background: transparent; font-size: 13px; color: var(--el-text-color-primary); }
</style>
