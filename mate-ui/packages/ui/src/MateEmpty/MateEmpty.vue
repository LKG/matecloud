<template>
  <div class="mc-empty" :style="{ padding: `${padding}px 0` }">
    <slot name="icon" />
    <p class="mc-empty__text"><slot>{{ text }}</slot></p>
    <div v-if="$slots.action" class="mc-empty__action">
      <slot name="action" />
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * MateEmpty — minimal empty-state placeholder.
 * Replaces the inline {@code <div style="padding:48px 0; color:var(--mc-text-muted);">No data</div>}
 * pattern that appeared in 6 CRUD views.
 */
withDefaults(defineProps<{
  text?: string
  padding?: number
}>(), {
  text: '暂无数据',
  padding: 48,
})
</script>

<style scoped>
.mc-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--mc-text-muted);
  font-size: 13px;
  text-align: center;
}
.mc-empty__text {
  margin: 0;
}
.mc-empty__action {
  margin-top: 8px;
}
</style>
