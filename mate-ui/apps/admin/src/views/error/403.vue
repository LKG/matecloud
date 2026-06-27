<template>
  <div class="error-page">
    <div class="error-card">
      <div class="error-code">403</div>
      <p class="error-title">{{ t('error.forbiddenTitle') }}</p>
      <p class="error-message">{{ t('error.forbiddenMessage') }}</p>
      <div class="error-actions">
        <el-button @click="$router.back()">{{ t('common.back') }}</el-button>
        <el-button type="primary" @click="$router.push('/')">{{ t('common.home') }}</el-button>
      </div>
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
  min-width: 320px;
}
.error-code {
  font-size: 72px;
  font-weight: 900;
  line-height: 1;
  color: var(--mc-warning, #f79009);
  margin-bottom: 16px;
}
.error-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--mc-text-primary);
  margin: 0 0 6px;
}
.error-message {
  font-size: 14px;
  color: var(--mc-text-muted);
  margin: 0 0 24px;
}
.error-actions {
  display: flex;
  gap: 8px;
  justify-content: center;
}
</style>
