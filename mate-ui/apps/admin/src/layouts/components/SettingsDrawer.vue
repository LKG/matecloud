<template>
  <MateDrawer
    :model-value="modelValue"
    :title="t('layout.settings')"
    size="320px"
    direction="rtl"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="settings-body">
      <!-- Dark mode -->
      <div class="setting-row">
        <span class="setting-label">{{ t('layout.darkMode') }}</span>
        <el-switch :model-value="system.isDark" @change="system.toggleDark()" />
      </div>

      <!-- Tab bar -->
      <div class="setting-row">
        <span class="setting-label">{{ t('layout.showTabs') }}</span>
        <el-switch :model-value="system.showTabs" @change="system.setShowTabs($event as boolean)" />
      </div>

      <!-- Layout mode (side vs top-nav) -->
      <div class="setting-section">
        <span class="setting-label">{{ t('layout.layoutMode') }}</span>
        <div class="layout-grid">
          <button
            v-for="opt in modeOptions"
            :key="opt.value"
            class="layout-thumb"
            :class="{ active: system.layoutMode === opt.value }"
            :title="opt.label"
            @click="system.setLayoutMode(opt.value)"
          >
            <span class="thumb-preview" :class="opt.preview">
              <span class="t-top"></span>
              <span class="t-mid">
                <span class="t-rail"></span>
                <span class="t-body"></span>
              </span>
            </span>
            <span class="thumb-label">{{ opt.label }}</span>
          </button>
        </div>
      </div>

      <!-- Menu layout (single column vs two-column light/dark) — only in side mode -->
      <div v-if="system.layoutMode === 'side'" class="setting-section">
        <span class="setting-label">{{ t('layout.menuLayout') }}</span>
        <div class="layout-grid">
          <button
            v-for="opt in layoutOptions"
            :key="opt.value"
            class="layout-thumb"
            :class="{ active: opt.active() }"
            :title="opt.label"
            @click="system.setSidebarStyle(opt.value)"
          >
            <span class="thumb-preview" :class="opt.preview">
              <span class="t-rail"></span>
              <span class="t-sub"></span>
              <span class="t-body"></span>
            </span>
            <span class="thumb-label">{{ opt.label }}</span>
          </button>
        </div>
      </div>

      <!-- Theme color -->
      <div class="setting-section">
        <span class="setting-label">{{ t('layout.themeColor') }}</span>
        <div class="color-grid">
          <button
            v-for="color in presetColors"
            :key="color"
            class="color-swatch"
            :class="{ active: system.themeColor === color }"
            :style="{ background: color }"
            @click="system.setThemeColor(color)"
          >
            <span v-if="system.themeColor === color" class="check">&#10003;</span>
          </button>
        </div>
      </div>
    </div>
  </MateDrawer>
</template>

<script setup lang="ts">
import { MateDrawer } from '@matecloud/ui'
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useSystemStore } from '@/stores/system'

defineProps<{ modelValue: boolean }>()
defineEmits<{ 'update:modelValue': [value: boolean] }>()

const { t } = useI18n()
const system = useSystemStore()

// Layout mode: traditional side menu vs top-nav (level-1 in header).
const modeOptions = computed(() => [
  { value: 'side' as const, label: t('layout.layoutSide'), preview: 'is-mode-side' },
  { value: 'top' as const, label: t('layout.layoutTop'), preview: 'is-mode-top' },
])

// Menu layout options. "Single" groups the single-column variants; the two
// two-column variants reuse the SAME menu (auth.menuTree) — only the skin differs.
const layoutOptions = computed(() => [
  {
    value: 'rounded' as const,
    label: t('layout.menuSingle'),
    preview: 'is-single',
    active: () => ['default', 'dark', 'rounded'].includes(system.sidebarStyle),
  },
  {
    value: 'column' as const,
    label: t('layout.menuColumnLight'),
    preview: 'is-col-light',
    // 'split' (the removed dark two-column) also resolves here so anyone with it
    // stored still sees a selected option.
    active: () => system.sidebarStyle === 'column' || system.sidebarStyle === 'split',
  },
])

