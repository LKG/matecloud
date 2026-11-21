/**
 * CRM 条件查询(动态高级筛选)共享逻辑 — RFC-066。
 *
 * 职责:
 *  1. useCrmAdvancedQuery(module): 拉取字段元数据 + 个人常用筛选 CRUD 状态。
 *  2. buildAdvancedQuery(...): 把列表页"快捷栏"折算成条件组, 叠加用户高级组(组间 OR),
 *     产出统一 AdvancedQuery 请求体。keyword 按 kwFields 扇出成多个 OR 组,
 *     每组都 AND 上快捷条件, 保持与原 page() 相同的多列模糊搜索语义。
 */
import { ref } from 'vue'
import { queryApi } from '@matecloud/core'
import type {
  AdvancedQuery, ConditionGroup, QueryCondition, QueryFieldMeta, SavedFilterVO,
} from '@matecloud/core'

/** 快捷栏折算参数。 */
export interface QuickFold {
  pageNum: number
  pageSize: number
  mineOnly?: boolean
  /** 简单等值条件(status/level 等快捷下拉), 空值由调用方剔除。 */
  base?: QueryCondition[]
  /** 关键字模糊搜索值。 */
  kw?: string
  /** 关键字扇出的字段 fieldKey 列表。 */
  kwFields?: string[]
  /** 用户高级条件组(组间 OR)。 */
  groups?: ConditionGroup[]
  orderField?: string
  orderAsc?: boolean
}

function condNotEmpty(c: QueryCondition): boolean {
  if (!c.fieldKey || !c.operator) return false
  if (c.operator === 'IS_NULL' || c.operator === 'IS_NOT_NULL') return true
  return !!c.values && c.values.length > 0 && c.values.some((v) => v !== '' && v != null)
}

/** 高级组里是否存在有效条件(决定走 search 还是沿用 page)。 */
export function hasValidAdvanced(groups: ConditionGroup[]): boolean {
  return (groups || []).some((g) => (g.conditions || []).some(condNotEmpty))
}

/** 折算成统一 AdvancedQuery 请求体。 */
export function buildAdvancedQuery(fold: QuickFold): AdvancedQuery {
  const quick = (fold.base || []).filter(condNotEmpty)
  const kws: QueryCondition[] = fold.kw?.trim()
    ? (fold.kwFields || []).map((fieldKey) => ({ fieldKey, operator: 'LIKE', values: [fold.kw!.trim()] }))
    : []
  const users = (fold.groups || [])
    .map((g) => ({ conditions: (g.conditions || []).filter(condNotEmpty) }))
    .filter((g) => g.conditions.length > 0)

  // 组间 OR: 每个输出组 = 一个 keyword 扇出 × 一个用户组 + 快捷条件(AND)。
  const kwUnits: QueryCondition[][] = kws.length ? kws.map((k) => [k]) : [[]]
  const userUnits: QueryCondition[][] = users.length ? users.map((u) => u.conditions) : [[]]

  const groups: ConditionGroup[] = []
  for (const ku of kwUnits) {
    for (const uu of userUnits) {
      const conditions = [...ku, ...quick, ...uu]
      if (conditions.length) groups.push({ conditions })
    }
  }

  const q: AdvancedQuery = { groups, pageNum: fold.pageNum, pageSize: fold.pageSize }
  if (fold.mineOnly) q.mineOnly = true
  if (fold.orderField) {
    q.orderField = fold.orderField
    q.orderAsc = fold.orderAsc
  }
  return q
}

/** 模块级查询状态: 字段元数据 + 常用筛选。 */
export function useCrmAdvancedQuery(moduleKey: string) {
  const fields = ref<QueryFieldMeta[]>([])
  const savedFilters = ref<SavedFilterVO[]>([])
  const advGroups = ref<ConditionGroup[]>([])

  async function loadFields() {
    try {
      const res = await queryApi.fields(moduleKey)
      fields.value = res.data || []
    } catch {
      fields.value = []
    }
  }
  async function loadSaved() {
    try {
      const res = await queryApi.savedFilter.list(moduleKey)
      savedFilters.value = res.data || []
    } catch {
      savedFilters.value = []
    }
  }
  async function saveAs(name: string): Promise<boolean> {
    try {
      await queryApi.savedFilter.save({ moduleKey, name, query: { groups: advGroups.value } })
      await loadSaved()
      return true
    } catch {
      return false
    }
  }
  async function removeSaved(id: string): Promise<boolean> {
    try {
      await queryApi.savedFilter.remove(id)
      await loadSaved()
      return true
    } catch {
      return false
    }
  }
  /** 套用常用筛选(深拷贝, 避免共享引用)。 */
  function applySaved(sf: SavedFilterVO) {
    const cloned: ConditionGroup[] = (sf.query?.groups || []).map((g) => ({
      conditions: (g.conditions || []).map((c) => ({ ...c, values: c.values ? [...c.values] : undefined })),
    }))
    advGroups.value = cloned
  }
  function clearAdvanced() {
    advGroups.value = []
  }

  return { fields, savedFilters, advGroups, loadFields, loadSaved, saveAs, removeSaved, applySaved, clearAdvanced }
}
