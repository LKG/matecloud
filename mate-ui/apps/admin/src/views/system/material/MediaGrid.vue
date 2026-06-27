<template>
  <div class="grid">
    <div
      v-for="m in items"
      :key="m.id"
      class="mitem"
      :class="{ sel: selected.includes(m.id) }"
      @click="onClick(m)"
    >
      <span class="chk" :class="{ on: batch }" @click.stop="$emit('toggle', m.id)"></span>
      <div class="actions" @click.stop>
        <span class="act" :title="t('material.actRename')" @click="$emit('action', { act: 'rename', item: m })"><Pencil :size="14" /></span>
        <span class="act" :title="t('material.actMove')" @click="$emit('action', { act: 'move', item: m })"><FolderInput :size="14" /></span>
        <MateCopyButton :content="m.url" :copy-text="t('material.actCopy')" :copied-text="t('material.linkCopied')" :size="14" />
        <MateInlineConfirm
          :title="t('material.confirmDelete', { name: m.name })"
          :confirm-text="t('material.delete')"
          :cancel-text="t('material.cancel')"
          @confirm="$emit('action', { act: 'delete', item: m })"
        >
          <span class="act" :title="t('material.actDelete')"><Trash2 :size="14" /></span>
        </MateInlineConfirm>
      </div>
      <div class="thumb">
        <MateMediaThumb :src="m.url" :type="type" :alt="m.name" :error-text="t('material.loadFailed')" />
      </div>
      <div class="meta">
        <div class="fname" :title="m.name">{{ m.name }}</div>
        <div class="fsub"><span>{{ fmtSize(m.size) }}</span><span>{{ fmtDate(m.createdAt) }}</span></div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { useI18n } from 'vue-i18n'
import { FolderInput, Pencil, Trash2 } from 'lucide-vue-next'
import { MateCopyButton, MateInlineConfirm, MateMediaThumb } from '@matecloud/ui'
import type { MaterialItem } from '@matecloud/core'
import { fmtSize, fmtDate } from './format'

const { t } = useI18n()
const props = defineProps<{ items: MaterialItem[]; type: string; batch: boolean; selected: string[] }>()
const emit = defineEmits<{
  (e: 'toggle', id: string): void
  (e: 'action', payload: { act: string; item: MaterialItem }): void
}>()

function onClick(m: MaterialItem) {
  if (props.batch) emit('toggle', m.id)
  else emit('action', { act: 'preview', item: m })
}
</script>

<style scoped>
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 14px; margin-top: 16px; }
.mitem { border: 1px solid var(--el-border-color); border-radius: 8px; overflow: hidden; background: var(--el-bg-color); position: relative; transition: .15s; }
.mitem:hover { box-shadow: var(--el-box-shadow-light); border-color: var(--el-border-color-darker); }
.mitem.sel { border-color: var(--el-color-primary); box-shadow: 0 0 0 3px var(--el-color-primary-light-8); }
.thumb { height: 118px; background: var(--el-fill-color-light); position: relative; overflow: hidden; }
.meta { padding: 9px 10px; }
.fname { font-size: 12.5px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.fsub { font-size: 11px; color: var(--el-text-color-disabled); margin-top: 2px; display: flex; justify-content: space-between; }
.chk { position: absolute; top: 8px; left: 8px; width: 18px; height: 18px; border-radius: 5px; border: 1.5px solid #fff; background: rgba(0, 0, 0, .25); display: none; cursor: pointer; z-index: 2; }
.mitem:hover .chk, .chk.on { display: block; }
.mitem.sel .chk { background: var(--el-color-primary); border-color: var(--el-color-primary); }
.actions { position: absolute; top: 8px; right: 8px; display: none; gap: 4px; z-index: 2; }
.mitem:hover .actions { display: flex; }
.act { width: 26px; height: 26px; border-radius: 6px; background: rgba(255, 255, 255, .92); border: 1px solid var(--el-border-color); display: grid; place-items: center; cursor: pointer; font-size: 13px; color: var(--el-text-color-regular); }
.act:hover { background: var(--el-bg-color-overlay); color: var(--el-color-primary); }
/* keep the embedded copy button the same size as the other action chips */
.actions :deep(.mcb) { width: 26px; height: 26px; border-radius: 6px; background: rgba(255, 255, 255, .92); }
.actions :deep(.mcb:hover) { background: var(--el-bg-color-overlay); }
</style>
