<template>
  <div v-show="system.showTabs" class="layout-tabbar">
    <el-tabs
      :model-value="route.name as string"
      :closable="tabbar.tabCount > 1"
      @tab-click="handleClick"
      @tab-remove="handleRemove"
    >
      <el-tab-pane v-for="tab in tabbar.tabList" :key="tab.name" :name="tab.name">
        <template #label>
          <MateDropdown trigger="contextmenu" placement="bottom-start">
            <span class="tab-label" :class="{ active: route.name === tab.name }">
              {{ t(tab.title) }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item :disabled="isFirst(tab.name)" @click="tabbar.closeLeft(tab.name); goTo(tab.name)">
                  {{ t('layout.closeLeft') || 'Close Left' }}
                </el-dropdown-item>
                <el-dropdown-item :disabled="isLast(tab.name)" @click="tabbar.closeRight(tab.name); goTo(tab.name)">
                  {{ t('layout.closeRight') || 'Close Right' }}
                </el-dropdown-item>
                <el-dropdown-item :disabled="tabbar.tabCount <= 1" @click="tabbar.closeOthers(tab.name); goTo(tab.name)">
                  {{ t('layout.closeOthers') }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </MateDropdown>
        </template>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { MateDropdown } from '@matecloud/ui'
import { watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import type { TabsPaneContext } from 'element-plus'
import { useTabBarStore } from '@/stores/tabbar'
import { useSystemStore } from '@/stores/system'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const tabbar = useTabBarStore()
const system = useSystemStore()

onMounted(() => tabbar.addTab(route))
watch(route, (r) => tabbar.addTab(r))

function handleClick(pane: TabsPaneContext) {
  const tab = tabbar.tabList.find((t) => t.name === pane.props.name)
  if (tab) router.push({ name: tab.name, query: tab.query })
}

function handleRemove(name: string | number) {
  const key = String(name)
  if (route.name === key) {
    const list = tabbar.tabList
    const idx = list.findIndex((t) => t.name === key)
    const next = list[idx === 0 ? 1 : idx - 1]
    if (next) router.push({ name: next.name })
  }
  tabbar.removeTab(key)
}

function goTo(name: string) {
  if (route.name !== name) router.push({ name })
}

function isFirst(name: string) {
  return tabbar.tabList[0]?.name === name
}
function isLast(name: string) {
  const list = tabbar.tabList
  return list[list.length - 1]?.name === name
}
</script>

<style scoped>
.layout-tabbar {
  position: relative;
  z-index: 5;
  padding: 0 16px;
  background: var(--mc-bg-elevated);
  border-bottom: 1px solid var(--mc-border-light);
  flex-shrink: 0;
}

.tab-label {
  display: inline-block;
  font-size: 13px;
  padding: 0 4px;
  outline: none;
}
.tab-label.active {
  color: var(--mc-primary);
  font-weight: 600;
}

:deep(.el-tabs__header) { margin: 0; }
:deep(.el-tabs__nav-wrap::after) { display: none; }
:deep(.el-tabs__content) { display: none; }
:deep(.el-tabs__active-bar) { display: none; }
:deep(.el-tabs__item) {
  display: inline-flex !important;
  align-items: center;
  padding: 0 16px !important;
  height: var(--mc-tab-height) !important;
}
:deep(.el-tabs__item.is-active) {
  background: var(--mc-primary-light-5);
  border-radius: var(--mc-radius) var(--mc-radius) 0 0;
}
</style>
