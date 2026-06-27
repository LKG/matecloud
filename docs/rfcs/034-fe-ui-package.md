# RFC-034: Frontend UI Package (Shared Business Components)

- **Status**: Draft
- **Created**: 2026-04-12
- **Author**: MateCloud Team
- **Wave**: FE-2 (parallel with RFC-033, RFC-035)

## Background

`mate-ui/packages/ui/` provides shared business components built on Element Plus. These components encapsulate common CRUD patterns (table with search, form dialog, file upload) so that every business module page can be assembled declaratively via config objects rather than repetitive template code. All components are TypeScript-first and integrate with `@matecloud/core` for dict translation and API calls.

## Design

---

## Change 1: package.json

Create `mate-ui/packages/ui/package.json`

```json
{
  "name": "@matecloud/ui",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "main": "./src/index.ts",
  "types": "./src/index.ts",
  "exports": {
    ".": {
      "import": "./src/index.ts",
      "types": "./src/index.ts"
    }
  },
  "scripts": {
    "lint": "eslint src/",
    "typecheck": "vue-tsc --noEmit"
  },
  "dependencies": {
    "@matecloud/core": "workspace:*",
    "element-plus": "^2.9.1",
    "vue": "^3.5.13"
  },
  "devDependencies": {
    "typescript": "^5.7.3",
    "vue-tsc": "^2.2.8"
  }
}
```

---

## Change 2: tsconfig.json

Create `mate-ui/packages/ui/tsconfig.json`

```json
{
  "extends": "../../tsconfig.base.json",
  "compilerOptions": {
    "composite": true,
    "rootDir": "src",
    "outDir": "dist",
    "declaration": true,
    "declarationMap": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"]
    }
  },
  "include": ["src/**/*.ts", "src/**/*.vue"],
  "exclude": ["node_modules", "dist"]
}
```

---

## Change 3: MateTable — types.ts

Create `mate-ui/packages/ui/src/MateTable/types.ts`

```typescript
import type { PageQuery, PageResult } from '@matecloud/core';

/**
 * Column type for special rendering.
 * - 'datetime': format as date string
 * - 'dict': translate value via dict store
 * - 'tag': render as el-tag with dict label
 */
export type ColumnType = 'datetime' | 'dict' | 'tag';

/**
 * Column configuration for MateTable.
 */
export interface MateTableColumn {
  /** Field name in row data */
  prop: string;
  /** Column header label */
  label: string;
  /** Column width in px */
  width?: number | string;
  /** Minimum column width in px */
  minWidth?: number | string;
  /** Special column type */
  type?: ColumnType;
  /** Dict type code (required when type is 'dict' or 'tag') */
  dict?: string;
  /** Custom formatter function */
  formatter?: (row: Record<string, any>, column: any, cellValue: any) => string;
  /** Whether column is sortable */
  sortable?: boolean;
  /** Fixed column position */
  fixed?: 'left' | 'right';
  /** Slot name for custom cell rendering */
  slot?: string;
  /** Text alignment */
  align?: 'left' | 'center' | 'right';
  /** Whether to show overflow tooltip */
  showOverflowTooltip?: boolean;
}

/**
 * Props for MateTable component.
 */
export interface MateTableProps {
  /** API function that returns paginated data */
  api: (params: PageQuery & Record<string, any>) => Promise<PageResult<any>>;
  /** Column configurations */
  columns: MateTableColumn[];
  /** Search field configurations (for built-in search bar) */
  searchFields?: MateSearchField[];
  /** Row key field name, default 'id' */
  rowKey?: string;
  /** Whether to show pagination, default true */
  pagination?: boolean;
  /** Default page size, default 10 */
  defaultPageSize?: number;
  /** Whether to show selection column */
  selection?: boolean;
  /** Whether to show index column */
  index?: boolean;
  /** Whether to auto-fetch on mount, default true */
  immediate?: boolean;
}

/**
 * Search field configuration (inline in MateTable).
 */
export interface MateSearchField {
  field: string;
  label: string;
  type: 'input' | 'select' | 'dateRange';
  dict?: string;
  placeholder?: string;
}
```

---

## Change 4: MateTable — useMateTable.ts

Create `mate-ui/packages/ui/src/MateTable/useMateTable.ts`

