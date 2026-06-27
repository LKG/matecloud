import { ref, shallowRef, computed, type Ref, type ComputedRef } from 'vue'

/**
 * 统一异步状态编排 —— 替代 99 个视图各自手写的 loading/try-catch/ElMessage 样板。
 *
 * 一个数据加载有四种 UI 态: 加载中 / 出错(可重试) / 空 / 有数据。本 composable 把这四态
 * 收敛成单一状态机, 配合 `<MateAsync>` 声明式渲染, 保证全站加载体验一致。
 *
 * @example
 * const { data, state, run } = useAsync(() => userApi.list(), { immediate: true })
 * // 模板: <MateAsync :state="state" :empty="!data?.length" @retry="run"> ... </MateAsync>
 */
export type AsyncState = 'idle' | 'loading' | 'error' | 'success'

export interface UseAsyncOptions<T> {
  /** 挂载即执行 (默认 false)。 */
  immediate?: boolean
  /** 初始数据。 */
  initial?: T
  /** 成功回调。 */
  onSuccess?: (data: T) => void
  /** 失败回调 (返回 true 表示已处理, 抑制默认错误透出)。 */
  onError?: (err: unknown) => boolean | void
  /** 重复触发时丢弃在途的旧请求结果 (默认 true, 防竞态错序)。 */
  dropStale?: boolean
}

export interface UseAsyncReturn<T, A extends unknown[]> {
  data: Ref<T | undefined>
  error: Ref<unknown>
  state: Ref<AsyncState>
  loading: ComputedRef<boolean>
  /** 执行(或重试); 透传参数给 fetcher; 返回结果或在出错时 reject。 */
  run: (...args: A) => Promise<T>
  /** 复位到 idle。 */
  reset: () => void
}

export function useAsync<T, A extends unknown[] = []>(
  fetcher: (...args: A) => Promise<T>,
  options: UseAsyncOptions<T> = {},
): UseAsyncReturn<T, A> {
  const { immediate = false, initial, onSuccess, onError, dropStale = true } = options

  const data = shallowRef<T | undefined>(initial) as Ref<T | undefined>
  const error = ref<unknown>(undefined)
  const state = ref<AsyncState>('idle')
  const loading = computed(() => state.value === 'loading')

  let token = 0

  async function run(...args: A): Promise<T> {
    const current = ++token
    state.value = 'loading'
    error.value = undefined
    try {
      const result = await fetcher(...args)
      // 竞态保护: 只有最后一次触发的结果才落地
      if (dropStale && current !== token) return result
      data.value = result
      state.value = 'success'
      onSuccess?.(result)
      return result
    } catch (err) {
      if (dropStale && current !== token) throw err
      error.value = err
      state.value = 'error'
      onError?.(err)
      throw err
    }
  }

  function reset() {
    token++
    data.value = initial
    error.value = undefined
    state.value = 'idle'
  }

  if (immediate) {
    void run(...([] as unknown as A))
  }

  return { data, error, state, loading, run, reset }
}
