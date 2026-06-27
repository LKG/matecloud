import { client } from '../client'
import type { Result } from '../../types/result'

/** 灰度路由模式。 */
export type RouteMode = 'OFF' | 'HEADER' | 'WEIGHTED'

/** 限流维度。 */
export type RateLimitKeyType = 'IP' | 'USER' | 'PATH' | 'SERVICE'

/** 单服务灰度规则(与后端 GrayRule record 对齐)。 */
export interface GrayRule {
  service: string
  mode: RouteMode
  headerName: string
  weights: Record<string, number>
  fallbackVersion: string
  enabled: boolean
}

/** 单服务限流规则。 */
export interface RateLimitRule {
  service: string
  enabled: boolean
  key: RateLimitKeyType
  rate: number
  interval: number
}

/** 单服务超时覆盖规则。 */
export interface TimeoutRule {
  service: string
  enabled: boolean
  connectTimeoutMs: number
  responseTimeoutMs: number
}

/** 某版本的实例计数。 */
export interface VersionStat {
  version: string
  count: number
}

/** 单个实例明细。 */
export interface InstanceView {
  host: string
  port: number
  version: string
}

/** 服务总览:实例/版本分布 + 实例明细 + 三类规则现状。 */
export interface ServiceView {
  service: string
  instanceCount: number
  versions: VersionStat[]
  instances: InstanceView[]
  gray: GrayRule
  rateLimit: RateLimitRule
  timeout: TimeoutRule
}

/** 路由 dry-run 结果。 */
export interface RouteTestResult {
  service: string
  mode: RouteMode
  requestedVersion: string
  outcome: 'ROUND_ROBIN' | 'MATCHED' | 'FALLBACK' | 'NONE'
  hitVersion: string
  hitInstanceCount: number
  detail: string
  /** 落点实例地址(host:port (version)),最多 20 个。 */
  targets: string[]
}

/** 全局超时视图。 */
export interface TimeoutView {
  globalConnectTimeoutMs: number | null
  globalResponseTimeoutMs: number | null
  overrides: TimeoutRule[]
}

/**
 * 网关治理 API —— 直连网关本地接口 {@code /api/v1/gateway/**}(client baseURL 已含 /api/v1)。
 * 仅超管可访问(网关侧 admin-paths 限定 ROLE_ADMIN)。
 */
export const gatewayApi = {
  /** 服务 + 实例版本分布 + 规则现状,三页共用。 */
  services: () => client.get<any, Result<ServiceView[]>>('/gateway/services'),

  grayRules: () => client.get<any, Result<GrayRule[]>>('/gateway/gray'),
  saveGray: (rule: GrayRule) => client.put<any, Result<void>>('/gateway/gray', rule),
  testRoute: (service: string, version: string) =>
    client.post<any, Result<RouteTestResult>>('/gateway/gray/test', { service, version }),

  rateLimitRules: () => client.get<any, Result<RateLimitRule[]>>('/gateway/ratelimit'),
  saveRateLimit: (rule: RateLimitRule) => client.put<any, Result<void>>('/gateway/ratelimit', rule),

  timeoutView: () => client.get<any, Result<TimeoutView>>('/gateway/timeout'),
  saveTimeout: (rule: TimeoutRule) => client.put<any, Result<void>>('/gateway/timeout', rule),
}
