import { reactive, createApp, h } from 'vue'
import MateMessageBoxHost from './MateMessageBoxHost.vue'

/**
 * MateMessageBox — token-styled modal confirm / alert / prompt, drop-in
 * replacement for Element Plus `ElMessageBox`. Same call surface
 * (`confirm/alert/prompt(message, title?, options?)`, callable form) and the
 * same Promise contract: resolves on confirm, **rejects with `'cancel'`** on any
 * dismissal (cancel button / × / ESC / overlay) — so existing
 * `.catch(() => {})` and `if (e !== 'cancel')` call sites keep working unchanged.
 *
 * Host (`MateMessageBoxHost`) is mounted once in App.vue; a detached fallback
 * self-mounts only when no in-tree host exists. See {@link MateMessage}.
 */
export type MateBoxType = 'success' | 'error' | 'warning' | 'info'

export interface MateMessageBoxOptions {
  title?: string
  message?: string
  type?: MateBoxType
  confirmButtonText?: string
  cancelButtonText?: string
  showCancelButton?: boolean
  /** dismiss when the dim overlay is clicked. Default true. */
  closeOnClickModal?: boolean
  /** confirm button uses the danger (red) style — for destructive actions. */
  confirmDanger?: boolean
  // ---- prompt only ----
  showInput?: boolean
  inputValue?: string
  inputType?: string
  inputPlaceholder?: string
  inputPattern?: RegExp
  inputValidator?: (value: string) => boolean | string
  inputErrorMessage?: string
}

export interface MateBoxItem extends MateMessageBoxOptions {
  id: number
  kind: 'confirm' | 'alert' | 'prompt'
  inputModel: string
  inputError: string
  resolve: (value: unknown) => void
  reject: (reason?: unknown) => void
}

export const messageBoxState = reactive<{ boxes: MateBoxItem[] }>({ boxes: [] })

let seed = 0
let hostRegistered = false
let lazyMounted = false

export function notifyBoxHostRegistered() { hostRegistered = true }

function ensureHost() {
  if (hostRegistered || lazyMounted || typeof document === 'undefined') return
  lazyMounted = true
  const root = document.createElement('div')
  root.className = 'mate-messagebox-root'
  document.body.appendChild(root)
  createApp({ render: () => h(MateMessageBoxHost) }).mount(root)
}

function dropBox(id: number) {
  const idx = messageBoxState.boxes.findIndex(b => b.id === id)
  if (idx !== -1) messageBoxState.boxes.splice(idx, 1)
}

/** Confirm pressed — validate (prompt) then resolve & remove. Returns false if
 *  validation failed so the host can keep the dialog open. */
export function confirmBox(id: number): boolean {
  const box = messageBoxState.boxes.find(b => b.id === id)
  if (!box) return true
  if (box.kind === 'prompt') {
    const v = box.inputModel
    if (box.inputPattern && !box.inputPattern.test(v)) {
      box.inputError = box.inputErrorMessage || '输入格式不正确'
      return false
    }
    if (box.inputValidator) {
      const r = box.inputValidator(v)
      if (r === false || typeof r === 'string') {
        box.inputError = typeof r === 'string' ? r : (box.inputErrorMessage || '输入格式不正确')
        return false
      }
    }
    box.inputError = ''
    box.resolve({ value: v, action: 'confirm' })
  } else {
    box.resolve('confirm')
  }
  dropBox(id)
  return true
}

/** Cancel / × / ESC / overlay — reject with 'cancel' (EP-compatible). */
export function cancelBox(id: number) {
  const box = messageBoxState.boxes.find(b => b.id === id)
  if (!box) return
  box.reject('cancel')
  dropBox(id)
}

function normalize(
  message: string,
  titleOrOptions?: string | MateMessageBoxOptions,
  options?: MateMessageBoxOptions,
): MateMessageBoxOptions & { message: string } {
  if (titleOrOptions && typeof titleOrOptions === 'object') return { message, ...titleOrOptions }
  return { message, title: titleOrOptions, ...(options || {}) }
}

function open(kind: MateBoxItem['kind'], opts: MateMessageBoxOptions & { message: string }): Promise<any> {
  ensureHost()
  return new Promise((resolve, reject) => {
    const id = ++seed
    messageBoxState.boxes.push(reactive({
      id,
      kind,
      title: opts.title ?? (kind === 'prompt' ? '请输入' : '提示'),
      message: opts.message ?? '',
      type: opts.type,
      confirmButtonText: opts.confirmButtonText ?? '确定',
      cancelButtonText: opts.cancelButtonText ?? '取消',
      showCancelButton: opts.showCancelButton ?? (kind !== 'alert'),
      closeOnClickModal: opts.closeOnClickModal ?? true,
      confirmDanger: opts.confirmDanger ?? false,
      showInput: opts.showInput ?? (kind === 'prompt'),
      inputType: opts.inputType ?? 'text',
      inputPlaceholder: opts.inputPlaceholder ?? '',
      inputPattern: opts.inputPattern,
      inputValidator: opts.inputValidator,
      inputErrorMessage: opts.inputErrorMessage,
      inputModel: opts.inputValue ?? '',
      inputError: '',
      resolve,
      reject,
    }) as MateBoxItem)
  })
}

/** Resolved value of a prompt — mirrors ElMessageBox.prompt's `{ value, action }`. */
export interface MatePromptResult { value: string; action: 'confirm' }

type BoxArgs = [
  message: string,
  titleOrOptions?: string | MateMessageBoxOptions,
  options?: MateMessageBoxOptions,
]
type ConfirmCall = (...args: BoxArgs) => Promise<'confirm'>
type PromptCall = (...args: BoxArgs) => Promise<MatePromptResult>

type MateMessageBoxApi = {
  (options: MateMessageBoxOptions & { message: string }): Promise<'confirm'>
  confirm: ConfirmCall
  alert: ConfirmCall
  prompt: PromptCall
  close: () => void
}

const MateMessageBox = ((options: MateMessageBoxOptions & { message: string }) =>
  open('confirm', options)) as MateMessageBoxApi

MateMessageBox.confirm = (m, t, o) => open('confirm', normalize(m, t, o))
MateMessageBox.alert = (m, t, o) => open('alert', normalize(m, t, o))
MateMessageBox.prompt = (m, t, o) => open('prompt', normalize(m, t, o))
MateMessageBox.close = () => { messageBoxState.boxes.slice().forEach(b => cancelBox(b.id)) }

export { MateMessageBox }
