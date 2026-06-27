/**
 * Aligned with Java: vip.mate.base.result.Result<T>
 */
export interface Result<T = unknown> {
  code: string
  msg: string
  success: boolean
  data: T
  /** 链路追踪 ID — 后端出错时盖章, 用于报障关联日志 (NON_NULL: 可能缺省) */
  traceId?: string
}

export interface PageQuery {
  pageNum?: number
  pageSize?: number
}

export interface PageResult<T> {
  list: T[]
  total: number
}
