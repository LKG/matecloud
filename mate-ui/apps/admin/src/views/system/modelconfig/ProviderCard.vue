<template>
  <div class="pcard">
    <div class="pc-head">
      <div class="top">
        <img v-if="logo" class="plogo plogo-img" :src="logo" :alt="provider.vendor" />
        <div v-else class="plogo" :style="{ background: vendorColor(provider.vendor) }">
          {{ vendorInitials(provider.name) }}
        </div>
        <div class="meta">
          <div class="pn">
            {{ provider.name }}
            <el-tag v-if="!provider.enabled" size="small" type="info" effect="plain" class="off-tag">
              {{ t('modelConfig.disabled') }}
            </el-tag>
          </div>
          <div class="pm">{{ vendorLabel }}</div>
        </div>
      </div>
      <div class="modrow">
        <span
          v-for="m in provider.modalities"
          :key="m"
          class="modal"
          :class="modalityClass(m)"
        >{{ t(`modelConfig.modality.${m}`) }}</span>
      </div>
    </div>
    <div class="pc-foot">
      <span class="key" :class="{ unset: !hasKey }">
        <KeyRound :size="13" />
        {{ hasKey ? t('modelConfig.keyConfigured') : t('modelConfig.keyMissing') }}
      </span>
      <span class="mcount">· {{ t('modelConfig.modelCount', { n: provider.models?.length || 0 }) }}</span>
      <span class="gacts">
        <button class="ghost" :title="t('modelConfig.test')" @click="$emit('test', provider.id)">
          <Activity :size="15" />
        </button>
        <button class="ghost" :title="t('modelConfig.edit')" @click="$emit('edit', provider)">
          <Settings :size="15" />
        </button>
        <MateInlineConfirm
          :title="t('modelConfig.confirmDelete', { name: provider.name })"
          variant="delete"
          @confirm="$emit('delete', provider.id)"
        >
          <button class="ghost danger" :title="t('modelConfig.delete')">
            <Trash2 :size="15" />
          </button>
        </MateInlineConfirm>
      </span>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Activity, KeyRound, Settings, Trash2 } from 'lucide-vue-next'
import { MateInlineConfirm } from '@matecloud/ui'
import type { ModelProviderView } from '@matecloud/core'
import { modalityClass, vendorColor, vendorIcon, vendorInitials } from './theme'

const props = defineProps<{ provider: ModelProviderView }>()
defineEmits<{
  (e: 'test', id: string): void
  (e: 'edit', provider: ModelProviderView): void
  (e: 'delete', id: string): void
}>()

const { t, te } = useI18n()

const vendorLabel = computed(() => {
  const k = `modelConfig.vendor.${props.provider.vendor}`
  return te(k) ? t(k) : props.provider.vendor
})

const logo = computed(() => vendorIcon(props.provider.vendor))

// A credential is "configured" when any SECRET field has a stored (masked) value.
const hasKey = computed(() => {
  const secretKeys = props.provider.fields.filter((f) => f.type === 'SECRET').map((f) => f.key)
  if (!secretKeys.length) return true
  return secretKeys.some((k) => {
    const v = props.provider.values?.[k]
    return v !== undefined && v !== null && String(v).length > 0
  })
})
</script>

<style src="./modality.css"></style>
<style scoped>
.pcard {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 14px;
  background: var(--el-bg-color);
  overflow: hidden;
  transition: box-shadow 0.18s, border-color 0.18s;
}
.pcard:hover {
  box-shadow: 0 4px 12px -4px rgba(16, 24, 40, 0.12);
  border-color: var(--el-border-color);
}
.pc-head { padding: 13px 14px 12px; }
.top { display: flex; align-items: center; gap: 10px; }
.plogo {
  width: 36px; height: 36px; border-radius: 10px;
  display: grid; place-items: center;
  color: #fff; font-weight: 800; font-size: 13px; flex: none;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.18);
}
.plogo-img {
  background: var(--mc-logo-plate); object-fit: contain; padding: 5px;
  box-shadow: inset 0 0 0 1px var(--el-border-color-lighter);
}
.meta { min-width: 0; }
.pn { font-weight: 600; font-size: 13.5px; display: flex; align-items: center; gap: 6px; }
.off-tag { transform: scale(0.86); transform-origin: left center; }
.pm { font-size: 11px; color: var(--el-text-color-secondary); margin-top: 1px; }
.modrow { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 11px; }
.pc-foot {
  display: flex; align-items: center; gap: 7px;
  border-top: 1px solid var(--el-border-color-lighter);
  padding: 7px 10px 7px 12px;
  font-size: 11.5px; color: var(--el-text-color-secondary);
}
.pc-foot .key { display: flex; align-items: center; gap: 6px; }
.pc-foot .key.unset { color: var(--el-color-warning); }
.pc-foot .mcount { color: var(--el-text-color-secondary); }
.pc-foot .gacts { margin-left: auto; display: flex; gap: 1px; opacity: 0; transition: opacity 0.15s; }
.pcard:hover .pc-foot .gacts { opacity: 1; }
.ghost {
  display: inline-grid; place-items: center;
  width: 26px; height: 26px;
  border: none; background: transparent; border-radius: 7px;
  color: var(--el-text-color-secondary); cursor: pointer; transition: 0.12s;
}
.ghost:hover { background: var(--el-fill-color); color: var(--el-color-primary); }
.ghost.danger:hover { color: var(--el-color-danger); }
</style>
