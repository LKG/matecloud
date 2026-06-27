import type { Component } from 'vue'

export interface ListColumn {
  /** Unique key, also used as row[key] accessor */
  key: string
  /** Display label for column header */
  label: string
  /** Fixed width (px or css string). If omitted, column grows to fill. */
  width?: string
  /** Text alignment */
  align?: 'left' | 'center' | 'right'
  /** Use monospace font */
  mono?: boolean
}
