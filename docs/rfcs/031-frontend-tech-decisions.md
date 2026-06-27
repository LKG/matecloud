# RFC-031: Frontend Tech Decisions — Every Choice Earns Its Place

- **Status**: Draft
- **Created**: 2026-04-12
- **Author**: MateCloud Team

> "When you first start off trying to solve a problem, the first solutions you come up with
> are very complex, and most people stop there. But if you keep going... you can often
> arrive at some very elegant and simple solutions."

## 一、UI 组件库: 谁是王者

### 数据

| 组件库 | 周下载 | GitHub Stars | 组件数 | 一句话 |
|--------|--------|-------------|--------|--------|
| **Element Plus** | 436K | 27K | 70+ | Vue 生态霸主，生态最厚 |
| Ant Design Vue | 125K | 21K | 60+ | Ant Design 的 Vue 版，企业级严谨 |
| Naive UI | 90K | 18K | 80+ | Vue 作者推荐，TypeScript 极致 |
| Arco Design Vue | 36K | 3K | 60+ | 字节跳动内部沉淀 |

### 判定: Element Plus

不是因为它最好看。是因为它的**生态是其他三个加起来的三倍**。

你搜「Vue3 admin」，排名前 20 的模板有 15 个用 Element Plus。你遇到问题去搜，Element Plus 的答案永远最多。这就是 iPod 选 iTunes 的逻辑——不是技术最优，是生态最强。

但我要加一个关键架构决策：**UI 可替换**。

---

## 二、Admin 模板: 自建还是站在巨人肩上

### 数据

| 模板 | Stars | 架构 | UI库 | 特点 |
|------|-------|------|------|------|
| **Vben Admin** | 31.8K | Monorepo + 可换 UI | Shadcn/Element/Naive/Ant | 架构最强，可换 UI 适配器 |
| Vue Pure Admin | 19.9K | 单包 | Element Plus | 干净轻量，文档好 |
| Soybean Admin | 12K+ | 单包 | Naive UI | 设计好看 |

### 判定: 学习 Vben 的架构，但自建

Vben Admin 有一个 **insanely great** 的设计——**可换 UI 适配器**。同一套业务代码，底层 UI 库可以在 Element Plus、Naive UI、Ant Design Vue 之间切换。

这正好和 MateCloud 的产品逻辑吻合——**卖给不同客户，他们可能有不同的 UI 偏好**。

MateCloud 不直接 fork Vben，但借鉴它的适配器思路：

```typescript
// packages/ui/src/adapter/index.ts
export interface UIAdapter {
  // 表格
  Table: Component
  TableColumn: Component
  // 表单
  Form: Component
  FormItem: Component
  Input: Component
  Select: Component
  // 弹窗
  Dialog: Component
  // 按钮
  Button: Component
  // 消息
  message: { success: Function; error: Function; warning: Function }
  notification: { success: Function; error: Function }
}

// packages/ui/src/adapter/element-plus.ts
import { ElTable, ElForm, ElButton, ElMessage ... } from 'element-plus'
export const elementPlusAdapter: UIAdapter = {
  Table: ElTable,
  Form: ElForm,
  Button: ElButton,
  message: ElMessage,
  ...
}

// packages/ui/src/adapter/naive-ui.ts (备选)
import { NDataTable, NForm, NButton ... } from 'naive-ui'
export const naiveUIAdapter: UIAdapter = { ... }
```

**默认 Element Plus。客户要换？改一行配置。**

```typescript
// apps/admin/src/main.ts
import { createApp } from 'vue'
import { setupUI } from '@matecloud/ui'
import { elementPlusAdapter } from '@matecloud/ui/adapter/element-plus'

const app = createApp(App)
setupUI(app, elementPlusAdapter) // 一行切换
```

---

## 三、CSS 方案: Tailwind CSS 4 vs UnoCSS

### 数据

| 方案 | HMR 速度 | 产出大小 | 生态 |
|------|---------|---------|------|
| **Tailwind CSS 4** | 100-500ms (大项目) | ~32KB | Shadcn/daisyUI/Headless UI |
| UnoCSS | 10-20ms | ~4KB | 较小，但兼容 Tailwind 语法 |

### 判定: UnoCSS

等一下。我知道前面 RFC-030 选了 Tailwind 4。但数据改变了我的想法。

UnoCSS 的 HMR 速度是 Tailwind 的 **10-50 倍**。在管理后台这种组件密集的场景，开发体验差距是真实的。而且 UnoCSS 兼容 Tailwind 的 class 语法——`bg-blue-500`、`flex`、`p-4` 全部通用。

**用 UnoCSS，写 Tailwind 语法。** 开发者不需要学新东西，但 HMR 快 10 倍。

```typescript
// uno.config.ts
import { defineConfig, presetWind4 } from 'unocss'

export default defineConfig({
  presets: [
    presetWind4(), // 兼容 Tailwind CSS 4 语法
  ],
})
```

