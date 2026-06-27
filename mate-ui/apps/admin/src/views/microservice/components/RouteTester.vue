<template>
  <div class="tester">
    <div class="tester-title"><Route :size="15" />路由测试器</div>
    <div class="tester-row">
      <el-select v-model="service" placeholder="选择服务" style="width: 180px">
        <el-option v-for="s in services" :key="s.service" :label="s.service" :value="s.service" />
      </el-select>
      <span class="tester-label">X-Service-Version</span>
      <el-input v-model="version" placeholder="如 2.0.0" style="width: 140px" />
      <el-button type="primary" :loading="testing" @click="run">
        <Play :size="14" class="mr-1" />模拟请求
      </el-button>
    </div>
    <div v-if="result" class="tester-out" :class="outcomeClass">
      <component :is="outcomeIcon" :size="15" class="mr-1" />{{ result.detail }}
    </div>
    <div v-if="result && result.targets.length" class="targets">
      <span class="targets-label">落点实例</span>
      <el-tag v-for="t in result.targets" :key="t" size="small" class="target-tag mc-mono" disable-transitions>{{ t }}</el-tag>
    </div>
    <p class="tester-note">dry-run:按当前实例与规则推演命中/降级,不发真实流量。</p>
  </div>
</template>

<script setup lang="ts">
import { MateMessage } from '@matecloud/ui'
import { ref, computed, watch } from 'vue'
import { Route, Play, CircleCheck, TriangleAlert, CircleX, Shuffle } from 'lucide-vue-next'
import { gatewayApi, type ServiceView, type RouteTestResult } from '@matecloud/core'

const props = defineProps<{ services: ServiceView[] }>()

const service = ref('')
const version = ref('2.0.0')
const testing = ref(false)
const result = ref<RouteTestResult | null>(null)

// 服务列表就绪后默认选中第一个,免得用户先要手动选。
watch(() => props.services, (list) => {
  if (!service.value && list.length) service.value = list[0].service
}, { immediate: true })

const outcomeClass = computed(() => {
  switch (result.value?.outcome) {
    case 'MATCHED': return 'ok'
    case 'FALLBACK': return 'warn'
    case 'NONE': return 'danger'
    default: return 'info'
  }
})
const outcomeIcon = computed(() => {
  switch (result.value?.outcome) {
    case 'MATCHED': return CircleCheck
    case 'FALLBACK': return TriangleAlert
    case 'NONE': return CircleX
    default: return Shuffle
  }
})

async function run() {
  if (!service.value) { MateMessage.warning('请选择服务'); return }
  testing.value = true
  try {
    const res = await gatewayApi.testRoute(service.value, version.value)
    if (res.success && res.data) result.value = res.data
  } catch (e: any) {
    MateMessage.error(e?.msg || '测试失败')
  } finally {
    testing.value = false
  }
}
</script>

<style scoped>
.tester {
  background: var(--mc-bg);
  border: 1px solid var(--mc-divider-regular, rgb(16 24 40 / 0.08));
  border-radius: var(--mc-radius);
  padding: 16px;
}
.tester-title {
  display: flex; align-items: center; gap: 6px;
  font-size: 14px; font-weight: 600; margin-bottom: 12px;
  color: var(--mc-text-primary);
}
.tester-title svg { color: var(--mc-primary); }
.tester-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.tester-label { font-size: 13px; color: var(--mc-text-muted); }
.tester-out { margin-top: 12px; font-size: 13px; display: flex; align-items: center; }
.tester-out.ok { color: var(--mc-success); }
.tester-out.warn { color: var(--mc-warning); }
.tester-out.danger { color: var(--mc-danger); }
.tester-out.info { color: var(--mc-text-secondary); }
.targets { margin-top: 10px; display: flex; align-items: center; flex-wrap: wrap; gap: 6px; }
.targets-label { font-size: 12px; color: var(--mc-text-muted); margin-right: 2px; }
.target-tag { font-family: 'SF Mono', 'JetBrains Mono', monospace; }
.tester-note { margin: 10px 0 0; font-size: 11px; color: var(--mc-text-disabled); }
.mr-1 { margin-right: 4px; }
.mc-mono { font-family: 'SF Mono', 'JetBrains Mono', monospace; }
</style>
