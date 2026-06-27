<template>
  <aside class="cats">
    <div class="cat-list">
      <div class="cat" :class="{ active: active === '' }" @click="$emit('select', '')">
        <span class="nm">{{ t('material.allMaterials') }}</span><span class="n">{{ cat.total }}</span>
      </div>
      <div
        v-for="c in cat.categories"
        :key="c.id"
        class="cat"
        :class="{ active: active === c.id }"
        @click="$emit('select', c.id)"
      >
        <span class="nm">{{ c.name }}</span>
        <span class="n">{{ c.count }}</span>
        <MateDropdown trigger="click" @command="(cmd) => onCommand(cmd, c)" @click.stop>
          <MoreHorizontal :size="15" class="more" @click.stop />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="rename">{{ t('material.actRename') }}</el-dropdown-item>
              <el-dropdown-item command="delete" divided>{{ t('material.delete') }}</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </MateDropdown>
      </div>
      <div class="cat" :class="{ active: active === '0' }" @click="$emit('select', '0')">
        <span class="nm">{{ t('material.ungrouped') }}</span><span class="n">{{ cat.ungrouped }}</span>
      </div>
    </div>
    <div class="cat-add" @click="openCreate"><Plus :size="14" /> {{ t('material.addCategory') }}</div>

    <!-- 新建分组 / 重命名分组 —— 统一使用项目封装的 MateDialog -->
    <MateDialog
      v-model="createOpen"
      :title="t('material.newCategory')"
      width="420px"
      :confirm-text="t('material.create')"
      :cancel-text="t('material.cancel')"
      @submit="submitCreate"
    >
      <el-input
        ref="createInputRef"
        v-model="createName"
        :placeholder="t('material.categoryPlaceholder')"
        maxlength="20"
        show-word-limit
        clearable
        @keyup.enter="submitCreate"
      />
    </MateDialog>

    <MateDialog
      v-model="renameOpen"
      :title="t('material.renameCategory')"
      width="420px"
      :confirm-text="t('material.save')"
      :cancel-text="t('material.cancel')"
      @submit="submitRename"
    >
      <el-input
        ref="renameInputRef"
        v-model="renameName"
        :placeholder="t('material.categoryNewPlaceholder')"
        maxlength="20"
        show-word-limit
        clearable
        @keyup.enter="submitRename"
      />
    </MateDialog>
  </aside>
</template>

<script lang="ts" setup>
import { nextTick, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MoreHorizontal, Plus } from 'lucide-vue-next'
import type { InputInstance } from 'element-plus'
import { MateDialog, MateMessage, MateMessageBox, MateDropdown } from '@matecloud/ui'
import type { MaterialCategory, MaterialCategoryList } from '@matecloud/core'

const { t } = useI18n()

defineProps<{ cat: MaterialCategoryList; active: string }>()
const emit = defineEmits<{
  (e: 'select', id: string): void
  (e: 'create', name: string): void
  (e: 'rename', payload: { id: string; name: string }): void
  (e: 'delete', id: string): void
}>()

// ---- 新建分组 ----
const createOpen = ref(false)
const createName = ref('')
const createInputRef = ref<InputInstance>()

function openCreate() {
  createName.value = ''
  createOpen.value = true
  nextTick(() => createInputRef.value?.focus())
}
function submitCreate() {
  const name = createName.value.trim()
  if (!name) {
    MateMessage.warning(t('material.nameRequired'))
    return
  }
  emit('create', name)
  createOpen.value = false
}

// ---- 重命名分组 ----
const renameOpen = ref(false)
const renameName = ref('')
const renameId = ref('')
const renameInputRef = ref<InputInstance>()

function openRename(c: MaterialCategory) {
  renameId.value = c.id
  renameName.value = c.name
  renameOpen.value = true
  nextTick(() => renameInputRef.value?.focus())
}
function submitRename() {
  const name = renameName.value.trim()
  if (!name) {
    MateMessage.warning(t('material.nameRequired'))
    return
  }
  emit('rename', { id: renameId.value, name })
  renameOpen.value = false
}

async function onCommand(cmd: string, c: MaterialCategory) {
  if (cmd === 'rename') {
    openRename(c)
  } else if (cmd === 'delete') {
    try {
      await MateMessageBox.confirm(t('material.confirmDeleteCategory', { name: c.name }), t('material.tip'), {
        type: 'warning', confirmButtonText: t('material.delete'), cancelButtonText: t('material.cancel'),
      })
      emit('delete', c.id)
    } catch { /* cancelled */ }
  }
}
</script>

<style scoped>
.cats { width: 212px; flex: none; border-right: 1px solid var(--el-border-color); padding: 14px; display: flex; flex-direction: column; gap: 10px; }
.cat-list { flex: 1; display: flex; flex-direction: column; gap: 2px; overflow: auto; }
.cat { display: flex; align-items: center; gap: 8px; padding: 8px 10px; border-radius: 6px; cursor: pointer; font-size: 13px; color: var(--el-text-color-regular); }
.cat:hover { background: var(--el-fill-color-light); }
.cat.active { background: var(--el-color-primary-light-9); color: var(--el-color-primary); font-weight: 600; }
.cat .nm { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cat .n { font-size: 11px; color: var(--el-text-color-disabled); }
.cat .more { opacity: 0; color: var(--el-text-color-disabled); padding: 0 2px; }
.cat:hover .more { opacity: 1; }
.cat-add { border: 1px dashed var(--el-border-color); border-radius: 6px; padding: 8px; display: flex; align-items: center; justify-content: center; gap: 4px; color: var(--el-text-color-secondary); cursor: pointer; font-size: 13px; }
.cat-add:hover { border-color: var(--el-color-primary); color: var(--el-color-primary); }
</style>
