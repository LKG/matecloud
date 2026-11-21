<template>
  <div class="mc-cb">
    <div v-for="(group, gi) in groups" :key="gi" class="mc-cb-group">
      <div class="mc-cb-group__head">
        <span class="mc-cb-group__title">条件组 {{ gi + 1 }}</span>
        <span v-if="gi > 0" class="mc-cb-group__rel">OR</span>
        <button class="mc-cb-link mc-cb-link--danger" @click="removeGroup(gi)">删除组</button>
      </div>

      <div v-for="(cond, ci) in group.conditions" :key="ci" class="mc-cb-row">
        <span v-if="ci > 0" class="mc-cb-row__rel">AND</span>

        <el-select v-model="cond.fieldKey" placeholder="字段" style="width: 150px" @change="onFieldChange(cond)">
          <el-option v-for="f in fields" :key="f.fieldKey" :label="f.label" :value="f.fieldKey" />
        </el-select>

        <el-select v-model="cond.operator" placeholder="算子" style="width: 120px" @change="onOperatorChange(cond)">
          <el-option v-for="op in operatorsOf(cond.fieldKey)" :key="op" :label="OPERATOR_LABELS[op]" :value="op" />
        </el-select>

        <!-- 空判断: 无需值 -->
        <span v-if="isNullCheck(cond.operator)" class="mc-cb-hint">—</span>

        <!-- 多值(IN) -->
        <template v-else-if="isMultiValue(cond)">
          <el-select
            v-if="typeOf(cond) === 'SELECT'"
            :model-value="cond.values || []" multiple clearable style="width: 240px"
            placeholder="值(多选)"
            @update:model-value="(v: any) => (cond.values = (v || []).map(String))"
          >
            <el-option v-for="o in optionsOf(cond.fieldKey)" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
          <el-select
            v-else
            :model-value="cond.values || []" multiple filterable clearable allow-create default-first-option
            style="width: 260px" placeholder="值(回车可添加多个)"
            @update:model-value="(v: any) => (cond.values = (v || []).map(String))"
          >
            <el-option v-for="o in optionsOf(cond.fieldKey)" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </template>

        <!-- 区间(BETWEEN): 两个值 -->
        <template v-else-if="cond.operator === 'BETWEEN'">
          <div class="mc-cb-between">
            <template v-if="typeOf(cond) === 'DATE' || typeOf(cond) === 'DATETIME'">
              <el-date-picker
                :model-value="valueOf(cond, 0)" :type="typeOf(cond) === 'DATE' ? 'date' : 'datetime'"
                :value-format="typeOf(cond) === 'DATE' ? 'YYYY-MM-DD' : 'YYYY-MM-DD HH:mm:ss'"
                placeholder="起" style="width: 170px"
                @update:model-value="(v: any) => setValue(cond, 0, v)"
              />
              <span class="mc-cb-between__sep">~</span>
              <el-date-picker
                :model-value="valueOf(cond, 1)" :type="typeOf(cond) === 'DATE' ? 'date' : 'datetime'"
                :value-format="typeOf(cond) === 'DATE' ? 'YYYY-MM-DD' : 'YYYY-MM-DD HH:mm:ss'"
                placeholder="止" style="width: 170px"
                @update:model-value="(v: any) => setValue(cond, 1, v)"
              />
            </template>
            <template v-else-if="typeOf(cond) === 'NUMBER'">
              <el-input-number
                :model-value="valueOf(cond, 0) == null ? undefined : Number(valueOf(cond, 0))"
                :controls="false" placeholder="最小" style="width: 110px"
                @update:model-value="(v: any) => setValue(cond, 0, v == null ? '' : String(v))"
              />
              <span class="mc-cb-between__sep">~</span>
              <el-input-number
                :model-value="valueOf(cond, 1) == null ? undefined : Number(valueOf(cond, 1))"
                :controls="false" placeholder="最大" style="width: 110px"
                @update:model-value="(v: any) => setValue(cond, 1, v == null ? '' : String(v))"
              />
            </template>
            <template v-else>
              <el-input :model-value="valueOf(cond, 0)" placeholder="起" style="width: 120px" @update:model-value="(v: any) => setValue(cond, 0, v)" />
              <span class="mc-cb-between__sep">~</span>
              <el-input :model-value="valueOf(cond, 1)" placeholder="止" style="width: 120px" @update:model-value="(v: any) => setValue(cond, 1, v)" />
            </template>
          </div>
        </template>

        <!-- 单值 -->
        <template v-else>
          <el-select
            v-if="typeOf(cond) === 'SELECT'"
            :model-value="valueOf(cond, 0)" clearable style="width: 180px" placeholder="值"
            @update:model-value="(v: any) => setValue(cond, 0, v)"
          >
            <el-option v-for="o in optionsOf(cond.fieldKey)" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
          <el-select
            v-else-if="typeOf(cond) === 'BOOLEAN'"
            :model-value="valueOf(cond, 0)" clearable style="width: 120px" placeholder="值"
            @update:model-value="(v: any) => setValue(cond, 0, v)"
          >
            <el-option label="是" value="true" />
            <el-option label="否" value="false" />
          </el-select>
          <el-date-picker
            v-else-if="typeOf(cond) === 'DATE' || typeOf(cond) === 'DATETIME'"
            :model-value="valueOf(cond, 0)" :type="typeOf(cond) === 'DATE' ? 'date' : 'datetime'"
            :value-format="typeOf(cond) === 'DATE' ? 'YYYY-MM-DD' : 'YYYY-MM-DD HH:mm:ss'"
            clearable style="width: 180px" placeholder="值"
            @update:model-value="(v: any) => setValue(cond, 0, v)"
          />
          <el-input-number
            v-else-if="typeOf(cond) === 'NUMBER'"
            :model-value="valueOf(cond, 0) == null ? undefined : Number(valueOf(cond, 0))"
            :controls="false" placeholder="值" style="width: 150px"
            @update:model-value="(v: any) => setValue(cond, 0, v == null ? '' : String(v))"
          />
          <el-input
            v-else
            :model-value="valueOf(cond, 0)" clearable style="width: 180px"
            :placeholder="cond.operator === 'LIKE' ? '包含关键字' : '值'"
            @update:model-value="(v: any) => setValue(cond, 0, v)"
          />
        </template>

        <button class="mc-cb-link mc-cb-link--danger" @click="group.conditions.splice(ci, 1)">删除</button>
      </div>

      <button class="mc-cb-link" @click="addCondition(group)">+ 添加条件</button>
    </div>

    <div class="mc-cb-foot">
      <button class="mc-cb-link" @click="addGroup()">+ 添加条件组(组间 OR)</button>
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ConditionGroup, FilterOperator, QueryCondition, QueryFieldMeta, QueryFieldType } from './types'

