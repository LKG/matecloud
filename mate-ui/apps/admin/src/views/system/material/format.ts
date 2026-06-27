export function fmtSize(n: number): string {
  if (!n) return '0 B'
  const u = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let v = n
  while (v >= 1024 && i < u.length - 1) {
    v /= 1024
    i++
  }
  return (i === 0 ? v : v.toFixed(1)) + ' ' + u[i]
}

/** 后端 createdAt 形如 "yyyy-MM-dd HH:mm:ss" → 展示 "MM-dd HH:mm" */
export function fmtDate(s?: string): string {
  if (!s) return ''
  const t = s.replace('T', ' ')
  return t.length >= 16 ? t.slice(5, 16) : t
}
