<template>
  <div class="error-page">
    <div class="error-card">
      <div class="error-code">404</div>
      <p class="error-message">Page not found</p>
      <el-button type="primary" @click="$router.push('/')">{{ t('common.back') }}</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
</script>

<style scoped>
.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--mc-bg);
}
.error-card {
  text-align: center;
  padding: 48px;
  background: var(--mc-bg-elevated);
  border: 1px solid var(--mc-divider-regular);
  border-radius: var(--mc-radius-lg);
}
.error-code {
  font-size: 72px;
  font-weight: 900;
  line-height: 1;
  color: var(--mc-text-disabled);
  margin-bottom: 16px;
}
.error-message {
  font-size: 16px;
  color: var(--mc-text-muted);
  margin: 0 0 24px;
}
</style>
