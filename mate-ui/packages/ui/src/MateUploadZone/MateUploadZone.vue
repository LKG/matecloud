<template>
  <div class="muz">
    <MateDropzone
      :accept="accept" :multiple="multiple" :max-size-m-b="maxSizeMB"
      :text="text" :hint="hint" :disabled="disabled"
      @files="enqueue"
      @reject="onReject" />

    <TransitionGroup v-if="items.length" name="muz-list" tag="div" class="muz-items">
      <div v-for="it in items" :key="it.id" class="muz-item" :class="`is-${it.status}`">
        <div class="muz-item-ic" :class="extClass(it.file.name)">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><path d="M14 2v6h6" />
          </svg>
        </div>
        <div class="muz-item-main">
          <div class="muz-item-row">
            <span class="muz-item-name" :title="it.file.name">{{ it.file.name }}</span>
            <span class="muz-item-size">{{ formatSize(it.file.size) }}</span>
          </div>
          <div class="muz-item-bar">
            <div class="muz-item-fill" :style="{ width: barWidth(it) }" />
          </div>
          <div v-if="it.message" class="muz-item-msg">{{ it.message }}</div>
        </div>
        <div class="muz-item-ops">
          <span v-if="it.status === 'uploading'" class="muz-spin" />
          <button v-else-if="it.status === 'error'" class="muz-btn" :title="retryLabel" @click="retry(it)">
            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12a9 9 0 1 1-3-6.7" /><path d="M21 3v6h-6" /></svg>
          </button>
          <span v-else class="muz-ok">✓</span>
          <button class="muz-btn" :title="removeLabel" @click="remove(it)">
            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6 6 18" /><path d="m6 6 12 12" /></svg>
          </button>
        </div>
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import MateDropzone from '../MateDropzone/MateDropzone.vue'

/**
 * MateUploadZone — Upload composite: MateDropzone picker on top, a
 * per-file queue below (type icon, name, size, progress bar, status, retry /
 * remove). The host passes {@code uploader(file)}; this component owns queue
 * state and sequences uploads, so every page gets identical upload UX without
 * re-implementing list rendering.
 */
export interface UploadItem {
  id: string
  file: File
  status: 'uploading' | 'success' | 'error'
  message?: string
}

const props = withDefaults(defineProps<{
  /** Performs one upload; resolve = success, reject/throw = error (message shown). */
  uploader: (file: File) => Promise<unknown>
  accept?: string
  multiple?: boolean
  maxSizeMB?: number
  text?: string
  hint?: string
  disabled?: boolean
  /** Auto-remove successful items after N ms (0 = keep). */
  successTtlMs?: number
  retryLabel?: string
  removeLabel?: string
}>(), {
  accept: '',
  multiple: true,
  maxSizeMB: 0,
  hint: '',
  disabled: false,
  successTtlMs: 4000,
  retryLabel: 'Retry',
  removeLabel: 'Remove',
})

const emit = defineEmits<{
  (e: 'done', item: UploadItem): void
  (e: 'error', item: UploadItem): void
  (e: 'reject', payload: { reason: 'type' | 'size'; file: File }): void
}>()

const items = ref<UploadItem[]>([])
let seq = 0

function enqueue(files: File[]) {
  for (const file of files) {
    const item: UploadItem = { id: `u${++seq}-${Date.now()}`, file, status: 'uploading' }
    items.value.push(item)
    void run(item)
  }
}

async function run(item: UploadItem) {
  item.status = 'uploading'
  item.message = undefined
  try {
    await props.uploader(item.file)
    item.status = 'success'
    emit('done', item)
    if (props.successTtlMs > 0) {
      setTimeout(() => remove(item), props.successTtlMs)
    }
  } catch (e: any) {
    item.status = 'error'
    item.message = e?.message || 'Upload failed'
    emit('error', item)
  }
}

function retry(item: UploadItem) { void run(item) }

function remove(item: UploadItem) {
  items.value = items.value.filter(i => i.id !== item.id)
}

