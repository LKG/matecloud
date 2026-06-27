<template>
  <div class="ai-session-item" :class="{ 'is-active': active }" @click="$emit('select')">
    <div class="ai-session-item__top">
      <span class="ai-session-item__title">{{ title }}</span>
      <span v-if="time" class="ai-session-item__time">{{ time }}</span>
    </div>
    <div class="ai-session-item__bottom">
      <slot name="badge">
        <span v-if="agentLabel" class="ai-session-item__agent">{{ agentLabel }}</span>
      </slot>
      <span v-if="messageCount != null" class="ai-session-item__count">{{ messageCount }} 条</span>
      <span v-if="model" class="ai-session-item__model">{{ model }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * AiSessionItem — sidebar list row for a chat session. Used by the chat
 * workspace's left rail and by the Library page's "Sessions" tab.
 *
 * Designed to be a thin presentational component; the caller owns selection
 * state and emits {@code select} on click.
 */
defineProps<{
  title: string
  active?: boolean
  agentLabel?: string
  /** Pre-formatted "5 分钟前" style relative time. */
  time?: string
  messageCount?: number
  model?: string
}>()

defineEmits<{ (e: 'select'): void }>()
</script>

<style scoped>
.ai-session-item {
  display: block;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.12s ease;
  border: 1px solid transparent;
}
.ai-session-item:hover {
  background: var(--mc-fill);
}
.ai-session-item.is-active {
  background: color-mix(in srgb, var(--mc-primary) 10%, transparent);
  border-color: color-mix(in srgb, var(--mc-primary) 25%, transparent);
}

.ai-session-item__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}
.ai-session-item__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--mc-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
.ai-session-item__time {
  font-size: 11px;
  color: var(--mc-text-disabled);
  flex-shrink: 0;
}

.ai-session-item__bottom {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: var(--mc-text-muted);
}
.ai-session-item__agent {
  font-weight: 600;
  color: var(--mc-primary);
}
.ai-session-item__count,
.ai-session-item__model {
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
}
</style>
