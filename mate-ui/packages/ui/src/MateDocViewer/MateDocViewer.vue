<template>
  <Teleport to="body">
    <transition name="dv-fade">
      <div v-if="modelValue" class="dv-mask" @click.self="close">
        <div class="dv-panel" @click.stop>
          <!-- 头部 -->
          <header class="dv-hd">
            <span class="dv-name" :title="name">{{ name }}</span>
            <div class="dv-tools">
              <button class="dv-btn" :title="L.download" @click="download">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" /><path d="M7 10l5 5 5-5" /><path d="M12 15V3" /></svg>
              </button>
              <button class="dv-btn" :title="L.openNewTab" @click="openNewTab">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M15 3h6v6M10 14 21 3M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" /></svg>
              </button>
              <button class="dv-btn close" :title="L.close" @click="close">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6 6 18M6 6l12 12" /></svg>
              </button>
            </div>
          </header>

          <!-- 正文 -->
          <div class="dv-body">
            <!-- PDF:浏览器原生渲染 -->
            <iframe v-if="effectiveKind === 'pdf'" :src="url" class="dv-frame" :title="name"></iframe>

            <!-- 图片 -->
            <div v-else-if="effectiveKind === 'image'" class="dv-img-wrap">
              <img :src="url" :alt="name" class="dv-img" />
            </div>

            <!-- 文本 / 代码 -->
            <div v-else-if="effectiveKind === 'text'" class="dv-text-wrap">
              <div v-if="loading" class="dv-state">{{ L.loading }}</div>
              <pre v-else class="dv-text">{{ text }}</pre>
            </div>

            <!-- 兜底:无法在线预览 -->
            <div v-else class="dv-fallback">
              <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.3"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><path d="M14 2v6h6" /></svg>
              <div class="dv-fb-name">{{ name }}</div>
              <div class="dv-fb-tip">{{ L.unsupported }}</div>
              <div class="dv-fb-actions">
                <button class="dv-action primary" @click="download">{{ L.download }}</button>
                <button class="dv-action" @click="openNewTab">{{ L.openNewTab }}</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

/**
 * MateDocViewer — in-app document preview.
 * <p>
 * PDFs render in a native {@code <iframe>} (no extra dependency); text-like
 * files (txt / md / csv / json / log / code) are fetched and shown inline;
 * images render directly; everything else (Office, archives, unknown) falls
 * back to a download / open-in-new-tab panel — mirroring the common approach
 * of previewing only what the browser can render natively.
 */
const TEXT_EXTS = ['txt', 'md', 'markdown', 'csv', 'tsv', 'json', 'log', 'xml', 'yml', 'yaml', 'ini', 'conf', 'properties', 'sql', 'js', 'ts', 'css', 'html', 'java', 'py', 'go', 'sh', 'bat']
const IMAGE_EXTS = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg']
const MAX_TEXT_BYTES = 512 * 1024 // 512KB cap for inline text

const props = withDefaults(defineProps<{
  modelValue: boolean
  url: string
  name: string
  /** Optional explicit extension; otherwise derived from {@code name}. */
  ext?: string
  closeText?: string
  downloadText?: string
  openNewTabText?: string
  unsupportedText?: string
  loadingText?: string
}>(), {})

const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const { t } = useI18n()
const L = computed(() => ({
  close: props.closeText ?? t('uikit.close'),
  download: props.downloadText ?? t('uikit.download'),
  openNewTab: props.openNewTabText ?? t('uikit.openNewTab'),
  unsupported: props.unsupportedText ?? t('uikit.docUnsupported'),
  loading: props.loadingText ?? t('uikit.loading'),
}))

const text = ref('')
const loading = ref(false)

const ext = computed(() => {
  if (props.ext) return props.ext.toLowerCase()
  const i = props.name.lastIndexOf('.')
  return i >= 0 ? props.name.slice(i + 1).toLowerCase() : ''
})

const kind = computed<'pdf' | 'image' | 'text' | 'other'>(() => {
  const e = ext.value
  if (e === 'pdf') return 'pdf'
  if (IMAGE_EXTS.includes(e)) return 'image'
  if (TEXT_EXTS.includes(e)) return 'text'
  return 'other'
})