function onReject(payload: { reason: 'type' | 'size'; file: File }) {
  emit('reject', payload)
}

function barWidth(it: UploadItem): string {
  return it.status === 'uploading' ? '60%' : '100%'
}

/** 按扩展名给文件图标着色 (pdf 红 / word 蓝 / 表格 绿 / 文本 灰)。 */
function extClass(name: string): string {
  const ext = name.slice(name.lastIndexOf('.') + 1).toLowerCase()
  if (ext === 'pdf') return 'ext-pdf'
  if (ext === 'doc' || ext === 'docx') return 'ext-doc'
  if (ext === 'xls' || ext === 'xlsx' || ext === 'csv') return 'ext-xls'
  return 'ext-txt'
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}
</script>

<style scoped>
.muz-items { margin-top: 10px; display: flex; flex-direction: column; gap: 6px; }
.muz-item {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 10px; border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px; background: var(--el-bg-color);
}
.muz-item.is-error { border-color: var(--el-color-danger-light-5); background: var(--el-color-danger-light-9); }
.muz-item-ic {
  width: 28px; height: 28px; border-radius: 7px; flex-shrink: 0;
  display: grid; place-items: center;
  background: var(--el-color-primary-light-9); color: var(--el-color-primary);
}
.muz-item.is-error .muz-item-ic { background: var(--el-color-danger-light-8); color: var(--el-color-danger); }
.muz-item-ic.ext-pdf { background: var(--el-color-danger-light-9); color: var(--el-color-danger); }
.muz-item-ic.ext-doc { background: var(--el-color-primary-light-9); color: var(--el-color-primary); }
.muz-item-ic.ext-xls { background: var(--el-color-success-light-9); color: var(--el-color-success); }
.muz-item-ic.ext-txt { background: var(--el-fill-color); color: var(--el-text-color-secondary); }
.muz-item-main { flex: 1; min-width: 0; }
.muz-item-row { display: flex; align-items: baseline; gap: 8px; }
.muz-item-name {
  flex: 1; min-width: 0; font-size: 12.5px; font-weight: 600;
  color: var(--el-text-color-primary);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.muz-item-size { font-size: 11px; color: var(--el-text-color-secondary); flex-shrink: 0; }
.muz-item-bar {
  height: 3px; border-radius: 2px; margin-top: 5px;
  background: var(--el-fill-color); overflow: hidden;
}
.muz-item-fill {
  height: 100%; border-radius: 2px; background: var(--el-color-primary);
  transition: width .4s ease;
}
.muz-item.is-uploading .muz-item-fill { animation: muz-pulse 1.2s ease-in-out infinite; }
.muz-item.is-success .muz-item-fill { background: var(--el-color-success); }
.muz-item.is-error .muz-item-fill { background: var(--el-color-danger); }
.muz-item-msg { margin-top: 4px; font-size: 11.5px; color: var(--el-color-danger); }
.muz-item-ops { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
.muz-ok { color: var(--el-color-success); font-size: 13px; font-weight: 700; }
.muz-btn {
  border: none; background: transparent; cursor: pointer; padding: 3px;
  border-radius: 5px; color: var(--el-text-color-secondary);
  display: grid; place-items: center;
}
.muz-btn:hover { background: var(--el-fill-color); color: var(--el-text-color-primary); }
.muz-spin {
  width: 12px; height: 12px; border-radius: 50%;
  border: 2px solid var(--el-color-primary-light-7);
  border-top-color: var(--el-color-primary);
  animation: muz-rot .8s linear infinite;
}
@keyframes muz-rot { to { transform: rotate(360deg); } }
@keyframes muz-pulse { 0%, 100% { opacity: .6; } 50% { opacity: 1; } }
.muz-list-enter-active, .muz-list-leave-active { transition: all .25s ease; }
.muz-list-enter-from, .muz-list-leave-to { opacity: 0; transform: translateY(-4px); }
</style>
