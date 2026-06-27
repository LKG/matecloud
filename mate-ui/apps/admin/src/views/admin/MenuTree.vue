<template>
  <MatePageCard :title="t('menu.title')" :description="t('menu.menuDesc') || 'Configure navigation menus, routes, and permissions'">
    <div class="menu-layout">
      <!-- Left: Tree -->
      <div class="tree-panel">
        <div class="tree-header">
          <span class="tree-title">{{ t('menu.tree') || 'Menu Tree' }}</span>
          <button class="tree-add-btn" @click="handleAddRoot">
            <Plus :size="14" />
            <span>{{ t('menu.addRoot') }}</span>
          </button>
        </div>

        <!-- Loading -->
        <div v-if="treeLoading" class="tree-empty">
          <el-icon class="is-loading" :size="18"><Loading /></el-icon>
        </div>

        <!-- Tree nodes -->
        <div v-else-if="treeData.length" class="tree-body">
          <tree-node
            v-for="node in treeData"
            :key="node.id"
            :node="node"
            :active-id="currentNodeId"
            :depth="0"
            :lang="locale"
            @select="handleNodeClick"
            @add-child="handleAddChild"
          />
        </div>

        <div v-else class="tree-empty">
          <p>{{ t('common.noData') }}</p>
        </div>
      </div>

      <!-- Right: Detail form -->
      <div class="form-panel">
        <div class="form-header">
          <span class="form-title">{{ isEdit ? t('menu.editMenu') : t('menu.createMenu') }}</span>
          <MateBadge v-if="isEdit" type="info">ID: {{ currentNodeId }}</MateBadge>
        </div>

        <el-form ref="formRef" :model="formModel" :rules="rules" label-position="top" class="menu-form">
          <div class="form-grid">
            <el-form-item :label="t('menu.name')" prop="name">
              <el-input v-model="formModel.name" :placeholder="t('menu.namePlaceholder') || '中文菜单名'" />
            </el-form-item>
            <el-form-item :label="t('menu.nameEn')" prop="nameEn">
              <el-input v-model="formModel.nameEn" :placeholder="t('menu.nameEnPlaceholder') || 'English menu name'" />
            </el-form-item>
          </div>
          <div class="form-grid">
            <el-form-item :label="t('menu.shortName')" prop="shortName">
              <el-input v-model="formModel.shortName" :placeholder="t('menu.shortNamePlaceholder') || '分栏窄栏显示用,如 系统'" maxlength="16" show-word-limit />
            </el-form-item>
            <el-form-item />
          </div>
          <div class="form-grid">
            <el-form-item :label="t('menu.type')" prop="type">
              <el-select v-model="formModel.type" style="width: 100%">
                <el-option :label="t('menu.typeDirectory')" value="M" />
                <el-option :label="t('menu.typeMenu')" value="C" />
                <el-option :label="t('menu.typeButton')" value="F" />
              </el-select>
            </el-form-item>
            <el-form-item />
          </div>
          <div class="form-grid">
            <el-form-item :label="t('menu.path')">
              <el-input v-model="formModel.path" placeholder="/system/users" />
            </el-form-item>
            <el-form-item :label="t('menu.component')">
              <el-input v-model="formModel.component" placeholder="system/UserList" />
            </el-form-item>
          </div>
          <div class="form-grid">
            <el-form-item :label="t('menu.perms')">
              <el-input v-model="formModel.perms" placeholder="system:user:list" />
            </el-form-item>
            <el-form-item :label="t('menu.icon')">
              <IconPicker v-model="formModel.icon" />
            </el-form-item>
          </div>
          <el-form-item :label="t('menu.sort')">
            <el-input-number v-model="formModel.sort" :min="0" style="width: 120px" />
          </el-form-item>
        </el-form>

        <div class="form-actions">
          <el-button type="primary" :loading="submitLoading" @click="handleSave">
            {{ isEdit ? t('common.update') : t('common.create') }}
          </el-button>
          <el-button v-if="isEdit" type="danger" :loading="submitLoading" @click="handleDelete">
            {{ t('common.delete') }}
          </el-button>
          <el-button @click="resetForm">{{ t('common.reset') }}</el-button>
        </div>
      </div>
    </div>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, defineComponent, h, markRaw, type Component } from 'vue'
import { Plus, FolderOpen, FileText, Zap } from 'lucide-vue-next'
import * as LucideIcons from 'lucide-vue-next'
import * as EpIcons from '@element-plus/icons-vue'

