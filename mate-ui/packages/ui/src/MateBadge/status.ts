export type BadgeType = 'success' | 'warning' | 'danger' | 'info' | 'default'

/**
 * Map a raw status value (string/number/boolean) to a {@link BadgeType}.
 *
 * - {@code domain: 'entity'} (default) — ACTIVE/1 → success, DISABLED/0 → warning, DELETED/-1 → danger.
 * - {@code domain: 'log'} — numeric 0 → success (OK), 1 → danger (Fail).
 */
export function mapStatus(
  status: string | number | boolean | null | undefined,
  domain: 'entity' | 'log' = 'entity',
): BadgeType {
  if (status === null || status === undefined) return 'default'
  if (typeof status === 'boolean') return status ? 'success' : 'warning'

  if (typeof status === 'number') {
    if (domain === 'log') return status === 0 ? 'success' : 'danger'
    if (status === 1) return 'success'
    if (status === 0) return 'warning'
    if (status < 0) return 'danger'
    return 'info'
  }

  const s = String(status).toUpperCase()
  if (s === 'ACTIVE' || s === 'ENABLED' || s === 'SUCCESS' || s === 'OK') return 'success'
  if (s === 'DISABLED' || s === 'FROZEN' || s === 'PENDING' || s === 'WARN' || s === 'WARNING') return 'warning'
  if (s === 'DELETED' || s === 'FAIL' || s === 'FAILED' || s === 'ERROR' || s === 'DANGER') return 'danger'
  if (s === 'INFO') return 'info'
  return 'default'
}
