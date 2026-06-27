<template>
  <MatePageCard
    :title="t('storage.title')"
    :description="t('storage.description')"
  >
    <template #actions>
      <el-button type="primary" @click="handleUpload">
        <UploadCloud :size="14" class="mr-1" />{{ t('storage.upload') }}
      </el-button>
    </template>

    <!-- Overall stats -->
    <div class="stat-grid">
      <div class="stat-card">
        <HardDrive :size="18" class="stat-icon" />
        <div>
          <div class="stat-label">{{ t('storage.totalFiles') }}</div>
          <div class="stat-value">{{ stats.totalFiles.toLocaleString() }}</div>
        </div>
      </div>
      <div class="stat-card">
        <Database :size="18" class="stat-icon" />
        <div>
          <div class="stat-label">{{ t('storage.totalSize') }}</div>
          <div class="stat-value">{{ stats.totalSize }}</div>
        </div>
      </div>
      <div class="stat-card">
        <FileImage :size="18" class="stat-icon" />
        <div>
          <div class="stat-label">{{ t('storage.images') }}</div>
          <div class="stat-value">{{ stats.images }}</div>
        </div>
      </div>
      <div class="stat-card">
        <FileText :size="18" class="stat-icon" />
        <div>
          <div class="stat-label">{{ t('storage.documents') }}</div>
          <div class="stat-value">{{ stats.documents }}</div>
        </div>
      </div>
    </div>

    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <el-input
        v-model="keyword"
        :placeholder="t('storage.searchPlaceholder')"
        clearable
        style="width: 260px"
        @keyup.enter="handleSearch"
      >
        <template #prefix><Search :size="14" /></template>
      </el-input>
      <el-select v-model="typeFilter" clearable style="width: 140px" :placeholder="t('storage.fileType')">
        <el-option label="image" value="image" />
        <el-option label="document" value="document" />
        <el-option label="video" value="video" />
        <el-option label="other" value="other" />
      </el-select>
    </MateSearchBar>

    <MateTable
      :columns="columns"
      :data="rows"
      :loading="loading"
      row-key="id"
      :action-width="170"
      :action-label="t('common.action')"
    >
      <template #col-fileName="{ row }">
        <div class="file-cell">
          <component :is="iconFor(row.type)" :size="16" class="file-icon" :class="`file-icon--${row.type}`" />
          <div>
            <span class="file-name">{{ row.fileName }}</span>
            <span class="file-meta">{{ row.bucket }} · {{ row.extension }}</span>
          </div>
        </div>
      </template>
      <template #col-size="{ row }">
        <span class="mc-mono">{{ row.size }}</span>
      </template>
      <template #col-uploadedBy="{ row }">
        <span>{{ row.uploadedBy }}</span>
      </template>
      <template #col-uploadedAt="{ row }">
        <span class="mc-mono">{{ row.uploadedAt }}</span>
      </template>
      <template #actions="{ row }">
        <button class="mc-action-btn" @click="handleDownload(row)">{{ t('storage.download') }}</button>
        <button class="mc-action-btn" @click="handleCopyLink(row)">{{ t('storage.copyLink') }}</button>
        <MateInlineConfirm :title="t('common.deleteConfirm')" @confirm="handleDelete(row)">
                      <button class="mc-action-btn mc-action-btn--danger">{{ t('common.delete') }}</button>
          
        </MateInlineConfirm>
      </template>
    </MateTable>

    <MatePagination
      v-model:page-num="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      @change="() => {}"
    />

    <p class="demo-hint">
      <Info :size="13" class="mr-1" />
      {{ t('storage.hint') }}
    </p>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  Search, UploadCloud, HardDrive, Database, FileImage,
  FileText, FileVideo, FileArchive, Info,
} from 'lucide-vue-next'
import { MatePageCard, MateTable, MateSearchBar, MatePagination, type MateColumn, MateMessage, MateInlineConfirm } from '@matecloud/ui'

defineOptions({ name: 'StorageView' })

const { t } = useI18n()

const columns: MateColumn[] = [
  { prop: 'fileName', label: t('storage.fileName') },
  { prop: 'size', label: t('storage.fileSize'), width: 100, align: 'right' },
  { prop: 'uploadedBy', label: t('storage.uploadedBy'), width: 140 },
  { prop: 'uploadedAt', label: t('storage.uploadedAt'), width: 170 },
]

type FileType = 'image' | 'document' | 'video' | 'other'

interface FileRow {
  id: string
  fileName: string
  extension: string
  type: FileType
  size: string
  bucket: string
  uploadedBy: string
  uploadedAt: string
}

const all = ref<FileRow[]>([])
const loading = ref(false)
const keyword = ref('')
const typeFilter = ref<FileType | ''>('')
const pageNum = ref(1)
const pageSize = ref(10)

const stats = ref({ totalFiles: 0, totalSize: '0 B', images: 0, documents: 0 })

