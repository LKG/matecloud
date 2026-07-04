import axios from 'axios'
import type { Result } from '../types/result'
import type { LoginResult } from '../types/user'
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
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('mate:unauthorized'))
  }
}

// ---------------------------------------------------------------------------
// 无感刷新 (silent token refresh)
//
// The access token is short-lived — Sa-Token freezes it after 30 min of
// inactivity (sa-token.active-timeout). When a request then comes back 401, we
// exchange the long-lived refresh token for a fresh access token and REPLAY the
// original request, so the user never sees a login bounce until the refresh
// token itself expires.
// ---------------------------------------------------------------------------
const REFRESH_TOKEN_KEY = 'mate_refresh_token'

/** True for the refresh call itself — we must never try to refresh a refresh. */
function isRefreshCall(config?: { url?: string }): boolean {
  return !!config?.url && config.url.includes('/auth/refresh')
}

// Single-flight: a burst of parallel requests all 401 at once, but the refresh
// token is single-use (rotated server-side) — two refreshes would race and the
// second would fail. So every caller awaits ONE shared in-flight refresh.
let refreshPromise: Promise<string | null> | null = null

async function performRefresh(): Promise<string | null> {
  const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
  if (!refreshToken) return null
  try {
    // Bare axios (not `client`) so this bypasses the interceptors below — a
    // failing refresh must not recurse back into refresh handling.
    const res = await axios.post('/api/v1/auth/refresh', { refreshToken })
    const body = res.data as Result<LoginResult>
    if (!body || body.success === false || !body.data) return null
    const lr = body.data
    localStorage.setItem('mate_token', lr.tokenValue)
    localStorage.setItem('mate_token_name', lr.tokenName)
    if (lr.refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY, lr.refreshToken)
    // Let the auth store mirror the new token into its reactive state.
    if (typeof window !== 'undefined') {
      window.dispatchEvent(
        new CustomEvent('mate:token-refreshed', {
          detail: { token: lr.tokenValue, tokenName: lr.tokenName },
        }),
      )
    }
    return lr.tokenValue
  } catch {
    return null
  }
}

function getRefreshedToken(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = performRefresh().finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

/**
 * Try to silently renew the session for a request that came back unauthenticated.
 * Returns true when a fresh access token was obtained and `config` has been
 * marked for a one-shot replay (`client(config)`); false when the caller should
 * give up and end the session. The `_retry` guard prevents an infinite refresh
 * loop if the replayed request 401s again.
 */
async function tryRefresh(config: any): Promise<boolean> {
  if (!config || config._retry || isRefreshCall(config)) return false
  if (!localStorage.getItem(REFRESH_TOKEN_KEY)) return false
  const newToken = await getRefreshedToken()
  if (!newToken) return false
  config._retry = true
  return true
}

client.interceptors.response.use(
  async (response) => {
    const data = response.data as Result<unknown>

    // Backend always returns Result<T> wrapper
    if (data && typeof data === 'object' && 'success' in data && data.success === false) {
      if (data.code && SESSION_GONE_CODES.has(data.code)) {
        // Service-tier session loss (e.g. currentUser() → SECB001). Try a silent
        // refresh + replay before giving up on the session.
        if (await tryRefresh(response.config)) {
          return client(response.config)
        }
        handleSessionGone()
      }
      throw new BizError(data.code, data.msg, data.traceId)
    }

    return data as any
  },
  async (error) => {
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
        // Access token expired/frozen at the gateway. Try a silent refresh +
        // replay of the original request first; only end the session if that
        // fails (no/invalid refresh token).
        if (await tryRefresh(error.config)) {
          return client(error.config)
        }
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
