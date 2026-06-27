<script setup lang="ts">
/**
 * MatePane — 主流知识库产品风格带头部条的内容容器 (RFC-053 #2).
 *
 * 用法 / 替换:
 * - 替换 `<el-card shadow="never">` 在配置 / 详情 / 列表分块场景的重复使用
 * - 替换"自造 border + radius + header bar"的 ~10 行 scoped style 模板
 *
 * 设计要点 (镜像 主流对话产品):
 * - 12px 圆角 + rgba(15 23 42 / 0.06) 软边
 * - 头部条带浅渐变背景 + 等宽 meta 标签 (schema key 或路径)
 * - tone variants: ok/warn/danger 给出色块头部 (tester verdict)
 *
 * <MatePane title="脱敏字段" meta="redaction" tone="default">
 *   <template #header-extra><MateTag variant="primary">3/4</MateTag></template>
 *   <template #header-actions>
 *     <el-button size="small">操作</el-button>
 *   </template>
 *   ...内容...
 * </MatePane>
 */
import { computed, ref, watch } from 'vue'

const props = withDefaults(defineProps<{
  /** 头部主标题. 可选; 若空则隐藏 header. */
  title?: string
  /** 头部右上 meta 标签 (一般是 schema key, 等宽字体显示). */
  meta?: string
  /** 头部色调; 默认 = 中性. ok/warn/danger 用于 verdict 或状态强调. */
  tone?: 'default' | 'ok' | 'warn' | 'danger'
  /** 主体 padding: default 14px / compact 10px / none (用于内嵌列表行). */
  bodyPadding?: 'default' | 'compact' | 'none'
  /** 紧凑模式: 减少外边距 (用于 dense 列表). */
  dense?: boolean
  /** 可折叠: 头部可点击收起/展开 body, 右侧出现 chevron. */
  collapsible?: boolean
  /** 初始折叠 (非受控). 受控时用 v-model:collapsed. */
  defaultCollapsed?: boolean
  /** 受控折叠状态 (传入即受控, 配合 update:collapsed). */
  collapsed?: boolean
}>(), {
  tone: 'default',
  bodyPadding: 'default',
  dense: false,
  collapsible: false,
  defaultCollapsed: false,
})

const emit = defineEmits<{ 'update:collapsed': [v: boolean] }>()

// Internal state is the single source of truth. NOTE: `collapsed` is a Boolean
// prop, and Vue casts an ABSENT boolean prop to `false` (not undefined) — so we
// can't detect "controlled vs uncontrolled" via `props.collapsed !== undefined`
// (that check is always true and would pin uncontrolled panes permanently open).
// Instead we seed from defaultCollapsed and sync from props.collapsed only when
// a real boolean is supplied (v-model usage); toggle() drives it otherwise.
const internalCollapsed = ref(props.defaultCollapsed)
watch(() => props.collapsed, (v) => {
  if (typeof v === 'boolean') internalCollapsed.value = v
}, { immediate: true })
const isCollapsed = computed(() => internalCollapsed.value)
function toggle() {
  if (!props.collapsible) return
  internalCollapsed.value = !internalCollapsed.value
  emit('update:collapsed', internalCollapsed.value)
}

defineSlots<{
  /** 头部主标题左侧图标 (一般 lucide-vue-next 13–14px). */
  'header-icon'(): any
  /** 头部主标题右侧附加 (一般 <MateTag> 副标或简短计数). */
  'header-extra'(): any
  /** 头部右上动作组 (按钮等). 设置时覆盖 meta 显示位置. */
  'header-actions'(): any
  /** 主体内容. */
  default(): any
}>()
</script>

