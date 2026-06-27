import { client } from '../client'
import type { Result } from '../../types/result'

/** Result of one organization sync run (backend SyncResult). */
export interface SsoSyncResult {
  provider: string
  deptCount: number
  userAdded: number
  userUpdated: number
  userRemoved: number
  success: boolean
  error?: string | null
}

export type SsoSyncMode = 'FULL' | 'INCREMENTAL'

/**
 * SSO / 身份接入管理。Provider 凭证走通用渠道配置(channelApi, channel='identity');
 * 这里只补"手动触发组织同步"——拉取外部部门 + 成员落到本地 mate_dept / mate_admin。
 */
export const ssoApi = {
  /** Trigger an organization sync for a provider. */
  sync: (provider: string, mode: SsoSyncMode = 'FULL') =>
    client.post<any, Result<SsoSyncResult>>(`/admin/sso/sync/${provider}`, null, { params: { mode } }),
}
