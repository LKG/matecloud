import { materialApi, type MaterialItem, type MaterialType } from '@matecloud/core'

/** S3 分片下限 5MB(除最后一片);取 5MB 作为每片大小。 */
const PART_SIZE = 5 * 1024 * 1024

/** 单次整传与分片的分界:超过该大小走分片直传(应小于 servlet 上限,留余量)。 */
export const SINGLE_MAX = 20 * 1024 * 1024

/** 单片最大重试次数(应对网络抖动);abort 不重试。 */
const PART_MAX_RETRY = 3

function sleep(ms: number) {
  return new Promise((r) => setTimeout(r, ms))
}

/**
 * 把一片 blob 经预签名 URL 直传对象存储。用 XHR 以拿到字节级进度;
 * 预签名 URL 自带签名,<b>不能</b>附加任何鉴权头,否则签名校验失败。
 */
function putPart(
  url: string,
  blob: Blob,
  onLoaded: (loaded: number) => void,
  signal?: AbortSignal,
): Promise<void> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('PUT', url)
    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) onLoaded(e.loaded)
    }
    xhr.onload = () =>
      xhr.status >= 200 && xhr.status < 300
        ? resolve()
        : reject(new Error(`分片上传失败 (HTTP ${xhr.status})`))
    xhr.onerror = () => reject(new Error('分片上传网络错误'))
    if (signal) {
      signal.addEventListener('abort', () => {
        xhr.abort()
        reject(new DOMException('aborted', 'AbortError'))
      })
    }
    xhr.send(blob)
  })
}

/**
 * 大文件分片直传:init → 逐片预签名 PUT(浏览器直传对象存储,不经应用服务器)→ complete。
 * 任一步失败则 abort 释放已上传分片。{@code onProgress} 回传 0–100 的整体百分比。
 */
export async function chunkedUpload(
  file: File,
  type: MaterialType,
  categoryId: string | undefined,
  onProgress: (percent: number) => void,
  signal?: AbortSignal,
): Promise<MaterialItem> {
  const contentType = file.type || 'application/octet-stream'
  const { data: init } = await materialApi.initMultipart({
    type,
    filename: file.name,
    contentType,
    size: file.size,
  })
  const { objectName, uploadId } = init
  try {
    const totalParts = Math.max(1, Math.ceil(file.size / PART_SIZE))
    let baseLoaded = 0
    for (let i = 0; i < totalParts; i++) {
      const partNumber = i + 1
      const start = i * PART_SIZE
      const blob = file.slice(start, Math.min(start + PART_SIZE, file.size))
      // 每片重试:预签名 URL 有时效,故每次重试都重新申请;abort 立即放弃
      for (let attempt = 1; ; attempt++) {
        try {
          const { data: part } = await materialApi.partUrl({ objectName, uploadId, partNumber })
          await putPart(
            part.url,
            blob,
            (loaded) => onProgress(Math.min(99, Math.round(((baseLoaded + loaded) / file.size) * 100))),
            signal,
          )
          break
        } catch (e: any) {
          if (e?.name === 'AbortError' || attempt >= PART_MAX_RETRY) throw e
          await sleep(attempt * 500)
        }
      }
      baseLoaded += blob.size
      onProgress(Math.min(99, Math.round((baseLoaded / file.size) * 100)))
    }
    const { data: vo } = await materialApi.completeMultipart({
      type,
      categoryId,
      objectName,
      uploadId,
      filename: file.name,
      size: file.size,
      contentType,
    })
    onProgress(100)
    return vo
  } catch (e) {
    // 释放已上传分片,忽略 abort 自身的失败
    await materialApi.abortMultipart(objectName, uploadId).catch(() => {})
    throw e
  }
}
