/**
 * Capability icon helper — maps a capability key to an inline line-SVG markup
 * string (no emoji), mirroring the prototype's `capIcon`. Used by the model
 * selector / system-model rows to annotate model abilities. The returned string
 * is the inner <svg> markup; callers wrap it (e.g. v-html) and supply title.
 */
export type CapKey = 'vision' | 'tools' | 'stream' | 'json' | 'reason' | 'async'

interface CapDef {
  /** i18n key suffix under modelConfig.cap.* */
  label: string
  /** svg fill */
  fill: 'none' | 'currentColor'
  /** inner svg path markup */
  path: string
}

export const CAPS: Record<CapKey, CapDef> = {
  vision: { label: 'vision', fill: 'none', path: '<path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="2.6"/>' },
  tools: { label: 'tools', fill: 'none', path: '<path d="m9 8-3.5 4 3.5 4M15 8l3.5 4L15 16"/>' },
  stream: { label: 'stream', fill: 'currentColor', path: '<path d="M13 2 4 13h6l-1 9 9-12h-6z"/>' },
  json: { label: 'json', fill: 'none', path: '<path d="M9 4c-2 0-2 3-2 4s0 3-2 4c2 1 2 3 2 4s0 4 2 4M15 4c2 0 2 3 2 4s0 3 2 4c-2 1-2 3-2 4s0 4-2 4"/>' },
  reason: { label: 'reason', fill: 'currentColor', path: '<path d="M12 3l1.6 4.7L18 9l-4.4 1.3L12 15l-1.6-4.7L6 9l4.4-1.3z"/>' },
  async: { label: 'async', fill: 'none', path: '<circle cx="12" cy="12" r="9"/><path d="M12 7.5v5l3 2"/>' },
}

export const CAP_KEYS: CapKey[] = ['vision', 'tools', 'stream', 'json', 'reason', 'async']

/** Inner <svg> markup for a capability (empty string for unknown keys). */
export function capSvg(key: string): string {
  const c = CAPS[key as CapKey]
  if (!c) return ''
  return `<svg viewBox="0 0 24 24" fill="${c.fill}" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" width="14" height="14">${c.path}</svg>`
}
