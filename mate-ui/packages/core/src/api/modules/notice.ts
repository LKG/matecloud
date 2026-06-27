import { client } from '../client'
import type { Result, PageResult } from '../../types/result'

/** Notice channel — aligned with backend NoticeChannel. */
export type NoticeChannel = 'SMS' | 'EMAIL' | 'WECHAT' | 'PUSH'

/** Notice business type — aligned with backend BusinessType. */
export type NoticeBusinessType = 'VERIFY_CODE' | 'ORDER_NOTIFY' | 'SYSTEM_ALERT' | 'MARKETING'

/** Notice status code: 0=PENDING, 1=SUCCESS, 2=FAILED. */
export interface NoticeRecord {
  id: string
  channel: NoticeChannel
  target: string
  status: number
  statusLabel: string
  businessType: string
  content: string
  resultMessage?: string
  retryCount: number
  sentAt?: string
  createdAt: string
}

export interface NoticeQuery {
  pageNum?: number
  pageSize?: number
  channel?: string
  status?: number
  businessType?: string
  keyword?: string
}

export interface SendNoticeCommand {
  channel: NoticeChannel
  target: string
  businessType: NoticeBusinessType
  content: string
}

/**
 * Notice center API — aligned with NoticeController (gateway path /notice).
 */
export const noticeApi = {
  /** Paginated notice records, newest first. */
  page: (params?: NoticeQuery) =>
    client.get<any, Result<PageResult<NoticeRecord>>>('/notice', { params }),

  /** Send a notification (params are query string, not body). */
  send: (data: SendNoticeCommand) =>
    client.post<any, Result<string>>('/notice/send', null, { params: data }),

  /** Retry a failed notification. */
  retry: (noticeId: string) =>
    client.post<any, Result<void>>('/notice/retry', null, { params: { noticeId } }),
}
