<template>
  <transition name="mc-batchbar-fade">
    <div v-if="count > 0" class="mc-batchbar" :class="{ 'mc-batchbar--sticky': sticky }">
      <div class="mc-batchbar__count">
        <CheckSquare :size="14" class="mc-batchbar__icon" />
        <i18n-t keypath="common.batchSelected" tag="span">
          <template #n><b>{{ count }}</b></template>
        </i18n-t>
      </div>

      <div class="mc-batchbar__actions">
        <slot />
      </div>

      <button class="mc-batchbar__clear" @click="$emit('clear')">
        {{ clearText ?? t('common.cancel') }}
      </button>
    </div>
  </transition>
</template>

<script setup lang="ts">
/**
 * MateBatchBar — fixed-height action strip that fades in when the host
 * table has at least one row selected.
 *
 * Slot-based by design: every CRUD list has its own set of bulk verbs
 * (freeze vs disable, delete vs archive, sometimes "assign role"). Trying
 * to express all of those through a prop schema bloats the API surface for
 * marginal saving — a `<el-button>` per action in the default slot is more
 * direct and lets the host wire its own permission code, confirm flow,
 * loading state, etc.
 *
 * The bar itself owns:
 *  - the "已选 N 项" label (i18n key {@code common.batchSelected})
 *  - a "取消" link that emits {@code clear} so the host can reset selection
 *  - layout / spacing / fade transition
 *
 * Usage:
 *
 *   <MateBatchBar :count="selectedIds.length" @clear="selectedIds = []">
 *     <el-button size="small" @click="batchFreeze">{{ t('user.batchFreeze') }}</el-button>
 *     <MateInlineConfirm @confirm="batchDelete" ...>
 *       <el-button size="small" type="danger">{{ t('common.batchDelete') }}</el-button>
 *     </MateInlineConfirm>
 *   </MateBatchBar>
 *
 * Pair with {@code useBatch} from @matecloud/core to handle the result shape
 * (success/fail toast + per-id errors) without writing the same try/catch
 * twice.
 */
import MateInlineConfirm from '../MateInlineConfirm/MateInlineConfirm.vue'
import { useI18n } from 'vue-i18n'
import { CheckSquare } from 'lucide-vue-next'

withDefaults(defineProps<{
  /** Selected row count — bar renders only when {@code > 0}. */
  count: number
  /** Override the "Cancel" link text. */
  clearText?: string
  /**
   * Sticky-positioned at the top of the scroll container. Off by default
   * (sits inline above the table). Useful for very long lists where the
   * user might forget there's a selection.
   */
  sticky?: boolean
}>(), {
  sticky: false,
})

defineEmits<{
  /** Fired when the user hits "Cancel" — host should reset selection state. */
  clear: []
}>()

const { t } = useI18n()
</script>

<style scoped>
.mc-batchbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 14px;
  margin-bottom: 12px;
  border-radius: 8px;
  background: var(--mc-state-accent-hover, rgb(21 90 239 / 0.06));
  border: 1px solid var(--mc-primary-light-7, rgb(21 90 239 / 0.18));
  font-size: 13px;
}

/* Sticky variant — pin to top of the scroll viewport */
.mc-batchbar--sticky {
  position: sticky;
  top: 0;
  z-index: 4;
  backdrop-filter: blur(6px);
}

.mc-batchbar__count {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--mc-text-primary);
}
.mc-batchbar__count b {
  color: var(--mc-primary);
  font-weight: 700;
  margin: 0 2px;
}
.mc-batchbar__icon {
  color: var(--mc-primary);
}

.mc-batchbar__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.mc-batchbar__clear {
  border: none;
  background: none;
  color: var(--mc-text-muted);
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: color 0.15s, background 0.15s;
}
.mc-batchbar__clear:hover {
  color: var(--mc-text-primary);
  background: var(--mc-state-hover, rgb(200 206 218 / 0.15));
}

/* Fade transition */
.mc-batchbar-fade-enter-active,
.mc-batchbar-fade-leave-active {
  transition: opacity 0.18s, transform 0.18s;
}
.mc-batchbar-fade-enter-from,
.mc-batchbar-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
