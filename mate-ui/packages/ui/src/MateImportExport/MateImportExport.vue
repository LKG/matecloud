<template>
  <!--
    Vue 3 multi-root: two buttons + a dialog under one component. The buttons
    inherit their position from wherever the parent mounts the component
    (e.g. inside a <MatePageCard> #actions slot). The dialog is teleported
    to body by el-dialog automatically so it sits above everything regardless.
  -->
  <el-button
    v-if="exportUrl && canExport"
    :loading="exportDownloading"
    @click="handleExport"
  >
    <Download :size="14" class="mr-1" />{{ exportText ?? t('common.export') }}
  </el-button>

  <el-button
    v-if="importUrl && canImport"
    @click="openImportDialog"
  >
    <Upload :size="14" class="mr-1" />{{ importText ?? t('common.import') }}
  </el-button>

  <el-dialog
    v-if="importUrl"
    v-model="importVisible"
    :title="importDialogTitle ?? t('common.import')"
    :width="dialogWidth"
    destroy-on-close
    :lock-scroll="false"
  >
    <div class="mc-iex-body">
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :show-file-list="true"
        :limit="1"
        :on-change="onFilePicked"
        :on-remove="clearFile"
        :accept="accept"
        drag
      >
        <div class="el-upload__text">
          {{ importDropText ?? defaultDropText }}
        </div>
        <template #tip>
          <div class="el-upload__tip">
            {{ importTip ?? defaultTip }}
            <a
              v-if="templateUrl"
              href="#"
              class="mc-iex-template-link"
              @click.prevent="handleDownloadTemplate"
            >
              <Download :size="12" />{{ templateText ?? t('common.importDownloadTemplate') }}
            </a>
          </div>
        </template>
      </el-upload>

      <!-- Last-run summary, only shown after at least one import in this dialog session -->
      <div v-if="importResult" class="mc-iex-result">
        <div class="mc-iex-summary">
          <MateBadge type="info">{{ t('common.importTotal') }}: {{ importResult.total }}</MateBadge>
          <MateBadge type="success">{{ t('common.importSuccessCount') }}: {{ importResult.success }}</MateBadge>
          <MateBadge v-if="importResult.fail > 0" type="danger">
            {{ t('common.importFailCount') }}: {{ importResult.fail }}
          </MateBadge>
        </div>
        <div v-if="importResult.errors?.length" class="mc-iex-errors">
          <div class="mc-iex-errors__title">{{ t('common.importErrors') }}</div>
          <ul>
            <li v-for="(err, i) in importResult.errors" :key="i">{{ err }}</li>
          </ul>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="importVisible = false">{{ t('common.cancel') }}</el-button>
      <el-button
        type="primary"
        :loading="importUploading"
        :disabled="!importFile"
        @click="handleImport"
      >
        {{ t('common.import') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
/**
 * MateImportExport — one-shot import/export control used by CRUD list pages.
 *
 * Responsibilities:
 *   - render an Export button (optional) that streams an xlsx to the browser
 *   - render an Import button (optional) that opens a drag-and-drop dialog
 *   - optionally offer a "Download template" link inside the import dialog
 *   - surface server-side permission gates via {@link useAuthStore#hasPermission}
 *   - emit {@code imported} so the parent refreshes its list after success
 *
 * Everything is driven by props — no side files, no mixins. Drop the tag
 * inside a `<MatePageCard>` #actions slot and you get both buttons plus the
 * full import dialog in ~6 lines of host markup.
 *
 * Why a single multi-root component rather than two (MateExportButton +
 * MateImportButton)? The import dialog has to live somewhere; colocating it
 * with the button that opens it keeps state ownership clear and avoids an
 * awkward "drop the dialog somewhere" step for the host.
 */
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { type UploadFile, type UploadInstance } from 'element-plus'
import { Download, Upload } from 'lucide-vue-next'
import { MateMessage } from '../MateMessage/message'
import {
  useAuthStore,
  useExport,
  useImport,
  type ImportResult,
} from '@matecloud/core'
import MateBadge from '../MateBadge/MateBadge.vue'

const props = withDefaults(defineProps<{
  /** Export endpoint path (no {@code /api/v1} prefix). Omit to hide the export button. */
  exportUrl?: string
  /** Query params for the export request — usually the current search filters. */
  exportParams?: Record<string, any>
  /** Suggested filename used when the server omits Content-Disposition. */
  exportFilename?: string
  /** Custom label; defaults to i18n {@code common.export}. */
  exportText?: string
  /** Permission code required to see the export button. No code → always visible. */
  exportPerm?: string

  /** Import endpoint path (multipart/form-data, field name "file"). Omit to hide the import button. */
  importUrl?: string
  /** Template download endpoint. Omit to hide the "Download template" link. */
  templateUrl?: string
  /** Filename for the downloaded template. */
  templateFilename?: string
  /** Custom "Download template" link label. */
  templateText?: string
  /** Import button label. */
  importText?: string
  /** Import dialog title; defaults to i18n {@code common.import}. */
  importDialogTitle?: string
  /** Text shown inside the upload drop zone. */
  importDropText?: string
  /** Tip text shown below the drop zone (left of the template link). */
  importTip?: string
  /** Permission code required to see the import button. */
  importPerm?: string
  /** Override el-dialog width. */
  dialogWidth?: string
  /** Allowed file extensions. */
  accept?: string
}>(), {
  exportParams: () => ({}),
  dialogWidth: '520px',
  accept: '.xlsx,.xls',
})

const emit = defineEmits<{
  /** Fired after a successful import (full or partial). Parent usually calls {@code loadData}. */
  imported: [result: ImportResult]
  /** Fired after a successful export. Rarely used — mostly for analytics hooks. */
  exported: []
}>()

const { t } = useI18n()
const auth = useAuthStore()

// ----- Permission gates -----
// If the caller didn't supply a perm code, show the button; otherwise delegate
// to the auth store. Using hasPermission (not v-permission directive) so this
// component stays reusable in apps that don't register the directive.
const canExport = computed(() => !props.exportPerm || auth.hasPermission(props.exportPerm))
const canImport = computed(() => !props.importPerm || auth.hasPermission(props.importPerm))

// ----- Defaults for copy -----
const defaultDropText = computed(() => t('common.importDropText'))
const defaultTip = computed(() => t('common.importTip'))

// ----- Export -----
const { exportExcel, downloading: exportDownloading } = useExport()

async function handleExport() {
  try {
    await exportExcel(
      props.exportUrl!,
      props.exportParams,
      props.exportFilename,
    )
    MateMessage.success(t('common.exportSuccess'))
    emit('exported')
  } catch (e: any) {
    MateMessage.error(e?.msg || e?.message || t('common.exportFailed'))
  }
}

// ----- Import dialog -----
const { importExcel, uploading: importUploading } = useImport()
const importVisible = ref(false)
const importFile = ref<File | null>(null)
const importResult = ref<ImportResult | null>(null)
const uploadRef = ref<UploadInstance>()

function openImportDialog() {
  importFile.value = null
  importResult.value = null
  uploadRef.value?.clearFiles()
  importVisible.value = true
}

function onFilePicked(file: UploadFile) {
  // el-upload wraps the browser File in `raw`; multipart upload needs the
  // real File so the boundary can be generated.
  importFile.value = file.raw ?? null
}

function clearFile() {
  importFile.value = null
}

async function handleImport() {
  if (!importFile.value) {
    MateMessage.warning(t('common.importPickFirst'))
    return
  }
  try {
    const result = await importExcel(props.importUrl!, importFile.value)
    importResult.value = result
    if (result.fail === 0) {
      MateMessage.success(t('common.importAllSuccess', { n: result.success }))
      importVisible.value = false
    } else {
      MateMessage.warning(t('common.importPartial', { ok: result.success, bad: result.fail }))
    }
    emit('imported', result)
  } catch (e: any) {
    MateMessage.error(e?.msg || e?.message || t('common.importFailed'))
  }
}

async function handleDownloadTemplate() {
  if (!props.templateUrl) return
  try {
    await exportExcel(props.templateUrl, undefined, props.templateFilename)
  } catch (e: any) {
    MateMessage.error(e?.msg || e?.message || t('common.exportFailed'))
  }
}
</script>

<style scoped>
.mc-iex-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.mc-iex-template-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--mc-primary);
  text-decoration: none;
  margin-left: 8px;
  font-size: 12px;
}
.mc-iex-template-link:hover {
  text-decoration: underline;
}
.mc-iex-result {
  padding: 12px;
  border: 1px solid var(--mc-divider-regular, rgb(16 24 40 / 0.08));
  border-radius: 6px;
  background: var(--mc-bg);
}
.mc-iex-summary {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.mc-iex-errors {
  margin-top: 10px;
}
.mc-iex-errors__title {
  font-size: 12px;
  color: var(--mc-text-muted);
  margin-bottom: 4px;
}
.mc-iex-errors ul {
  margin: 0;
  padding-left: 18px;
  max-height: 160px;
  overflow-y: auto;
  font-size: 12px;
  color: var(--mc-danger);
}
.mc-iex-errors li {
  line-height: 1.6;
}
</style>
