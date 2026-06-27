<template>
  <MateDialog :model-value="visible" :title="title" width="580px" @close="$emit('close')">
    <el-upload
      drag
      multiple
      :http-request="customUpload"
      name="file"
      :accept="accept"
      :show-file-list="false"
      :before-upload="beforeUpload"
    >
      <div class="drop">
        <UploadCloud :size="30" class="big" />
        <div class="t">{{ t('material.dragOrBrowse') }} <span class="lk">{{ t('material.browse') }}</span></div>
        <div class="s">{{ hint }}</div>
      </div>
    </el-upload>

    <div class="uplist" v-if="queue.length">
      <div v-for="q in queue" :key="q.uid" class="uprow" :class="{ err: q.status === 'error' }">
        <FileTypeIcon :ext="q.ext" size="sm" />
        <span class="nm" :title="q.name">{{ q.name }} <span class="sz">· {{ fmtSize(q.size) }}</span></span>
        <template v-if="q.status === 'uploading'">
          <el-progress :percentage="q.percent" :stroke-width="6" :show-text="false" style="width:120px" />
          <span class="pct">{{ q.percent }}%</span>
        </template>
        <span v-else-if="q.status === 'done'" class="ok"><Check :size="14" /> {{ t('material.done') }}</span>
        <template v-else>
          <span class="errc">{{ q.msg }}</span>
        </template>
        <X :size="14" class="xrm" @click="remove(q.uid)" />
      </div>
    </div>

    <div class="field">
      <span class="lbl">{{ t('material.intoGroup') }}</span>
      <el-select v-model="group" :placeholder="t('material.ungrouped')" size="default" style="width:200px">
        <el-option :label="t('material.ungrouped')" value="0" />
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
    </div>

    <template #footer>
      <el-button @click="$emit('close')">{{ t('material.close') }}</el-button>
    </template>
  </MateDialog>
</template>

<script lang="ts" setup>
import { MateMessage, MateDialog } from '@matecloud/ui'
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Check, UploadCloud, X } from 'lucide-vue-next'
import { materialApi, type MaterialCategory, type MaterialType } from '@matecloud/core'
import FileTypeIcon from './FileTypeIcon.vue'
import { fmtSize } from './format'
import { SINGLE_MAX, chunkedUpload } from './chunkUpload'

const { t } = useI18n()

const props = defineProps<{
  visible: boolean
  type: MaterialType
  categoryId: string
  categories: MaterialCategory[]
}>()
const emit = defineEmits<{ (e: 'close'): void; (e: 'uploaded'): void }>()

const ACCEPT: Record<MaterialType, string> = {
  image: '.jpg,.jpeg,.png,.gif,.webp,.bmp',
  video: '.mp4,.mov,.webm,.mkv,.avi,.m4v',
  doc: '.doc,.docx,.xls,.xlsx,.ppt,.pptx,.pdf,.txt,.csv,.md,.zip,.rar,.7z',
}
const MAX_MB: Record<MaterialType, number> = { image: 10, video: 200, doc: 50 }
const TITLE_KEY: Record<MaterialType, string> = { image: 'uploadImage', video: 'uploadVideo', doc: 'uploadDoc' }
const HINT_KEY: Record<MaterialType, string> = { image: 'hintImage', video: 'hintVideo', doc: 'hintDoc' }

type Q = { uid: number; name: string; ext: string; size: number; percent: number; status: 'uploading' | 'done' | 'error'; msg?: string }
const queue = ref<Q[]>([])
const group = ref('0')

watch(() => props.visible, (v) => {
  if (v) {
    queue.value = []
    group.value = props.categoryId && props.categoryId !== '' ? props.categoryId : '0'
  }
})

const title = computed(() => t(`material.${TITLE_KEY[props.type]}`))
const hint = computed(() => t(`material.${HINT_KEY[props.type]}`))
const accept = computed(() => ACCEPT[props.type])

function extOf(name: string) {
  const i = name.lastIndexOf('.')
  return i >= 0 ? name.slice(i + 1).toLowerCase() : ''
}

function beforeUpload(file: File) {
  const ext = extOf(file.name)
  if (!accept.value.split(',').includes('.' + ext)) {
    MateMessage.error(t('material.unsupported', { name: file.name }))
    return false
  }
  if (file.size > MAX_MB[props.type] * 1024 * 1024) {
    MateMessage.error(t('material.oversize', { mb: MAX_MB[props.type], name: file.name }))
    return false
  }
  return true
}

function upsert(uid: number, patch: Partial<Q>, base?: { name: string; size: number }) {
  const q = queue.value.find((x) => x.uid === uid)
  if (q) Object.assign(q, patch)
  else if (base) queue.value.unshift({ uid, name: base.name, ext: extOf(base.name), size: base.size, percent: 0, status: 'uploading', ...patch })
}
/**
 * 自定义上传:小文件单次整传(走应用服务器),大文件分片直传(浏览器→对象存储)。
 * el-upload 的 :http-request 接管,使用我们自己的队列做进度/状态展示。
 */
async function customUpload(options: { file: File & { uid: number } }) {
  const file = options.file
  const uid = file.uid
  upsert(uid, { percent: 0, status: 'uploading' }, { name: file.name, size: file.size })
  try {
    if (file.size > SINGLE_MAX) {
      await chunkedUpload(file, props.type, group.value, (p) => upsert(uid, { percent: p }))
    } else {
      await materialApi.uploadSingle(file, props.type, group.value, (p) => upsert(uid, { percent: p }))
    }
    upsert(uid, { percent: 100, status: 'done' })
    emit('uploaded')
  } catch (e: any) {
    upsert(uid, { status: 'error', msg: e?.message || t('material.netError') })
  }
}
function remove(uid: number) {
  queue.value = queue.value.filter((x) => x.uid !== uid)
}
</script>

<style scoped>
.drop { padding: 22px; text-align: center; }
.drop .big { font-size: 28px; }
.drop .t { font-weight: 600; margin-top: 6px; }
.drop .t .lk { color: var(--el-color-primary); }
.drop .s { color: var(--el-text-color-secondary); font-size: 12px; margin-top: 6px; }
.uplist { margin-top: 12px; display: flex; flex-direction: column; gap: 6px; max-height: 220px; overflow: auto; }
.uprow { display: flex; align-items: center; gap: 10px; font-size: 13px; border: 1px solid var(--el-border-color); border-radius: 6px; padding: 7px 10px; }
.uprow.err { border-color: var(--el-color-danger-light-5); background: var(--el-color-danger-light-9); }
.uprow .nm { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.uprow .sz { color: var(--el-text-color-disabled); font-size: 11px; }
.pct { color: var(--el-text-color-secondary); font-size: 12px; width: 36px; text-align: right; }
.ok { color: var(--el-color-success); display: inline-flex; align-items: center; gap: 3px; }
.big { color: var(--el-text-color-secondary); }
.errc { color: var(--el-color-danger); }
.xrm { cursor: pointer; color: var(--el-text-color-disabled); }
.xrm:hover { color: var(--el-color-danger); }
.field { display: flex; align-items: center; gap: 12px; margin-top: 16px; }
.field .lbl { color: var(--el-text-color-secondary); font-size: 13px; }
</style>
