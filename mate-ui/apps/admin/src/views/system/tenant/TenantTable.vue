<template>
  <MateTable
    :columns="columns"
    :data="data"
    :loading="loading"
    row-key="id"
    :action-width="300"
    :action-label="t('common.action')"
  >
    <template #col-tenantName="{ row }">
      <MateEntityCell :name="row.tenantName" :sub="row.tenantCode" sub-mono />
    </template>

    <template #col-contact="{ row }">
      <div>{{ row.contactName || '-' }}</div>
      <div class="text-xs text-gray-400">{{ row.contactPhone || '' }}</div>
    </template>

    <template #col-packageId="{ row }">
      {{ getPackageName(row.packageId) }}
    </template>

    <template #col-status="{ row }">
      <MateBadge :status="statusBadge(row.status)">
        {{ statusLabel(row.status) }}
      </MateBadge>
    </template>

    <template #col-expireAt="{ row }">
      <span :class="{ 'text-red-500': isExpired(row.expireAt) }">
        {{ row.expireAt ? formatDate(row.expireAt) : t('tenant.permanent') }}
      </span>
    </template>

    <template #actions="{ row }">
      <button class="mc-action-btn" @click="$emit('edit', row)">
        {{ t('common.edit') }}
      </button>
      <button
        v-if="row.status === 0"
        class="mc-action-btn mc-action-btn--warn"
        @click="$emit('suspend', row)"
      >{{ t('tenant.suspend') }}</button>
      <button
        v-if="row.status === 1 || row.status === 2"
        class="mc-action-btn mc-action-btn--success"
        @click="$emit('activate', row)"
      >{{ t('tenant.activate') }}</button>
      <button class="mc-action-btn" @click="$emit('renew', row)">
        {{ t('tenant.renew') }}
      </button>
      <MateInlineConfirm :title="t('common.deleteConfirm')" @confirm="$emit('delete', row)">
                  <button class="mc-action-btn mc-action-btn--danger">
            {{ t('common.delete') }}
          </button>
        
      </MateInlineConfirm>
    </template>
  </MateTable>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { MateTable, MateBadge, MateEntityCell, type MateColumn, MateInlineConfirm } from '@matecloud/ui'
import type { TenantInfo } from '@matecloud/core'

defineProps<{
  data: TenantInfo[]
  loading: boolean
  getPackageName: (id: string) => string
  statusBadge: (status: number) => string
  statusLabel: (status: number) => string
  isExpired: (expireAt: string) => boolean
  formatDate: (dateStr: string) => string
}>()

defineEmits<{
  (e: 'edit', row: TenantInfo): void
  (e: 'suspend', row: TenantInfo): void
  (e: 'activate', row: TenantInfo): void
  (e: 'renew', row: TenantInfo): void
  (e: 'delete', row: TenantInfo): void
}>()

const { t } = useI18n()

const columns: MateColumn[] = [
  { prop: 'tenantName', label: t('tenant.tenantName') },
  { prop: 'contact', label: t('tenant.contact'), width: 150 },
  { prop: 'packageId', label: t('tenant.package'), width: 120 },
  { prop: 'domain', label: t('tenant.domain'), width: 150 },
  { prop: 'status', label: t('common.status'), width: 100 },
  { prop: 'expireAt', label: t('tenant.expireAt'), width: 130 },
  { prop: 'createdAt', label: t('common.createdAt'), type: 'datetime', width: 160 },
]
</script>
