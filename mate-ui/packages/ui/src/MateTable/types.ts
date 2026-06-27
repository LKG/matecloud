export interface MateColumn {
  /** Row data property key */
  prop: string
  /** Column header label */
  label: string
  /** Fixed width */
  width?: number | string
  /** Minimum width */
  minWidth?: number | string
  /** Fixed position */
  fixed?: 'left' | 'right'
  /** Cell alignment */
  align?: 'left' | 'center' | 'right'
  /** Use monospace font */
  mono?: boolean
  /** Built-in type rendering: datetime formats date, status renders tag */
  type?: 'default' | 'datetime' | 'status'
  /** Show this column? default true */
  show?: boolean
}
