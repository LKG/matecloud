<template>
  <div class="mc-search-bar">
    <!-- Host-defined filter controls go here: inputs, selects, date pickers… -->
    <slot />

    <el-button type="primary" size="default" @click="$emit('search')">
      <Search :size="14" class="mc-search-bar__icon" />
      {{ displaySearchText }}
    </el-button>

    <el-button v-if="showReset" size="default" @click="$emit('reset')">
      {{ displayResetText }}
    </el-button>

    <!-- Optional right-aligned extras (e.g. "Export" / "Import" buttons). -->
    <div v-if="$slots.trailing" class="mc-search-bar__trailing">
      <slot name="trailing" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Search } from 'lucide-vue-next'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

/**
 * MateSearchBar — filter row above every list page.
 */
const props = withDefaults(defineProps<{
  searchText?: string
  resetText?: string
  showReset?: boolean
}>(), {
  showReset: true,
})

const displaySearchText = computed(() => props.searchText || t('common.search'))
const displayResetText = computed(() => props.resetText || t('common.reset'))

defineEmits<{
  search: []
  reset: []
}>()
</script>

<style scoped>
.mc-search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.mc-search-bar__icon {
  margin-right: 4px;
}
.mc-search-bar__trailing {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}
</style>