/**
 * MateConditionBuilder — 动态高级筛选条件构建器。
 *
 * 组内 AND / 组间 OR; 每行 字段→算子→值, 值控件按字段元数据 fieldType 渲染,
 * 算子下拉按 field.operators 过滤。值统一以 string 数组存 QueryCondition.values,
 * 类型转换/参数绑定由后端安全翻译器完成。
 */
const props = defineProps<{ fields: QueryFieldMeta[] }>()
const groups = defineModel<ConditionGroup[]>({ required: true })

const OPERATOR_LABELS: Record<FilterOperator, string> = {
  EQ: '等于', NE: '不等于', LIKE: '包含', GT: '大于', GE: '大于等于',
  LT: '小于', LE: '小于等于', IN: '属于', BETWEEN: '区间',
  IS_NULL: '为空', IS_NOT_NULL: '不为空',
}

function fieldOf(fieldKey?: string): QueryFieldMeta | undefined {
  return props.fields.find((f) => f.fieldKey === fieldKey)
}
function typeOf(cond: QueryCondition): QueryFieldType {
  return fieldOf(cond.fieldKey)?.fieldType ?? 'TEXT'
}
function operatorsOf(fieldKey?: string): FilterOperator[] {
  return fieldOf(fieldKey)?.operators ?? ['EQ', 'NE', 'LIKE', 'IS_NULL', 'IS_NOT_NULL']
}
function optionsOf(fieldKey?: string) {
  return fieldOf(fieldKey)?.options ?? []
}
function isNullCheck(op: FilterOperator): boolean {
  return op === 'IS_NULL' || op === 'IS_NOT_NULL'
}
function isMultiValue(cond: QueryCondition): boolean {
  return cond.operator === 'IN'
}

function valueOf(cond: QueryCondition, index: number): string | undefined {
  return cond.values?.[index]
}
function setValue(cond: QueryCondition, index: number, v: any) {
  const values = cond.values ? [...cond.values] : []
  values[index] = v == null ? '' : String(v)
  // 空串尾巴裁剪: 全空则置 undefined 便于"有效条件"判断
  while (values.length > 0 && values[values.length - 1] === '') values.pop()
  cond.values = values.length ? values : undefined
}

function onFieldChange(cond: QueryCondition) {
  const ops = operatorsOf(cond.fieldKey)
  if (!cond.operator || !ops.includes(cond.operator)) cond.operator = ops[0]
  cond.values = undefined
}
function onOperatorChange(cond: QueryCondition) {
  if (isNullCheck(cond.operator)) cond.values = undefined
  else if (isMultiValue(cond) && !Array.isArray(cond.values)) cond.values = []
}

function addCondition(group: ConditionGroup) {
  const first = props.fields[0]
  group.conditions.push({
    fieldKey: first?.fieldKey ?? '',
    operator: operatorsOf(first?.fieldKey)[0],
    values: undefined,
  })
}
function addGroup() {
  const group: ConditionGroup = { conditions: [] }
  addCondition(group)
  groups.value = [...groups.value, group]
}
function removeGroup(gi: number) {
  groups.value = groups.value.filter((_, i) => i !== gi)
}

/** 是否含有效条件(供页面判断走 search 还是走原 page)。 */
const hasValidConditions = computed(() =>
  (groups.value || []).some((g) => (g.conditions || []).some((c) => c.fieldKey && c.operator && (isNullCheck(c.operator) || (c.values && c.values.length > 0)))),
)
defineExpose({ hasValidConditions })
</script>

<style scoped>
.mc-cb {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.mc-cb-group {
  border: 1px solid var(--el-border-color-lighter, #e4e7ed);
  border-radius: 8px;
  padding: 12px;
  background: var(--el-fill-color-blank, #fff);
}
.mc-cb-group__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.mc-cb-group__title {
  font-size: 13px;
  font-weight: 600;
}
.mc-cb-group__rel {
  font-size: 12px;
  color: var(--el-color-primary, #409eff);
  font-weight: 600;
}
.mc-cb-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.mc-cb-row__rel {
  font-size: 12px;
  color: var(--el-text-color-secondary, #909399);
  width: 34px;
  text-align: center;
  flex-shrink: 0;
}
.mc-cb-between {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.mc-cb-between__sep {
  color: var(--el-text-color-secondary, #909399);
}
.mc-cb-hint {
  color: var(--el-text-color-placeholder, #a8abb2);
}
.mc-cb-link {
  border: none;
  background: none;
  cursor: pointer;
  color: var(--el-color-primary, #409eff);
  font-size: 12px;
  padding: 2px 4px;
}
.mc-cb-link--danger {
  color: var(--el-color-danger, #f56c6c);
}
.mc-cb-foot {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
