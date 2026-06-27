import { MateMessage } from '@matecloud/ui'
import { ref, computed } from 'vue'
import { gatewayApi, type ServiceView } from '@matecloud/core'

/**
 * 网关治理三页(灰度/限流/超时)共享的服务列表数据源。
 *
 * 后端 `/gateway/services` 一次返回服务 + 实例版本分布 + 三类规则现状,三页各取所需,
 * 故抽成一个 composable 复用拉取/loading/错误处理逻辑。
 */
export function useGatewayServices() {
  const services = ref<ServiceView[]>([])
  const loading = ref(false)

  async function load() {
    loading.value = true
    try {
      const res = await gatewayApi.services()
      if (res.success && res.data) services.value = res.data
    } catch (e: any) {
      MateMessage.error(e?.msg || '加载服务失败')
    } finally {
      loading.value = false
    }
  }

  const instanceTotal = computed(() => services.value.reduce((a, s) => a + s.instanceCount, 0))

  return { services, loading, load, instanceTotal }
}
