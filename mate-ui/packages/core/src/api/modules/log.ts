import { client } from '../client'
import type { Result, PageResult } from '../../types/result'

/** Summary row for the list view. Detail dialogs fetch by id for full body. */
export interface OperationLogItem {
  id: string
  userId: string
  username: string
  module: string
  operationType: string
  description?: string
  requestMethod: string
  requestUrl: string
  clientIp: string
  status: number
  errorMsg: string
  duration: number
  createdAt: string
}

export interface LoginLogItem {
  id: string
  username: string
  clientIp: string
  userAgent: string
  loginType: string
  status: number
  failMsg: string
  createdAt: string
}

export interface OperationLogQuery {
  pageNum?: number
  pageSize?: number
  module?: string
  username?: string
  status?: number
  operationType?: string
  /** ISO datetime, inclusive */
  startTime?: string
  /** ISO datetime, inclusive */
  endTime?: string
}

export interface LoginLogQuery {
  pageNum?: number
  pageSize?: number
  username?: string
  status?: number
  startTime?: string
  endTime?: string
}

export const logApi = {
  operationLogs: (params?: OperationLogQuery) =>
    client.get<any, Result<OperationLogItem[] | PageResult<OperationLogItem>>>(
      '/admin/operation-logs',
      { params },
    ),

  operationLogDetail: (id: string) =>
    client.get<any, Result<OperationLogItem & { requestParams?: string; responseResult?: string; userAgent?: string; description?: string; location?: string }>>(
      `/admin/operation-logs/${id}`,
    ),

  /** Batch delete operation logs. */
  operationLogBatchDelete: (ids: string[]) =>
    client.delete<any, Result<void>>('/admin/operation-logs', { data: ids }),

  loginLogs: (params?: LoginLogQuery) =>
    client.get<any, Result<LoginLogItem[] | PageResult<LoginLogItem>>>(
      '/admin/login-logs',
      { params },
    ),

  loginLogDetail: (id: string) =>
    client.get<any, Result<LoginLogItem & { location?: string; browser?: string; os?: string }>>(
      `/admin/login-logs/${id}`,
    ),
}