defineOptions({ name: 'MenuTreeView' })
import { useI18n } from 'vue-i18n'
import { Loading } from '@element-plus/icons-vue'
import { MatePageCard, MateBadge, MateMessage, MateMessageBox } from '@matecloud/ui'
import { IconPicker } from '@/components/IconPicker'
import { adminApi, type MenuType } from '@matecloud/core'
import type { FormInstance } from 'element-plus'

// Recursive tree node — declared locally so the SFC has a single default export.
const TreeNode: any = defineComponent({
  name: 'TreeNode',
  props: {
    node: { type: Object, required: true },
    activeId: { type: String as any, default: null },
    depth: { type: Number, default: 0 },
    lang: { type: String, default: 'zh-CN' },
  },
  emits: ['select', 'add-child'],
  setup(props, { emit }) {
    const fallbackIcon = (type: string) => {
      if (type === 'M' || type === 'DIRECTORY') return FolderOpen
      if (type === 'F' || type === 'BUTTON') return Zap
      return FileText
    }

    // Resolve icon name → component (Lucide first, then Element Plus)
    const iconCache = new Map<string, Component>()
    const resolveIcon = (name: string): Component | null => {
      if (iconCache.has(name)) return iconCache.get(name)!
      const comp = (LucideIcons as unknown as Record<string, Component>)[name]
        ?? (EpIcons as Record<string, Component>)[name]
      if (comp) {
        const raw = markRaw(comp)
        iconCache.set(name, raw)
        return raw
      }
      return null
    }

    const nodeLabel = (node: any) =>
      props.lang === 'en-US' && node.nameEn ? node.nameEn : node.name

    return () => {
      const node = props.node as any
      const isActive = node.id === props.activeId
      const hasChildren = node.children?.length > 0

      // Use the node's icon field if set, otherwise fall back to type-based icon
      const iconComp = node.icon ? resolveIcon(node.icon) : null
      const displayIcon = iconComp || fallbackIcon(node.type)

      const item = h('div', {
        class: ['tree-node', isActive && 'tree-node--active'],
        style: { paddingLeft: `${12 + props.depth * 16}px` },
        onClick: (e: Event) => { e.stopPropagation(); emit('select', node) },
      }, [
        h(displayIcon, { size: 15, class: 'tree-node__icon' }),
        h('span', { class: 'tree-node__label' }, nodeLabel(node)),
        h('button', {
          class: 'tree-node__add',
          onClick: (e: Event) => { e.stopPropagation(); emit('add-child', node) },
        }, [h(Plus, { size: 12 })]),
      ])

      const children = hasChildren
        ? node.children.map((child: any) =>
            h(TreeNode, {
              node: child,
              activeId: props.activeId,
              depth: props.depth + 1,
              lang: props.lang,
              onSelect: (n: any) => emit('select', n),
              onAddChild: (n: any) => emit('add-child', n),
            }),
          )
        : []

      return h('div', [item, ...children])
    }
  },
})

const { t, locale } = useI18n()
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const treeLoading = ref(false)
const treeData = ref<any[]>([])
const isEdit = ref(false)
const currentNodeId = ref<string | undefined>(undefined)

const formModel = reactive({
  name: '', nameEn: '', shortName: '', parentId: '', path: '', component: '',
  perms: '', type: 'C' as MenuType, icon: '', sort: 0,
})

const rules = {
  name: [{ required: true, message: t('menu.nameRequired'), trigger: 'blur' }],
  type: [{ required: true, message: t('menu.typeRequired'), trigger: 'change' }],
}

onMounted(() => loadTree())

async function loadTree() {
  treeLoading.value = true
  try {
    const res = await adminApi.menuTree()
    treeData.value = res.data ?? res ?? []
  } catch {
    treeData.value = []
  } finally {
    treeLoading.value = false
  }
}

function handleNodeClick(data: any) {
  isEdit.value = true
  currentNodeId.value = data.id
  Object.assign(formModel, {
    name: data.name || '', nameEn: data.nameEn || '', shortName: data.shortName || '',
    parentId: data.parentId || '',
    path: data.path || '', component: data.component || '',
    perms: data.perms || '', type: data.type || 'C',
    icon: data.icon || '', sort: data.sort ?? 0,
  })
}

function handleAddRoot() {
  resetForm()
  formModel.parentId = ''
}

function handleAddChild(data: any) {
  resetForm()
  formModel.parentId = data.id
}