```typescript
import { ref, reactive, onMounted } from 'vue';
import type { PageQuery, PageResult } from '@matecloud/core';
import type { MateTableProps } from './types';

/**
 * Composable that manages MateTable state: loading, pagination, data fetching.
 */
export function useMateTable(props: MateTableProps) {
  const loading = ref(false);
  const tableData = ref<Record<string, any>[]>([]);
  const total = ref(0);
  const selectedRows = ref<Record<string, any>[]>([]);

  const pagination = reactive<PageQuery>({
    pageNum: 1,
    pageSize: props.defaultPageSize ?? 10,
  });

  const searchParams = reactive<Record<string, any>>({});

  /**
   * Fetch data from the API.
   */
  async function fetchData(): Promise<void> {
    loading.value = true;
    try {
      const params = {
        ...pagination,
        ...searchParams,
      };
      const result: PageResult<any> = await props.api(params);
      tableData.value = result.list;
      total.value = result.total;
    } catch (error) {
      console.error('[MateTable] fetch error:', error);
      tableData.value = [];
      total.value = 0;
    } finally {
      loading.value = false;
    }
  }

  /**
   * Handle search — reset to page 1 and re-fetch.
   */
  function handleSearch(params: Record<string, any>): void {
    Object.assign(searchParams, params);
    pagination.pageNum = 1;
    fetchData();
  }

  /**
   * Handle search reset.
   */
  function handleReset(): void {
    Object.keys(searchParams).forEach((key) => {
      delete searchParams[key];
    });
    pagination.pageNum = 1;
    fetchData();
  }

  /**
   * Handle page change.
   */
  function handlePageChange(page: number): void {
    pagination.pageNum = page;
    fetchData();
  }

  /**
   * Handle page size change.
   */
  function handleSizeChange(size: number): void {
    pagination.pageSize = size;
    pagination.pageNum = 1;
    fetchData();
  }

  /**
   * Handle row selection change.
   */
  function handleSelectionChange(rows: Record<string, any>[]): void {
    selectedRows.value = rows;
  }

  /**
   * Refresh current page data.
   */
  function refresh(): void {
    fetchData();
  }

  // Auto-fetch on mount
  onMounted(() => {
    if (props.immediate !== false) {
      fetchData();
    }
  });

  return {
    loading,
    tableData,
    total,
    selectedRows,
    pagination,
    searchParams,
    fetchData,
    handleSearch,
    handleReset,
    handlePageChange,
    handleSizeChange,
    handleSelectionChange,
    refresh,
  };
}
```

---

## Change 5: MateTable — MateTable.vue

Create `mate-ui/packages/ui/src/MateTable/MateTable.vue`

