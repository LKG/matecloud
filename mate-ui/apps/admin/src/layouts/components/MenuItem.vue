<template>
  <el-menu-item v-if="!item.children?.length" :index="item.path">
    <el-icon><component :is="icon" :size="16" /></el-icon>
    <template #title>{{ label }}</template>
  </el-menu-item>

  <el-sub-menu v-else :index="item.path || String(item.id)">
    <template #title>
      <el-icon><component :is="icon" :size="16" /></el-icon>
      <span>{{ label }}</span>
    </template>
    <menu-item v-for="c in item.children" :key="c.id" :item="c" />
  </el-sub-menu>
</template>

<script setup lang="ts">
import { computed, markRaw } from 'vue'
import { useI18n } from 'vue-i18n'
import type { MenuItem as MenuItemType } from '@matecloud/core'
import { FolderOpen } from 'lucide-vue-next'
import * as LucideIcons from 'lucide-vue-next'
import * as EpIcons from '@element-plus/icons-vue'

defineOptions({ name: 'MenuItem' })
const props = defineProps<{ item: MenuItemType }>()
const { locale } = useI18n()

const iconCache = new Map<string, any>()
function resolveIcon(name: string) {
  if (!name) return FolderOpen
  if (iconCache.has(name)) return iconCache.get(name)
  const comp = (LucideIcons as any)[name] ?? (EpIcons as any)[name]
  if (comp) {
    const raw = markRaw(comp)
    iconCache.set(name, raw)
    return raw
  }
  return FolderOpen
}

const icon = computed(() => resolveIcon(props.item.icon))
const label = computed(() =>
  locale.value === 'en-US' && props.item.nameEn ? props.item.nameEn : props.item.name,
)
</script>
