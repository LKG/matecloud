import { client } from '../client'
import type { Result } from '../../types/result'

/** Field input type — mirrors backend vip.mate.base.channel.FieldType. */
export type ModelFieldType = 'TEXT' | 'SECRET' | 'TEXTAREA' | 'MAP'

/** Config field metadata for a model provider (drives the dynamic form). */
export interface ModelFieldSpec {
  key: string
  label: string
  type: ModelFieldType
  required: boolean
  placeholder?: string
  tip?: string
}

/** A configured provider: vendor + fields + (masked) current credential values. */
export interface ModelProviderView {
  id: string
  vendor: string
  name: string
  modalities: string[]
  enabled: boolean
  sort: number | null
  fields: ModelFieldSpec[]
  values: Record<string, any>
  /** Editable list of model ids this provider can serve (plaintext). */
  models: string[]
}

/** A registered vendor descriptor (addable provider template). */
export interface ModelDescriptorView {
  vendor: string
  name: string
  describe: string
  modalities: string[]
  fields: ModelFieldSpec[]
  /** Curated common model ids for this vendor (used to pre-fill / suggest). */
  suggestedModels?: string[]
}

/** Per-type default model row (joined with provider name/vendor). */
export interface SystemModelView {
  modelType: string
  providerId: string | null
  providerName: string | null
  vendor: string | null
  model: string | null
}

/** External OpenAI-compatible gateway config (token masked on read). */
export interface ModelGatewayView {
  enabled: boolean
  gatewayType: string
  baseUrl: string | null
  token: string
  defaultGroup: string | null
  modelMapping: string | null
}

export interface ModelTestResult {
  success: boolean
  message: string
}

/** Payload for creating / updating a provider. */
export interface ModelProviderRequest {
  vendor: string
  name: string
  modalities: string[]
  enabled: boolean
  sort?: number | null
  values: Record<string, any>
  /** Editable list of model ids this provider can serve. */
  models?: string[]
}

/** Payload for saving the external gateway config. */
export interface ModelGatewayRequest {
  enabled: boolean
  gatewayType: string
  baseUrl: string | null
  token: string
  defaultGroup: string | null
  modelMapping: string | null
}

/**
 * AI model configuration center (系统管理 · 配置中心 · 模型配置).
 *
 * The platform is a Spring AI *consumer*: configure providers + credentials,
 * pick a default model per modality, and optionally delegate supply to an
 * external OpenAI-compatible gateway. Each provider self-describes its
 * credential fields (descriptor) so a single generic form covers every vendor.
 * Secret fields come back masked ("******") and are preserved on save when left
 * masked / blank. NOTE: this is the new /admin/model/* surface — not /ai/providers.
 */
export const modelApi = {
  /** List configured providers for the current tenant (secrets masked). */
  providers: () =>
    client.get<any, Result<ModelProviderView[]>>('/admin/model/providers'),

  /** Create a provider (id auto-assigned). Returns the new provider id. */
  createProvider: (req: ModelProviderRequest) =>
    client.post<any, Result<string>>('/admin/model/providers', req),

  /** Create or update a provider. Returns the provider id. */
  saveProvider: (id: string, req: ModelProviderRequest) =>
    client.put<any, Result<string>>(`/admin/model/providers/${id}`, req),

  deleteProvider: (id: string) =>
    client.delete<any, Result<void>>(`/admin/model/providers/${id}`),

  /** Lightweight connectivity probe against the provider's base URL. */
  testProvider: (id: string) =>
    client.post<any, Result<ModelTestResult>>(`/admin/model/providers/${id}/test`),

  /**
   * Live-fetch the provider's model list via its OpenAI-compatible /models endpoint
   * (主流对话产品-style). Requires a saved provider (credentials decrypted server-side only).
   */
  fetchModels: (id: string) =>
    client.post<any, Result<string[]>>(`/admin/model/providers/${id}/fetch-models`),

  /** All registered vendor descriptors (addable provider templates). */
  descriptors: () =>
    client.get<any, Result<ModelDescriptorView[]>>('/admin/model/descriptors'),

  /** All logical model types (modalities). */
  modalities: () =>
    client.get<any, Result<string[]>>('/admin/model/modalities'),

  /** Per-type default model assignments for the current tenant. */
  systemModels: () =>
    client.get<any, Result<SystemModelView[]>>('/admin/model/system-models'),

  /** Assign (or update) the default provider+model for a model type. */
  saveSystemModel: (type: string, providerId: string, model: string) =>
    client.put<any, Result<void>>(`/admin/model/system-models/${type}`, { providerId, model }),

  /** External gateway config (token masked on read). */
  gateway: () =>
    client.get<any, Result<ModelGatewayView>>('/admin/model/gateway'),

  /** Save gateway config; blank / masked token keeps the stored one. */
  saveGateway: (req: ModelGatewayRequest) =>
    client.put<any, Result<void>>('/admin/model/gateway', req),

  /** Probe the configured gateway base URL. */
  testGateway: () =>
    client.post<any, Result<ModelTestResult>>('/admin/model/gateway/test'),
}
