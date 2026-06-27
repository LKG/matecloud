# RFC-053: 前端组件化纵深 — 借鉴 Dify 的设计实践

- **Status**: Proposed
- **Created**: 2026-05-28
- **Wave**: FE-7 (post componentization)
- **Dependencies**: RFC-030 (前端架构)、RFC-034 (ui 包)、RFC-047 (UI 组件化)
- **Prototypes**:
  - `docs/prototypes/mate-design-system.html` — 组件库总览 + 三层 Token + 暗色
  - `docs/prototypes/mate-list-url-state.html` — 列表页 URL 即状态
  - `docs/prototypes/mate-flow-canvas.html` — AI 编排画布
  - `docs/prototypes/mate-data-layer.html` — 数据获取层方案
  - `docs/prototypes/mate-console-topnav.html` — 顶部一级 + 左侧二三级布局
  - `docs/prototypes/mate-console-launcher.html` — 多业务一级菜单(应用启动器)
  - `docs/prototypes/mate-console-responsive.html` — 响应式 + 跨端预览框
  - `docs/prototypes/mate-console.html` — **集大成稿**(上述布局 + 响应式 + 跨端,推荐主参考)

## Context

RFC-047 把 `packages/ui` 的共享组件补齐、CRUD 样板收敛后,mate-ui 的**工程化地基已相当扎实**:pnpm + Turbo monorepo、三端共享 `core`、100+ CSS 变量的设计 token、暗色 + 主题色切换、类型化 axios + 8 个 API 模块、菜单驱动动态路由、vue-i18n 双语。

对照 `C:/codes/dify/web`(Next.js + React + Tailwind 的生产级前端)做了一轮审计,结论是:**两者地基相当,mate-ui 的差距集中在"组件化纵深"和几个管理后台高价值范式上**,而非基础设施缺失。本 RFC 记录这些借鉴点、各自的 Vue/Element Plus 等价方案,以及落地节奏。

跨技术栈的对应关系(Dify 的 React 方案 → mate-ui 的 Vue 等价)是本 RFC 的核心,避免照搬不适用的库。

---

## 借鉴点总览

| # | 借鉴点 | Dify 怎么做 | mate-ui 等价方案 | 档位 |
|---|---|---|---|---|
| 1 | base 原子组件层 + 变体系统 | `app/components/base/` + `class-variance-authority` | `packages/ui/src/base/` + `cva`(框架无关) | 🟢 高 ROI |
| 2 | 组件文档站 | Storybook + `*.stories.tsx` | **Histoire**(Vue 原生) + `*.story.vue` | 🟢 高 ROI |
| 3 | Token 语义分层 | `--color-components-input-bg-disabled` 组件级语义 token | 在调色板/语义两层上补"组件语义层" | 🟢 高 ROI |
| 4 | URL 即状态 | `nuqs` 把筛选/分页/弹窗同步进 URL | `@vueuse/core` 的 `useUrlSearchParams` 封 `useQueryState` | 🟡 范式 |
| 5 | 数据获取层 | `service/use-*` + React Query | `@tanstack/vue-query` 加一层 query hooks | 🟡 范式 |
| 6 | 画布类组件拆分 | workflow:zustand 多 slice + zundo + reactflow | Vue Flow + Pinia slice 化 + history 栈 | 🟡 范式 |
| 7 | 流式 SSE 事件契约 | `service/base.ts` 15+ 类型化回调 | AI chat 流式回调类型化 | ⚪ 锦上添花 |
| 8 | 命令式弹窗 | `ModalContextProvider` dynamic 队列 | `useDialog()` 服务 | ⚪ 锦上添花 |
| 9 | ESLint 严格化 | 禁 any / 禁 barrel / type 优先 | 收紧现有规则 | ⚪ 锦上添花 |
| 10 | i18n 按需加载 | `i18next-resources-to-backend` | 按模块/语言分包懒加载 | ⚪ 锦上添花 |

**不建议照搬**:`use-context-selector`(Pinia 本身细粒度,无需)、ORPC(后端是 Dubbo + Smart-Doc,体系不同)。

---

## 🟢 第一档:高 ROI、改动可控

### 1. base 原子组件层 + CVA 变体系统

**问题**:mate-ui 直接用 Element Plus + `packages/ui/src/styles/element-override.css`(400+ 行覆盖),变体散落、不可枚举、不可类型校验。

**方案**:
- 新建 `packages/ui/src/base/`,放无业务的原子组件(`MateButton / MateTag / MateInput` 等薄封装)。
- 引入 `class-variance-authority`(**纯函数、框架无关**,Vue `<script setup>` 可直接用),把变体声明成 `*-variants.ts`:

