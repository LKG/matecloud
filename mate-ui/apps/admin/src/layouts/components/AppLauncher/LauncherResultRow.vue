<template>
  <button
    class="res-row"
    :class="{ 'is-active': active }"
    @click="$emit('select')"
    @mousemove="$emit('hover')"
  >
    <span class="res-icon" :class="{ 'res-icon--app': !!color }" :style="iconStyle">
      <component :is="icon" :size="16" />
    </span>
    <span class="res-main">
      <span class="res-label">
        <template v-for="(seg, i) in segments" :key="i">
          <mark v-if="seg.hit" class="res-hit">{{ seg.text }}</mark>
          <template v-else>{{ seg.text }}</template>
        </template>
      </span>
      <span v-if="sub" class="res-sub">{{ sub }}</span>
    </span>
    <kbd v-if="active" class="res-enter">↵</kbd>
  </button>
</template>

<script setup lang="ts">
import { computed, type Component } from 'vue'

const props = defineProps<{
  icon: Component
  label: string
  sub?: string
  query?: string
  color?: string
  active?: boolean
}>()
defineEmits<{ select: []; hover: [] }>()

const iconStyle = computed(() => (props.color ? { '--app-color': props.color } : undefined))

/** Split the label around the matched query so it can be highlighted. */
const segments = computed(() => {
  const q = (props.query ?? '').trim()
  if (!q) return [{ text: props.label, hit: false }]
  const lower = props.label.toLowerCase()
  const idx = lower.indexOf(q.toLowerCase())
  if (idx < 0) return [{ text: props.label, hit: false }]
  return [
    { text: props.label.slice(0, idx), hit: false },
    { text: props.label.slice(idx, idx + q.length), hit: true },
    { text: props.label.slice(idx + q.length), hit: false },
  ].filter((s) => s.text.length)
})
</script>

<style scoped>
.res-row {
  display: flex;
  align-items: center;
  gap: 11px;
  width: 100%;
  padding: 9px 12px;
  border: 1px solid transparent;
  border-radius: var(--mc-radius);
  background: transparent;
  cursor: pointer;
  text-align: left;
  font-family: inherit;
  transition: background 0.12s, border-color 0.12s;
}
.res-row.is-active {
  background: var(--mc-state-accent-active);
  border-color: rgba(var(--mc-primary-rgb), 0.18);
}

.res-icon {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--mc-primary);
  background: rgba(var(--mc-primary-rgb), 0.1);
}
/* App rows carry their brand color as a soft gradient tile. */
.res-icon--app {
  color: #fff;
  background: linear-gradient(135deg, var(--app-color), color-mix(in srgb, var(--app-color) 72%, #000));
  box-shadow: 0 4px 10px -3px color-mix(in srgb, var(--app-color) 60%, transparent);
}

.res-main { display: flex; flex-direction: column; min-width: 0; flex: 1; }
.res-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--mc-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.res-hit { background: transparent; color: var(--mc-primary); font-weight: 800; }
.res-sub {
  font-size: 11px;
  color: var(--mc-text-muted);
  margin-top: 1px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.res-enter {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--mc-primary);
  border: 1px solid rgba(var(--mc-primary-rgb), 0.25);
  border-radius: 5px;
  padding: 0 7px;
  line-height: 18px;
  background: var(--mc-bg-elevated);
}
</style>
