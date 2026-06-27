# 前端开发指南

## 新增页面

### 1. 创建视图组件

在 `apps/admin/src/views/` 下对应模块目录创建 `.vue` 文件：

```vue
<template>
  <MatePageCard title="订单管理" description="订单列表和管理">
    <MateTable :columns="columns" :data="data" :loading="loading" />
  </MatePageCard>
</template>

<script setup lang="ts">
import { MatePageCard, MateTable, type MateColumn } from '@matecloud/ui'

defineOptions({ name: 'OrderListView' })

const columns: MateColumn[] = [
  { prop: 'orderNo', label: '订单号', minWidth: 150 },
  { prop: 'status', label: '状态', width: 100 },
]

// ...
</script>
```

### 2. 添加路由

在后台菜单管理中添加菜单项，前端路由由后端菜单动态生成。

### 3. 添加 API

在 `packages/core/src/api/` 中添加 API 客户端：

```typescript
import { request } from '../request'

export const orderApi = {
  list: (params: OrderQuery) => request.get<PageResult<Order>>('/api/v1/orders', { params }),
  create: (data: CreateOrderCmd) => request.post<void>('/api/v1/orders', data),
}
```

## 使用共享组件

从 `@matecloud/ui` 导入：

```typescript
import { MateTable, MatePageCard, MateStatTile } from '@matecloud/ui'
```

从 `@matecloud/hooks` 导入：

```typescript
import { useTable, useForm, useDict } from '@matecloud/hooks'
```

## 国际化

在 `src/i18n/zh-CN.ts` 和 `src/i18n/en-US.ts` 中添加对应的翻译 key。

## TypeScript

- 所有组件使用 `<script setup lang="ts">`
- API 类型定义放在 `@matecloud/core` 的 `types/` 目录
- 使用 `defineOptions({ name: 'XxxView' })` 命名组件