function resetForm() {
  isEdit.value = false
  currentNodeId.value = undefined
  Object.assign(formModel, {
    name: '', nameEn: '', shortName: '', parentId: '', path: '', component: '',
    perms: '', type: 'C', icon: '', sort: 0,
  })
  formRef.value?.resetFields()
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (isEdit.value && currentNodeId.value) {
      await adminApi.menuUpdate(currentNodeId.value, { ...formModel })
      MateMessage.success(t('common.updateSuccess'))
    } else {
      await adminApi.menuCreate({ ...formModel })
      MateMessage.success(t('common.createSuccess'))
    }
    await loadTree()
    resetForm()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete() {
  if (!currentNodeId.value) return
  try {
    await MateMessageBox.confirm(t('common.deleteConfirm'), t('common.warning'), { type: 'warning' })
    await adminApi.menuDelete(currentNodeId.value)
    MateMessage.success(t('common.deleteSuccess'))
    await loadTree()
    resetForm()
  } catch { /* cancelled */ }
}
</script>

<style scoped>
.menu-layout {
  display: flex; gap: 20px;
  height: calc(100vh - var(--mc-header-height) - var(--mc-tab-height) - 140px);
  min-height: 360px;
}

/* Phone: stack the tree above the edit form (they don't fit side by side),
   cap the tree height, and collapse the 2-col form fields to a single column. */
@media (max-width: 768px) {
  .menu-layout {
    flex-direction: column;
    height: auto;
    min-height: 0;
    gap: 12px;
  }
  .tree-panel { width: auto; }
  .tree-body { max-height: 40vh; }
  .form-panel { max-width: none; }
  .form-grid { grid-template-columns: 1fr; }
}

/* ---- Tree panel ---- */
.tree-panel {
  width: 280px; flex-shrink: 0;
  border: 1px solid var(--mc-divider-regular, rgb(16 24 40 / 0.08));
  border-radius: var(--mc-radius);
  display: flex; flex-direction: column;
  overflow: hidden;
}
.tree-header {
  display: flex; align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid var(--mc-divider-subtle, rgb(16 24 40 / 0.04));
}
.tree-title {
  font-size: 13px; font-weight: 600;
  color: var(--mc-text-primary);
}
.tree-add-btn {
  display: inline-flex; align-items: center; gap: 4px;
  border: none; background: none; cursor: pointer;
  font-size: 12px; font-weight: 500;
  color: var(--mc-primary);
  padding: 4px 8px; border-radius: 6px;
  transition: background 0.12s;
}
.tree-add-btn:hover {
  background: var(--mc-state-accent-hover, #EFF4FF);
}

.tree-body {
  flex: 1; overflow-y: auto; padding: 4px 0;
}
.tree-empty {
  flex: 1; display: flex; align-items: center;
  justify-content: center;
  color: var(--mc-text-muted); font-size: 13px;
  min-height: 120px;
}

/* Tree node items */
:deep(.tree-node) {
  display: flex; align-items: center;
  height: 34px; padding-right: 8px;
  cursor: pointer; gap: 8px;
  transition: background 0.1s;
  border-radius: 0;
}
:deep(.tree-node:hover) {
  background: var(--mc-state-hover, rgb(200 206 218 / 0.2));
}
:deep(.tree-node--active) {
  background: var(--mc-menu-bg-active, rgb(21 90 239 / 0.08)) !important;
}
:deep(.tree-node__icon) {
  flex-shrink: 0;
  color: var(--mc-text-muted);
}
:deep(.tree-node--active .tree-node__icon) {
  color: var(--mc-primary);
}
:deep(.tree-node__label) {
  flex: 1; min-width: 0;
  font-size: 13px; font-weight: 500;
  color: var(--mc-menu-text, #495464);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
:deep(.tree-node--active .tree-node__label) {
  color: var(--mc-menu-text-active, #18222F);
  font-weight: 600;
}
:deep(.tree-node__add) {
  display: none; border: none; background: none;
  cursor: pointer; padding: 2px;
  color: var(--mc-text-muted); border-radius: 4px;
}
:deep(.tree-node:hover .tree-node__add) {
  display: flex;
}
:deep(.tree-node__add:hover) {
  color: var(--mc-primary);
  background: var(--mc-state-accent-hover, #EFF4FF);
}

/* ---- Form panel ---- */
.form-panel {
  flex: 1; min-width: 0; max-width: 560px;
  overflow-y: auto;
}
.form-header {
  display: flex; align-items: center; gap: 10px;
  margin-bottom: 20px;
}
.form-title {
  font-size: 14px; font-weight: 600;
  color: var(--mc-text-primary);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 16px;
}

.form-actions {
  margin-top: 24px; padding-top: 16px;
  border-top: 1px solid var(--mc-divider-subtle, rgb(16 24 40 / 0.04));
  display: flex; gap: 8px;
}
</style>