<template>
  <section class="mc-pane" :class="[`mc-pane--${tone}`, { 'mc-pane--dense': dense, 'mc-pane--collapsed': collapsible && isCollapsed }]">
    <header v-if="title || $slots['header-icon'] || $slots['header-actions']"
            class="mc-pane__hd" :class="{ 'is-clickable': collapsible }"
            @click="toggle">
      <span class="mc-pane__title">
        <slot name="header-icon" />
        <span v-if="title">{{ title }}</span>
        <slot name="header-extra" />
      </span>
      <div v-if="$slots['header-actions']" class="mc-pane__actions" @click.stop>
        <slot name="header-actions" />
      </div>
      <span v-else-if="meta && !(collapsible && isCollapsed)" class="mc-pane__meta">{{ meta }}</span>
      <svg v-if="collapsible" class="mc-pane__chevron" :class="{ 'is-open': !isCollapsed }"
           viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor"
           stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="m6 9 6 6 6-6" />
      </svg>
    </header>
    <div v-show="!(collapsible && isCollapsed)" class="mc-pane__bd" :class="`mc-pane__bd--${bodyPadding}`">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.mc-pane {
  position: relative;
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 12px;
  background: var(--el-bg-color, #fff);
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  overflow: hidden;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}
.mc-pane--dense { border-radius: 10px; }

.mc-pane__hd {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 11px 14px;
  background: linear-gradient(180deg, var(--mc-fill-hover) 0%, transparent 100%);
  border-bottom: 1px solid var(--mc-border-light, rgba(15, 23, 42, 0.05));
}
.mc-pane__title {
  display: inline-flex; align-items: center; gap: 7px;
  flex: 1; min-width: 0;
  font-size: 13px; font-weight: 600;
  color: var(--el-text-color-primary);
  letter-spacing: -0.1px;
}

/* collapsible header */
.mc-pane__hd.is-clickable { cursor: pointer; user-select: none; }
.mc-pane__hd.is-clickable:hover { background: var(--mc-fill-hover); }
.mc-pane__chevron {
  flex-shrink: 0;
  color: var(--el-text-color-secondary);
  transition: transform 0.2s ease;
}
.mc-pane__chevron.is-open { transform: rotate(180deg); }
.mc-pane--collapsed .mc-pane__hd { border-bottom-color: transparent; }
.mc-pane__meta {
  font-family: 'SF Mono', 'JetBrains Mono', ui-monospace, monospace;
  font-size: 11px;
  color: var(--el-text-color-secondary);
  background: rgba(148, 163, 184, 0.12);
  padding: 1px 7px;
  border-radius: 4px;
  flex-shrink: 0;
}
.mc-pane__actions { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.mc-pane__bd--default { padding: 14px; }
.mc-pane__bd--compact { padding: 10px; }
.mc-pane__bd--none    { padding: 0; }

/* ===== tone variants ===== */
.mc-pane--ok .mc-pane__hd {
  background: linear-gradient(180deg, rgba(31, 122, 77, 0.08) 0%, rgba(31, 122, 77, 0.02) 100%);
  border-bottom-color: rgba(31, 122, 77, 0.15);
}
.mc-pane--ok .mc-pane__title { color: #1f7a4d; }
.mc-pane--ok .mc-pane__meta  { background: rgba(31, 122, 77, 0.1); color: #1f7a4d; }

.mc-pane--warn .mc-pane__hd {
  background: linear-gradient(180deg, rgba(169, 113, 15, 0.08) 0%, rgba(169, 113, 15, 0.02) 100%);
  border-bottom-color: rgba(169, 113, 15, 0.15);
}
.mc-pane--warn .mc-pane__title { color: #a9710f; }
.mc-pane--warn .mc-pane__meta  { background: rgba(169, 113, 15, 0.1); color: #a9710f; }

.mc-pane--danger .mc-pane__hd {
  background: linear-gradient(180deg, rgba(187, 59, 44, 0.08) 0%, rgba(187, 59, 44, 0.02) 100%);
  border-bottom-color: rgba(187, 59, 44, 0.15);
}
.mc-pane--danger .mc-pane__title { color: #bb3b2c; }
.mc-pane--danger .mc-pane__meta  { background: rgba(187, 59, 44, 0.1); color: #bb3b2c; }
</style>
