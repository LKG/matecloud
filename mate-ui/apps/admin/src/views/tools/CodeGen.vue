<template>
  <MatePageCard
    :title="t('codegen.title')"
    :description="t('codegen.description')"
  >
    <MateSearchBar @search="handleSearch" @reset="handleReset">
      <el-input
        v-model="keyword"
        :placeholder="t('codegen.searchPlaceholder')"
        clearable
        style="width: 260px"
        @keyup.enter="handleSearch"
      >
        <template #prefix><Search :size="14" /></template>
      </el-input>
      <el-select v-model="schemaFilter" clearable style="width: 180px" :placeholder="t('codegen.schema')">
        <el-option v-for="s in schemas" :key="s" :label="s" :value="s" />
      </el-select>
    </MateSearchBar>

    <MateTable
      :columns="columns"
      :data="rows"
      :loading="loading"
      row-key="tableName"
      :action-width="180"
      :action-label="t('common.action')"
    >
      <template #col-tableName="{ row }">
        <div class="table-cell">
          <Database :size="14" class="table-icon" />
          <div>
            <span class="table-name">{{ row.tableName }}</span>
            <span class="table-comment">{{ row.comment }}</span>
          </div>
        </div>
      </template>
      <template #col-schema="{ row }">
        <el-tag size="small" type="info">{{ row.schema }}</el-tag>
      </template>
      <template #col-columnCount="{ row }">
        <span class="mc-mono">{{ row.columnCount }}</span>
      </template>
      <template #col-generated="{ row }">
        <MateBadge :type="row.generated ? 'success' : 'default'" :dot="false">
          {{ row.generated ? t('codegen.generated') : t('codegen.notGenerated') }}
        </MateBadge>
      </template>
      <template #actions="{ row }">
        <button class="mc-action-btn" @click="openPreview(row)">{{ t('codegen.preview') }}</button>
        <button class="mc-action-btn mc-action-btn--success" @click="openGenerate(row)">{{ t('codegen.generate') }}</button>
      </template>
    </MateTable>

    <MatePagination
      v-model:page-num="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      @change="() => {}"
    />

    <p class="demo-hint">
      <Info :size="13" class="mr-1" />
      {{ t('codegen.hint') }}
    </p>

    <!-- Preview dialog -->
    <MateDialog
      v-model="previewVisible"
      :title="t('codegen.previewTitle')"
      width="760px"
      :show-footer="false"
    >
      <template #footer>
        <el-button @click="previewVisible = false">{{ t('common.close') }}</el-button>
      </template>
      <div v-if="previewTable" class="preview-body">
        <div class="preview-meta">
          <span class="mc-mono">{{ previewTable.tableName }}</span>
          <span class="meta-sep">·</span>
          <span>{{ previewTable.comment }}</span>
        </div>
        <el-tabs v-model="previewTab">
          <el-tab-pane label="Entity.java" name="entity">
            <pre class="code-block">{{ renderEntity(previewTable) }}</pre>
          </el-tab-pane>
          <el-tab-pane label="Mapper.java" name="mapper">
            <pre class="code-block">{{ renderMapper(previewTable) }}</pre>
          </el-tab-pane>
          <el-tab-pane label="Service.java" name="service">
            <pre class="code-block">{{ renderService(previewTable) }}</pre>
          </el-tab-pane>
          <el-tab-pane label="Controller.java" name="controller">
            <pre class="code-block">{{ renderController(previewTable) }}</pre>
          </el-tab-pane>
        </el-tabs>
      </div>
    </MateDialog>

    <!-- Generate dialog -->
    <MateDialog
      v-model="generateVisible"
      :title="t('codegen.generateTitle')"
      width="520px"
      :submitting="submitting"
      :confirm-text="t('codegen.doGenerate')"
      @submit="handleGenerate"
    >
      <MateForm
        ref="genFormRef"
        :schema="generateSchema"
        :model-value="genForm"
        label-position="top"
        @update:model-value="(v: any) => Object.assign(genForm, v)"
      />
    </MateDialog>
  </MatePageCard>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search, Database, Info } from 'lucide-vue-next'
import { MatePageCard, MateTable, MateSearchBar, MatePagination, MateDialog, MateBadge, MateForm, type MateColumn, type FormSchema, MateMessage } from '@matecloud/ui'

defineOptions({ name: 'CodeGenView' })

const { t } = useI18n()

const columns: MateColumn[] = [
  { prop: 'tableName', label: t('codegen.tableName') },
  { prop: 'schema', label: t('codegen.schema'), width: 140 },
  { prop: 'columnCount', label: t('codegen.columnCount'), width: 90, align: 'center' },
  { prop: 'engine', label: t('codegen.engine'), width: 100 },
  { prop: 'rows', label: t('codegen.rows'), width: 100, align: 'right' },
  { prop: 'generated', label: t('codegen.status'), width: 120 },
]

interface DbTable {
  tableName: string
  comment: string
  schema: string
  columnCount: number
  engine: string
  rows: number
  generated: boolean
}