const presetColors = [
  '#155AEF',
  '#17B26A',
  '#F04438',
  '#0BA5EC',
  '#7A5AF8',
]
</script>

<style scoped>
.settings-body { display: flex; flex-direction: column; gap: 28px; }

.setting-row {
  display: flex; align-items: center; justify-content: space-between;
}
.setting-section { display: flex; flex-direction: column; gap: 12px; }
.setting-label { font-size: 14px; font-weight: 500; color: var(--mc-text-primary); }

/* ---- Menu layout thumbnails ---- */
.layout-grid { display: flex; gap: 14px; }
.layout-thumb {
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  background: none; border: none; padding: 0; cursor: pointer;
}
.thumb-preview {
  display: flex;
  width: 66px; height: 46px;
  border: 2px solid var(--mc-border, rgb(16 24 40 / 0.12));
  border-radius: 7px;
  overflow: hidden;
  background: #fff;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.layout-thumb:hover .thumb-preview { border-color: var(--mc-text-muted); }
.layout-thumb.active .thumb-preview {
  border-color: var(--mc-primary);
  box-shadow: 0 0 0 2px rgba(var(--mc-primary-rgb, 21 90 239), 0.2);
}
.thumb-preview .t-rail,
.thumb-preview .t-sub { height: 100%; flex-shrink: 0; }
.thumb-preview .t-body { flex: 1; height: 100%; background: #f3f4f6; }

/* single column */
.thumb-preview.is-single .t-rail { width: 16px; background: var(--mc-primary); }
.thumb-preview.is-single .t-sub { display: none; }
/* light two-column */
.thumb-preview.is-col-light .t-rail { width: 11px; background: #c8cdd6; }
.thumb-preview.is-col-light .t-sub { width: 18px; background: #eef1f5; }
/* dark two-column */
.thumb-preview.is-col-dark .t-rail { width: 11px; background: #1d1e2c; }
.thumb-preview.is-col-dark .t-sub { width: 18px; background: #eef1f5; }

.thumb-label { font-size: 12px; color: var(--mc-text-muted); }
.layout-thumb.active .thumb-label { color: var(--mc-primary); }

/* ---- Layout-mode thumbnails (side vs top) ---- */
.thumb-preview.is-mode-side,
.thumb-preview.is-mode-top { flex-direction: column; }
.thumb-preview .t-top { width: 100%; height: 11px; flex-shrink: 0; }
.thumb-preview .t-mid { display: flex; flex: 1; min-height: 0; }
.thumb-preview .t-mid .t-rail { height: 100%; }
.thumb-preview .t-mid .t-body { flex: 1; height: 100%; background: #f3f4f6; }
/* side mode: primary rail on the left, no top bar accent */
.thumb-preview.is-mode-side .t-top { background: #eef1f5; }
.thumb-preview.is-mode-side .t-mid .t-rail { width: 16px; background: var(--mc-primary); }
/* top mode: primary top bar, light sub-rail on the left */
.thumb-preview.is-mode-top .t-top { background: var(--mc-primary); }
.thumb-preview.is-mode-top .t-mid .t-rail { width: 13px; background: #eef1f5; }

/* ---- Color picker ---- */
.color-grid { display: flex; gap: 10px; }
.color-swatch {
  width: 32px; height: 32px;
  border-radius: 8px;
  border: 2px solid transparent;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: transform 0.15s, border-color 0.15s;
}
.color-swatch:hover { transform: scale(1.1); }
.color-swatch.active {
  border-color: var(--mc-text-primary);
  box-shadow: 0 0 0 2px var(--mc-bg), 0 0 0 4px var(--mc-text-muted);
}
.check { color: #fff; font-size: 14px; font-weight: 700; }
</style>
