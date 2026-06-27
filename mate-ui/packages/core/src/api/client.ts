import axios from 'axios'
import type { Result } from '../types/result'
import { BizError } from '../error/BizError'
import { tryMock } from './mock/data'

export const client = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

/**
 * Request interceptor:
 * - Inject Sa-Token from localStorage (aligned with mate-sa-token-starter)
 *   The header name defaults to {@code Authorization} (matching
 *   sa-token.token-name in mate-defaults.yml) but falls back to a stored
 *   {@code mate_token_name} so the backend can change its scheme without
 *   a frontend code change.
 * - Inject Tenant ID (aligned with mate-tenant-starter)
 */
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('mate_token')
  if (token) {
    const headerName = localStorage.getItem('mate_token_name') || 'Authorization'
    // Sa-Token expects "Bearer <token>" (configured via sa-token.token-prefix in mate-defaults.yml)
    config.headers[headerName] = token.startsWith('Bearer ') ? token : `Bearer ${token}`
  }

  const tenantId = localStorage.getItem('mate_tenant_id')
  if (tenantId) {
    config.headers['X-Tenant-Id'] = tenantId
  }

  return config
})

/**
 * Response interceptor:
 * - Unwrap Result<T> from backend
 * - Throw BizError on business failure
 * - Handle HTTP 401 (gateway-level Sa-Token reject) → redirect to login
 * - Handle backend SECB001/SECB002 in 200-wrapped envelopes → also redirect
 * - Handle 403 (no permission)
 * - Handle 429 (rate limited by mate-security-starter @RateLimit)
 *
 * Why SECB001 also redirects: mate-auth's currentUser() throws BizException
 * for "Not logged in"; GlobalExceptionHandler returns HTTP 200 with
 * {"success":false,"code":"SECB001",...} so the HTTP-status branch below
 * never fires for service-tier session loss. Without this, the SPA shows a
 * toast and stays on the protected route instead of pushing to /login.
 */
const SESSION_GONE_CODES = new Set(['SECB001', 'SECB002'])

function handleSessionGone() {
  localStorage.removeItem('mate_token')
  localStorage.removeItem('mate_token_name')
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('mate:unauthorized'))
  }
}

client.interceptors.response.use(
  (response) => {
    const data = response.data as Result<unknown>

    // Backend always returns Result<T> wrapper
    if (data && typeof data === 'object' && 'success' in data && data.success === false) {
      if (data.code && SESSION_GONE_CODES.has(data.code)) {
        handleSessionGone()
      }
      throw new BizError(data.code, data.msg, data.traceId)
    }

    return data as any
  },
  (error) => {
    const status = error.response?.status
    // traceId 优先取响应体 (Result.traceId, 无跨域读头限制), 兜底取响应头 X-Trace-Id
    // (网关自产的拒绝响应——限流/鉴权失败——只有头, 没有 Result 体)。
    const traceId: string | undefined =
      error.response?.data?.traceId ?? error.response?.headers?.['x-trace-id']

    // ---- Gateway errors (502/503/504) or network failure → try mock ----
    const isUnreachable = !error.response || status === 502 || status === 503 || status === 504
    if (isUnreachable) {
      const mockResult = tryMock(error.config || {})
      if (mockResult) {
        console.warn('[MateCloud] Backend unreachable — using mock data for:', error.config?.url)
        if (mockResult.success === false) {
          throw new BizError(mockResult.code, mockResult.msg)
        }
        return mockResult as any
      }
      throw new BizError('NETWORK', 'Network error — backend is not running')
    }

    // ---- Real backend errors ----
    switch (status) {
      case 401:
        // Emit custom event so the app layer (router) can handle navigation
        // without creating a circular dependency between core and router.
        handleSessionGone()
        throw new BizError('401', 'Session expired, please login again', traceId)
      case 403:
        if (typeof window !== 'undefined') {
          window.dispatchEvent(new CustomEvent('mate:forbidden'))
        }
        throw new BizError('403', 'No permission', traceId)
      case 429:
        throw new BizError('429', 'Too many requests, please try later', traceId)
      default:
        throw new BizError(String(status), error.response.data?.msg || 'Server error', traceId)
    }

    throw new BizError(String(status), 'Request failed', traceId)
  },
)
