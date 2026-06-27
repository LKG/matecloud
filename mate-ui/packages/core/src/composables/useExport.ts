import axios, { type AxiosRequestConfig } from 'axios'

/**
 * Browser-side Excel export helper.
 *
 * Uses a raw axios instance (NOT the shared {@code client} from
 * {@code api/client.ts}) because the shared client's response interceptor
 * unwraps {@code Result<T>} — that strips the {@code Content-Disposition}
 * header we need to read the server-suggested filename. We reconstruct the
 * same auth/tenant headers manually so the two code paths stay interchangeable
 * from the backend's perspective.
 *
 * Usage:
 *   const { exportExcel, downloading } = useExport()
 *   await exportExcel('/users/export', { keyword: 'admin' })
 *
 * The Blob is consumed by creating a temporary {@code <a download>} anchor;
 * the object URL is released right after click so memory doesn't leak even
 * on rapid repeated exports.
 */
import { ref } from 'vue'

export function useExport() {
  const downloading = ref(false)

  async function exportExcel(
    path: string,
    params?: Record<string, any>,
    fallbackFileName?: string,
  ): Promise<void> {
    downloading.value = true
    try {
      const config: AxiosRequestConfig = {
        baseURL: '/api/v1',
        method: 'get',
        url: path,
        params,
        responseType: 'blob',
        timeout: 60_000,
        headers: buildHeaders(),
      }
      const res = await axios.request<Blob>(config)
      const filename = resolveFileName(res.headers?.['content-disposition'], fallbackFileName)
      triggerDownload(res.data, filename)
    } finally {
      downloading.value = false
    }
  }

  return { exportExcel, downloading }
}

/** Pull Sa-Token + Tenant id out of localStorage — same contract as client.ts. */
function buildHeaders(): Record<string, string> {
  const headers: Record<string, string> = {}
  const token = typeof localStorage !== 'undefined' ? localStorage.getItem('mate_token') : null
  if (token) {
    const headerName = localStorage.getItem('mate_token_name') || 'Authorization'
    headers[headerName] = token
  }
  const tenantId = typeof localStorage !== 'undefined' ? localStorage.getItem('mate_tenant_id') : null
  if (tenantId) headers['X-Tenant-Id'] = tenantId
  return headers
}

/**
 * Parse RFC 5987 / RFC 6266 {@code filename*=UTF-8''encoded} and legacy
 * {@code filename="..."} from a Content-Disposition header. Falls back to
 * the caller-supplied value or a timestamped default.
 */
function resolveFileName(header: string | undefined, fallback?: string): string {
  if (header) {
    const star = /filename\*\s*=\s*UTF-8''([^;]+)/i.exec(header)
    if (star) {
      try {
        return decodeURIComponent(star[1])
      } catch {
        /* fall through */
      }
    }
    const plain = /filename\s*=\s*"?([^";]+)"?/i.exec(header)
    if (plain) {
      // Server in mate-excel-starter URL-encodes the base name so we run
      // one decode pass to get something readable (e.g. users.xlsx).
      try {
        return decodeURIComponent(plain[1])
      } catch {
        return plain[1]
      }
    }
  }
  if (fallback) return fallback
  const ts = new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-')
  return `export-${ts}.xlsx`
}

function triggerDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
  // Release immediately — browsers keep the blob alive while the download
  // task is running, revoking the URL doesn't cancel it.
  URL.revokeObjectURL(url)
}
