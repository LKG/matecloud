import { reactive, createApp, h } from 'vue'
import MateMessageHost from './MateMessageHost.vue'

/**
 * MateMessage — token-styled toast, drop-in replacement for Element Plus
 * `ElMessage`. Same call surface (`MateMessage.success(msg, opts?)`, callable
 * `MateMessage(msg)`, `.closeAll()`) so migrating a call site is a pure import
 * swap. Reference UX: 主流前端产品的 toast (singleton manager + portal host +
 * typed icons + 玻璃拟态卡片), implemented for Vue 3 with zero extra deps — icons
 * are inline SVG and every color comes from the global `--mc-*` design tokens,
 * so dark mode follows automatically.
 */
export type MateMessageType = 'success' | 'error' | 'warning' | 'info'
export type MateMessagePlacement = 'top' | 'center' | 'bottom'

export interface MateMessageOptions {
  message?: string
  type?: MateMessageType
  /** ms; `0` keeps it open until closed manually. Default 3000. */
  duration?: number
  /** show an inline close (×) button. Default false. */
  showClose?: boolean
  /** where on screen the toast appears. Default 'top' (top-center, like EP). */
  placement?: MateMessagePlacement
  /** invoked after the toast leaves the DOM. */
  onClose?: () => void
}

export interface MateMessageItem {
  id: number
  message: string
  type: MateMessageType
  placement: MateMessagePlacement
  showClose: boolean
  duration: number
  onClose?: () => void
  timer?: ReturnType<typeof setTimeout>
}

export interface MateMessageHandle {
  close: () => void
}

// Module-level singleton store shared between the imperative API (callable
// from anywhere, incl. plain .ts composables) and the host component.
export const messageState = reactive<{ items: MateMessageItem[] }>({ items: [] })

let seed = 0
let hostRegistered = false
let lazyMounted = false

/** Called by MateMessageHost.onMounted so the lazy fallback knows a host already
 *  lives in the app tree (e.g. mounted once in App.vue) and stays out of the way. */
export function notifyHostRegistered() { hostRegistered = true }

function ensureHost() {
  // Prefer an in-tree <MateMessageHost/> (App.vue) — synchronous reactivity, no
  // mount-timing gap. Only when none exists (plain .ts usage, non-admin app)
  // do we self-mount a detached app, mirroring how ElMessage bootstraps itself.
  if (hostRegistered || lazyMounted || typeof document === 'undefined') return
  lazyMounted = true
  const root = document.createElement('div')
  root.className = 'mate-message-root'
  document.body.appendChild(root)
  createApp({ render: () => h(MateMessageHost) }).mount(root)
}

function remove(id: number) {
  const idx = messageState.items.findIndex(i => i.id === id)
  if (idx === -1) return
  const [item] = messageState.items.splice(idx, 1)
  if (item.timer) clearTimeout(item.timer)
  item.onClose?.()
}

function startTimer(item: MateMessageItem) {
  if (item.duration > 0) item.timer = setTimeout(() => remove(item.id), item.duration)
}

/** Pause auto-dismiss while hovered (parity with EP's behaviour). */
export function pauseMessage(id: number) {
  const item = messageState.items.find(i => i.id === id)
  if (item?.timer) { clearTimeout(item.timer); item.timer = undefined }
}

export function resumeMessage(id: number) {
  const item = messageState.items.find(i => i.id === id)
  if (item && !item.timer) startTimer(item)
}

export function closeMessage(id: number) { remove(id) }

function normalize(arg: string | MateMessageOptions): MateMessageOptions {
  return typeof arg === 'string' ? { message: arg } : { ...arg }
}

function add(opts: MateMessageOptions): MateMessageHandle {
  ensureHost()
  const id = ++seed
  const item: MateMessageItem = reactive({
    id,
    message: opts.message ?? '',
    type: opts.type ?? 'info',
    placement: opts.placement ?? 'top',
    showClose: opts.showClose ?? false,
    duration: opts.duration ?? 3000,
    onClose: opts.onClose,
  })
  messageState.items.push(item)
  startTimer(item)
  return { close: () => remove(id) }
}

type MateMessageApi = {
  (arg: string | MateMessageOptions): MateMessageHandle
  success: (arg: string | MateMessageOptions) => MateMessageHandle
  error: (arg: string | MateMessageOptions) => MateMessageHandle
  warning: (arg: string | MateMessageOptions) => MateMessageHandle
  info: (arg: string | MateMessageOptions) => MateMessageHandle
  closeAll: () => void
}

const MateMessage = ((arg: string | MateMessageOptions) => add(normalize(arg))) as MateMessageApi

;(['success', 'error', 'warning', 'info'] as const).forEach((type) => {
  MateMessage[type] = (arg: string | MateMessageOptions) => add({ ...normalize(arg), type })
})

MateMessage.closeAll = () => {
  messageState.items.slice().forEach(i => remove(i.id))
}

export { MateMessage }