```vue
<template>
  <div class="mate-table">
    <!-- Search bar (inline) -->
    <MateSearch
      v-if="searchFields && searchFields.length > 0"
      :fields="searchFields"
      v-model="searchModel"
      @search="handleSearch"
      @reset="handleReset"
    />

    <!-- Toolbar slot -->
    <div class="mate-table__toolbar" v-if="$slots.toolbar">
      <slot name="toolbar" />
    </div>

    <!-- Table -->
    <el-table
      v-loading="loading"
      :data="tableData"
      :row-key="rowKey"
      border
      stripe
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <!-- Selection column -->
      <el-table-column v-if="selection" type="selection" width="50" align="center" />

      <!-- Index column -->
      <el-table-column v-if="index" type="index" label="#" width="60" align="center" />

      <!-- Data columns -->
      <el-table-column
        v-for="col in columns"
        :key="col.prop"
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth"
        :sortable="col.sortable"
        :fixed="col.fixed"
        :align="col.align ?? 'center'"
        :show-overflow-tooltip="col.showOverflowTooltip !== false"
      >
        <template #default="{ row, column: elCol, $index }">
          <!-- Custom slot -->
          <slot
            v-if="col.slot"
            :name="col.slot"
            :row="row"
            :column="col"
            :index="$index"
          />

          <!-- Dict tag type -->
          <el-tag
            v-else-if="col.type === 'tag' && col.dict"
            :type="getDictTagType(col.dict, row[col.prop])"
          >
            {{ getDictLabel(col.dict, String(row[col.prop])) }}
          </el-tag>

          <!-- Dict text type -->
          <span v-else-if="col.type === 'dict' && col.dict">
            {{ getDictLabel(col.dict, String(row[col.prop])) }}
          </span>

          <!-- Datetime type -->
          <span v-else-if="col.type === 'datetime'">
            {{ formatDatetime(row[col.prop]) }}
          </span>

          <!-- Custom formatter -->
          <span v-else-if="col.formatter">
            {{ col.formatter(row, elCol, row[col.prop]) }}
          </span>

          <!-- Default -->
          <span v-else>{{ row[col.prop] }}</span>
        </template>
      </el-table-column>

      <!-- Actions column slot -->
      <el-table-column
        v-if="$slots.actions"
        label="Actions"
        fixed="right"
        :width="actionsWidth"
        align="center"
      >
        <template #default="{ row, $index }">
          <slot name="actions" :row="row" :index="$index" />
        </template>
      </el-table-column>
    </el-table>

    <!-- Pagination -->
    <div class="mate-table__pagination" v-if="pagination !== false">
      <el-pagination
        v-model:current-page="paginationState.pageNum"
        v-model:page-size="paginationState.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useDictStore } from '@matecloud/core';
import MateSearch from '../MateSearch/MateSearch.vue';
import { useMateTable } from './useMateTable';
import type { MateTableColumn, MateSearchField } from './types';
import type { PageQuery, PageResult } from '@matecloud/core';

const props = withDefaults(defineProps<{
  api: (params: PageQuery & Record<string, any>) => Promise<PageResult<any>>;
  columns: MateTableColumn[];
  searchFields?: MateSearchField[];
  rowKey?: string;
  pagination?: boolean;
  defaultPageSize?: number;
  selection?: boolean;
  index?: boolean;
  immediate?: boolean;
  actionsWidth?: number | string;
}>(), {
  rowKey: 'id',
  pagination: true,
  defaultPageSize: 10,
  selection: false,
  index: false,
  immediate: true,
  actionsWidth: 200,
});

const emit = defineEmits<{
  (e: 'selection-change', rows: Record<string, any>[]): void;
}>();

const searchModel = ref<Record<string, any>>({});

const {
  loading,
  tableData,
  total,
  pagination: paginationState,
  fetchData,
  handleSearch,
  handleReset,
  handlePageChange,
  handleSizeChange,
  handleSelectionChange: onSelectionChange,
  refresh,
} = useMateTable(props);

function handleSelectionChange(rows: Record<string, any>[]): void {
  onSelectionChange(rows);
  emit('selection-change', rows);
}

// ---- Dict helpers ----
const dictStore = useDictStore();

function getDictLabel(dictType: string, value: string): string {
  return dictStore.getDictLabel(dictType, value);
}

const TAG_TYPES: Array<'' | 'success' | 'warning' | 'danger' | 'info'> = [
  '',
  'success',
  'warning',
  'danger',
  'info',
];

function getDictTagType(dictType: string, value: any): '' | 'success' | 'warning' | 'danger' | 'info' {
  const items = dictStore.dictCache.get(dictType);
  if (!items) return '';
  const idx = items.findIndex((d) => d.value === String(value));
  if (idx < 0) return '';
  return TAG_TYPES[idx % TAG_TYPES.length];
}

function formatDatetime(val: string | null | undefined): string {
  if (!val) return '';
  try {
    return new Date(val).toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  } catch {
    return val;
  }
}

// Expose methods for parent component
defineExpose({ refresh, fetchData });
</script>

<style scoped>
.mate-table__toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}

.mate-table__pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
```

---

## Change 6: MateForm — types.ts

Create `mate-ui/packages/ui/src/MateForm/types.ts`

```typescript
import type { FormRules } from 'element-plus';

/**
 * Supported form field types.
 */
export type FormFieldType =
  | 'input'
  | 'textarea'
  | 'select'
  | 'radio'
  | 'switch'
  | 'treeSelect'
  | 'upload'
  | 'datetime';

/**
 * Schema for a single form field.
 */
export interface FormSchema {
  /** Field name in model (supports dot notation e.g. "address.city") */
  field: string;
  /** Label text */
  label: string;
  /** Field type */
  type: FormFieldType;
  /** Dict type code for auto-loading options (select / radio) */
  dict?: string;
  /** Remote API function for loading options */
  api?: () => Promise<Array<{ label: string; value: any }>>;
  /** Extra props passed to the underlying Element Plus component */
  props?: Record<string, any>;
  /** Validation rules for this field */
  rules?: FormRules[string];
  /** Column span (out of 24), default 12 for two-column layout */
  span?: number;
  /** Default value */
  defaultValue?: any;
  /** Whether the field is visible (can be a boolean or a function) */
  visible?: boolean | ((model: Record<string, any>) => boolean);
  /** Placeholder text */
  placeholder?: string;
}
```

