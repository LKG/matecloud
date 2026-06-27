export type FormFieldType =
  | 'input'
  | 'password'
  | 'textarea'
  | 'number'
  | 'select'
  | 'radio'
  | 'switch'
  | 'treeSelect'
  | 'date'
  | 'datetime'
  | 'daterange'
  | 'datetimerange'
  | 'custom'

export interface FormSchema {
  /** Key on the model object (required) */
  field: string
  /** Label text shown in the form item */
  label: string
  /** Field renderer to use */
  type: FormFieldType
  /** For select/radio: static options */
  options?: { label: string; value: any }[]
  /** Element Plus rules */
  rules?: any[]
  /** Grid span (out of 24), default 24 */
  span?: number
  /** Placeholder for text-like inputs */
  placeholder?: string
  /** Default value seeded on first render */
  defaultValue?: any
  /** Extra props forwarded to the underlying element (e.g. `max`, `rows`, `show-password`) */
  props?: Record<string, any>
  /** Hide field when this predicate returns false (receives current model) */
  visible?: (model: Record<string, any>) => boolean
  /** Disable field statically or dynamically (receives current model) */
  disabled?: boolean | ((model: Record<string, any>) => boolean)
}
