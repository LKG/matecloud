import { MateMessage } from '@matecloud/ui'
import type { App } from 'vue'
import { BizError } from '@matecloud/core'

/**
 * 全局错误网 —— 与 useAsync 的逐请求错误处理互补的最后一道防线:
 * 捕获 Vue 渲染/生命周期里未被 try-catch 的异常, 以及全局 unhandledrejection,
 * 统一降级为可读 toast + 结构化 console 分组, 杜绝"白屏无提示"。
 *
 * 设计: 已被业务层处理过的错误 (BizError, 多由 useAsync.onError 透出) 不再二次弹窗;
 * 节流防止同类错误刷屏; 401/403 已由 api client 的 CustomEvent 接管, 这里只兜真正未处理的。
 */
const THROTTLE_MS = 3000
let lastShownAt = 0
let lastMessage = ''

function notify(message: string, raw: unknown) {
  const now = Date.now()
  // 同消息 3s 内只弹一次, 防循环报错刷屏
  if (message === lastMessage && now - lastShownAt < THROTTLE_MS) return
  lastShownAt = now
  lastMessage = message
  MateMessage.error(message)
  // 结构化日志便于排查 (toast 只给摘要, 控制台给全貌)
  console.groupCollapsed(`%c[error-net] ${message}`, 'color:#f56c6c')
  console.error(raw)
  console.groupEnd()
}

function readableOf(err: unknown): string {
  if (err instanceof BizError) return err.msg || err.code || '操作失败'
  if (err instanceof Error) return err.message || '发生未知错误'
  if (typeof err === 'string') return err
  return '发生未知错误'
}

/** 安装到 Vue app + window; 在 app.mount 之前调用。 */
export function installErrorNet(app: App) {
  app.config.errorHandler = (err, _instance, info) => {
    // info 是 Vue 的错误来源 (如 'render', 'setup'); 已处理的 BizError 不再弹
    if (err instanceof BizError) {
      console.debug('[error-net] handled BizError surfaced to Vue:', err.code, info)
      return
    }
    notify(readableOf(err), err)
  }

  window.addEventListener('unhandledrejection', (event) => {
    const reason = event.reason
    // 业务层已 toast 过的 (useAsync.onError) 走的是 reject, 这里避免重复:
    // BizError 视为已处理, 仅记录不弹窗
    if (reason instanceof BizError) {
      console.debug('[error-net] unhandled BizError rejection (already surfaced):', reason.code)
      return
    }
    notify(readableOf(reason), reason)
  })
}