---

## Change 7: MateForm — MateForm.vue

Create `mate-ui/packages/ui/src/MateForm/MateForm.vue`

```vue
<template>
  <el-form
    ref="formRef"
    :model="modelValue"
    :rules="rules"
    :label-width="labelWidth"
    v-bind="$attrs"
  >
    <el-row :gutter="16">
      <template v-for="item in schema" :key="item.field">
        <el-col
          v-if="isVisible(item)"
          :span="item.span ?? 12"
        >
          <el-form-item :label="item.label" :prop="item.field" :rules="item.rules">
            <!-- Input -->
            <el-input
              v-if="item.type === 'input'"
              :model-value="modelValue[item.field]"
              :placeholder="item.placeholder ?? `Please enter ${item.label}`"
              v-bind="item.props"
              @update:model-value="updateField(item.field, $event)"
            />

            <!-- Textarea -->
            <el-input
              v-else-if="item.type === 'textarea'"
              type="textarea"
              :model-value="modelValue[item.field]"
              :placeholder="item.placeholder ?? `Please enter ${item.label}`"
              :rows="3"
              v-bind="item.props"
              @update:model-value="updateField(item.field, $event)"
            />

            <!-- Select -->
            <el-select
              v-else-if="item.type === 'select'"
              :model-value="modelValue[item.field]"
              :placeholder="item.placeholder ?? `Please select ${item.label}`"
              clearable
              style="width: 100%"
              v-bind="item.props"
              @update:model-value="updateField(item.field, $event)"
            >
              <el-option
                v-for="opt in getOptions(item)"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>

            <!-- Radio -->
            <el-radio-group
              v-else-if="item.type === 'radio'"
              :model-value="modelValue[item.field]"
              v-bind="item.props"
              @update:model-value="updateField(item.field, $event)"
            >
              <el-radio
                v-for="opt in getOptions(item)"
                :key="opt.value"
                :value="opt.value"
              >
                {{ opt.label }}
              </el-radio>
            </el-radio-group>

            <!-- Switch -->
            <el-switch
              v-else-if="item.type === 'switch'"
              :model-value="modelValue[item.field]"
              v-bind="item.props"
              @update:model-value="updateField(item.field, $event)"
            />

            <!-- Tree Select -->
            <el-tree-select
              v-else-if="item.type === 'treeSelect'"
              :model-value="modelValue[item.field]"
              :data="getOptions(item)"
              :placeholder="item.placeholder ?? `Please select ${item.label}`"
              clearable
              check-strictly
              style="width: 100%"
              v-bind="item.props"
              @update:model-value="updateField(item.field, $event)"
            />

            <!-- Datetime -->
            <el-date-picker
              v-else-if="item.type === 'datetime'"
              :model-value="modelValue[item.field]"
              type="datetime"
              :placeholder="item.placeholder ?? `Please select ${item.label}`"
              style="width: 100%"
              value-format="YYYY-MM-DD HH:mm:ss"
              v-bind="item.props"
              @update:model-value="updateField(item.field, $event)"
            />

            <!-- Upload (delegated to MateUpload) -->
            <MateUpload
              v-else-if="item.type === 'upload'"
              :model-value="modelValue[item.field]"
              v-bind="item.props"
              @update:model-value="updateField(item.field, $event)"
            />
          </el-form-item>
        </el-col>
      </template>
    </el-row>
  </el-form>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { useDictStore } from '@matecloud/core';
import MateUpload from '../MateUpload/MateUpload.vue';
import type { FormSchema } from './types';

const props = withDefaults(defineProps<{
  schema: FormSchema[];
  modelValue: Record<string, any>;
  labelWidth?: string;
  rules?: FormRules;
}>(), {
  labelWidth: '100px',
});

const emit = defineEmits<{
  (e: 'update:modelValue', value: Record<string, any>): void;
}>();

const formRef = ref<FormInstance>();
const dictStore = useDictStore();

// Stores for dynamically loaded options (dict or api)
const optionsMap = reactive<Record<string, Array<{ label: string; value: any }>>>({});

function updateField(field: string, value: any): void {
  emit('update:modelValue', { ...props.modelValue, [field]: value });
}

function isVisible(item: FormSchema): boolean {
  if (item.visible === undefined) return true;
  if (typeof item.visible === 'function') return item.visible(props.modelValue);
  return item.visible;
}

function getOptions(item: FormSchema): Array<{ label: string; value: any }> {
  return optionsMap[item.field] ?? [];
}

/**
 * Load dict/api options for select, radio, treeSelect fields.
 */
async function loadOptions(): Promise<void> {
  for (const item of props.schema) {
    if (['select', 'radio', 'treeSelect'].includes(item.type)) {
      if (item.dict) {
        const dictData = await dictStore.getDict(item.dict);
        optionsMap[item.field] = dictData.map((d) => ({ label: d.label, value: d.value }));
      } else if (item.api) {
        optionsMap[item.field] = await item.api();
      }
    }
  }
}

onMounted(() => {
  loadOptions();
});

/**
 * Validate the form. Returns a promise that resolves to true/false.
 */
async function validate(): Promise<boolean> {
  if (!formRef.value) return false;
  try {
    await formRef.value.validate();
    return true;
  } catch {
    return false;
  }
}

/**
 * Reset form fields and validation state.
 */
function resetFields(): void {
  formRef.value?.resetFields();
}

defineExpose({ validate, resetFields, formRef });
</script>
```

