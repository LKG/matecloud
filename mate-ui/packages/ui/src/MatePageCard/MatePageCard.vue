<template>
  <div class="mc-page">
    <div class="mc-page__card">
      <div v-if="title || $slots.header" class="mc-page__header">
        <slot name="header">
          <div class="mc-page__header-text">
            <h2 class="mc-page__title">{{ title }}</h2>
            <p v-if="description" class="mc-page__desc">{{ description }}</p>
          </div>
        </slot>
        <div v-if="$slots.actions" class="mc-page__actions">
          <slot name="actions" />
        </div>
      </div>
      <div class="mc-page__body">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title?: string
  description?: string
}>()
</script>

<style scoped>
.mc-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 8px;       /* 紧凑外边距:layout-content(8) + .mc-page(8) = 16px */
  /* 卡片填满视口,内部布局负责滚动(列表在表格内、其它在 body),
     这里 NOT overflow:auto —— 外围不再出现滚动条。 */
}
.mc-page__card {
  position: relative;
  /* 卡片填满视口可用区:让内部的 body/table 接管滚动(滚动条在卡片内,
     不在 .mc-page 外围),符合列表页的标准 ERP 体验。 */
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--mc-surface-overlay, rgba(255, 255, 255, 0.72));
  backdrop-filter: blur(12px) saturate(1.1);
  -webkit-backdrop-filter: blur(12px) saturate(1.1);
  border-radius: var(--mc-radius-xl, 16px);
  border: 1px solid var(--mc-border, rgb(16 24 40 / 0.08));
  box-shadow: var(--mc-shadow-soft, var(--mc-shadow));
  padding: 20px;      /* 略减 24→20,与紧凑外边距整体协调 */
  overflow: hidden;
}
.mc-page__card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: var(--mc-glow, none);
  opacity: 0.5;
  pointer-events: none;
  z-index: 0;
}
.mc-page__header {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}
.mc-page__header-text {
  flex: 1;
  min-width: 0;
}
.mc-page__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--mc-text-primary);
  margin: 0;
  line-height: 1.5;
}
.mc-page__desc {
  font-size: 13px;
  color: var(--mc-text-muted);
  margin: 2px 0 0;
  line-height: 1.5;
}
.mc-page__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.mc-page__body {
  position: relative;
  z-index: 1;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  /* 非表格页(如缓存监控)兜底:内容超出时由 body 滚动 —— 仍在卡片内、不在外围。
     列表页的表格用 height:100% 自己填满 + 内部滚动 + sticky 表头,body 不触发滚动。 */
  overflow-y: auto;
}

/* ---- Phone (≤768px) ----
   The header is a single flex row (title | actions). With several non-shrinking
   action buttons the title gets starved to ~1 char wide and wraps vertically.
   Stack the header so the title spans full width and actions drop below and
   wrap. Also trim the card padding to give content more room. */
@media (max-width: 768px) {
  .mc-page { padding: 6px; }
  .mc-page__card {
    padding: 14px;
    border-radius: var(--mc-radius-lg, 12px);
  }
  .mc-page__header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
    margin-bottom: 16px;
  }
  .mc-page__actions { flex-wrap: wrap; }
}
</style>
