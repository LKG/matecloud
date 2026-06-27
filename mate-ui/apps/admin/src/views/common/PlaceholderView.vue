<template>
  <div class="placeholder">
    <div class="placeholder-card">
      <Construction :size="36" :stroke-width="1.5" class="placeholder-icon" />
      <h2 class="placeholder-title">{{ route.meta.title ? t(route.meta.title as string) : route.name }}</h2>
      <p class="placeholder-desc">{{ t('common.underDevelopment') || 'This page is under development' }}</p>
      <p class="placeholder-path">{{ route.path }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Construction } from 'lucide-vue-next'

const route = useRoute()
const { t } = useI18n()
</script>

<style scoped>
.placeholder {
  display: flex; align-items: center; justify-content: center;
  min-height: 400px;
}
.placeholder-card {
  text-align: center;
  padding: 48px;
  border-radius: var(--mc-radius-lg);
  background: var(--mc-bg-elevated);
  border: 1px solid var(--mc-divider-regular);
}
.placeholder-icon { color: var(--mc-text-muted); margin: 0 auto 16px; }
.placeholder-title { font-size: 18px; font-weight: 600; color: var(--mc-text-primary); margin: 0 0 8px; }
.placeholder-desc { font-size: 14px; color: var(--mc-text-muted); margin: 0 0 12px; }
.placeholder-path { font-size: 12px; color: var(--mc-text-disabled); font-family: monospace; margin: 0; }
</style>
