/**
 * Business error from backend Result<T>.
 * Aligned with Java: vip.mate.base.exception.BizException
 */
export class BizError extends Error {
  readonly code: string
  readonly msg: string
  /** 链路追踪 ID — 后端返回时带上, 供错误提示展示/复制, 报障关联日志 */
  readonly traceId?: string

  constructor(code: string, msg: string, traceId?: string) {
    super(msg)
    this.code = code
    this.msg = msg
    this.traceId = traceId
    this.name = 'BizError'
    Object.setPrototypeOf(this, BizError.prototype)
  }

  /** Check if this error matches a specific code */
  is(code: string): boolean {
    return this.code === code
  }
}
