# 组件库

`@matecloud/ui` 提供一组共享 UI 组件，基于 Element Plus 封装。

## MateTable

通用数据表格，封装分页、排序、操作列。

```vue
<MateTable
  :columns="columns"
  :data="data"
  :loading="loading"
  row-key="id"
  :action-width="120"
  action-label="操作"
>
  <template #col-status="{ row }">
    <el-tag :type="row.status === 1 ? 'success' : 'info'">
      {{ row.status === 1 ? '启用' : '禁用' }}
    </el-tag>
  </template>
  <template #actions="{ row }">
    <el-button link type="primary" @click="edit(row)">编辑</el-button>
    <el-button link type="danger" @click="remove(row)">删除</el-button>
  </template>
</MateTable>
```

### Props

| 属性 | 类型 | 说明 |
|------|------|------|
| columns | `MateColumn[]` | 列定义 |
| data | `any[]` | 数据源 |
| loading | `boolean` | 加载状态 |
| row-key | `string` | 行唯一标识 |
| action-width | `number` | 操作列宽度 |
| action-label | `string` | 操作列标题 |

## MatePageCard

页面级卡片容器，提供标题、描述和操作区域。

```vue
<MatePageCard title="用户管理" description="管理系统用户和权限">
  <template #actions>
    <el-button type="primary" @click="add">新增</el-button>
  </template>
  <!-- 页面内容 -->
</MatePageCard>
```

## MateStatTile

统计数据卡片。

```vue
<MateStatTile label="注册用户" :value="1234" tone="success" />
```

### Props

| 属性 | 类型 | 说明 |
|------|------|------|
| label | `string` | 标签 |
| value | `string \| number` | 数值 |
| tone | `'info' \| 'success' \| 'warning' \| 'danger'` | 颜色语义 |
