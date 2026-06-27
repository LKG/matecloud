<script setup lang="ts">
/**
 * MateInfoBanner — 主流知识库产品风格软色调信息条 (RFC-053 #3).
 *
 * 替换:
 * - `<el-alert type="info" :closable="false" show-icon>` 在配置 / 详情页"页头说明"
 *   场景的厚重边框样式
 *
 * 设计要点 (镜像 主流对话产品 scopebar):
 * - 软底 8% 透明 + 18% 边框 (与 tone 同色)
 * - 10px 圆角, 左侧图标插槽 (lucide 13–14px)
 * - 不可关闭、无标题 -- 只承担"说明 / 上下文" 角色
 *
 * <MateInfoBanner tone="info">
 *   <template #icon><Globe :size="14" /></template>
 *   保存后对所有租户即时生效。
 * </MateInfoBanner>
 */
withDefaults(defineProps<{
  /** 色调; info/ok/warn/danger. 默认 info. */
  tone?: 'info' | 'ok' | 'warn' | 'danger'
}>(), { tone: 'info' })

defineSlots<{
  /** 左侧图标 (一般 lucide-vue-next 14px). */
  icon(): any
  /** 主文本. 内可有 <b> / <code>. */
  default(): any
}>()
</script>

<template>
  <div class="mc-banner" :class="`mc-banner--${tone}`">
    <span v-if="$slots.icon" class="mc-banner__ic"><slot name="icon" /></span>
    <span class="mc-banner__msg"><slot /></span>
  </div>
</template>

<style scoped>
.mc-banner {
  display: flex; align-items: flex-start; gap: 9px;
  padding: 11px 14px;
  border: 1px solid;
  border-radius: 10px;
  font-size: 12.5px;
  line-height: 1.55;
  color: var(--el-text-color-regular);
}
.mc-banner__ic { flex-shrink: 0; margin-top: 1px; }
.mc-banner__msg :deep(b) { color: var(--el-text-color-primary); }
.mc-banner__msg :deep(code) {
  font-family: 'SF Mono', 'JetBrains Mono', ui-monospace, monospace;
  font-size: 11.5px;
  background: rgba(148, 163, 184, 0.15);
  border-radius: 4px;
  padding: 1px 5px;
  color: var(--el-text-color-primary);
}

.mc-banner--info   { background: rgba(64, 110, 211, 0.06); border-color: rgba(64, 110, 211, 0.18); }
.mc-banner--info   .mc-banner__ic { color: #2451b8; }
.mc-banner--ok     { background: rgba(31, 122, 77, 0.06); border-color: rgba(31, 122, 77, 0.18); }
.mc-banner--ok     .mc-banner__ic { color: #1f7a4d; }
.mc-banner--warn   { background: rgba(169, 113, 15, 0.06); border-color: rgba(169, 113, 15, 0.18); }
.mc-banner--warn   .mc-banner__ic { color: #a9710f; }
.mc-banner--danger { background: rgba(187, 59, 44, 0.06); border-color: rgba(187, 59, 44, 0.18); }
.mc-banner--danger .mc-banner__ic { color: #bb3b2c; }
</style>
