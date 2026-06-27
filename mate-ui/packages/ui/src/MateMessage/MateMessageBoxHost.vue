<template>
  <Teleport to="body">
    <!-- Plain v-if (no <Transition>): closing removes the node instantly so a
         fixed full-screen overlay can never orphan and trap clicks. Enter feel
         comes from self-completing CSS @keyframes, not a Vue leave transition.
         Stacked boxes show one at a time, newest on top. -->
    <div
      v-if="topBox"
      :key="topBox.id"
      class="mate-box-overlay"
      @click.self="onOverlay(topBox)"
    >
        <div class="mate-box" role="dialog" aria-modal="true">
          <!-- header -->
          <div class="mate-box__header">
            <h3 class="mate-box__title">{{ topBox.title }}</h3>
            <button class="mate-box__x" type="button" aria-label="close" @click="cancelBox(topBox.id)">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6 6 18M6 6l12 12" /></svg>
            </button>
          </div>

          <!-- body -->
          <div class="mate-box__body">
            <span v-if="topBox.type" class="mate-box__icon" :class="`is-${topBox.type}`" v-html="ICONS[topBox.type]" />
            <div class="mate-box__content">
              <p class="mate-box__message">{{ topBox.message }}</p>
              <template v-if="topBox.showInput">
                <input
                  ref="inputRef"
                  v-model="topBox.inputModel"
                  class="mate-box__input"
                  :class="{ 'is-error': topBox.inputError }"
                  :type="topBox.inputType"
                  :placeholder="topBox.inputPlaceholder"
                  @keyup.enter="onConfirm(topBox)"
                  @input="topBox.inputError = ''"
                />
                <p v-if="topBox.inputError" class="mate-box__input-error">{{ topBox.inputError }}</p>
              </template>
            </div>
          </div>

          <!-- footer -->
          <div class="mate-box__footer">
            <button
              v-if="topBox.showCancelButton"
              class="mate-box__btn mate-box__btn--cancel"
              type="button"
              @click="cancelBox(topBox.id)"
            >{{ topBox.cancelButtonText }}</button>
            <button
              class="mate-box__btn mate-box__btn--confirm"
              :class="{ 'is-danger': topBox.confirmDanger || topBox.type === 'error' }"
              type="button"
              @click="onConfirm(topBox)"
            >{{ topBox.confirmButtonText }}</button>
          </div>
        </div>
      </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, onUnmounted, ref, watch, nextTick } from 'vue'
import {
  messageBoxState, confirmBox, cancelBox, notifyBoxHostRegistered,
  type MateBoxItem,
} from './messageBox'

/**
 * MateMessageBoxHost — render surface for {@link MateMessageBox}. Mount once near
 * the app root (App.vue); singleton-guarded so a lazy fallback never double-renders.
 */
const ICONS: Record<string, string> = {
  success: '<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M8.5 12.5l2.5 2.5 4.5-5"/></svg>',
  error: '<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M15 9l-6 6M9 9l6 6"/></svg>',
  warning: '<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 7.5v5M12 16v.5"/></svg>',
  info: '<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 7.5v.5"/></svg>',
}

let primaryClaimed = false
const isPrimary = ref(false)
onMounted(() => {
  notifyBoxHostRegistered()
  if (!primaryClaimed) { primaryClaimed = true; isPrimary.value = true }
  window.addEventListener('keydown', onKey)
})
onBeforeUnmount(() => { if (isPrimary.value) primaryClaimed = false })
onUnmounted(() => window.removeEventListener('keydown', onKey))

const boxes = computed(() => (isPrimary.value ? messageBoxState.boxes : []))
/** Only the newest box is shown (modals don't stack visually). */
const topBox = computed(() => (boxes.value.length ? boxes.value[boxes.value.length - 1] : null))

const inputRef = ref<HTMLInputElement | null>(null)
// Autofocus the prompt input whenever the visible box changes.
watch(() => topBox.value?.id, () => {
  if (topBox.value?.showInput) nextTick(() => inputRef.value?.focus())
})

function onConfirm(box: MateBoxItem) { confirmBox(box.id) }

function onOverlay(box: MateBoxItem) {
  if (box.closeOnClickModal) cancelBox(box.id)
}

/** ESC cancels the top-most dialog. */
function onKey(e: KeyboardEvent) {
  if (e.key !== 'Escape' || !topBox.value) return
  cancelBox(topBox.value.id)
}
</script>

