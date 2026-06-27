<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    :width="width"
    class="mate-dialog"
    align-center
    append-to-body
    :destroy-on-close="destroyOnClose"
    :lock-scroll="lockScroll"
    :close-on-click-modal="closeOnClickModal"
    :close-on-press-escape="closeOnPressEscape"
    @update:model-value="onUpdate"
    @close="$emit('close')"
    @open="$emit('open')"
  >
    <template v-if="$slots.header" #header>
      <slot name="header" />
    </template>

    <!-- Own scoped scroll container — does NOT depend on Element Plus's
         internal .el-dialog__body class / class fall-through. Caps the content
         height so long forms scroll internally and the dialog stays in view. -->
    <div class="mate-dialog__scroll">
      <slot />
    </div>

    <template #footer>
      <slot name="footer" :close="close" :submit="submit">
        <template v-if="showFooter">
          <el-button @click="close">{{ cancelText || displayCancel }}</el-button>
          <el-button
            type="primary"
            :loading="submitting"
            :disabled="confirmDisabled"
            @click="submit"
          >
            {{ confirmText || displayConfirm }}
          </el-button>
        </template>
      </slot>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const displayCancel = computed(() => t('common.cancel'))
const displayConfirm = computed(() => t('common.confirm'))

/**
 * MateDialog — wrapper around {@code el-dialog} that applies the MateCloud
 * defaults (destroy-on-close, lock-scroll=false) and ships a default
 * Cancel / Confirm footer. Replaces the 11 near-identical el-dialog blocks
 * across the CRUD views.
 *
 * Common patterns it expects the host to use:
 *
 *   <MateDialog v-model="open" title="Edit" :submitting="saving" @submit="save">
 *     <MateForm :schema="schema" v-model="form" ref="formRef" />
 *   </MateDialog>
 *
 * Parent is responsible for validating its form before acting on the
 * {@link submit} event. If you need to keep the dialog open after a
 * validation/ network failure, just don't set {@code open = false}.
 *
 * For custom footers (e.g. an extra "Reset" button), use the `footer` slot —
 * it receives `{ close, submit }` functions that mirror the default button
 * behavior.
 */
withDefaults(defineProps<{
  /** Open state (v-model) */
  modelValue: boolean
  title?: string
  width?: string | number
  submitting?: boolean
  confirmText?: string
  cancelText?: string
  /** Disable the confirm button (e.g. when the form is invalid). */
  confirmDisabled?: boolean
  /** Show the default Cancel/Confirm buttons. Set false for view-only dialogs. */
  showFooter?: boolean
  destroyOnClose?: boolean
  lockScroll?: boolean
  closeOnClickModal?: boolean
  closeOnPressEscape?: boolean
}>(), {
  title: '',
  width: '480px',
  submitting: false,
  showFooter: true,
  destroyOnClose: true,
  lockScroll: false,
  closeOnClickModal: false,
  closeOnPressEscape: true,
  confirmDisabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [val: boolean]
  submit: []
  close: []
  open: []
}>()

function onUpdate(val: boolean) {
  emit('update:modelValue', val)
}

function close() {
  emit('update:modelValue', false)
}

function submit() {
  emit('submit')
}
</script>

<style scoped>
/* Scoped to our own wrapper div (Vue keeps the data-v attribute even after
   el-dialog teleports the content to <body>), so it's immune to Element Plus
   class-name changes and class fall-through quirks. Caps the form area and
   scrolls it; el-dialog's header/footer stay outside this div and remain fixed.
   align-center (set on the dialog) keeps the whole thing vertically centered. */
.mate-dialog__scroll {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
  /* Without this, overflow-x computes to `auto` (CSS spec: when one axis is
     non-visible the other resolves to auto), so the vertical scrollbar's width
     triggers a spurious horizontal scrollbar. */
  overflow-x: hidden;
}
</style>
