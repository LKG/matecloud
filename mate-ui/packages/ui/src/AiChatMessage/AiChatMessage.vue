<template>
  <div class="ai-chat-msg" :class="['ai-chat-msg--' + role]">
    <div class="ai-chat-msg__avatar">
      <slot name="avatar">{{ avatarInitial }}</slot>
    </div>
    <div class="ai-chat-msg__body">
      <div class="ai-chat-msg__meta">
        <span class="ai-chat-msg__role">{{ roleLabel }}</span>
        <span v-if="time" class="ai-chat-msg__time">{{ time }}</span>
        <span v-if="latencyMs != null" class="ai-chat-msg__latency">· {{ latencyLabel }}</span>
        <slot name="meta-extra" />
      </div>
      <div class="ai-chat-msg__content">
        <slot>
          <span v-if="content" v-html="renderedContent" />
        </slot>
      </div>
      <div v-if="$slots.footer" class="ai-chat-msg__footer">
        <slot name="footer" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

/**
 * AiChatMessage — single message bubble for chat transcripts.
 *
 * Layout: 32px avatar on the left, body on the right with a meta line
 * (role + time + latency) and the content. {@code role} drives the color
 * scheme. {@code content} renders as text with basic newline-to-&lt;br&gt;
 * escaping; for richer content (markdown, code blocks, tool chips) use
 * the default slot to project your own children.
 */
type Role = 'user' | 'assistant' | 'system' | 'tool'

const props = withDefaults(defineProps<{
  role: Role
  content?: string
  /** Pre-formatted timestamp string (e.g. "14:32"). */
  time?: string
  /** Latency in ms (assistant turns). */
  latencyMs?: number
  /** Show inside the avatar — defaults to first letter of role. */
  avatarText?: string
}>(), {
  role: 'user',
})

const roleLabel = computed(() => {
  switch (props.role) {
    case 'user': return '我'
    case 'assistant': return 'AI'
    case 'system': return '系统'
    case 'tool': return '工具'
    default: return props.role
  }
})

const avatarInitial = computed(() => {
  if (props.avatarText) return props.avatarText
  return props.role === 'user' ? '我'
       : props.role === 'system' ? 'S'
       : props.role === 'tool' ? 'T'
       : 'AI'
})

const latencyLabel = computed(() => {
  if (props.latencyMs == null) return ''
  return props.latencyMs < 1000
    ? props.latencyMs + 'ms'
    : (props.latencyMs / 1000).toFixed(1) + 's'
})

function escape(str: string): string {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

const renderedContent = computed(() => {
  if (!props.content) return ''
  return escape(props.content).replace(/\n/g, '<br/>')
})
</script>

<style scoped>
.ai-chat-msg {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--mc-border-light, transparent);
}
.ai-chat-msg:last-child { border-bottom: none; }

.ai-chat-msg__avatar {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, var(--mc-primary), var(--mc-primary-hover));
}
.ai-chat-msg--user .ai-chat-msg__avatar {
  background: linear-gradient(135deg, #475467, #1D2939);
}
.ai-chat-msg--assistant .ai-chat-msg__avatar {
  background: linear-gradient(135deg, var(--mc-primary), var(--mc-primary-hover));
}
.ai-chat-msg--system .ai-chat-msg__avatar {
  background: linear-gradient(135deg, #98A2B3, #667085);
}
.ai-chat-msg--tool .ai-chat-msg__avatar {
  background: linear-gradient(135deg, #7C3AED, #5B21B6);
}

.ai-chat-msg__body {
  flex: 1;
  min-width: 0;
}
.ai-chat-msg__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--mc-text-muted);
  margin-bottom: 4px;
}
.ai-chat-msg__role {
  font-weight: 600;
  color: var(--mc-text-primary);
}
.ai-chat-msg__time, .ai-chat-msg__latency {
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
  font-size: 11px;
}
.ai-chat-msg__content {
  font-size: 13px;
  line-height: 1.65;
  color: var(--mc-text-primary);
  word-break: break-word;
  white-space: pre-wrap;
}
.ai-chat-msg__content :deep(pre),
.ai-chat-msg__content :deep(code) {
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
  background: var(--mc-fill);
  border-radius: 6px;
  padding: 1px 6px;
  font-size: 12px;
}
.ai-chat-msg__content :deep(pre) {
  padding: 10px 12px;
  margin: 6px 0;
  overflow-x: auto;
}
.ai-chat-msg__footer {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}
</style>
