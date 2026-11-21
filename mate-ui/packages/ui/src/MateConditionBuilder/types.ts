/**
 * 条件查询构建器类型 — 与后端 vip.mate.crm.query 下发的元数据/请求体结构对齐。
 * (与 @matecloud/core 的同名类型结构兼容, ui 包不依赖 core 故本地声明。)
 */

export type QueryFieldType = 'TEXT' | 'NUMBER' | 'DATE' | 'DATETIME' | 'SELECT' | 'USER' | 'DEPT' | 'BOOLEAN'

export type FilterOperator =
  | 'EQ' | 'NE' | 'LIKE' | 'GT' | 'GE' | 'LT' | 'LE'
  | 'IN' | 'BETWEEN' | 'IS_NULL' | 'IS_NOT_NULL'

export interface QueryFieldOption {
  value: string
  label: string
}

/** 可查字段元数据(不含真实列名)。 */
export interface QueryFieldMeta {
  fieldKey: string
  label: string
  fieldType: QueryFieldType
  operators: FilterOperator[]
  options?: QueryFieldOption[] | null
}

export interface QueryCondition {
  fieldKey: string
  operator: FilterOperator
  values?: string[]
}

/** 条件组(组内 AND, 组间 OR)。 */
export interface ConditionGroup {
  conditions: QueryCondition[]
}
