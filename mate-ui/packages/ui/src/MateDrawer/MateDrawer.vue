<template>
  <el-drawer
    :model-value="modelValue"
    :title="title"
    :size="size"
    :direction="direction"
    class="mate-drawer"
    append-to-body
    :destroy-on-close="destroyOnClose"
    :close-on-click-modal="closeOnClickModal"
    :close-on-press-escape="closeOnPressEscape"
    @update:model-value="onUpdate"
    @close="$emit('close')"
    @open="$emit('open')"
  >
    <template v-if="$slots.header" #header>
      <slot name="header" />
    </template>

    <!-- Pass content straight through: el-drawer's body already scrolls and is
         full-height, so (unlike MateDialog) no extra wrapper — keeps faithful
         to el-drawer and avoids breaking layout-chrome drawers (sidebar etc.). -->
    <slot />

    <template v-if="$slots.footer || showFooter" #footer>
      <slot name="footer" :close="close" :submit="submit">
        <el-button @click="close">{{ cancelText || displayCancel }}</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="confirmDisabled"
          @click="submit"
        >
          {{ confirmText || displayConfirm }}
        </el-button>
      </slot>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const displayCancel = computed(() => t('common.cancel'))
const displayConfirm = computed(() => t('common.confirm'))

/**
 * MateDrawer — wrapper around {@code el-drawer} that applies the MateCloud
 * defaults (append-to-body, destroy-on-close) and an optional Cancel/Confirm
 * footer. Sibling of {@link MateDialog} for side-panel UIs. Non-prop attrs
 * fall through to the underlying el-drawer.
 *
 *   <MateDrawer v-model="open" title="编辑" size="620px" :submitting="saving" @submit="save">
 *     <MateForm ... />
 *   </MateDrawer>
 *
 * Footer renders only when `showFooter` is true or a `footer` slot is supplied —
 * view-only drawers get none. The `footer` slot receives `{ close, submit }`.
 */
withDefaults(defineProps<{
  /** Open state (v-model) */
  modelValue: boolean
  title?: string
  /** Drawer width (rtl/ltr) or height (ttb/btt). Accepts px/%/number. */
  size?: string | number
  direction?: 'rtl' | 'ltr' | 'ttb' | 'btt'
  submitting?: boolean
  confirmText?: string
  cancelText?: string
  confirmDisabled?: boolean
  /** Render the default Cancel/Confirm footer (off by default; drawers are often view-only). */
  showFooter?: boolean
  destroyOnClose?: boolean
  closeOnClickModal?: boolean
  closeOnPressEscape?: boolean
}>(), {
  title: '',
  direction: 'rtl',
  submitting: false,
  showFooter: false,
  destroyOnClose: true,
  closeOnClickModal: true,
  closeOnPressEscape: true,
  confirmDisabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [val: boolean]
  submit: []
  close: []
  open: []
}>()

function onUpdate(val: boolean) { emit('update:modelValue', val) }
function close() { emit('update:modelValue', false) }
function submit() { emit('submit') }
</script>
