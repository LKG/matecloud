<template>
  <span class="ai-tool-chip" :class="{ 'is-clickable': clickable }">
    <span class="ai-tool-chip__icon">⚙</span>
    <span class="ai-tool-chip__name">{{ toolName }}</span>
    <span v-if="server" class="ai-tool-chip__server">/ {{ server }}</span>
    <span v-if="durationMs != null" class="ai-tool-chip__time">{{ duration }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

/**
 * AiToolCallChip — inline chip showing a tool invocation inside a chat
 * transcript (e.g. {@code ⚙ list_users / database 124ms}). Visually
 * lightweight so a multi-call assistant turn doesn't blow up the layout.
 */
const props = withDefaults(defineProps<{
  toolName: string
  /** Optional MCP server / namespace the tool came from. */
  server?: string
  /** Latency in milliseconds for rendering as "124ms" or "1.2s". */
  durationMs?: number
  /** Render with a hover state to hint it's clickable. */
  clickable?: boolean
}>(), {
  clickable: false,
})

const duration = computed(() => {
  if (props.durationMs == null) return ''
  if (props.durationMs < 1000) return props.durationMs + 'ms'
  return (props.durationMs / 1000).toFixed(1) + 's'
})
</script>

<style scoped>
.ai-tool-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  font-size: 11px;
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
  background: var(--mc-fill);
  color: var(--mc-text-primary);
  border-radius: 6px;
  border: 1px solid var(--mc-border);
  line-height: 1.5;
}
.ai-tool-chip.is-clickable {
  cursor: pointer;
  transition: background 0.12s ease;
}
.ai-tool-chip.is-clickable:hover {
  background: color-mix(in srgb, var(--mc-primary) 8%, var(--mc-fill));
  border-color: color-mix(in srgb, var(--mc-primary) 30%, var(--mc-border));
}
.ai-tool-chip__icon  { opacity: 0.7; }
.ai-tool-chip__name  { font-weight: 600; }
.ai-tool-chip__server{ color: var(--mc-text-muted); }
.ai-tool-chip__time  { color: var(--mc-text-muted); margin-left: 4px; }
</style>
