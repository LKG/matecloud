<template>
  <MatePageCard :title="t('material.title')" :description="t('material.desc')">
    <el-tabs v-model="type" @tab-change="onTab">
      <el-tab-pane :label="t('material.tabImage')" name="image" />
      <el-tab-pane :label="t('material.tabVideo')" name="video" />
      <el-tab-pane :label="t('material.tabDoc')" name="doc" />
    </el-tabs>

    <div class="layout">
      <CategoryPanel
        :cat="cat"
        :active="categoryId"
        @select="onSelectCat"
        @create="createCategory"
        @rename="(p) => renameCategory(p.id, p.name)"
        @delete="deleteCategory"
      />

      <section class="content">
        <div class="toolbar">
          <el-button type="primary" @click="uploadVisible = true">
            <Upload :size="15" style="margin-right:5px" />{{ t('material.upload') }}{{ typeLabel }}
          </el-button>
          <el-button @click="toggleBatch">{{ batch ? t('material.exitBatch') : t('material.batch') }}</el-button>
          <div class="spacer"></div>
          <el-input v-model="keyword" :placeholder="t('common.search')" clearable style="width:220px" @keyup.enter="search" @clear="search">
            <template #suffix><Search :size="15" style="cursor:pointer" @click="search" /></template>
          </el-input>
        </div>

        <div class="batchbar" v-if="batch">
          <el-checkbox :model-value="allChecked" @change="selectAll">{{ t('material.selectAll') }}</el-checkbox>
          <span>{{ t('material.selected') }} <b>{{ selected.length }}</b> {{ t('material.items') }}</span>
          <div class="spacer"></div>
          <el-button size="small" :disabled="!selected.length" @click="openMove(selected)">{{ t('material.moveToGroup') }}</el-button>
          <el-button size="small" type="danger" :disabled="!selected.length" @click="batchDelete">{{ t('material.delete') }}</el-button>
        </div>

        <!-- 加载骨架 -->
        <div v-if="loading" class="mgrid-skel">
          <div v-for="i in 10" :key="i" class="mskel-item">
            <MateSkeleton :width="'100%'" :height="118" :radius="0" />
            <MateSkeleton :width="'70%'" :height="10" style="margin:10px 10px 0" />
            <MateSkeleton :width="'40%'" :height="10" style="margin:8px 10px 12px" />
          </div>
        </div>

        <template v-else>
          <DocList
            v-if="type === 'doc'"
            :items="items"
            :batch="batch"
            :selected="selected"
            :categories="cat.categories"
            @toggle="toggle"
            @action="onAction"
          />
          <MediaGrid
            v-else
            :items="items"
            :type="type"
            :batch="batch"
            :selected="selected"
            @toggle="toggle"
            @action="onAction"
          />
        </template>

        <div class="empty" v-if="!loading && !items.length">
          <Inbox :size="46" :stroke-width="1.4" />
          <div>{{ t('material.empty') }}</div>
          <el-button type="primary" size="small" @click="uploadVisible = true">{{ t('material.uploadNow') }}</el-button>
        </div>

        <div class="pager" v-if="pager.total > pager.pageSize">
          <el-pagination
            layout="total, prev, pager, next"
            :total="pager.total"
            :page-size="pager.pageSize"
            :current-page="pager.pageNum"
            @current-change="changePage"
          />
        </div>
      </section>
    </div>

    <UploadModal
      :visible="uploadVisible"
      :type="type"
      :category-id="categoryId"
      :categories="cat.categories"
      @close="uploadVisible = false"
      @uploaded="refresh"
    />

    <!-- 移动到分组 —— 统一使用 MateDialog -->
    <MateDialog
      v-model="moveVisible"
      :title="t('material.moveTitle')"
      width="380px"
      :confirm-text="t('material.confirm')"
      :cancel-text="t('material.cancel')"
      @submit="doMove"
    >
      <el-select v-model="moveGroup" style="width:100%">
        <el-option :label="t('material.ungrouped')" value="0" />
        <el-option v-for="c in cat.categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
    </MateDialog>

    <!-- 素材重命名 —— 统一使用 MateDialog -->
    <MateDialog
      v-model="renameVisible"
      :title="t('material.renameTitle')"
      width="420px"
      :confirm-text="t('material.save')"
      :cancel-text="t('material.cancel')"
      @submit="doRename"
    >
      <el-input
        ref="renameInputRef"
        v-model="renameName"
        :placeholder="t('material.newName')"
        maxlength="60"
        clearable
        @keyup.enter="doRename"
      />
    </MateDialog>

    <!-- 图片 / 视频统一灯箱(文案由组件内置双语) -->
    <MateMediaViewer
      v-model="viewerVisible"
      :items="mediaItems"
      :initial-index="viewerIndex"
    />

    <!-- 文档预览(PDF / 文本内联,其它兜底下载) -->
    <MateDocViewer
      v-model="docVisible"
      :url="docUrl"
      :name="docName"
    />
  </MatePageCard>
</template>

<script lang="ts" setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Inbox, Search, Upload } from 'lucide-vue-next'
import { materialApi, type MaterialItem, type MaterialType } from '@matecloud/core'
import { MateDialog, MateDocViewer, MateMediaViewer, MateSkeleton, MatePageCard, type MediaItem, MateMessage, MateMessageBox } from '@matecloud/ui'
import type { InputInstance } from 'element-plus'
import { useMaterial } from '@/composables/useMaterial'
import CategoryPanel from './material/CategoryPanel.vue'
import MediaGrid from './material/MediaGrid.vue'
import DocList from './material/DocList.vue'
import UploadModal from './material/UploadModal.vue'

