<template>
  <button
    class="app-card"
    :class="{ 'app-card--active': active }"
    :style="{ '--app-color': color }"
    @click="$emit('select')"
  >
    <span class="app-card__icon">
      <component :is="icon" :size="18" />
    </span>
    <span class="app-card__meta">
      <span class="app-card__name">{{ label }}</span>
      <span v-if="desc" class="app-card__desc">{{ desc }}</span>
    </span>
    <span
      v-if="pinnable"
      class="app-card__star"
      :class="{ 'is-on': pinned }"
      :title="pinned ? t('layout.unpin') : t('layout.pinToBar')"
      @click.stop="$emit('toggle-pin')"
    >
      <Star :size="14" />
    </span>
  </button>
</template>

<script setup lang="ts">
import type { Component } from 'vue'
import { useI18n } from 'vue-i18n'
import { Star } from 'lucide-vue-next'

defineProps<{
  icon: Component
  color: string
  label: string
  desc?: string
  active?: boolean
  pinned?: boolean
  pinnable?: boolean
}>()
defineEmits<{ select: []; 'toggle-pin': [] }>()

const { t } = useI18n()
</script>

<style scoped>
.app-card {
  display: flex;
  align-items: center;
  gap: 11px;
  width: 100%;
  padding: 12px;
  border: 1px solid var(--mc-divider-subtle, rgb(16 24 40 / 0.05));
  border-radius: var(--mc-radius-lg);
  background: var(--mc-surface-overlay, rgba(255, 255, 255, 0.6));
  cursor: pointer;
  position: relative;
  text-align: left;
  font-family: inherit;
  overflow: hidden;
  transition: border-color 0.16s, box-shadow 0.16s, transform 0.16s, background 0.16s;
}
/* A faint wash of the app's brand color reveals on hover for a lively feel. */
.app-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, color-mix(in srgb, var(--app-color) 10%, transparent), transparent 60%);
  opacity: 0;
  transition: opacity 0.16s;
  pointer-events: none;
}
.app-card:hover {
  border-color: color-mix(in srgb, var(--app-color) 45%, transparent);
  box-shadow: 0 8px 22px -10px color-mix(in srgb, var(--app-color) 55%, transparent);
  transform: translateY(-1px);
}
.app-card:hover::before { opacity: 1; }
.app-card--active {
  border-color: color-mix(in srgb, var(--app-color) 55%, transparent);
  background: var(--mc-bg-elevated);
}

.app-card__icon {
  position: relative;
  z-index: 1;
  width: 38px;
  height: 38px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--app-color), color-mix(in srgb, var(--app-color) 70%, #000));
  box-shadow: 0 5px 14px -5px color-mix(in srgb, var(--app-color) 70%, transparent),
    inset 0 1px 0 rgba(255, 255, 255, 0.25);
}
.app-card__meta { position: relative; z-index: 1; display: flex; flex-direction: column; min-width: 0; }
.app-card__name {
  font-size: 13px; font-weight: 700; color: var(--mc-text-primary);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.app-card__desc {
  font-size: 11px; color: var(--mc-text-muted); margin-top: 2px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

.app-card__star {
  position: absolute; top: 8px; right: 9px; z-index: 2;
  display: inline-flex; color: var(--mc-text-disabled);
  opacity: 0; transition: opacity 0.12s, color 0.12s;
}
.app-card:hover .app-card__star { opacity: 1; }
.app-card__star.is-on { opacity: 1; color: var(--mc-warning); }
.app-card__star.is-on :deep(svg) { fill: currentColor; }
</style>
