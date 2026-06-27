<template>
  <MatePageCard :title="t('ai.tools.title')" :description="t('ai.tools.description')">
    <MateSearchBar @search="onSearch" @reset="reset">
      <el-input v-model="keyword" :placeholder="t('ai.tools.searchPlaceholder')"
                style="width: 240px" clearable @keyup.enter="onSearch" />
      <el-select v-model="mcpFilter" :placeholder="t('ai.tools.mcpPlaceholder')" clearable style="width: 180px">
        <el-option v-for="m in mcps" :key="m.id" :label="m.name" :value="m.code" />
      </el-select>
    </MateSearchBar>

    <MateTable :columns="columns" :data="filteredTools" :loading="loading"
               row-key="name" :action-width="0">
      <template #col-name="{ row }">
        <AiToolCallChip :tool-name="row.name" :server="row.server" />
      </template>
      <template #col-source="{ row }">
        <MateBadge v-if="row.server" type="info">{{ row.server }}</MateBadge>
        <MateBadge v-else type="success">@Tool</MateBadge>
      </template>
      <template #col-description="{ row }">
        <span>{{ row.description || '-' }}</span>
      </template>
    </MateTable>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { aiApi, type McpServerView } from '@matecloud/core'
import {
  MatePageCard, MateSearchBar, MateTable, MateBadge,
  AiToolCallChip, type MateColumn,
} from '@matecloud/ui'

defineOptions({ name: 'AiToolsView' })

interface ToolEntry {
  name: string
  description?: string
  /** When non-null, this tool comes from an MCP server (server code). */
  server?: string
}

const { t } = useI18n()
const route = useRoute()
const keyword = ref('')
const mcpFilter = ref<string>((route.query.mcp as string) || '')
const loading = ref(false)
const tools = ref<ToolEntry[]>([])
const mcps = ref<McpServerView[]>([])

const columns = computed<MateColumn[]>(() => [
  { prop: 'name', label: t('ai.tools.colTool'), width: 320 },
  { prop: 'source', label: t('ai.tools.colSource'), width: 140 },
  { prop: 'description', label: t('ai.tools.colDescription') },
])

const filteredTools = computed(() => {
  let list = tools.value
  if (mcpFilter.value) list = list.filter(it => it.server === mcpFilter.value)
  if (keyword.value) {
    const k = keyword.value.toLowerCase()
    list = list.filter(it => it.name.toLowerCase().includes(k) ||
                             (it.description || '').toLowerCase().includes(k))
  }
  return list
})

function onSearch() { /* computed handles filtering */ }
function reset() { keyword.value = ''; mcpFilter.value = '' }

async function load() {
  loading.value = true
  try {
    const [toolsRes, mcpRes] = await Promise.all([
      aiApi.toolList(),
      aiApi.mcpListEnabled(),
    ])
    tools.value = ((toolsRes as any).data || []).map((tt: any) => ({
      name: tt.name,
      description: tt.description || tt.desc,
      server: tt.server || tt.mcpCode,
    }))
    mcps.value = (mcpRes as any).data || []
  } catch {
    tools.value = []
    mcps.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