const { t } = useI18n()
const {
  type, categoryId, keyword, loading, items, cat, pager,
  refresh, switchType, selectCategory, search, changePage,
  createCategory, renameCategory, deleteCategory,
  renameItem, moveItem, deleteItem,
} = useMaterial()

const batch = ref(false)
const selected = ref<string[]>([])
const uploadVisible = ref(false)
const moveVisible = ref(false)
const moveGroup = ref('0')
const moveIds = ref<string[]>([])
const renameVisible = ref(false)
const renameName = ref('')
const renameId = ref('')
const renameInputRef = ref<InputInstance>()
const viewerVisible = ref(false)
const viewerIndex = ref(0)
const docVisible = ref(false)
const docUrl = ref('')
const docName = ref('')
// 当前网格里所有可视媒体(图片 / 视频),供统一灯箱左右切换
const mediaItems = computed<MediaItem[]>(() =>
  items.value
    .filter((m) => m.type === 'image' || m.type === 'video')
    .map((m) => ({ type: m.type as 'image' | 'video', url: m.url, name: m.name })))

const typeLabel = computed(() => ({
  image: t('material.typeImage'), video: t('material.typeVideo'), doc: t('material.typeDoc'),
} as Record<string, string>)[type.value])
const allChecked = computed(() => items.value.length > 0 && selected.value.length === items.value.length)

onMounted(refresh)

async function onTab(name: string | number) {
  clearSel()
  await switchType(String(name) as MaterialType)
}
async function onSelectCat(id: string) {
  clearSel()
  await selectCategory(id)
}
function toggleBatch() { batch.value = !batch.value; clearSel() }
function clearSel() { selected.value = [] }
function toggle(id: string) {
  const i = selected.value.indexOf(id)
  if (i >= 0) selected.value.splice(i, 1)
  else selected.value.push(id)
}
function selectAll(v: any) { selected.value = v ? items.value.map((m) => m.id) : [] }

async function onAction({ act, item }: { act: string; item: MaterialItem }) {
  if (act === 'preview') {
    if (item.type === 'image' || item.type === 'video') {
      // 图片 / 视频:统一灯箱预览,索引对应可视媒体列表
      viewerIndex.value = Math.max(0, mediaItems.value.findIndex((m) => m.url === item.url))
      viewerVisible.value = true
    } else {
      // 文档 / 其它:应用内文档预览(PDF/文本内联,Office 等兜底下载)
      const { data } = await materialApi.url(item.id)
      docUrl.value = data.url
      docName.value = item.name
      docVisible.value = true
    }
  } else if (act === 'rename') {
    renameId.value = item.id
    renameName.value = item.name
    renameVisible.value = true
    nextTick(() => renameInputRef.value?.focus())
  } else if (act === 'move') {
    openMove([item.id])
  } else if (act === 'delete') {
    // 删除已由列表/网格项内的 MateInlineConfirm 气泡确认,这里直接执行
    await deleteItem(item.id)
  }
}

async function doRename() {
  const name = renameName.value.trim()
  if (!name) {
    MateMessage.warning(t('material.nameRequired'))
    return
  }
  await renameItem(renameId.value, name)
  renameVisible.value = false
  MateMessage.success(t('material.renamed'))
}

function openMove(ids: string[]) {
  moveIds.value = ids
  moveGroup.value = '0'
  moveVisible.value = true
}
async function doMove() {
  const target = moveGroup.value === '0' ? null : moveGroup.value
  for (const id of moveIds.value) {
    await moveItem(id, target)
  }
  moveVisible.value = false
  clearSel()
  MateMessage.success(t('material.moved'))
}
async function batchDelete() {
  try {
    await MateMessageBox.confirm(t('material.confirmBatchDelete', { n: selected.value.length }), t('material.tip'),
      { type: 'warning', confirmButtonText: t('material.delete'), cancelButtonText: t('material.cancel') })
    for (const id of [...selected.value]) {
      await materialApi.delete(id)
    }
    clearSel()
    await refresh()
    MateMessage.success(t('material.deleted'))
  } catch { /* cancelled */ }
}
</script>

<style scoped>
/* 填满卡片可用高度,使整块布局(分组面板 + 内容区)底部对齐,不再留白 */
.layout { display: flex; gap: 0; flex: 1 1 auto; min-height: 420px; border: 1px solid var(--el-border-color-lighter); border-radius: 10px; overflow: hidden; margin-top: 8px; }
.content { flex: 1; min-width: 0; min-height: 0; padding: 16px 20px; display: flex; flex-direction: column; overflow-y: auto; }
.toolbar { display: flex; align-items: center; gap: 10px; }
.toolbar .spacer { flex: 1; }
.batchbar { display: flex; align-items: center; gap: 12px; margin-top: 14px; padding: 8px 14px; background: var(--el-color-primary-light-9); border: 1px solid var(--el-color-primary-light-7); border-radius: 8px; font-size: 13px; }
.batchbar .spacer { flex: 1; }
.empty { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: var(--el-text-color-disabled); gap: 10px; }
.empty .ic { font-size: 40px; }
.pager { display: flex; justify-content: flex-end; margin-top: 16px; }
.mgrid-skel { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 14px; margin-top: 16px; }
.mskel-item { border: 1px solid var(--el-border-color); border-radius: 8px; overflow: hidden; background: var(--el-bg-color); }
</style>