const schemas = ['mate_admin', 'mate_system', 'mate_notice']

const all = ref<DbTable[]>([
  { tableName: 'mate_admin', comment: '管理员', schema: 'mate_admin', columnCount: 9, engine: 'InnoDB', rows: 3, generated: true },
  { tableName: 'mate_role', comment: '角色', schema: 'mate_admin', columnCount: 7, engine: 'InnoDB', rows: 24, generated: true },
  { tableName: 'mate_menu', comment: '菜单', schema: 'mate_admin', columnCount: 12, engine: 'InnoDB', rows: 19, generated: true },
  { tableName: 'mate_dict_type', comment: '字典类型', schema: 'mate_admin', columnCount: 6, engine: 'InnoDB', rows: 8, generated: true },
  { tableName: 'mate_dict_data', comment: '字典数据', schema: 'mate_admin', columnCount: 8, engine: 'InnoDB', rows: 32, generated: true },
  { tableName: 'mate_config', comment: '系统配置', schema: 'mate_admin', columnCount: 8, engine: 'InnoDB', rows: 10, generated: true },
  { tableName: 'mate_operation_log', comment: '操作日志', schema: 'mate_admin', columnCount: 13, engine: 'InnoDB', rows: 1420, generated: true },
  { tableName: 'mate_login_log', comment: '登录日志', schema: 'mate_admin', columnCount: 10, engine: 'InnoDB', rows: 832, generated: true },
  { tableName: 'mate_user', comment: '用户', schema: 'mate_system', columnCount: 11, engine: 'InnoDB', rows: 1256, generated: true },
  { tableName: 'mate_user_role', comment: '用户-角色关联', schema: 'mate_system', columnCount: 4, engine: 'InnoDB', rows: 3841, generated: true },
  { tableName: 'mate_notice', comment: '通知', schema: 'mate_notice', columnCount: 10, engine: 'InnoDB', rows: 128, generated: true },
  { tableName: 'mate_order', comment: '订单（示例，未生成）', schema: 'mate_system', columnCount: 14, engine: 'InnoDB', rows: 0, generated: false },
  { tableName: 'mate_order_item', comment: '订单明细（示例，未生成）', schema: 'mate_system', columnCount: 9, engine: 'InnoDB', rows: 0, generated: false },
])

const loading = ref(false)
const keyword = ref('')
const schemaFilter = ref('')
const pageNum = ref(1)
const pageSize = ref(10)

const filtered = computed(() => {
  let list = all.value
  if (schemaFilter.value) list = list.filter(t => t.schema === schemaFilter.value)
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    list = list.filter(t =>
      t.tableName.toLowerCase().includes(kw) ||
      t.comment.toLowerCase().includes(kw),
    )
  }
  return list
})
const total = computed(() => filtered.value.length)
const rows = computed(() => {
  const start = (pageNum.value - 1) * pageSize.value
  return filtered.value.slice(start, start + pageSize.value)
})

function handleSearch() { pageNum.value = 1 }
function handleReset() { keyword.value = ''; schemaFilter.value = ''; pageNum.value = 1 }

onMounted(() => { /* static data */ })

// ---- Preview ----
const previewVisible = ref(false)
const previewTab = ref('entity')
const previewTable = ref<DbTable | null>(null)

function openPreview(row: DbTable) {
  previewTable.value = row
  previewTab.value = 'entity'
  previewVisible.value = true
}

function camelOf(snake: string): string {
  return snake
    .replace(/^mate_/, '')
    .replace(/_(\w)/g, (_, c) => c.toUpperCase())
    .replace(/^./, s => s.toUpperCase())
}
function lowerCamel(snake: string): string {
  const c = camelOf(snake)
  return c.charAt(0).toLowerCase() + c.slice(1)
}

function renderEntity(t0: DbTable): string {
  const Class = camelOf(t0.tableName)
  return `package vip.mate.${t0.schema.replace('mate_', '')}.domain.entity;

import lombok.Data;
import lombok.experimental.SuperBuilder;
import com.baomidou.mybatisplus.annotation.TableName;
import vip.mate.base.entity.BaseEntity;

/**
 * ${t0.comment} — ${t0.columnCount} columns
 */
@Data
@SuperBuilder
@TableName("${t0.tableName}")
public class ${Class} extends BaseEntity {

    private String name;
    private Integer status;
    private String remark;
    // ... ${t0.columnCount - 3} more fields
}
`
}

function renderMapper(t0: DbTable): string {
  const Class = camelOf(t0.tableName)
  return `package vip.mate.${t0.schema.replace('mate_', '')}.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import vip.mate.${t0.schema.replace('mate_', '')}.domain.entity.${Class};

@Mapper
public interface ${Class}Mapper extends BaseMapper<${Class}> {
}
`
}

