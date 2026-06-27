<script setup lang="ts">
/**
 * MateSettingsRow — 主流知识库产品风格设置项行 (RFC-053 新条目).
 *
 * 替换 "label + hint + 右侧控件 (开关 / 按钮 / 简短输入)" 在 30+ 处设置页的
 * 自造 div + 自造 css. 抽出后整站统一节奏: 16-18px padding, 12-13px label,
 * 11.5-12px hint, 软边 + 行 hover.
 *
 * <MateSettingsRow label="启用智能体" hint="关闭后前台不展示此智能体">
 *   <ElSwitch v-model="enabled" />
 * </MateSettingsRow>
 *
 * <MateSettingsRow label="默认入口" hint="租户内最多一个">
 *   <template #icon><Star :size="14" /></template>
 *   <ElSwitch v-model="isDefault" />
 * </MateSettingsRow>
 */
withDefaults(defineProps<{
  /** 主标题 (左侧). */
  label: string
  /** 副提示 (副标题). 可选. */
  hint?: string
  /** 紧凑模式 (上下 padding 减少). */
  dense?: boolean
  /** 状态: warn 提示色块 (橙色边框 + 浅黄底). 用于 "数据库优先" 等冲突态. */
  tone?: 'default' | 'warn' | 'danger'
}>(), {
  dense: false,
  tone: 'default',
})

defineSlots<{
  /** 主标题左侧图标. */
  icon(): any
  /** 右侧控件: ElSwitch / ElButton / 简短输入框. */
  default(): any
}>()
</script>

<template>
  <div class="mc-srow" :class="[`mc-srow--${tone}`, { 'mc-srow--dense': dense }]">
    <div class="mc-srow__main">
      <div class="mc-srow__title">
        <slot name="icon" />
        <span class="mc-srow__label">{{ label }}</span>
      </div>
      <div v-if="hint" class="mc-srow__hint">{{ hint }}</div>
    </div>
    <div class="mc-srow__ctrl">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.mc-srow {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 12px 14px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 10px;
  background: var(--el-bg-color, #fff);
  transition: background 0.18s ease, border-color 0.18s ease;
}
.mc-srow:hover { background: var(--mc-fill-hover); }
.mc-srow--dense { padding: 9px 12px; border-radius: 8px; }
.mc-srow--warn {
  border-color: rgba(169, 113, 15, 0.2);
  background: rgba(169, 113, 15, 0.04);
}
.mc-srow--danger {
  border-color: rgba(187, 59, 44, 0.2);
  background: rgba(187, 59, 44, 0.04);
}
.mc-srow__main { flex: 1; min-width: 0; }
.mc-srow__title {
  display: flex; align-items: center; gap: 7px;
  font-size: 13px; font-weight: 600;
  color: var(--el-text-color-primary);
}
.mc-srow__label { letter-spacing: -0.1px; }
.mc-srow__hint {
  margin-top: 3px;
  font-size: 11.5px; color: var(--el-text-color-secondary);
  line-height: 1.5;
}
.mc-srow__ctrl { flex-shrink: 0; display: flex; align-items: center; gap: 6px; }
</style>
