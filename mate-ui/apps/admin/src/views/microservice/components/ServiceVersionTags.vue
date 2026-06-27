<template>
  <span v-if="!versions || !versions.length" class="muted">无实例</span>
  <MateTooltip
    v-for="v in versions || []"
    :key="v.version"
    placement="top"
    :disabled="!instancesOf(v.version).length"
  >
    <template #content>
      <div class="tip-title">{{ v.version }} · {{ v.count }} 个实例</div>
      <div v-for="ins in instancesOf(v.version)" :key="ins.host + ins.port" class="tip-row mc-mono">
        {{ ins.host }}:{{ ins.port }}
      </div>
    </template>
    <el-tag size="small" class="ver-tag" disable-transitions>{{ v.version }} ×{{ v.count }}</el-tag>
  </MateTooltip>
</template>

<script setup lang="ts">
import { MateTooltip } from '@matecloud/ui'
import type { VersionStat, InstanceView } from '@matecloud/core'

const props = defineProps<{ versions: VersionStat[]; instances?: InstanceView[] }>()

function instancesOf(version: string): InstanceView[] {
  return (props.instances || []).filter((i) => i.version === version)
}
</script>

<style scoped>
.ver-tag {
  margin-right: 5px;
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
  cursor: default;
}
.muted {
  color: var(--mc-text-disabled);
  font-size: 12px;
}
.tip-title { font-weight: 500; margin-bottom: 4px; }
.tip-row { font-size: 12px; line-height: 1.6; opacity: 0.9; }
.mc-mono { font-family: 'SF Mono', 'JetBrains Mono', monospace; }
</style>
