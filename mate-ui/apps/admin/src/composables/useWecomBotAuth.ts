import { MateMessage } from '@matecloud/ui'
import { ref } from 'vue'

/**
 * 企业微信「智能机器人」官方 JS SDK 扫码授权(灵感 MateClaw {@code useWecomBotAuth}).
 *
 * <p>动态加载企微 CDN 上的 SDK, 调用 {@code openBotInfoAuthWindow} 弹出企微原生
 * 扫码授权窗口, 用户在企微 APP 内扫码 → 授权后浏览器直接拿到 {@code botid} + {@code secret}。
 * 全程不走自建后端, 是企微官方推荐的「智能机器人接入」方式。</p>
 *
 * <p>独立于「自建应用」配置 (corpId + agentId + secret), 两套凭据互不影响。</p>
 */
const SDK_URL = 'https://wwcdn.weixin.qq.com/node/wework/js/wecom-aibot-sdk@0.1.0.min.js'
const SOURCE = 'matecloud'

let sdkLoadPromise: Promise<void> | null = null

function loadSDK(): Promise<void> {
  if ((window as any).WecomAIBotSDK) return Promise.resolve()
  if (sdkLoadPromise) return sdkLoadPromise
  sdkLoadPromise = new Promise<void>((resolve, reject) => {
    const script = document.createElement('script')
    script.src = SDK_URL
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => {
      sdkLoadPromise = null  // allow retry next call
      reject(new Error('WeCom SDK load failed'))
    }
    document.body.appendChild(script)
  })
  return sdkLoadPromise
}

export interface WecomBotAuthResult {
  botid: string
  secret: string
}

export function useWecomBotAuth(onSuccess: (r: WecomBotAuthResult) => void) {
  const loading = ref(false)

  async function start() {
    loading.value = true
    try {
      await loadSDK()
    } catch {
      MateMessage.error('企业微信 SDK 加载失败 (请检查网络是否能访问 wwcdn.weixin.qq.com)')
      loading.value = false
      return
    }

    const sdk = (window as any).WecomAIBotSDK
    if (!sdk || typeof sdk.openBotInfoAuthWindow !== 'function') {
      MateMessage.error('企业微信 SDK 加载异常, 无 openBotInfoAuthWindow 方法')
      loading.value = false
      return
    }
    loading.value = false

    let result: Promise<WecomBotAuthResult> | undefined
    try {
      result = sdk.openBotInfoAuthWindow({ source: SOURCE })
    } catch (e: any) {
      MateMessage.error('打开扫码授权窗口失败: ' + (e?.message || e))
      return
    }
    if (!result || typeof result.then !== 'function') return

    result.then(
      (bot) => {
        if (bot?.botid && bot?.secret) {
          onSuccess(bot)
          MateMessage.success(`授权成功, 已捕获机器人 ${bot.botid.slice(0, 8)}…`)
        } else {
          MateMessage.warning('授权返回结果不完整')
        }
      },
      (error: { code?: string; message?: string }) => {
        if (error?.code === 'WINDOW_BLOCKED') {
          MateMessage.error('授权窗口被浏览器拦截, 请检查弹窗权限')
        } else if (error?.code === 'CANCELLED') {
          MateMessage.info('已取消授权')
        } else {
          MateMessage.error('授权失败: ' + (error?.message || error?.code || '未知错误'))
        }
      },
    )
  }

  return { loading, start }
}
