import { MateMessage } from '@matecloud/ui'
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  modelApi,
  type ModelDescriptorView,
  type ModelGatewayView,
  type ModelProviderRequest,
  type ModelProviderView,
  type SystemModelView,
} from '@matecloud/core'

/**
 * Data + operations for the 模型配置 page (Spring AI consumer side).
 * Loads providers / descriptors / system defaults / gateway and exposes
 * the mutating actions. Views consume this so presentation stays thin.
 */
export function useModelConfig() {
  const { t } = useI18n()

  const loading = ref(false)
  const providers = ref<ModelProviderView[]>([])
  const descriptors = ref<ModelDescriptorView[]>([])
  const systemModels = ref<SystemModelView[]>([])
  const gateway = reactive<ModelGatewayView>({
    enabled: false,
    gatewayType: 'NEW_API',
    baseUrl: null,
    token: '',
    defaultGroup: null,
    modelMapping: null,
  })

  async function loadProviders() {
    const { data } = await modelApi.providers()
    providers.value = data || []
  }

  async function loadDescriptors() {
    const { data } = await modelApi.descriptors()
    descriptors.value = data || []
  }

  async function loadSystemModels() {
    const { data } = await modelApi.systemModels()
    systemModels.value = data || []
  }

  async function loadGateway() {
    const { data } = await modelApi.gateway()
    Object.assign(gateway, data)
  }

  async function refresh() {
    loading.value = true
    try {
      await Promise.all([loadProviders(), loadDescriptors(), loadSystemModels(), loadGateway()])
    } finally {
      loading.value = false
    }
  }

  async function saveProvider(id: string | null, req: ModelProviderRequest) {
    if (id) {
      await modelApi.saveProvider(id, req)
    } else {
      await modelApi.createProvider(req)
    }
    MateMessage.success(t('modelConfig.saveSuccess'))
    await Promise.all([loadProviders(), loadSystemModels()])
  }

  async function deleteProvider(id: string) {
    await modelApi.deleteProvider(id)
    MateMessage.success(t('modelConfig.deleted'))
    await Promise.all([loadProviders(), loadSystemModels()])
  }

  async function testProvider(id: string) {
    const { data } = await modelApi.testProvider(id)
    if (data.success) MateMessage.success(data.message || t('modelConfig.testPass'))
    else MateMessage.error(data.message || t('modelConfig.testFail'))
  }

  async function saveSystemModel(type: string, providerId: string, model: string) {
    await modelApi.saveSystemModel(type, providerId, model)
    MateMessage.success(t('modelConfig.saveSuccess'))
    await loadSystemModels()
  }

  async function saveGateway() {
    await modelApi.saveGateway({
      enabled: gateway.enabled,
      gatewayType: gateway.gatewayType,
      baseUrl: gateway.baseUrl,
      token: gateway.token,
      defaultGroup: gateway.defaultGroup,
      modelMapping: gateway.modelMapping,
    })
    MateMessage.success(t('modelConfig.saveSuccess'))
    await loadGateway()
  }

  async function testGateway() {
    const { data } = await modelApi.testGateway()
    if (data.success) MateMessage.success(data.message || t('modelConfig.testPass'))
    else MateMessage.error(data.message || t('modelConfig.testFail'))
  }

  return {
    loading, providers, descriptors, systemModels, gateway,
    refresh, loadProviders, loadSystemModels, loadGateway,
    saveProvider, deleteProvider, testProvider,
    saveSystemModel, saveGateway, testGateway,
  }
}
