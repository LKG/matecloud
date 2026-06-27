<template>
  <span class="mc-badge" :class="'mc-badge--' + effectiveType">
    <span v-if="dot" class="mc-badge__dot" />
    <slot>{{ label }}</slot>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { mapStatus, type BadgeType } from './status'

/**
 * MateBadge — pill-shaped status badge.
 *
 * Two ways to use it:
 *   1. Explicit type  — {@code <MateBadge type="success">Active</MateBadge>}
 *   2. Status mapping — {@code <MateBadge :status="row.status">Active</MateBadge>}
 *      The {@code status} prop auto-derives a sensible badge color so views
 *      stop hand-writing {@code row.status === 'ACTIVE' ? 'success' : 'warning'}
 *      ternaries.
 *
 * Mapping:
 *   ACTIVE / ENABLED / OK / SUCCESS / 1 / true   → success
 *   DISABLED / FROZEN / PENDING / 0              → warning
 *   DELETED / FAIL / ERROR / -1                  → danger
 *
 * For log rows set {@code domain="log"}: then numeric {@code 0} means "OK"
 * (success) and {@code 1} means "Fail" (danger) — matches OperationLog /
 * LoginLog conventions.
 */
const props = withDefaults(defineProps<{
  type?: BadgeType
  status?: string | number | boolean | null
  /** Switch the status mapping. 'entity' (default) vs 'log'. */
  domain?: 'entity' | 'log'
  label?: string
  dot?: boolean
}>(), {
  type: 'default',
  dot: true,
  domain: 'entity',
})

const effectiveType = computed<BadgeType>(() => {
  if (props.status === undefined || props.status === null) return props.type
  return mapStatus(props.status, props.domain)
})

</script>

<style scoped>
.mc-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 8px 2px 6px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  line-height: 18px;
}
.mc-badge__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* ---- Variants (MateCloud badge colors) ---- */
.mc-badge--success {
  background: rgb(23 178 106 / 0.08);
  color: #067647;
}
.mc-badge--success .mc-badge__dot { background: #17B26A; }

.mc-badge--warning {
  background: rgb(247 144 9 / 0.08);
  color: #B54708;
}
.mc-badge--warning .mc-badge__dot { background: #F79009; }

.mc-badge--danger {
  background: rgb(240 68 56 / 0.08);
  color: #B42318;
}
.mc-badge--danger .mc-badge__dot { background: #F04438; }

.mc-badge--info {
  background: rgb(54 191 250 / 0.08);
  color: #026AA2;
}
.mc-badge--info .mc-badge__dot { background: #36BFFA; }

.mc-badge--default {
  background: var(--mc-fill);
  color: var(--mc-text-muted);
}
.mc-badge--default .mc-badge__dot { background: var(--mc-text-disabled); }

/* ---- Dark mode refinements ---- */
html.dark .mc-badge--success { color: #47CD89; }
html.dark .mc-badge--warning { color: #FDB022; }
html.dark .mc-badge--danger { color: #F97066; }
html.dark .mc-badge--info { color: #36BFFA; }
</style>
