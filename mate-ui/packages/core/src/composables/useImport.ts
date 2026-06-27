import { ref } from 'vue'
import { client } from '../api/client'
import type { Result } from '../types/result'

/**
 * Mirrors {@code vip.mate.starter.excel.model.ImportResult} on the server.
 * Kept in sync by hand — there are only four fields and they've been stable.
 */
export interface ImportResult {
  total: number
  success: number
  fail: number
  errors: string[]
}

/**
 * Browser-side Excel import helper.
 *
 * Uses the shared {@code client} (not raw axios like {@link useExport} does)
 * because we WANT the response interceptor to run: the server returns
 * {@code Result<ImportResult>} as JSON, and the interceptor unwraps it
 * exactly like every other API call. Only downloads need raw axios — uploads
 * travel through the normal pipe.
 *
 * Usage:
 *
 *   const { importExcel, uploading } = useImport()
 *   const result = await importExcel('/users/import', file)
 *   // result: { total, success, fail, errors[] }
 */
export function useImport() {
  const uploading = ref(false)

  async function importExcel(path: string, file: File): Promise<ImportResult> {
    uploading.value = true
    try {
      const form = new FormData()
      form.append('file', file)
      // IMPORTANT: do NOT set Content-Type explicitly — axios / the browser
      // needs to add the boundary parameter automatically. Setting the
      // header to 'multipart/form-data' without a boundary breaks parsing
      // at the Spring side.
      const res = await client.post<any, Result<ImportResult>>(path, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 120_000, // imports can take a while; default 15s is too short
      })
      return res.data
    } finally {
      uploading.value = false
    }
  }

  return { importExcel, uploading }
}
