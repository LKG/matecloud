<template>
  <MatePageCard :title="t('tenant.title')" :description="t('tenant.description')">
    <template #actions>
      <el-button type="primary" @click="openFormDialog()">
        <Plus :size="14" class="mr-1" />{{ t('tenant.createTenant') }}
      </el-button>
    </template>

    <!-- Search -->
    <TenantSearch
      v-model:keyword="keyword"
      @search="handleSearch"
      @reset="handleReset"
    />

    <!-- Table -->
    <TenantTable
      :data="tableData"
      :loading="loading"
      :get-package-name="getPackageName"
      :status-badge="statusBadge"
      :status-label="statusLabel"
      :is-expired="isExpired"
      :format-date="formatDate"
      @edit="openFormDialog"
      @suspend="handleSuspend"
      @activate="handleActivate"
      @renew="openRenewDialog"
      @delete="handleDelete"
    />

    <!-- Pagination -->
    <MatePagination
      v-model:page-num="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      @change="loadData"
    />

    <!-- Create / Edit -->
    <TenantFormDialog
      v-model:visible="formDialogVisible"
      :is-edit="isEdit"
      :form="form"
      :submitting="submitting"
      :packages="packages"
      @update:form="(v) => Object.assign(form, v)"
      @submit="submitForm"
    />

    <!-- Renew -->
    <TenantRenewDialog
      v-model:visible="renewDialogVisible"
      v-model:expire-at="renewExpireAt"
      :submitting="submitting"
      :row="renewingRow"
      :format-date="formatDate"
      @submit="submitRenew"
    />
  </MatePageCard>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Plus } from 'lucide-vue-next'
import { MatePageCard, MatePagination } from '@matecloud/ui'
import TenantSearch from './TenantSearch.vue'
import TenantTable from './TenantTable.vue'
import TenantFormDialog from './TenantFormDialog.vue'
import TenantRenewDialog from './TenantRenewDialog.vue'
import { useTenant } from './useTenant'

defineOptions({ name: 'TenantListView' })

const { t } = useI18n()

const {
  // list
  loading, tableData, total, pageNum, pageSize, keyword, packages,
  loadData, handleSearch, handleReset,
  // form
  formDialogVisible, isEdit, form, submitting,
  openFormDialog, submitForm,
  // status
  handleSuspend, handleActivate,
  // renew
  renewDialogVisible, renewingRow, renewExpireAt,
  openRenewDialog, submitRenew,
  // delete
  handleDelete,
  // helpers
  getPackageName, statusBadge, statusLabel, isExpired, formatDate,
} = useTenant()
</script>
