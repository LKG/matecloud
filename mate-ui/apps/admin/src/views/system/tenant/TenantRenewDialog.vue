<template>
  <MateDialog
    :model-value="visible"
    :title="t('tenant.renewTitle')"
    :submitting="submitting"
    @update:model-value="$emit('update:visible', $event)"
    @submit="$emit('submit')"
  >
    <el-form label-position="top">
      <el-form-item :label="t('tenant.currentExpiry')">
        <span>{{ row?.expireAt ? formatDate(row.expireAt) : t('tenant.permanent') }}</span>
      </el-form-item>
      <el-form-item :label="t('tenant.newExpiry')">
        <el-date-picker
          :model-value="expireAt"
          type="date"
          :placeholder="t('tenant.selectExpiry')"
          style="width: 100%"
          @update:model-value="$emit('update:expireAt', $event)"
        />
      </el-form-item>
    </el-form>
  </MateDialog>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { MateDialog } from '@matecloud/ui'
import type { TenantInfo } from '@matecloud/core'

defineProps<{
  visible: boolean
  submitting: boolean
  row: TenantInfo | null
  expireAt: Date | null
  formatDate: (dateStr: string) => string
}>()

defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'update:expireAt', val: Date | null): void
  (e: 'submit'): void
}>()

const { t } = useI18n()
</script>