function seedFiles(): FileRow[] {
  const images = [
    { n: 'avatar-admin.png', s: '12.4 KB' },
    { n: 'logo-dark.svg', s: '3.2 KB' },
    { n: 'banner-2026-04.jpg', s: '284 KB' },
    { n: 'product-thumb-001.webp', s: '64 KB' },
    { n: 'screenshot-dashboard.png', s: '1.2 MB' },
  ]
  const docs = [
    { n: 'user-import-template.xlsx', s: '18 KB' },
    { n: 'api-reference-v1.pdf', s: '2.4 MB' },
    { n: 'onboarding-guide.md', s: '8 KB' },
    { n: 'monthly-report-2026-03.xlsx', s: '36 KB' },
  ]
  const videos = [
    { n: 'demo-intro.mp4', s: '24 MB' },
  ]
  const others = [
    { n: 'system-backup-2026-04-12.zip', s: '18.2 MB' },
    { n: 'log-archive.tar.gz', s: '6.4 MB' },
  ]
  const build = (items: { n: string; s: string }[], type: FileType) =>
    items.map((x, i): FileRow => ({
      id: `${type}-${i}`,
      fileName: x.n,
      extension: x.n.split('.').pop()?.toUpperCase() ?? '',
      type,
      size: x.s,
      bucket: type === 'image' ? 'mate-public' : 'mate-private',
      uploadedBy: ['admin', 'operator', 'user001', 'user002'][i % 4],
      uploadedAt: `2026-04-${String(14 - (i % 10)).padStart(2, '0')} ${String(8 + i % 12).padStart(2, '0')}:${String((i * 7) % 60).padStart(2, '0')}`,
    }))

  return [
    ...build(images, 'image'),
    ...build(docs, 'document'),
    ...build(videos, 'video'),
    ...build(others, 'other'),
  ]
}

async function loadData() {
  loading.value = true
  try {
    await new Promise(r => setTimeout(r, 220))
    all.value = seedFiles()
    stats.value = {
      totalFiles: all.value.length,
      totalSize: '52.4 MB',
      images: all.value.filter(f => f.type === 'image').length,
      documents: all.value.filter(f => f.type === 'document').length,
    }
  } finally {
    loading.value = false
  }
}

const filtered = computed(() => {
  let list = all.value
  if (typeFilter.value) list = list.filter(f => f.type === typeFilter.value)
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    list = list.filter(f => f.fileName.toLowerCase().includes(kw))
  }
  return list
})
const total = computed(() => filtered.value.length)
const rows = computed(() => {
  const start = (pageNum.value - 1) * pageSize.value
  return filtered.value.slice(start, start + pageSize.value)
})

function handleSearch() { pageNum.value = 1 }
function handleReset() {
  keyword.value = ''
  typeFilter.value = ''
  pageNum.value = 1
}

function iconFor(type: FileType) {
  return ({
    image: FileImage,
    document: FileText,
    video: FileVideo,
    other: FileArchive,
  })[type]
}

function handleUpload() {
  MateMessage.info(t('storage.uploadDemo'))
}

function handleDownload(row: FileRow) {
  MateMessage.success(t('storage.downloadStart', { name: row.fileName }))
}

function handleCopyLink(row: FileRow) {
  const url = `https://minio.example.com/${row.bucket}/${row.fileName}`
  navigator.clipboard?.writeText(url).catch(() => {})
  MateMessage.success(t('storage.linkCopied'))
}

async function handleDelete(row: FileRow) {
  all.value = all.value.filter(f => f.id !== row.id)
  MateMessage.success(t('common.deleteSuccess'))
}

onMounted(loadData)
</script>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}
.stat-card {
  display: flex;
  gap: 12px;
  align-items: center;
  border: 1px solid var(--mc-divider-regular, rgb(16 24 40 / 0.08));
  border-radius: var(--mc-radius);
  padding: 14px 16px;
  background: var(--mc-bg-elevated);
}
.stat-icon { color: var(--mc-primary); flex-shrink: 0; }
.stat-label { color: var(--mc-text-muted); font-size: 12px; }
.stat-value {
  font-size: 20px; font-weight: 700;
  color: var(--mc-text-primary);
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
}

.file-cell { display: flex; align-items: center; gap: 10px; }
.file-icon { flex-shrink: 0; }
.file-icon--image { color: #36BFFA; }
.file-icon--document { color: #155AEF; }
.file-icon--video { color: #B54708; }
.file-icon--other { color: #98A2B3; }
.file-name {
  display: block;
  font-size: 13px; font-weight: 500;
  color: var(--mc-text-primary);
}
.file-meta {
  display: block;
  font-size: 12px;
  color: var(--mc-text-muted);
}

.demo-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  padding: 8px 12px;
  font-size: 12px;
  color: var(--mc-text-muted);
  background: rgb(54 191 250 / 0.06);
  border-radius: 6px;
}
</style>
