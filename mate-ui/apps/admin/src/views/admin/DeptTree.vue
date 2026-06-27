<template>
  <MatePageCard :title="t('dept.title')" :description="t('dept.desc')">
    <div class="menu-layout">
      <!-- Left: tree -->
      <div class="tree-panel">
        <div class="tree-header">
          <span class="tree-title">{{ t('dept.tree') }}</span>
          <button class="tree-add-btn" @click="handleAddRoot">
            <Plus :size="14" />
            <span>{{ t('dept.addRoot') }}</span>
          </button>
        </div>

        <div v-if="treeLoading" class="tree-empty">
          <el-icon class="is-loading" :size="18"><Loading /></el-icon>
        </div>
        <div v-else-if="treeData.length" class="tree-body">
          <tree-node
            v-for="node in treeData"
            :key="node.id"
            :node="node"
            :active-id="currentNodeId"
            :depth="0"
            @select="handleNodeClick"
            @add-child="handleAddChild"
          />
        </div>
        <div v-else class="tree-empty">
          <p>{{ t('common.noData') }}</p>
        </div>
      </div>

      <!-- Right: form -->
      <div class="form-panel">
        <div class="form-header">
          <span class="form-title">{{ isEdit ? t('dept.editDept') : t('dept.createDept') }}</span>
          <MateBadge v-if="isEdit" type="info">ID: {{ currentNodeId }}</MateBadge>
          <MateBadge v-if="parentName" type="info">{{ t('dept.parent') }}: {{ parentName }}</MateBadge>
        </div>

        <el-form ref="formRef" :model="formModel" :rules="rules" label-position="top" class="menu-form">
          <div class="form-grid">
            <el-form-item :label="t('dept.deptName')" prop="deptName">
              <el-input v-model="formModel.deptName" />
            </el-form-item>
            <el-form-item :label="t('dept.sort')">
              <el-input-number v-model="formModel.sort" :min="0" style="width: 120px" />
            </el-form-item>
          </div>
          <div class="form-grid">
            <el-form-item :label="t('dept.leader')">
              <el-input v-model="formModel.leader" />
            </el-form-item>
            <el-form-item :label="t('dept.phone')">
              <el-input v-model="formModel.phone" />
            </el-form-item>
          </div>
          <div class="form-grid">
            <el-form-item :label="t('dept.email')">
              <el-input v-model="formModel.email" />
            </el-form-item>
            <el-form-item v-if="isEdit" :label="t('dept.status')">
              <el-switch
                v-model="formModel.status"
                :active-value="0"
                :inactive-value="1"
                :active-text="t('common.enable')"
                :inactive-text="t('common.disable')"
              />
            </el-form-item>
          </div>
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
import { ref, reactive, onMounted, computed, defineComponent, h } from 'vue'
import { Plus, FolderTree } from 'lucide-vue-next'

defineOptions({ name: 'DeptTreeView' })
import { useI18n } from 'vue-i18n'
import { Loading } from '@element-plus/icons-vue'
import { MatePageCard, MateBadge, MateMessage, MateMessageBox } from '@matecloud/ui'
import { adminApi } from '@matecloud/core'
import type { FormInstance } from 'element-plus'

const TreeNode: any = defineComponent({
  name: 'DeptTreeNode',
  props: {
    node: { type: Object, required: true },
    activeId: { type: String as any, default: null },
    depth: { type: Number, default: 0 },
  },
  emits: ['select', 'add-child'],
  setup(props, { emit }) {
    return () => {
      const node = props.node as any
      const isActive = node.id === props.activeId
      const hasChildren = node.children?.length > 0
      const item = h('div', {
        class: ['tree-node', isActive && 'tree-node--active'],
        style: { paddingLeft: `${12 + props.depth * 16}px` },
        onClick: (e: Event) => { e.stopPropagation(); emit('select', node) },
      }, [
        h(FolderTree, { size: 15, class: 'tree-node__icon' }),
        h('span', { class: 'tree-node__label' }, node.deptName),
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
              onSelect: (n: any) => emit('select', n),
              onAddChild: (n: any) => emit('add-child', n),
            }),
          )
        : []
      return h('div', [item, ...children])
    }
  },
})

const { t } = useI18n()
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const treeLoading = ref(false)
const treeData = ref<any[]>([])
const isEdit = ref(false)
const currentNodeId = ref<string | undefined>(undefined)
const parentName = ref('')

const formModel = reactive({
  deptName: '', parentId: '0', sort: 0,
  leader: '', phone: '', email: '', status: 0,
})

const rules = {
  deptName: [{ required: true, message: t('dept.deptNameRequired'), trigger: 'blur' }],
}