这就像用了更好的发动机，但方向盘没变。开发者甚至不知道底层换了。

---

## 四、状态管理: 没有争议

**Pinia 3。** Vuex 已死。不浪费时间对比。

---

## 五、HTTP 客户端: Axios vs ofetch vs ky

| 方案 | 大小 | 拦截器 | SSR/Edge | 生态 |
|------|------|--------|----------|------|
| Axios | 13KB | 完善 | Node | 最成熟 |
| ofetch | 3KB | 基础 | 全端 | Nuxt 默认 |
| ky | 5KB | 中等 | 全端 | 现代 |

### 判定: Axios

管理后台不需要 Edge Runtime。Axios 的拦截器生态是最完善的——Token 注入、错误统一处理、请求取消、重试。

不是因为 Axios 最新。是因为它最稳。

---

## 六、图表: ECharts vs Chart.js

| 方案 | 大小 | 图表类型 | 中国市场 |
|------|------|---------|---------|
| **ECharts** | 800KB (可 tree-shake) | 50+ | 霸主 |
| Chart.js | 60KB | 8 | 轻量场景 |

### 判定: ECharts (按需引入)

管理后台的 Dashboard 需要丰富图表。ECharts 按需引入后，实际使用的图表类型只加载对应模块。

```typescript
// 按需引入，不是全量
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([BarChart, LineChart, PieChart, GridComponent, TooltipComponent, CanvasRenderer])
```

---

## 七、富文本: Tiptap vs WangEditor

| 方案 | 架构 | 扩展性 | 协同编辑 |
|------|------|--------|---------|
| **Tiptap** | Headless + ProseMirror | 极高 | 原生支持 (Yjs) |
| WangEditor | 一体化 | 中等 | 不支持 |

### 判定: Tiptap

Headless 架构意味着和任何 UI 库都不冲突。Element Plus 主题下长这样，Naive UI 主题下长那样。完美。

---

## 八、表单引擎: 代码驱动 vs JSON Schema

管理后台 80% 的页面是 CRUD 表单。写死 template 太慢。

### 判定: JSON Schema 驱动

```typescript
// 一个配置出一张表单
const schema: FormSchema[] = [
  { field: 'mobile', label: '手机号', type: 'input', rules: [{ required: true }] },
  { field: 'nickName', label: '昵称', type: 'input' },
  { field: 'status', label: '状态', type: 'select', dict: 'user_status' },
  { field: 'deptId', label: '部门', type: 'treeSelect', api: adminApi.deptTree },
  { field: 'avatar', label: '头像', type: 'upload', accept: 'image/*' },
]

// 使用
<MateForm :schema="schema" @submit="handleSubmit" />
```

80% 的表单用 Schema 搞定。20% 复杂表单手写 template。这就是产品思维——**覆盖 80% 的简单场景，不牺牲 20% 的灵活性。**

---

## 九、最终技术栈速查表

| 层次 | 选择 | 砍掉 | 理由 |
|------|------|------|------|
| **框架** | Vue 3.5 + TypeScript 5.7 | React/Svelte | 全栈 Vue 统一 |
| **构建** | Vite 6 + Turborepo + pnpm | Webpack/Nx/Lerna | 最快最轻 |
| **UI 库** | Element Plus (默认) | — | 生态最厚 (可换 Naive/Ant) |
| **UI 适配** | UIAdapter 接口 | 写死一个库 | 客户可换 UI |
| **CSS** | UnoCSS (Tailwind 兼容模式) | Tailwind 4 / SCSS | HMR 快 10 倍 |
| **状态** | Pinia 3 | Vuex | 官方推荐 |
| **HTTP** | Axios | ofetch/ky | 拦截器生态最完善 |
| **图表** | ECharts (按需) | Chart.js | 中国市场标配 |
| **富文本** | Tiptap (headless) | WangEditor/Quill | 最灵活 |
| **表单** | JSON Schema + 手写混合 | 全手写 | 80/20 法则 |
| **跨端** | uni-app | Taro | Vue 迁移成本低 58% |
| **桌面** | Tauri 2 | Electron | 体积小 96% |

---

## 十、对 RFC-030 的修正

RFC-030 中 Tailwind CSS 4 改为 **UnoCSS (presetWind4 兼容模式)**。其他架构决策不变。

Sources:
- [Element Plus vs Ant Design Vue vs Naive UI npm trends](https://npmtrends.com/@arco-design/web-vue-vs-ant-design-vue-vs-element-plus-vs-naive-ui-vs-vue-devui)
- [Vben Admin: Monorepo + Swappable UI](https://github.com/vbenjs/vue-vben-admin)
- [Vue Pure Admin](https://adminlte.io/blog/vue-admin-dashboard-templates/)
- [UnoCSS vs Tailwind CSS 2026](https://www.pkgpulse.com/blog/tailwind-vs-unocss-2026)
- [Tauri vs Electron 2026](https://tech-insider.org/tauri-vs-electron-2026/)
