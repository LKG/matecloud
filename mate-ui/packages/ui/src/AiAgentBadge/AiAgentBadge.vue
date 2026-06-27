<template>
  <span class="ai-agent-badge" :class="sizeClass" :style="brandStyle" :title="title">
    <span class="ai-agent-badge__icon">{{ initials }}</span>
    <span class="ai-agent-badge__label">
      <slot>{{ label }}</slot>
    </span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

/**
 * AiAgentBadge — compact identity chip for a CLI agent (Claude Code, Codex,
 * Gemini CLI ...). Renders a 2-letter brand square + the agent's display name.
 * Color is auto-derived from the agent code so the same agent always gets the
 * same hue across pages (workspace tabs, library rows, agent management).
 *
 * Usage:
 *   <AiAgentBadge code="claude-code" label="Claude Code" />
 *   <AiAgentBadge code="codex" label="Codex CLI" size="lg" />
 */
const props = withDefaults(defineProps<{
  code: string
  label?: string
  size?: 'sm' | 'md' | 'lg'
  /** Override the auto-derived two-letter initials. */
  initialsOverride?: string
  /** Override the auto-derived brand color (any CSS color). */
  colorOverride?: string
}>(), {
  size: 'md',
})

/** Curated brand colors for the well-known agents, fallback hashes by code. */
const KNOWN: Record<string, { color: string; initials: string }> = {
  'claude-code': { color: '#D97706', initials: 'CC' },
  'codex':       { color: '#10A37F', initials: 'CX' },
  'gemini':      { color: '#4285F4', initials: 'GM' },
  'opencode':    { color: '#7C3AED', initials: 'OC' },
  'amp':         { color: '#EC4899', initials: 'AP' },
}

function hashHue(code: string): number {
  let h = 0
  for (let i = 0; i < code.length; i++) h = (h * 31 + code.charCodeAt(i)) >>> 0
  return h % 360
}

const initials = computed(() => {
  if (props.initialsOverride) return props.initialsOverride
  if (KNOWN[props.code]) return KNOWN[props.code].initials
  const cleaned = (props.code || '?').replace(/[^a-z0-9]/gi, '')
  return cleaned.slice(0, 2).toUpperCase()
})

const brandColor = computed(() => {
  if (props.colorOverride) return props.colorOverride
  if (KNOWN[props.code]) return KNOWN[props.code].color
  return `hsl(${hashHue(props.code || '')}, 70%, 50%)`
})

const sizeClass = computed(() => 'ai-agent-badge--' + props.size)

const brandStyle = computed(() => ({
  '--ai-agent-color': brandColor.value,
}))

const title = computed(() => `${props.label || props.code} (${props.code})`)
</script>

<style scoped>
.ai-agent-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 8px 2px 2px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--ai-agent-color) 12%, transparent);
  color: var(--ai-agent-color);
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
  cursor: inherit;
}
.ai-agent-badge__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--ai-agent-color);
  color: #fff;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
.ai-agent-badge--sm { font-size: 11px; padding: 1px 6px 1px 1px; }
.ai-agent-badge--sm .ai-agent-badge__icon { width: 16px; height: 16px; font-size: 9px; }

.ai-agent-badge--md { font-size: 12px; }
.ai-agent-badge--md .ai-agent-badge__icon { width: 18px; height: 18px; }

.ai-agent-badge--lg { font-size: 13px; padding: 3px 10px 3px 3px; }
.ai-agent-badge--lg .ai-agent-badge__icon { width: 22px; height: 22px; font-size: 11px; }
</style>