```ts
// packages/ui/src/base/button/button-variants.ts
import { cva, type VariantProps } from 'class-variance-authority'
export const buttonVariants = cva('btn', {
  variants: {
    variant: { primary:'btn--primary', secondary:'btn--secondary', ghost:'btn--ghost', danger:'btn--danger' },
    size:    { sm:'btn--sm', md:'btn--md', lg:'btn--lg' },
  },
  defaultVariants: { variant:'primary', size:'md' },
})
export type ButtonVariants = VariantProps<typeof buttonVariants>
```

- 现有 18 个 `Mate*` 组件按需抽出 variants 文件,把 override.css 的"覆盖意图"固化成可枚举变体。

**收益**:变体类型安全(错误 variant 名编译期报错)、自动文档、去掉硬覆盖。详见原型 `mate-design-system.html` § 02。

### 2. 组件文档站(Histoire)

**问题**:18 个组件 + AI 套件全靠 JSDoc,设计协作与新人上手成本高。这是 mate-ui 当前**最明确的能力洞**。

**方案**:接 **Histoire**(Vue 原生、Vite 同源、比 Storybook 轻),为每个 `Mate*` 组件写 `*.story.vue`。既是文档,也是视觉回归基线。

```
pnpm story:dev   → 本地组件文档站
```

**节奏**:先给 MateTable / MateForm / 4 个 AI 组件各写 1 个 story,跑通工具链,再逐步补齐。

### 3. Token 语义分层

**问题**:mate-ui 的 token 停在**调色板层**(`--mc-primary`)和**语义层**(`--mc-bg`),组件里仍直接引这两层。换肤、做"高对比/紧凑模式"时,要改的点散落在组件 CSS 里。

**方案**:补第三层——**组件级语义 token**:

```css
/* 组件只消费这一层;换肤只改映射,组件 CSS 零改动 */
--mc-input-bg: var(--mc-bg-elevated);
--mc-input-bg-disabled: var(--mc-gray-50);
--mc-input-border-destructive: var(--mc-danger);
--mc-card-shadow: var(--mc-shadow-soft);
--mc-btn-primary-bg-hover: #0E4BD4;
```

`html.dark{}` 与未来的主题只需重映射这一层。详见原型 `mate-design-system.html` § 01(切换暗色 → 组件 CSS 不动)。

---

## 🟡 第二档:范式升级,价值高但侵入大

### 4. URL 即状态(管理后台刚需)

**问题**:列表页的筛选/分页/排序是组件内 `ref`,刷新即丢、无法分享、浏览器前进后退无效。

**方案**:用 `@vueuse/core` 的 `useUrlSearchParams('history')` 封装 `useQueryState` composable,放进现在**空着的 `packages/hooks`**。MateSearchBar / MateTable / MatePagination 全部 `v-model` 到它 —— **状态唯一来源是 URL**。

```ts
// packages/hooks/src/useListQuery.ts
import { useUrlSearchParams } from '@vueuse/core'
export function useListQuery() {
  const p = useUrlSearchParams('history')
  return {
    kw:   computed({ get:()=>p.kw ?? '',     set:v=>p.kw=v }),
    page: computed({ get:()=>+(p.page ?? 1), set:v=>p.page=String(v) }),
    sort: computed({ get:()=>p.sort ?? '',   set:v=>p.sort=v }),
  }
}
```

**收益**:刷新不丢条件、可分享深链接、后退即撤销。详见交互原型 `mate-list-url-state.html`(地址栏随操作实时变化)。**试点**:先用一个 CRUD 列表页验证。

### 5. 数据获取层(TanStack Vue Query)

**问题**:`packages/core/src/api/` 的"封装"做得好,但"调用之后"每个 view 各自 `loading.value` + try/catch,切走再回来重拉,跨页面数据不同步。约 30 个 view 复制粘贴这段。

**方案**:在现有 api 之上**加一层 query hooks**(`packages/core/src/queries/`),底层 axios client / 拦截器 / Result 解包 / mock 降级**全部保留**。

```ts
export function useUsers(query) {
  return useQuery({ queryKey:['users', query], queryFn:()=>userApi.page(unref(query)), staleTime:30_000 })
}
// 写操作:
useMutation({ mutationFn:userApi.update, onSuccess:()=>qc.invalidateQueries({queryKey:['users']}) })
```

**收益**:请求去重、后台刷新、写后自动失效同步。详见方案图 `mate-data-layer.html`。

**节奏**(范式迁移,不强推):
- **P1 试点** — 装 `@tanstack/vue-query`,UserList / Dashboard 两页改用 query hook(风险隔离)。
- **P2 沉淀** — 收进 `queries/`,约定 queryKey 命名与 staleTime 默认;写操作统一 useMutation + invalidate。
- **P3 铺开** — 新页面默认走 query 层;老页面迭代时顺手迁,不为迁移而迁移。两套写法可长期共存。