---

## Change 8: MateDialog — MateDialog.vue

Create `mate-ui/packages/ui/src/MateDialog/MateDialog.vue`

```vue
<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    :width="width"
    :close-on-click-modal="false"
    :close-on-press-escape="true"
    :destroy-on-close="true"
    append-to-body
    draggable
    @update:model-value="emit('update:modelValue', $event)"
    @close="emit('update:modelValue', false)"
  >
    <slot />

    <template #footer>
      <slot name="footer">
        <el-button @click="emit('update:modelValue', false)">Cancel</el-button>
        <el-button type="primary" :loading="confirmLoading" @click="emit('confirm')">
          Confirm
        </el-button>
      </slot>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  /** Dialog title */
  title?: string;
  /** Dialog width, e.g. "600px" or "50%" */
  width?: string;
  /** v-model for visibility */
  modelValue: boolean;
  /** Whether the confirm button shows loading spinner */
  confirmLoading?: boolean;
}>(), {
  title: '',
  width: '600px',
  confirmLoading: false,
});

const emit = defineEmits<{
  (e: 'update:modelValue', visible: boolean): void;
  (e: 'confirm'): void;
}>();
</script>
```

---

## Change 9: MateSearch — types.ts

Create `mate-ui/packages/ui/src/MateSearch/types.ts`

```typescript
/**
 * Search field type.
 */
export type SearchFieldType = 'input' | 'select' | 'dateRange';

/**
 * Configuration for a single search field.
 */
export interface SearchField {
  /** Field name in search params */
  field: string;
  /** Label text */
  label: string;
  /** Field type */
  type: SearchFieldType;
  /** Dict type code for auto-loading select options */
  dict?: string;
  /** Placeholder text */
  placeholder?: string;
}
```

---

## Change 10: MateSearch — MateSearch.vue

Create `mate-ui/packages/ui/src/MateSearch/MateSearch.vue`

