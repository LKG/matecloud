<template>
  <div class="crm-adv">
    <!-- 我的常用筛选: 套用 / 另存 / 删除 -->
    <el-dropdown trigger="click" :teleported="false" @visible-change="(v: boolean) => v && loadSaved()" @command="onSavedCommand">
      <MateButton variant="secondary" class="crm-adv__saved">
        我的筛选<ArrowDown :size="13" class="crm-adv__caret" />
      </MateButton>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item v-if="!savedFilters.length" disabled>暂无保存的筛选</el-dropdown-item>
          <el-dropdown-item v-for="sf in savedFilters" :key="sf.id" :command="'apply:' + sf.id">
            <span class="crm-adv__saved-item">
              {{ sf.name }}
              <button class="crm-adv__saved-del" @click.stop="handleRemoveSaved(sf)">✕</button>
            </span>
          </el-dropdown-item>
          <el-dropdown-item command="__save__" divided>另存为常用筛选…</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <MateButton :variant="expanded ? 'primary' : 'secondary'" @click="toggle">
      <template #icon><SlidersHorizontal :size="14" /></template>
      高级筛选
      <span v-if="activeCount" class="crm-adv__badge">{{ activeCount }}</span>
    </MateButton>

    <div v-if="expanded" class="crm-adv__panel">
        <div class="crm-adv__panel-head">
          <span class="crm-adv__panel-title">条件组之间 OR, 组内条件 AND</span>
          <button class="crm-adv__clear" @click="handleClear">清空条件</button>
        </div>
        <MateConditionBuilder v-if="fields.length" v-model="groups" :fields="fields" />
        <span v-else class="crm-adv__empty">该模块暂未配置可查字段</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowDown, SlidersHorizontal } from 'lucide-vue-next'
import type { ConditionGroup, SavedFilterVO } from '@matecloud/core'
import { MateButton, MateConditionBuilder, MateMessage, MateMessageBox } from '@matecloud/ui'
import { useCrmAdvancedQuery } from '../../composables/useCrmAdvancedQuery'

/**
 * CrmAdvancedFilter — 列表页"高级筛选"折叠面板 + "我的筛选"下拉。
 *
 * v-model 绑定高级条件组(组间 OR); 页面在 loadData 里用 buildAdvancedQuery
 * 把快捷栏与这些组一起折算成 AdvancedQuery。套用/清空后 emit('search') 触发重查。
 */
const props = defineProps<{ module: string }>()
const groups = defineModel<ConditionGroup[]>({ required: true })
const emit = defineEmits<{ search: [] }>()

const { fields, savedFilters, loadFields, loadSaved, saveAs, removeSaved, applySaved } =
  useCrmAdvancedQuery(props.module)

const expanded = ref(false)

const activeCount = computed(() =>
  (groups.value || []).reduce((n, g) => n + (g.conditions || []).filter((c) => c.fieldKey).length, 0),
)

function toggle() {
  expanded.value = !expanded.value
  if (expanded.value && !fields.value.length) loadFields()
}

function onSavedCommand(cmd: string) {
  if (cmd === '__save__') {
    handleSaveAs()
    return
  }
  if (cmd.startsWith('apply:')) {
    const sf = savedFilters.value.find((s) => s.id === cmd.slice(6))
    if (sf) {
      applySaved(sf)
      expanded.value = true
      emit('search')
    }
  }
}

async function handleSaveAs() {
  if (!activeCount.value) {
    MateMessage.warning('请先设置筛选条件')
    return
  }
  try {
    const { value } = await MateMessageBox.prompt('输入筛选名称', '另存为常用筛选', {
      inputPattern: /\S+/,
      inputErrorMessage: '名称不能为空',
    })
    if (await saveAs(String(value).trim())) MateMessage.success('已保存')
    else MateMessage.error('保存失败')
  } catch { /* cancelled */ }
}

async function handleRemoveSaved(sf: SavedFilterVO) {
  try {
    await MateMessageBox.confirm(`删除常用筛选「${sf.name}」?`, '提示', { type: 'warning' })
  } catch { return }
  if (await removeSaved(sf.id)) MateMessage.success('已删除')
}

function handleClear() {
  groups.value = []
  emit('search')
}

defineExpose({ loadSaved })

defineOptions({ name: 'CrmAdvancedFilter' })

onMounted(() => {
  // 元数据懒加载: 展开时才拉; 常用筛选下拉打开时拉。这里预热一次条件组容器。
  if (!groups.value) groups.value = []
})
</script>

<style scoped>
.crm-adv {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.crm-adv__caret {
  margin-left: 4px;
}
.crm-adv__badge {
  margin-left: 6px;
  background: var(--el-color-primary, #409eff);
  color: #fff;
  border-radius: 8px;
  font-size: 11px;
  padding: 0 6px;
  line-height: 16px;
}
.crm-adv__saved-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.crm-adv__saved-del {
  border: none;
  background: none;
  cursor: pointer;
  color: var(--el-text-color-secondary, #909399);
  font-size: 12px;
}
.crm-adv__saved-del:hover {
  color: var(--el-color-danger, #f56c6c);
}
.crm-adv__panel {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  z-index: 20;
  border: 1px solid var(--el-border-color-lighter, #e4e7ed);
  border-radius: 10px;
  background: var(--el-bg-color, #fff);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
  padding: 12px 14px;
}
.crm-adv__panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.crm-adv__panel-title {
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
}
.crm-adv__clear {
  border: none;
  background: none;
  cursor: pointer;
  font-size: 12px;
  color: var(--el-color-danger, #f56c6c);
}
.crm-adv__empty {
  font-size: 13px;
  color: var(--el-text-color-secondary, #909399);
}
</style>