### 6. 画布 / 编排类组件

**场景**:若 mate-ui 要做 AI 编排、流程画布(参见 `docs/prototypes/mate-ai-suite.html`)。

**方案**:照搬 Dify workflow 的结构思想——
- **Vue Flow**(reactflow 的 Vue 版)做画布(节点/边/缩放/拖拽)。
- **Pinia store 按 slice 拆**(node / edge / history / layout / panel),state 单一可序列化对象,变更走统一 `commit()`。
- **history 栈**实现 undo/redo(对标 Dify 的 zundo),`commit` 后 push、⌘Z 回退。
- 左侧节点选择器 + 中间画布 + 右侧选中节点配置面板的三栏布局。

详见可交互原型 `mate-flow-canvas.html`(拖入节点、配置、试运行、⌘Z 撤销均可操作)。

---

## ⚪ 第三档:锦上添花

- **7. 流式 SSE 事件契约** — Dify `service/base.ts` 把流拆成 `onNodeStarted / onMessageEnd / onError …` 15+ 类型化回调。mate-ui AI chat 已有流式,做工作流执行可视化时可借鉴此事件契约。
- **8. 命令式弹窗** — 做 `useDialog()` 服务统一管理,告别每页一堆 `dialogVisible`。
- **9. ESLint 严格化** — Dify 禁 `any` / 禁 barrel exports / type 优先;mate-ui 现 `any` 仅 warn,可逐步收紧(见 `tooling/eslint-config/`)。
- **10. i18n 按需加载** — zh/en 各 844 行,可按模块/语言分包懒加载,减小首屏 bundle。

---

## 🟢 控制台布局改版:顶部一级 + 左侧二三级 + 响应式 + 跨端

> 这是在上述借鉴点之上,对**整体控制台外壳(layout shell)**的一次改版提案,配套 4 个原型(见文件头,推荐主参考 `mate-console.html`)。它借鉴 Dify / 飞书工作台的导航范式,目标是支持**横向顶部菜单 + 业务模块持续增长 + 移动端可用**。

### 11. 布局形态:一级在顶、二三级在左

现状 `DefaultLayout.vue` 为传统"左侧栏一栏到底"。改版后:

- **顶栏** = 一级菜单(模块/应用)横向排布 + 品牌 + 全局搜索 + 工具区。
- **左侧栏** = 当前一级下的**二级分组 + 三级折叠子项**(三级用可展开容器 + 缩进连接线)。
- **内容区** = 面包屑(三级联动)+ keep-alive 页面。

菜单仍由后端 `menuTree` 驱动:把一级渲染到顶栏、二三级渲染到左侧即可,**菜单数据结构基本不变**。

### 12. 多业务一级菜单:应用启动器(App Launcher)

**核心问题**:平台菜单(系统/监控/工具)固定,但业务菜单随 `mate-cli new module mate-order` 这类微服务**持续增长**,横向顶栏宽度有限,不能全塞。

**策略**(对标 Dify workspace / 飞书工作台 / 九宫格):把每个业务微服务当作一个 **App**,分三种角色显示——

| 菜单类型 | 显示策略 |
|---|---|
| 少数高频固定菜单(仪表盘/系统管理) | 直接钉在顶栏 |
| 大量业务菜单(订单/商品/库存/CRM…) | 收进 **⊞ 应用启动器(mega 面板)**,按业务域分组 + 搜索 + ★ 钉选 |
| 超出顶栏宽度的部分 | 顶栏「更多 ▾」溢出下拉 + **⌘K 全局搜索**兜底 |

**后端配合**:`menuTree` 一级菜单加一个 `category` 字段(`平台能力 / 业务应用 / AI 套件 / 开发工具`)用于启动器分组;`business:boolean` 标记业务应用。其余复用现有动态菜单机制。每新增一个业务微服务,启动器自动多一张卡片,**顶栏布局零改动**。

### 13. 响应式:三档断点

| 断点 | 一级菜单 | 导航载体 | 侧栏 | 搜索 |
|---|---|---|---|---|
| 桌面 ≥1024px | 横向顶栏 | 顶栏 + 启动器 + 更多 | 常驻左侧 | 完整搜索框 |
| 平板 ≤1024px | 横向(启动器收成图标) | 同上 | 变窄 ~200px | 完整 |
| 手机 ≤768px | **隐藏**,移到底部 | **底部 Tab 栏**(4 常用 App + 全部)+ ☰ 抽屉 | **左侧抽屉**(滑入 + 遮罩) | 折叠成 🔍 图标 |

手机端把一级菜单转为原生**底部 Tab 栏**,☰ 汉堡打开**抽屉式侧栏**显示二三级,面包屑隐藏——移动端处理"多业务菜单"的标准范式。

