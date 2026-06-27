<template>
  <MateSearchBar @search="$emit('search')" @reset="$emit('reset')">
    <el-input
      :model-value="keyword"
      :placeholder="t('tenant.searchPlaceholder')"
      clearable
      style="width: 240px"
      @update:model-value="$emit('update:keyword', $event)"
      @keyup.enter="$emit('search')"
    >
      <template #prefix><Search :size="14" /></template>
    </el-input>
  </MateSearchBar>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Search } from 'lucide-vue-next'
import { MateSearchBar } from '@matecloud/ui'

defineProps<{
  keyword: string
}>()

defineEmits<{
  (e: 'update:keyword', val: string): void
  (e: 'search'): void
  (e: 'reset'): void
}>()

const { t } = useI18n()
</script>
