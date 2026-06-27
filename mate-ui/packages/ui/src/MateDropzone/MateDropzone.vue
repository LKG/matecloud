<template>
  <div
    class="mdz"
    :class="{ drag: dragOver, disabled }"
    @click="browse"
    @dragover.prevent="onDragOver"
    @dragleave.prevent="dragOver = false"
    @drop.prevent="onDrop"
  >
    <slot>
      <div class="mdz-ic">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" /><path d="M17 8l-5-5-5 5" /><path d="M12 3v12" /></svg>
      </div>
      <div class="mdz-main">
        {{ textLabel }} <b class="mdz-link">{{ browseLabel }}</b>
      </div>
      <div v-if="hint" class="mdz-hint">{{ hint }}</div>
    </slot>

    <input
      ref="inputRef"
      type="file"
      class="mdz-input"
      :accept="accept"
      :multiple="multiple"
      @change="onPick"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'

/**
 * MateDropzone — drag-and-drop / click-to-browse file picker with client-side
 * type + size validation. Emits validated File[] via {@code files}; rejects
 * (with a reason key) via {@code reject}. Reusable for material upload,
 * attachments, avatars, import, etc. The prompt text is bilingual by default.
 */
const props = withDefaults(defineProps<{
  accept?: string
  multiple?: boolean
  /** Per-file size limit in MB (0 = no limit). */
  maxSizeMB?: number
  text?: string
  browseText?: string
  hint?: string
  disabled?: boolean
}>(), {
  accept: '',
  multiple: true,
  maxSizeMB: 0,
  hint: '',
  disabled: false,
})

const { t } = useI18n()
const textLabel = computed(() => props.text ?? t('uikit.dropText'))
const browseLabel = computed(() => props.browseText ?? t('uikit.browse'))

const emit = defineEmits<{
  (e: 'files', files: File[]): void
  (e: 'reject', payload: { reason: 'type' | 'size'; file: File }): void
}>()

const inputRef = ref<HTMLInputElement>()
const dragOver = ref(false)

function browse() { if (!props.disabled) inputRef.value?.click() }
function onDragOver() { if (!props.disabled) dragOver.value = true }

function matchesAccept(file: File): boolean {
  if (!props.accept) return true
  const tokens = props.accept.split(',').map((s) => s.trim().toLowerCase()).filter(Boolean)
  const name = file.name.toLowerCase()
  const mime = file.type.toLowerCase()
  return tokens.some((tok) => {
    if (tok.startsWith('.')) return name.endsWith(tok)
    if (tok.endsWith('/*')) return mime.startsWith(tok.slice(0, -1))
    return mime === tok
  })
}

function accept(list: FileList | null) {
  if (!list || props.disabled) return
  const ok: File[] = []
  for (const file of Array.from(list)) {
    if (!matchesAccept(file)) { emit('reject', { reason: 'type', file }); continue }
    if (props.maxSizeMB > 0 && file.size > props.maxSizeMB * 1024 * 1024) { emit('reject', { reason: 'size', file }); continue }
    ok.push(file)
  }
  if (ok.length) emit('files', ok)
}

function onPick(e: Event) {
  accept((e.target as HTMLInputElement).files)
  ;(e.target as HTMLInputElement).value = ''
}
function onDrop(e: DragEvent) {
  dragOver.value = false
  accept(e.dataTransfer?.files ?? null)
}
</script>

<style scoped>
.mdz { position: relative; width: 100%; box-sizing: border-box; border: 1.5px dashed var(--el-border-color); border-radius: 12px; background: var(--el-fill-color-blank, #fafbfc); padding: 28px 20px; text-align: center; color: var(--el-text-color-secondary); cursor: pointer; transition: border-color .15s, background .15s, color .15s; }
.mdz:hover { border-color: var(--el-color-primary); }
.mdz.drag { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); color: var(--el-color-primary); }
.mdz.disabled { cursor: not-allowed; opacity: .6; }
.mdz-ic { width: 46px; height: 46px; border-radius: 12px; background: #fff; border: 1px solid var(--el-border-color); display: grid; place-items: center; margin: 0 auto 10px; color: var(--el-color-primary); }
.mdz-main { font-size: 14px; }
.mdz-link { color: var(--el-color-primary); }
.mdz-hint { font-size: 12px; color: var(--el-text-color-disabled); margin-top: 5px; }
.mdz-input { display: none; }
</style>