```vue
<template>
  <div class="mate-search">
    <el-form :model="formModel" inline>
      <template v-for="(field, index) in visibleFields" :key="field.field">
        <!-- Input -->
        <el-form-item :label="field.label" v-if="field.type === 'input'">
          <el-input
            v-model="formModel[field.field]"
            :placeholder="field.placeholder ?? `Enter ${field.label}`"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <!-- Select -->
        <el-form-item :label="field.label" v-else-if="field.type === 'select'">
          <el-select
            v-model="formModel[field.field]"
            :placeholder="field.placeholder ?? `Select ${field.label}`"
            clearable
            style="width: 200px"
          >
            <el-option
              v-for="opt in getOptions(field)"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>

        <!-- Date Range -->
        <el-form-item :label="field.label" v-else-if="field.type === 'dateRange'">
          <el-date-picker
            v-model="formModel[field.field]"
            type="daterange"
            range-separator="to"
            start-placeholder="Start"
            end-placeholder="End"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
      </template>

      <!-- Buttons -->
      <el-form-item>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          Search
        </el-button>
        <el-button @click="handleReset">
          <el-icon><Refresh /></el-icon>
          Reset
        </el-button>
        <el-button
          v-if="fields.length > defaultVisibleCount"
          link
          type="primary"
          @click="expanded = !expanded"
        >
          {{ expanded ? 'Collapse' : 'Expand' }}
          <el-icon>
            <ArrowUp v-if="expanded" />
            <ArrowDown v-else />
          </el-icon>
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { Search, Refresh, ArrowUp, ArrowDown } from '@element-plus/icons-vue';
import { useDictStore } from '@matecloud/core';
import type { SearchField } from './types';

const props = withDefaults(defineProps<{
  /** Search field configurations */
  fields: SearchField[];
  /** v-model for search params */
  modelValue?: Record<string, any>;
  /** Number of fields visible before expanding, default 3 */
  defaultVisibleCount?: number;
}>(), {
  defaultVisibleCount: 3,
});

const emit = defineEmits<{
  (e: 'search', params: Record<string, any>): void;
  (e: 'reset'): void;
  (e: 'update:modelValue', value: Record<string, any>): void;
}>();

const expanded = ref(false);
const dictStore = useDictStore();
const optionsMap = reactive<Record<string, Array<{ label: string; value: any }>>>({});

// Internal form model
const formModel = reactive<Record<string, any>>({});

const visibleFields = computed(() => {
  if (expanded.value) return props.fields;
  return props.fields.slice(0, props.defaultVisibleCount);
});

function getOptions(field: SearchField): Array<{ label: string; value: any }> {
  return optionsMap[field.field] ?? [];
}

async function loadDictOptions(): Promise<void> {
  for (const field of props.fields) {
    if (field.type === 'select' && field.dict) {
      const dictData = await dictStore.getDict(field.dict);
      optionsMap[field.field] = dictData.map((d) => ({ label: d.label, value: d.value }));
    }
  }
}

function handleSearch(): void {
  // Filter out empty/null/undefined values
  const params: Record<string, any> = {};
  for (const [key, val] of Object.entries(formModel)) {
    if (val !== null && val !== undefined && val !== '') {
      params[key] = val;
    }
  }
  emit('search', params);
  emit('update:modelValue', params);
}

function handleReset(): void {
  Object.keys(formModel).forEach((key) => {
    formModel[key] = undefined;
  });
  emit('reset');
  emit('update:modelValue', {});
}

onMounted(() => {
  loadDictOptions();
});
</script>

<style scoped>
.mate-search {
  margin-bottom: 16px;
  padding: 16px 16px 0;
  background: var(--el-bg-color);
  border-radius: 4px;
}
</style>
```

---

## Change 11: MateUpload — MateUpload.vue

Create `mate-ui/packages/ui/src/MateUpload/MateUpload.vue`

