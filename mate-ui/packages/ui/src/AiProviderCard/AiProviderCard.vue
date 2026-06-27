<template>
  <div class="ai-provider-card" :class="{ 'is-default': isDefault }">
    <div class="ai-provider-card__head">
      <div class="ai-provider-card__logo" :style="brandStyle">{{ initials }}</div>
      <div class="ai-provider-card__title">
        <span class="ai-provider-card__name">{{ name }}</span>
        <span class="ai-provider-card__vendor">{{ vendor }}</span>
      </div>
      <span v-if="isDefault" class="ai-provider-card__default">默认</span>
    </div>

    <dl class="ai-provider-card__meta">
      <div v-if="model">
        <dt>默认模型</dt>
        <dd>{{ model }}</dd>
      </div>
      <div v-if="apiKeyMasked">
        <dt>API Key</dt>
        <dd class="is-mono">{{ apiKeyMasked }}</dd>
      </div>
      <div v-if="baseUrl">
        <dt>Base URL</dt>
        <dd class="is-mono ai-provider-card__url">{{ baseUrl }}</dd>
      </div>
    </dl>

    <div v-if="$slots.actions" class="ai-provider-card__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

/**
 * AiProviderCard — visual card for an LLM provider entry in the
 * "Models &amp; Keys" page. Renders brand logo (initials), name + vendor,
 * masked API key (never the real value), default model and a footer slot
 * for actions (Edit / Test / Set default / Delete).
 */
const VENDOR_COLOR: Record<string, string> = {
  OPENAI:    '#10A37F',
  ANTHROPIC: '#D97706',
  GOOGLE:    '#4285F4',
  DEEPSEEK:  '#7C3AED',
  CUSTOM:    '#475467',
}

const props = defineProps<{
  name: string
  vendor: string
  model?: string
  apiKeyMasked?: string
  baseUrl?: string
  isDefault?: boolean
}>()

const initials = computed(() => {
  const cleaned = (props.name || props.vendor || '?').replace(/[^a-z0-9]/gi, '')
  return cleaned.slice(0, 2).toUpperCase()
})

const brandStyle = computed(() => ({
  background: VENDOR_COLOR[(props.vendor || '').toUpperCase()] || VENDOR_COLOR.CUSTOM,
}))
</script>

<style scoped>
.ai-provider-card {
  display: flex;
  flex-direction: column;
  background: var(--mc-bg-card);
  border: 1px solid var(--mc-border);
  border-radius: var(--mc-radius-xl, 12px);
  padding: 16px;
  transition: border-color 0.12s ease, box-shadow 0.12s ease;
}
.ai-provider-card:hover {
  border-color: color-mix(in srgb, var(--mc-primary) 25%, var(--mc-border));
  box-shadow: var(--mc-shadow-soft, 0 4px 12px rgba(15, 23, 42, 0.05));
}
.ai-provider-card.is-default {
  border-color: var(--mc-primary);
}

.ai-provider-card__head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.ai-provider-card__logo {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.5px;
  flex-shrink: 0;
}
.ai-provider-card__title {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}
.ai-provider-card__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--mc-text-primary);
}
.ai-provider-card__vendor {
  font-size: 11px;
  color: var(--mc-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.ai-provider-card__default {
  background: var(--mc-primary);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
}

.ai-provider-card__meta {
  margin: 0;
  display: grid;
  grid-template-columns: 1fr;
  gap: 6px;
  font-size: 12px;
  flex: 1;
}
.ai-provider-card__meta > div {
  display: flex;
  gap: 8px;
  align-items: baseline;
  min-width: 0;
}
.ai-provider-card__meta dt {
  margin: 0;
  color: var(--mc-text-muted);
  width: 72px;
  flex-shrink: 0;
}
.ai-provider-card__meta dd {
  margin: 0;
  color: var(--mc-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}
.ai-provider-card__meta .is-mono {
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
}
.ai-provider-card__url {
  direction: rtl;
  text-align: left;
}

.ai-provider-card__actions {
  display: flex;
  gap: 6px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--mc-border-light, var(--mc-border));
}
</style>
