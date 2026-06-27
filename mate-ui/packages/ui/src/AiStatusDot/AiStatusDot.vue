<template>
  <span class="ai-status-dot" :class="['ai-status-dot--' + state]" :title="title">
    <span class="ai-status-dot__dot" />
    <span v-if="label" class="ai-status-dot__label">{{ label }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

/**
 * AiStatusDot — small status indicator for connection / install / runtime
 * states across AI pages: MCP server liveness, provider test result, agent
 * install progress, streaming connection.
 *
 * States:
 *   online    — green, solid (default)
 *   offline   — gray
 *   error     — red
 *   pending   — amber, pulsing
 *   streaming — blue, pulsing
 */
type State = 'online' | 'offline' | 'error' | 'pending' | 'streaming'

const props = withDefaults(defineProps<{
  state: State
  label?: string
  /** Tooltip override (defaults to label or state name). */
  tooltip?: string
}>(), {
  state: 'online',
})

const title = computed(() => props.tooltip ?? props.label ?? props.state)
</script>

<style scoped>
.ai-status-dot {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--mc-text-muted);
  line-height: 1;
}
.ai-status-dot__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  box-shadow: 0 0 0 2px var(--mc-bg-card, transparent);
}
.ai-status-dot--online .ai-status-dot__dot   { background: #17B26A; }
.ai-status-dot--offline .ai-status-dot__dot  { background: #98A2B3; }
.ai-status-dot--error .ai-status-dot__dot    { background: #F04438; }
.ai-status-dot--pending .ai-status-dot__dot,
.ai-status-dot--streaming .ai-status-dot__dot {
  animation: ai-status-pulse 1.4s ease-in-out infinite;
}
.ai-status-dot--pending .ai-status-dot__dot   { background: #F79009; }
.ai-status-dot--streaming .ai-status-dot__dot { background: #2E90FA; }

.ai-status-dot--online   { color: #067647; }
.ai-status-dot--offline  { color: var(--mc-text-muted); }
.ai-status-dot--error    { color: #B42318; }
.ai-status-dot--pending  { color: #B54708; }
.ai-status-dot--streaming{ color: #175CD3; }

@keyframes ai-status-pulse {
  0%, 100% { transform: scale(1);   opacity: 1; }
  50%      { transform: scale(1.4); opacity: 0.5; }
}
</style>
