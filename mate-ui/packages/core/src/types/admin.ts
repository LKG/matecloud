/**
 * Admin-related types — aligned with mate-admin backend.
 *
 * These supplement the types defined inline in api/modules/admin.ts
 * and cover the Dict / Log detail entities added in RFC-046.
 */

export interface DictType {
  id: string
  dictName: string
  dictType: string
  status: number
  remark?: string
  createdAt: string
}

export interface CreateDictTypeCommand {
  dictName: string
  dictType: string
  status: number
  remark?: string
}

export interface UpdateDictTypeCommand extends Partial<CreateDictTypeCommand> {
  id: string
}

export interface DictDataItem {
  id: string
  dictType: string
  dictLabel: string
  dictValue: string
  sort: number
  status: number
  remark?: string
  createdAt: string
}

export interface CreateDictDataCommand {
  dictType: string
  dictLabel: string
  dictValue: string
  sort: number
  status: number
  remark?: string
}

export interface UpdateDictDataCommand extends Partial<CreateDictDataCommand> {
  id: string
}

/** Extended operation log with request/response detail */
export interface OperationLogDetail {
  id: string
  userId: string
  username: string
  module: string
  operationType: string
  requestMethod: string
  requestUrl: string
  requestParams?: string
  responseResult?: string
  clientIp: string
  location?: string
  userAgent?: string
  status: number
  errorMsg?: string
  duration: number
  createdAt: string
}

/** Extended login log with full context for the detail dialog */
export interface LoginLogDetail {
  id: string
  username: string
  clientIp: string
  location?: string
  userAgent?: string
  browser?: string
  os?: string
  loginType: string
  status: number
  failMsg?: string
  createdAt: string
}