async function loadText() {
  loading.value = true
  text.value = ''
  try {
    const res = await fetch(props.url)
    if (!res.ok) throw new Error(String(res.status))
    const blob = await res.blob()
    const sliced = blob.size > MAX_TEXT_BYTES ? blob.slice(0, MAX_TEXT_BYTES) : blob
    text.value = await sliced.text()
    if (blob.size > MAX_TEXT_BYTES) text.value += '\n\n… …'
  } catch {
    // CORS / network failure — degrade to the download fallback
    text.value = ''
    fallbackToOther.value = true
  } finally {
    loading.value = false
  }
}

// when text fetch fails we flip to the fallback panel
const fallbackToOther = ref(false)
const effectiveKind = computed(() => (fallbackToOther.value ? 'other' : kind.value))

function close() { emit('update:modelValue', false) }
function download() {
  const a = document.createElement('a')
  a.href = props.url
  a.download = props.name || 'file'
  a.target = '_blank'
  a.rel = 'noopener'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}
function openNewTab() { window.open(props.url, '_blank', 'noopener') }

function onKey(e: KeyboardEvent) { if (props.modelValue && e.key === 'Escape') close() }

watch(() => props.modelValue, (open) => {
  if (open) {
    fallbackToOther.value = false
    document.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    if (kind.value === 'text') loadText()
  } else {
    document.removeEventListener('keydown', onKey)
    document.body.style.overflow = ''
  }
})
</script>

<script lang="ts">
export default { name: 'MateDocViewer' }
</script>

<style scoped>
.dv-mask { position: fixed; inset: 0; z-index: 3000; background: rgba(0, 0, 0, .55); display: flex; align-items: center; justify-content: center; padding: 4vh 4vw; }
.dv-panel { width: min(960px, 96vw); height: min(88vh, 100%); background: var(--el-bg-color, #fff); border-radius: 12px; box-shadow: 0 20px 60px rgba(0, 0, 0, .35); display: flex; flex-direction: column; overflow: hidden; }
.dv-hd { display: flex; align-items: center; gap: 12px; padding: 12px 16px; border-bottom: 1px solid var(--el-border-color); }
.dv-name { flex: 1; font-weight: 600; font-size: 14px; color: var(--el-text-color-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dv-tools { display: flex; gap: 4px; flex: none; }
.dv-btn { width: 32px; height: 32px; border: none; border-radius: 8px; background: transparent; color: var(--el-text-color-regular); display: grid; place-items: center; cursor: pointer; transition: background .15s, color .15s; }
.dv-btn:hover { background: var(--el-fill-color); color: var(--el-color-primary); }
.dv-btn.close:hover { color: var(--el-color-danger); }
.dv-body { flex: 1; min-height: 0; background: var(--el-fill-color-lighter); }
.dv-frame { width: 100%; height: 100%; border: 0; }
.dv-img-wrap { width: 100%; height: 100%; display: grid; place-items: center; overflow: auto; padding: 16px; }
.dv-img { max-width: 100%; max-height: 100%; object-fit: contain; }
.dv-text-wrap { width: 100%; height: 100%; overflow: auto; }
.dv-text { margin: 0; padding: 16px 20px; font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 12.5px; line-height: 1.6; color: var(--el-text-color-primary); white-space: pre-wrap; word-break: break-word; }
.dv-state { padding: 40px; text-align: center; color: var(--el-text-color-secondary); }
.dv-fallback { height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px; color: var(--el-text-color-secondary); }
.dv-fb-name { font-weight: 600; color: var(--el-text-color-primary); }
.dv-fb-tip { font-size: 13px; color: var(--el-text-color-disabled); }
.dv-fb-actions { display: flex; gap: 10px; margin-top: 8px; }
.dv-action { border: 1px solid var(--el-border-color); background: var(--el-bg-color); border-radius: 8px; padding: 7px 16px; font-size: 13px; font-weight: 600; cursor: pointer; color: var(--el-text-color-regular); }
.dv-action:hover { background: var(--el-fill-color); }
.dv-action.primary { background: var(--el-color-primary); border-color: var(--el-color-primary); color: #fff; }
.dv-action.primary:hover { filter: brightness(.95); }

.dv-fade-enter-active, .dv-fade-leave-active { transition: opacity .18s ease; }
.dv-fade-enter-from, .dv-fade-leave-to { opacity: 0; }
</style>