### 14. 跨浏览器 / 跨端兼容清单

- **`<meta viewport>`** 必须补齐(现各页缺失,手机会按桌面宽缩放):`width=device-width` + `viewport-fit=cover`(刘海屏)+ `maximum-scale=1`(防 iOS 输入聚焦缩放)。
- **`100dvh`**(配 `100vh` 回退):移动端动态视口,避免被地址栏遮挡。
- **安全区 `env(safe-area-inset-*)`**:底部 Tab 栏避开 Home 条,顶栏避开刘海。
- **`backdrop-filter` 回退**:`@supports not(...)` 下降级为不透明背景(老 Firefox / 部分安卓)。
- **触摸优化**:`-webkit-tap-highlight-color:transparent`、`touch-action:manipulation`、`-webkit-overflow-scrolling:touch`、`@media (hover:none)` 去触屏 hover 残留、`text-size-adjust:100%`。
- **可点区** ≥ 40–44px;表格用横向滚动容器避免窄屏溢出。
- 标准属性 + `-webkit-` 前缀并存;避免 `:has()` 等兼容性差的选择器。

> 验证:已在 375px(手机)与 893/961px(桌面)两个视口下逐项断言显隐与定位,两断点均符合预期。

### 落地改造点(代码层)

| 文件 | 改动 |
|---|---|
| `apps/admin/src/layouts/DefaultLayout.vue` | shell 改为 顶栏 + (侧栏/抽屉) + 内容 + 底部栏;接 `useBreakpoints` 切换桌面/移动布局 |
| `apps/admin/src/layouts/LayoutHeader.vue` | 渲染一级菜单(pinned)、启动器入口、更多溢出、⌘K、搜索折叠 |
| `apps/admin/src/layouts/LayoutSidebar.vue` | 渲染当前一级的二/三级(折叠);移动端改 `position:fixed` 抽屉 + 遮罩 |
| 新增 `LayoutBottomBar.vue` | 移动端底部 Tab(前 4 pinned + 全部) |
| 新增 `AppLauncher.vue` | mega 面板:分组 + 搜索 + 钉选(钉选状态持久化到 `useSystemStore`) |
| `apps/admin/src/styles/tokens.css` | 补 `--safe-t/--safe-b`、`--mc-topbar-h`、`--mc-bottombar-h` |
| `apps/admin/index.html` | 补齐 `<meta viewport>` |
| 后端 `menuTree` | 一级菜单加 `category` + `business` 字段(向后兼容,无字段时归入"未分类") |

---

## 影响面

| 借鉴点 | 改动范围 | 兼容性 |
|---|---|---|
| 1 base + CVA | `packages/ui` 新增 base/,加依赖 `class-variance-authority` | 增量,旧组件不破坏 |
| 2 Histoire | 新增 `*.story.vue` + 工具链 | 纯增量,不影响运行时 |
| 3 Token 语义层 | `apps/admin/src/styles/tokens.css` 加变量 | 向后兼容,旧变量保留 |
| 4 URL 即状态 | `packages/hooks` 落地 + 列表页接入 | 增量,逐页迁移 |
| 5 Vue Query | 新增 `core/src/queries/` + 加依赖 | 两套写法共存 |
| 6 画布 | 新功能模块,加 `@vue-flow/core` | 独立新增 |
| 11–14 布局改版 | `layouts/*` 重构 + 新增 `AppLauncher`/`LayoutBottomBar` + `menuTree` 加 `category`/`business` 字段 + 补 viewport meta | layout 层重构,**不动业务页面**;`menuTree` 新字段向后兼容 |

无破坏性变更;1/2/3 可独立先行,4/5/6 按节奏渐进;11–14 为 layout 层改版,与业务页面解耦。

## 未决问题

- Histoire vs Storybook:倾向 Histoire(Vue 原生、轻),待工具链验证。
- Vue Query 是否纳入 `core` 默认依赖,还是仅 `admin` 引入(mobile/desktop 暂不需要)。
- 第三档各点优先级排序,留待评审。
- 布局改版(11–14)是否作为独立 Wave 先行落地(它与组件化借鉴 1–10 解耦,可单独排期)。
- 顶栏"钉选"状态存储位置:`useSystemStore` localStorage,还是随用户配置存后端(跨设备同步)。
- 一级菜单 `category` 分类口径由谁定义:后端菜单管理界面配置,还是前端约定固定几类。

## 落地建议

先做第一档(1/2/3):改动可控、收益直接、为后续打基础。第二档按"试点 → 沉淀 → 铺开"渐进,先各挑 1 个页面验证。第三档随迭代顺手做。

> 本 RFC 配套 4 个 HTML 原型(见文件头 Prototypes),评审时可直接在浏览器打开交互。
