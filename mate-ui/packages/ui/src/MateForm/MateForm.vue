<template>
  <el-form
    ref="formRef"
    :model="model"
    :label-width="labelWidth"
    :label-position="labelPosition"
    v-bind="$attrs"
  >
    <el-row :gutter="16">
      <template v-for="item in visibleSchema" :key="item.field">
        <el-col :span="item.span || 24">
          <el-form-item :label="item.label" :prop="item.field" :rules="item.rules">
            <!-- Input -->
            <el-input
              v-if="item.type === 'input'"
              v-model="model[item.field]"
              :placeholder="item.placeholder || item.label"
              :disabled="isDisabled(item)"
              clearable
              v-bind="item.props"
            />
            <!-- Password -->
            <el-input
              v-else-if="item.type === 'password'"
              v-model="model[item.field]"
              type="password"
              show-password
              :placeholder="item.placeholder || item.label"
              :disabled="isDisabled(item)"
              v-bind="item.props"
            />
            <!-- Textarea -->
            <el-input
              v-else-if="item.type === 'textarea'"
              v-model="model[item.field]"
              type="textarea"
              :rows="3"
              :placeholder="item.placeholder || item.label"
              :disabled="isDisabled(item)"
              v-bind="item.props"
            />
            <!-- Number -->
            <el-input-number
              v-else-if="item.type === 'number'"
              v-model="model[item.field]"
              :min="0"
              :disabled="isDisabled(item)"
              controls-position="right"
              style="width: 100%"
              v-bind="item.props"
            />
            <!-- Select -->
            <el-select
              v-else-if="item.type === 'select'"
              v-model="model[item.field]"
              :placeholder="item.placeholder || item.label"
              :disabled="isDisabled(item)"
              clearable
              style="width: 100%"
              v-bind="item.props"
            >
              <el-option
                v-for="opt in item.options || []"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
            <!-- Radio -->
            <el-radio-group
              v-else-if="item.type === 'radio'"
              v-model="model[item.field]"
              :disabled="isDisabled(item)"
            >
              <el-radio
                v-for="opt in item.options || []"
                :key="opt.value"
                :value="opt.value"
              >
                {{ opt.label }}
              </el-radio>
            </el-radio-group>
            <!-- Switch -->
            <el-switch
              v-else-if="item.type === 'switch'"
              v-model="model[item.field]"
              :disabled="isDisabled(item)"
              v-bind="item.props"
            />
            <!-- Date / Datetime / Range pickers -->
            <el-date-picker
              v-else-if="isDateField(item.type)"
              v-model="model[item.field]"
              :type="(item.type as any)"
              :placeholder="item.placeholder || item.label"
              :start-placeholder="item.props?.startPlaceholder"
              :end-placeholder="item.props?.endPlaceholder"
              :value-format="item.props?.valueFormat || defaultValueFormat(item.type)"
              :disabled="isDisabled(item)"
              style="width: 100%"
              v-bind="item.props"
            />
            <!-- Custom slot -->
            <slot
              v-else-if="item.type === 'custom'"
              :name="`field-${item.field}`"
              :model="model"
              :field="item.field"
              :disabled="isDisabled(item)"
            />
          </el-form-item>
        </el-col>
      </template>
    </el-row>
  </el-form>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import type { FormInstance } from 'element-plus'
import type { FormSchema, FormFieldType } from './types'

/**
 * MateForm — schema-driven form builder.
 *
 * Rendering:
 *   Each schema entry maps to an el-form-item with the appropriate input
 *   element selected by {@code type}. Supports dynamic visibility/disabled
 *   via predicate functions evaluated against the current model.
 *
 * Model binding:
 *   The component owns a reactive {@code model} that mirrors {@code v-model}.
 *   Important: we seed the model ONCE per "dialog open" (i.e. on schema change
 *   or an explicit reset), NOT on every modelValue deep change — otherwise
 *   typing in the form would immediately echo back and overwrite itself when
 *   the parent re-runs reactive effects.
 *
 * Exposed API:
 *   formRef      — underlying ElForm instance (for native validate / clear)
 *   validate()   — returns Promise<boolean>, rejects if invalid
 *   resetFields()— delegates to ElForm.resetFields (restores to defaults)
 *   getModel()   — shallow copy of the current model
 *   setModel()   — bulk-set fields (used by the parent when opening edit mode)
 */
const props = defineProps<{
  schema: FormSchema[]
  modelValue?: Record<string, any>
  labelWidth?: string
  labelPosition?: 'top' | 'left' | 'right'
}>()

const emit = defineEmits<{
  'update:modelValue': [val: Record<string, any>]
}>()

const formRef = ref<FormInstance>()
const model = reactive<Record<string, any>>({})

function seedModel(source?: Record<string, any>) {
  // Clear previous keys we seeded
  for (const k of Object.keys(model)) delete (model as any)[k]
  for (const item of props.schema) {
    const external = source?.[item.field]
    model[item.field] = external !== undefined ? external : (item.defaultValue ?? undefined)
  }
}

seedModel(props.modelValue)

// Reseed when the schema changes (e.g. a new dialog mounted with different fields)
watch(() => props.schema, () => seedModel(props.modelValue))

// If the parent PASSES in a new modelValue object instance (not nested mutation),
// treat that as an explicit "open with new data" and reseed. We intentionally do
// not deep-watch to avoid the "typing echoes" loop described above.
watch(() => props.modelValue, (val) => seedModel(val))

// Emit updates upward so v-model parent can read final values
watch(model, (val) => emit('update:modelValue', { ...val }), { deep: true })

const visibleSchema = computed(() =>
  props.schema.filter(item => !item.visible || item.visible(model)),
)

function isDisabled(item: FormSchema): boolean {
  if (typeof item.disabled === 'function') return item.disabled(model)
  return !!item.disabled
}

function isDateField(type: FormFieldType): boolean {
  return type === 'date' || type === 'datetime' || type === 'daterange' || type === 'datetimerange'
}

function defaultValueFormat(type: FormFieldType): string {
  if (type === 'date' || type === 'daterange') return 'YYYY-MM-DD'
  return 'YYYY-MM-DD HH:mm:ss'
}

async function validate(): Promise<boolean> {
  try {
    await formRef.value?.validate()
    return true
  } catch {
    return false
  }
}

function resetFields() {
  formRef.value?.resetFields()
}

function getModel() {
  return { ...model }
}

function setModel(val: Record<string, any>) {
  seedModel(val)
}

defineExpose({ validate, resetFields, getModel, setModel, formRef })

// Evaluate visibility once after mount so rules referencing the model work.
onMounted(() => void visibleSchema.value)
</script>