<style scoped>
.mate-box-overlay {
  position: fixed;
  inset: 0;
  z-index: 3200;          /* above EP dialogs (2000+) and app stacking contexts */
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: rgba(16, 24, 40, 0.45);
  backdrop-filter: blur(2px);
  -webkit-backdrop-filter: blur(2px);
}

.mate-box {
  width: 420px;
  max-width: 100%;
  background: var(--mc-bg-elevated);
  border: 1px solid var(--mc-border);
  border-radius: var(--mc-radius-lg);
  box-shadow: var(--mc-shadow-strong);
  overflow: hidden;
}

.mate-box__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px 0;
}
.mate-box__title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--mc-text-primary);
}
.mate-box__x {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px; height: 26px;
  border: none;
  background: transparent;
  border-radius: var(--mc-radius-sm);
  color: var(--mc-text-disabled);
  cursor: pointer;
  transition: color 0.15s, background 0.15s;
}
.mate-box__x:hover { color: var(--mc-text-secondary); background: var(--mc-fill-hover); }

.mate-box__body {
  display: flex;
  gap: 12px;
  padding: 16px 20px 20px;
}
.mate-box__icon {
  flex-shrink: 0;
  display: inline-flex;
  align-items: flex-start;
  padding-top: 1px;
}
.mate-box__icon.is-success { color: var(--mc-success); }
.mate-box__icon.is-error   { color: var(--mc-danger); }
.mate-box__icon.is-warning { color: var(--mc-warning); }
.mate-box__icon.is-info    { color: var(--mc-primary); }

.mate-box__content { flex: 1; min-width: 0; }
.mate-box__message {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--mc-text-secondary);
  word-break: break-word;
}
.mate-box__input {
  width: 100%;
  margin-top: 12px;
  padding: 8px 12px;
  font-size: 14px;
  color: var(--mc-field-text);
  background: var(--mc-field-bg);
  border: 1px solid var(--mc-field-border);
  border-radius: var(--mc-radius-sm);
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.mate-box__input:focus {
  border-color: var(--mc-field-border-focus);
  box-shadow: 0 0 0 3px var(--mc-field-ring);
}
.mate-box__input.is-error {
  border-color: var(--mc-field-border-destructive);
  box-shadow: 0 0 0 3px var(--mc-field-ring-destructive);
}
.mate-box__input-error {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--mc-danger);
}

.mate-box__footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 20px 20px;
}
.mate-box__btn {
  height: 36px;
  padding: 0 18px;
  font-size: 14px;
  font-weight: 500;
  border-radius: var(--mc-radius-sm);
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}
.mate-box__btn--cancel {
  background: var(--mc-btn-secondary-bg);
  border: 1px solid var(--mc-btn-secondary-border);
  color: var(--mc-btn-secondary-text);
}
.mate-box__btn--cancel:hover {
  border-color: var(--mc-btn-secondary-border-hover);
  color: var(--mc-primary);
}
.mate-box__btn--confirm {
  background: var(--mc-btn-primary-bg);
  border: 1px solid var(--mc-btn-primary-bg);
  color: var(--mc-btn-primary-text);
}
.mate-box__btn--confirm:hover { background: var(--mc-btn-primary-bg-hover); border-color: var(--mc-btn-primary-bg-hover); }
.mate-box__btn--confirm.is-danger {
  background: var(--mc-btn-danger-bg);
  border-color: var(--mc-btn-danger-bg);
  color: var(--mc-btn-danger-text);
}
.mate-box__btn--confirm.is-danger:hover { background: var(--mc-btn-danger-bg-hover); border-color: var(--mc-btn-danger-bg-hover); }

/* Enter feel via a self-completing CSS @keyframes on the card only. Closing is
   instant via v-if (no Vue transition), so the fixed overlay can never orphan
   and trap clicks. The pop animates transform ONLY — base opacity stays 1 — so
   the card is always visible even if the animation is interrupted/throttled. */
.mate-box { animation: mate-box-pop 0.24s cubic-bezier(0.22, 1, 0.36, 1); }

@keyframes mate-box-pop {
  from { transform: scale(0.94) translateY(6px); }
  to { transform: none; }
}

@media (prefers-reduced-motion: reduce) {
  .mate-box { animation: none; }
}
</style>
