<template>
  <table class="mtable">
    <thead>
      <tr>
        <th style="width:34px"></th>
        <th>{{ t('material.colName') }}</th>
        <th style="width:70px">{{ t('material.colType') }}</th>
        <th style="width:90px">{{ t('material.colSize') }}</th>
        <th style="width:120px">{{ t('material.colGroup') }}</th>
        <th style="width:120px">{{ t('material.colTime') }}</th>
        <th style="width:150px">{{ t('material.colAction') }}</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="m in items" :key="m.id" class="trow" :class="{ sel: selected.includes(m.id) }" @click="onClick(m)">
        <td><span class="cbx" :class="{ on: batch }" @click.stop="$emit('toggle', m.id)"></span></td>
        <td>
          <div class="tname">
            <FileTypeIcon :ext="m.ext" size="sm" />
            <span class="nm" :title="m.name">{{ m.name }}</span>
          </div>
        </td>
        <td><span class="ext">{{ m.ext }}</span></td>
        <td>{{ fmtSize(m.size) }}</td>
        <td><span class="gtag">{{ groupName(m.categoryId) }}</span></td>
        <td>{{ fmtDate(m.createdAt) }}</td>
        <td>
          <div class="tact" @click.stop>
            <span class="act" :title="t('material.actPreview')" @click="$emit('action', { act: 'preview', item: m })"><Download :size="14" /></span>
            <MateCopyButton :content="m.url" :copy-text="t('material.actCopy')" :copied-text="t('material.linkCopied')" :size="13" />
            <span class="act" :title="t('material.actMove')" @click="$emit('action', { act: 'move', item: m })"><FolderInput :size="14" /></span>
            <span class="act" :title="t('material.actRename')" @click="$emit('action', { act: 'rename', item: m })"><Pencil :size="14" /></span>
            <MateInlineConfirm
              :title="t('material.confirmDelete', { name: m.name })"
              :confirm-text="t('material.delete')"
              :cancel-text="t('material.cancel')"
              @confirm="$emit('action', { act: 'delete', item: m })"
            >
              <span class="act" :title="t('material.actDelete')"><Trash2 :size="14" /></span>
            </MateInlineConfirm>
          </div>
        </td>
      </tr>
    </tbody>
  </table>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Download, FolderInput, Pencil, Trash2 } from 'lucide-vue-next'
import { MateCopyButton, MateInlineConfirm } from '@matecloud/ui'
import type { MaterialCategory, MaterialItem } from '@matecloud/core'
import FileTypeIcon from './FileTypeIcon.vue'
import { fmtSize, fmtDate } from './format'

const { t } = useI18n()
const props = defineProps<{ items: MaterialItem[]; batch: boolean; selected: string[]; categories: MaterialCategory[] }>()
const emit = defineEmits<{
  (e: 'toggle', id: string): void
  (e: 'action', payload: { act: string; item: MaterialItem }): void
}>()

const nameMap = computed(() => {
  const m: Record<string, string> = {}
  props.categories.forEach((c) => { m[c.id] = c.name })
  return m
})
function groupName(id: string | null) {
  return id ? (nameMap.value[id] || '—') : t('material.ungrouped')
}
function onClick(m: MaterialItem) {
  if (props.batch) emit('toggle', m.id)
}
</script>

<style scoped>
.mtable { width: 100%; border-collapse: collapse; margin-top: 14px; font-size: 13px; }
.mtable th { text-align: left; color: var(--el-text-color-secondary); font-weight: 600; font-size: 12px; padding: 8px 12px; border-bottom: 1px solid var(--el-border-color); background: var(--el-fill-color-light); }
.mtable td { padding: 10px 12px; border-bottom: 1px solid var(--el-border-color-lighter); vertical-align: middle; }
.trow:hover { background: var(--el-fill-color-light); }
/* 选中态:淡底色 + 左侧主色强调条,避免整行铺满蓝色「全涂」观感 */
.trow.sel > td { background: color-mix(in srgb, var(--el-color-primary) 7%, transparent); }
.trow.sel > td:first-child { box-shadow: inset 3px 0 0 var(--el-color-primary); }
.tname { display: flex; align-items: center; gap: 10px; }
.tname .nm { font-weight: 600; color: var(--el-text-color-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 320px; }
.ext { font-size: 11px; font-weight: 700; text-transform: uppercase; color: var(--el-text-color-secondary); }
.gtag { font-size: 11px; background: var(--el-fill-color); color: var(--el-text-color-secondary); border-radius: 5px; padding: 2px 8px; }
.tact { display: flex; gap: 4px; opacity: 0; }
.trow:hover .tact { opacity: 1; }
.act { width: 24px; height: 24px; border-radius: 6px; background: var(--el-bg-color-overlay); border: 1px solid var(--el-border-color); display: grid; place-items: center; cursor: pointer; font-size: 12px; color: var(--el-text-color-regular); }
.act:hover { color: var(--el-color-primary); }
.tact :deep(.mcb) { width: 24px; height: 24px; border-radius: 6px; }
.cbx { width: 16px; height: 16px; border-radius: 4px; border: 1.5px solid var(--el-border-color); cursor: pointer; display: none; }
.trow:hover .cbx, .cbx.on { display: inline-block; }
.trow.sel .cbx { background: var(--el-color-primary); border-color: var(--el-color-primary); display: inline-block; }
</style>