```vue
<template>
  <el-upload
    :action="action"
    :headers="uploadHeaders"
    :accept="accept"
    :limit="limit"
    :file-list="fileList"
    :list-type="listType"
    :on-success="handleSuccess"
    :on-remove="handleRemove"
    :on-exceed="handleExceed"
    :on-preview="handlePreview"
    :before-upload="beforeUpload"
  >
    <template v-if="listType === 'picture-card'">
      <el-icon><Plus /></el-icon>
    </template>
    <template v-else>
      <el-button type="primary">
        <el-icon><Upload /></el-icon>
        Click to upload
      </el-button>
    </template>

    <template #tip>
      <div class="el-upload__tip" v-if="tip">
        {{ tip }}
      </div>
    </template>
  </el-upload>

  <!-- Image preview dialog -->
  <el-dialog v-model="previewVisible" title="Preview" width="800px" append-to-body>
    <img :src="previewUrl" alt="preview" style="width: 100%" />
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Plus, Upload } from '@element-plus/icons-vue';
import { getToken, getTenantId } from '@matecloud/core';
import type { UploadFile, UploadUserFile, UploadRawFile } from 'element-plus';

export interface MateFileItem {
  name: string;
  url: string;
}

const props = withDefaults(defineProps<{
  /** Upload endpoint URL */
  action?: string;
  /** Accepted file types, e.g. ".jpg,.png,.pdf" */
  accept?: string;
  /** Maximum number of files */
  limit?: number;
  /** v-model: list of uploaded files */
  modelValue?: MateFileItem[];
  /** Upload list type */
  listType?: 'text' | 'picture' | 'picture-card';
  /** Help text shown below the upload area */
  tip?: string;
  /** Max file size in MB */
  maxSize?: number;
}>(), {
  action: '/api/v1/files/upload',
  accept: '',
  limit: 5,
  modelValue: () => [],
  listType: 'text',
  tip: '',
  maxSize: 10,
});

const emit = defineEmits<{
  (e: 'update:modelValue', files: MateFileItem[]): void;
}>();

const previewVisible = ref(false);
const previewUrl = ref('');

const fileList = ref<UploadUserFile[]>(
  props.modelValue.map((f) => ({ name: f.name, url: f.url })),
);

// Sync fileList when modelValue changes externally
watch(
  () => props.modelValue,
  (val) => {
    fileList.value = val.map((f) => ({ name: f.name, url: f.url }));
  },
);

const uploadHeaders = computed(() => {
  const headers: Record<string, string> = {};
  const token = getToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const tenantId = getTenantId();
  if (tenantId) headers['X-Tenant-Id'] = tenantId;
  return headers;
});

function beforeUpload(file: UploadRawFile): boolean {
  if (props.maxSize && file.size / 1024 / 1024 > props.maxSize) {
    ElMessage.error(`File size cannot exceed ${props.maxSize}MB`);
    return false;
  }
  return true;
}

function handleSuccess(response: any, file: UploadFile): void {
  // Backend returns Result<{ name, url }> — already unwrapped by our Axios interceptor?
  // Upload component uses its own request, so response is raw.
  const data = response?.data ?? response;
  const newFile: MateFileItem = {
    name: data?.name ?? file.name,
    url: data?.url ?? '',
  };

  const updated = [...props.modelValue, newFile];
  emit('update:modelValue', updated);
}

function handleRemove(_file: UploadFile, fileListNow: UploadFile[]): void {
  const updated = fileListNow
    .filter((f) => f.url)
    .map((f) => ({ name: f.name, url: f.url! }));
  emit('update:modelValue', updated);
}

function handleExceed(): void {
  ElMessage.warning(`Maximum ${props.limit} files allowed`);
}

function handlePreview(file: UploadFile): void {
  if (file.url) {
    previewUrl.value = file.url;
    previewVisible.value = true;
  }
}
</script>
```

---

## Change 12: Component index and install

Create `mate-ui/packages/ui/src/index.ts`

```typescript
import type { App, Plugin } from 'vue';
import MateTable from './MateTable/MateTable.vue';
import MateForm from './MateForm/MateForm.vue';
import MateDialog from './MateDialog/MateDialog.vue';
import MateSearch from './MateSearch/MateSearch.vue';
import MateUpload from './MateUpload/MateUpload.vue';

// Re-export components
export { MateTable } from './MateTable/MateTable.vue';
export { MateForm } from './MateForm/MateForm.vue';
export { MateDialog } from './MateDialog/MateDialog.vue';
export { MateSearch } from './MateSearch/MateSearch.vue';
export { MateUpload } from './MateUpload/MateUpload.vue';

// Re-export types
export type { MateTableColumn, MateTableProps, MateSearchField } from './MateTable/types';
export type { FormSchema, FormFieldType } from './MateForm/types';
export type { SearchField, SearchFieldType } from './MateSearch/types';
export type { MateFileItem } from './MateUpload/MateUpload.vue';

// Re-export composables
export { useMateTable } from './MateTable/useMateTable';

// Plugin install
const components = [MateTable, MateForm, MateDialog, MateSearch, MateUpload];

const MateUI: Plugin = {
  install(app: App) {
    components.forEach((component) => {
      app.component(component.name ?? component.__name ?? '', component);
    });
  },
};

export default MateUI;
```