function renderService(t0: DbTable): string {
  const Class = camelOf(t0.tableName)
  const var0 = lowerCamel(t0.tableName)
  return `package vip.mate.${t0.schema.replace('mate_', '')}.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.${t0.schema.replace('mate_', '')}.domain.adapter.repository.${Class}Repository;
import vip.mate.${t0.schema.replace('mate_', '')}.domain.entity.${Class};

@Service
@RequiredArgsConstructor
public class ${Class}CommandService {

    private final ${Class}Repository ${var0}Repository;

    @Transactional
    public String create(${Class} ${var0}) {
        ${var0}Repository.save(${var0});
        return ${var0}.getId();
    }

    @Transactional
    public void update(${Class} ${var0}) {
        ${var0}Repository.update(${var0});
    }

    @Transactional
    public void delete(String id) {
        ${var0}Repository.deleteById(id);
    }
}
`
}

function renderController(t0: DbTable): string {
  const Class = camelOf(t0.tableName)
  const base = t0.tableName.replace(/^mate_/, '').replace(/_/g, '-') + 's'
  return `package vip.mate.${t0.schema.replace('mate_', '')}.trigger.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vip.mate.base.result.Result;
import vip.mate.${t0.schema.replace('mate_', '')}.application.command.${Class}CommandService;
import vip.mate.${t0.schema.replace('mate_', '')}.application.query.${Class}QueryService;

/**
 * ${t0.comment} REST endpoints.
 */
@RestController
@RequestMapping("/${base}")
@RequiredArgsConstructor
public class ${Class}Controller {

    private final ${Class}CommandService commandService;
    private final ${Class}QueryService queryService;

    @GetMapping
    public Result<?> list() {
        return Result.success(queryService.list());
    }

    @PostMapping
    public Result<String> create(@RequestBody ${Class} ${lowerCamel(t0.tableName)}) {
        return Result.success(commandService.create(${lowerCamel(t0.tableName)}));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody ${Class} body) {
        body.setId(id);
        commandService.update(body);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        commandService.delete(id);
        return Result.success();
    }
}
`
}

// ---- Generate ----
const generateVisible = ref(false)
const submitting = ref(false)
const genFormRef = ref<InstanceType<typeof MateForm>>()
const genForm = reactive<Record<string, any>>({
  tableName: '', module: '', packageName: 'vip.mate', author: '', overwrite: false,
})

const generateSchema = computed<FormSchema[]>(() => [
  { field: 'tableName', label: t('codegen.tableName'), type: 'input', disabled: true },
  {
    field: 'module',
    label: t('codegen.module'),
    type: 'input',
    placeholder: 'order / notice / ...',
    rules: [{ required: true, message: t('codegen.moduleRequired'), trigger: 'blur' }],
  },
  {
    field: 'packageName',
    label: t('codegen.packageName'),
    type: 'input',
    placeholder: 'vip.mate',
    rules: [{ required: true, message: t('codegen.packageRequired'), trigger: 'blur' }],
  },
  { field: 'author', label: t('codegen.author'), type: 'input' },
  { field: 'overwrite', label: t('codegen.overwrite'), type: 'switch' },
])

function openGenerate(row: DbTable) {
  genForm.tableName = row.tableName
  genForm.module = row.tableName.replace(/^mate_/, '').split('_')[0]
  genForm.packageName = 'vip.mate'
  genForm.author = 'MateCloud CLI'
  genForm.overwrite = false
  generateVisible.value = true
}

async function handleGenerate() {
  const valid = await genFormRef.value?.validate()
  if (!valid) return
  submitting.value = true
  try {
    await new Promise(r => setTimeout(r, 600))
    MateMessage.success(t('codegen.generateSuccess', { table: genForm.tableName }))
    const match = all.value.find(t => t.tableName === genForm.tableName)
    if (match) match.generated = true
    generateVisible.value = false
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.table-cell { display: flex; align-items: center; gap: 10px; }
.table-icon { color: var(--mc-primary); flex-shrink: 0; }
.table-name {
  display: block;
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
  font-size: 13px; font-weight: 500;
  color: var(--mc-text-primary);
}
.table-comment {
  display: block;
  font-size: 12px;
  color: var(--mc-text-muted);
}

.preview-body { min-height: 420px; }
.preview-meta {
  display: flex; align-items: center; gap: 6px;
  margin-bottom: 12px;
  font-size: 13px;
}
.preview-meta .mc-mono { color: var(--mc-text-primary); font-weight: 600; }
.meta-sep { color: var(--mc-text-disabled); }

.code-block {
  background: var(--mc-bg);
  border: 1px solid var(--mc-divider-subtle, rgb(16 24 40 / 0.06));
  border-radius: 6px;
  padding: 14px;
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
  font-size: 12px;
  line-height: 1.6;
  color: var(--mc-text-primary);
  max-height: 420px;
  overflow: auto;
  white-space: pre;
  margin: 0;
}

.demo-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  padding: 8px 12px;
  font-size: 12px;
  color: var(--mc-text-muted);
  background: rgb(54 191 250 / 0.06);
  border-radius: 6px;
}
</style>
