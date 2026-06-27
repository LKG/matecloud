import { ref } from 'vue'

/**
 * Mirrors {@code vip.mate.base.result.BatchResult} on the server.
 * Kept by hand — three fields, stable shape.
 */
export interface BatchFailure {
  id: string
  message: string
}

export interface BatchResult {
  successCount: number
  failCount: number
  failures: BatchFailure[]
}

/**
 * Wraps a batch HTTP call so the host view doesn't have to write the same
 * try / try-each / aggregate pattern in every list page.
 *
 * Two flavours, depending on whether the backend supports a real batch endpoint:
 *
 *   1. {@link runBatch} — call ONE batch endpoint with a list of ids and
 *      surface the {@link BatchResult} the server returns. Preferred when
 *      the API exposes {@code POST /xxx/batch-something}.
 *
 *   2. {@link runEach} — fall back to calling a single-row endpoint N times.
 *      Same shape comes back so the UI can render identically. Useful when
 *      the backend hasn't grown a batch endpoint yet but the UI already needs
 *      the bulk action.
 *
 * Both forms expose a {@code running} ref so the caller can disable buttons
 * during the round-trip.
 */
export function useBatch() {
  const running = ref(false)

  async function runBatch<T = BatchResult>(
    fn: (ids: string[]) => Promise<{ data: T }>,
    ids: string[],
  ): Promise<T> {
    if (!ids.length) {
      // Match the server's empty-batch shape so callers don't need a special branch.
      return { successCount: 0, failCount: 0, failures: [] } as unknown as T
    }
    running.value = true
    try {
      const res = await fn(ids)
      return res.data
    } finally {
      running.value = false
    }
  }

  async function runEach(
    fn: (id: string) => Promise<unknown>,
    ids: string[],
  ): Promise<BatchResult> {
    const result: BatchResult = { successCount: 0, failCount: 0, failures: [] }
    if (!ids.length) return result
    running.value = true
    try {
      // Sequential rather than Promise.all — keeps server-side load predictable
      // and lets the BizError messages stay attributable to specific ids.
      for (const id of ids) {
        try {
          await fn(id)
          result.successCount++
        } catch (e: any) {
          result.failures.push({ id, message: e?.msg || e?.message || 'unknown' })
          result.failCount++
        }
      }
      return result
    } finally {
      running.value = false
    }
  }

  return { runBatch, runEach, running }
}
