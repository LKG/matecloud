<template>
  <Teleport to="body">
    <div
      v-for="layer in layers"
      :key="layer.placement"
      class="mate-msg-layer"
      :class="`mate-msg-layer--${layer.placement}`"
    >
      <TransitionGroup :name="`mate-msg-${layer.placement}`">
        <div
          v-for="it in layer.items"
          :key="it.id"
          class="mate-msg"
          :class="`is-${it.type}`"
          role="alert"
          @mouseenter="pauseMessage(it.id)"
          @mouseleave="resumeMessage(it.id)"
        >
          <span class="mate-msg__icon" v-html="ICONS[it.type]" />
          <span class="mate-msg__text">{{ it.message }}</span>
          <button
            v-if="it.showClose"
            class="mate-msg__close"
            type="button"
            aria-label="close"
            @click="closeMessage(it.id)"
          >
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6 6 18M6 6l12 12" /></svg>
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import {
  messageState, pauseMessage, resumeMessage, closeMessage, notifyHostRegistered,
  type MateMessagePlacement,
} from './message'

/**
 * MateMessageHost — render surface for {@link MateMessage}. Mount it once near
 * the app root (App.vue); the imperative API falls back to self-mounting a
 * detached copy only if no in-tree host exists. One fixed layer per placement,
 * each a TransitionGroup so toasts stack and animate independently.
 *
 * Singleton-guarded: if more than one host is ever mounted (e.g. App.vue host
 * + a lazy fallback that raced it), only the first to mount renders, so toasts
 * never appear twice.
 */
let primaryClaimed = false
const isPrimary = ref(false)
onMounted(() => {
  notifyHostRegistered()
  if (!primaryClaimed) { primaryClaimed = true; isPrimary.value = true }
})
onBeforeUnmount(() => { if (isPrimary.value) primaryClaimed = false })

// Inline SVG so packages/ui stays dependency-free (no icon lib).
const ICONS: Record<string, string> = {
  success: '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>',
  error: '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M15 9l-6 6M9 9l6 6"/></svg>',
  warning: '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3 2 20h20L12 3z"/><path d="M12 10v4M12 17.5v.5"/></svg>',
  info: '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 7.5v.5"/></svg>',
}

const PLACEMENTS: MateMessagePlacement[] = ['top', 'center', 'bottom']

const layers = computed(() =>
  isPrimary.value
    ? PLACEMENTS
        .map(placement => ({ placement, items: messageState.items.filter(i => i.placement === placement) }))
        .filter(l => l.items.length > 0)
    : [],
)
</script>

<style scoped>
/* ── Layers (one per placement, full-bleed, click-through) ───────────────── */
.mate-msg-layer {
  position: fixed;
  z-index: 3100;            /* above EP dialogs (2000+) */
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  pointer-events: none;     /* layer is click-through; cards re-enable it */
  padding: 0 16px;
}
.mate-msg-layer--top {
  top: 24px; left: 0; right: 0;
}
.mate-msg-layer--center {
  top: 0; left: 0; right: 0; bottom: 0;
  justify-content: center;
}
.mate-msg-layer--bottom {
  bottom: 24px; left: 0; right: 0;
  justify-content: flex-end;
}

/* ── Card ────────────────────────────────────────────────────────────────── */
.mate-msg {
  pointer-events: auto;
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 240px;
  max-width: min(460px, calc(100vw - 32px));
  padding: 12px 14px;
  border-radius: var(--mc-radius-lg);
  background: var(--mc-bg-overlay);
  backdrop-filter: blur(12px) saturate(1.4);
  -webkit-backdrop-filter: blur(12px) saturate(1.4);
  border: 1px solid var(--mc-border);
  box-shadow: var(--mc-shadow-lg);
  color: var(--mc-text-primary);
  font-size: 14px;
  line-height: 1.5;
}
/* tinted left edge + halo per type — colour comes from the icon var below */
.mate-msg::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: linear-gradient(90deg, color-mix(in srgb, var(--tone) 12%, transparent), transparent 42%);
  pointer-events: none;
}
.mate-msg { position: relative; overflow: hidden; }

.mate-msg__icon {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--tone);
}
.mate-msg__text {
  flex: 1;
  min-width: 0;
  word-break: break-word;
  position: relative;
}
.mate-msg__close {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px; height: 20px;
  margin-left: 2px;
  border: none;
  background: transparent;
  border-radius: var(--mc-radius-sm);
  color: var(--mc-text-disabled);
  cursor: pointer;
  transition: color 0.15s, background 0.15s;
}
.mate-msg__close:hover { color: var(--mc-text-secondary); background: var(--mc-fill-hover); }

/* per-type accent colour (drives icon + halo) */
.mate-msg.is-success { --tone: var(--mc-success); }
.mate-msg.is-error   { --tone: var(--mc-danger); }
.mate-msg.is-warning { --tone: var(--mc-warning); }
.mate-msg.is-info    { --tone: var(--mc-primary); }

/* ── Transitions ─────────────────────────────────────────────────────────── */
.mate-msg-top-enter-active,
.mate-msg-bottom-enter-active,
.mate-msg-center-enter-active,
.mate-msg-top-leave-active,
.mate-msg-bottom-leave-active,
.mate-msg-center-leave-active { transition: all 0.32s cubic-bezier(0.22, 1, 0.36, 1); }
.mate-msg-top-leave-active,
.mate-msg-bottom-leave-active,
.mate-msg-center-leave-active { position: absolute; }

.mate-msg-top-enter-from,
.mate-msg-top-leave-to { opacity: 0; transform: translateY(-24px); }
.mate-msg-bottom-enter-from,
.mate-msg-bottom-leave-to { opacity: 0; transform: translateY(24px); }
.mate-msg-center-enter-from,
.mate-msg-center-leave-to { opacity: 0; transform: scale(0.9); }

.mate-msg-top-move,
.mate-msg-center-move,
.mate-msg-bottom-move { transition: transform 0.32s cubic-bezier(0.22, 1, 0.36, 1); }

@media (prefers-reduced-motion: reduce) {
  .mate-msg-top-enter-active, .mate-msg-bottom-enter-active, .mate-msg-center-enter-active,
  .mate-msg-top-leave-active, .mate-msg-bottom-leave-active, .mate-msg-center-leave-active { transition: opacity 0.2s; }
  .mate-msg-top-enter-from, .mate-msg-top-leave-to,
  .mate-msg-bottom-enter-from, .mate-msg-bottom-leave-to,
  .mate-msg-center-enter-from, .mate-msg-center-leave-to { transform: none; }
}
</style>