// flat index for resolving parent name display
const flat = computed(() => {
  const out: Record<string, any> = {}
  const walk = (nodes: any[]) => nodes.forEach(n => { out[n.id] = n; if (n.children) walk(n.children) })
  walk(treeData.value)
  return out
})

onMounted(() => loadTree())

async function loadTree() {
  treeLoading.value = true
  try {
    const res = await adminApi.deptTree()
    treeData.value = (res as any).data ?? res ?? []
  } catch {
    treeData.value = []
  } finally {
    treeLoading.value = false
  }
}

function handleNodeClick(data: any) {
  isEdit.value = true
  currentNodeId.value = data.id
  parentName.value = data.parentId && data.parentId !== '0' ? (flat.value[data.parentId]?.deptName ?? '') : ''
  Object.assign(formModel, {
    deptName: data.deptName || '', parentId: data.parentId || '0', sort: data.sort ?? 0,
    leader: data.leader || '', phone: data.phone || '', email: data.email || '',
    status: data.status ?? 0,
  })
}

function handleAddRoot() {
  resetForm()
  formModel.parentId = '0'
}

function handleAddChild(data: any) {
  resetForm()
  formModel.parentId = data.id
  parentName.value = data.deptName
}

function resetForm() {
  isEdit.value = false
  currentNodeId.value = undefined
  parentName.value = ''
  Object.assign(formModel, {
    deptName: '', parentId: '0', sort: 0, leader: '', phone: '', email: '', status: 0,
  })
  formRef.value?.resetFields()
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (isEdit.value && currentNodeId.value) {
      await adminApi.deptUpdate(currentNodeId.value, { ...formModel })
      MateMessage.success(t('common.updateSuccess'))
    } else {
      await adminApi.deptCreate({ ...formModel })
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
    await adminApi.deptDelete(currentNodeId.value)
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

/* Phone: stack the tree above the edit form and collapse 2-col fields. */
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
.tree-panel {
  width: 280px; flex-shrink: 0;
  border: 1px solid var(--mc-divider-regular, rgb(16 24 40 / 0.08));
  border-radius: var(--mc-radius);
  display: flex; flex-direction: column; overflow: hidden;
}
.tree-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid var(--mc-divider-subtle, rgb(16 24 40 / 0.04));
}
.tree-title { font-size: 13px; font-weight: 600; color: var(--mc-text-primary); }
.tree-add-btn {
  display: inline-flex; align-items: center; gap: 4px;
  border: none; background: none; cursor: pointer;
  font-size: 12px; font-weight: 500; color: var(--mc-primary);
  padding: 4px 8px; border-radius: 6px; transition: background 0.12s;
}
.tree-add-btn:hover { background: var(--mc-state-accent-hover, #EFF4FF); }
.tree-body { flex: 1; overflow-y: auto; padding: 4px 0; }
.tree-empty {
  flex: 1; display: flex; align-items: center; justify-content: center;
  color: var(--mc-text-muted); font-size: 13px; min-height: 120px;
}
:deep(.tree-node) {
  display: flex; align-items: center; height: 34px; padding-right: 8px;
  cursor: pointer; gap: 8px; transition: background 0.1s;
}
:deep(.tree-node:hover) { background: var(--mc-state-hover, rgb(200 206 218 / 0.2)); }
:deep(.tree-node--active) { background: var(--mc-menu-bg-active, rgb(21 90 239 / 0.08)) !important; }
:deep(.tree-node__icon) { flex-shrink: 0; color: var(--mc-text-muted); }
:deep(.tree-node--active .tree-node__icon) { color: var(--mc-primary); }
:deep(.tree-node__label) {
  flex: 1; min-width: 0; font-size: 13px; font-weight: 500;
  color: var(--mc-menu-text, #495464);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
:deep(.tree-node--active .tree-node__label) { color: var(--mc-menu-text-active, #18222F); font-weight: 600; }
:deep(.tree-node__add) {
  display: none; border: none; background: none; cursor: pointer; padding: 2px;
  color: var(--mc-text-muted); border-radius: 4px;
}
:deep(.tree-node:hover .tree-node__add) { display: flex; }
:deep(.tree-node__add:hover) { color: var(--mc-primary); background: var(--mc-state-accent-hover, #EFF4FF); }
.form-panel { flex: 1; min-width: 0; max-width: 560px; overflow-y: auto; }
.form-header { display: flex; align-items: center; gap: 10px; margin-bottom: 20px; flex-wrap: wrap; }
.form-title { font-size: 14px; font-weight: 600; color: var(--mc-text-primary); }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 4px 16px; }
.form-actions {
  margin-top: 24px; padding-top: 16px;
  border-top: 1px solid var(--mc-divider-subtle, rgb(16 24 40 / 0.04));
  display: flex; gap: 8px;
}
</style>
