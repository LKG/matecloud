import { client } from '../client'
import type { Result } from '../../types/result'

/**
 * Aggregated dashboard payload.
 *
 * <p>Mirrors the {@code DashboardVO} record on the server
 * (mate-admin/MonitorController). Field names are snake-free per Java's
 * Jackson defaults so no name remapping needed on the wire.
 *
 * <p>{@code last7Days} arrives chronological (oldest first). {@code services}
 * arrives in display order (gateway → auth → system → admin → notice).
 */
export interface DashboardVO {
  userCount: number
  todayLoginCount: number
  todayOpCount: number
  onlineCount: number
  last7Days: DailyStat[]
  services: ServiceStatus[]
}

export interface DailyStat {
  /** ISO date {@code yyyy-MM-dd} */
  date: string
  apiCalls: number
  logins: number
}

export interface ServiceStatus {
  name: string
  url: string
  /** {@code "UP" | "DOWN" | "UNKNOWN"} */
  state: 'UP' | 'DOWN' | 'UNKNOWN'
  /** Round-trip ms; null if the probe was skipped. */
  latencyMs: number | null
}

export const dashboardApi = {
  get: () => client.get<any, Result<DashboardVO>>('/admin/monitor/dashboard'),
}
