# 规则 05 · 前端（mate-ui）

> 适用于 `mate-ui/`（pnpm + turbo monorepo：`apps/{admin,desktop}` + `packages/{ui,core,hooks,utils}`，
> Vue 3 + Vite + Element Plus + TypeScript）。事实来源：本仓库现状 + 记忆库若干前端踩坑。

## 目录与消费

- 共享组件放 `packages/ui`（`@matecloud/ui`），从 `src/index.ts` **统一导出**；应用侧只从 `@matecloud/ui` 消费，不深路径 import。
- **`packages/ui` 内部互相引用走相对路径**（`../MateMessage/message`），**禁止 self-import `@matecloud/ui`**（会自引、易循环）。
- 共享逻辑/请求在 `@matecloud/core`，hooks 在 `@matecloud/hooks`，纯函数在 `@matecloud/utils`。

## 消息与弹窗：用自有组件，禁 Element Plus 的

- 轻提示用 **`MateMessage`**（替代 `ElMessage`），确认/输入弹窗用 **`MateMessageBox`**（替代 `ElMessageBox`）——
  两者在 `packages/ui/src/MateMessage/`，玻璃拟态 + 全程 `--mc-*` token、暗黑自适应，API 与 EP 同形（drop-in）。
- **禁止 `ElMessage` / `ElMessageBox`**：包括显式 `import ... from 'element-plus'`，也包括 EP unplugin **自动导入**后直接
  `ElMessage.success(...)`（手滑高发区——admin 配了 ElementPlusResolver 自动注册，CR 时盯紧）。
- host `<MateMessageHost/>` + `<MateMessageBoxHost/>` 在每个 app 的 `App.vue` **常驻挂载**一次（新建 app 必做）；
  纯 `.ts` 里调用也行（无 host 时会懒挂载兜底）。`MateMessageBox.prompt` 返回 `{ value, action }`，任意关闭方式都 `reject('cancel')`。
- ⚠️ 机检：`check-no-elmessage`（**阻断级**，存量已清零，保持为零）。

## 组件式弹窗/浮层：用 Mate 封装,别用裸 Element Plus

应用层(apps)的弹窗/浮层都走 `@matecloud/ui` 封装,不要直接写裸 `<el-*>`：

| 用途 | 用 | 替代 | 备注 |
|------|----|------|------|
| 模态对话框 | `MateDialog` | `el-dialog` | v-model/title/width + 默认 Cancel/Confirm footer;**仅查看的弹窗加 `:show-footer="false"`** 否则会注入确认按钮 |
| 侧边抽屉 | `MateDrawer` | `el-drawer` | footer 默认关(`showFooter` 或 `#footer` 才出);非 prop 属性透传 |
| 行内确认气泡 | `MateInlineConfirm` | `el-popconfirm` | 触发元素放**默认插槽**(非 `#reference`);`variant=delete/warning/info` |
| 悬浮提示 | `MateTooltip` | `el-tooltip` | 透传;`#content` 转发 |
| 下拉菜单 | `MateDropdown` | `el-dropdown` | 透传;`#dropdown` 内仍用 `el-dropdown-menu`/`el-dropdown-item`(EP) |
| 浮层 | `MatePopover` | `el-popover` | 透传;`#reference` + 默认插槽 |

- 全部 host(`MateMessageHost`/`MateMessageBoxHost`)在 `App.vue` 常驻;`MateDialog`/`MateDrawer` 自带 `append-to-body`。
- **例外(保留裸 EP)**：① `packages/ui`/`packages/core` 内部基元(`SliderCaptcha`、`MateImportExport`、各 Mate 封装自身的实现)——包内用相对路径,不 self-import;② `el-dropdown-menu`/`el-dropdown-item` 作为 `#dropdown` 槽内容仍是 EP。
- 迁移注意:`el-*` 标签正则改名时,`<el-dropdown\b` 会**误伤** `<el-dropdown-menu>`/`<el-dropdown-item>`(`\b` 在连字符处成立),改名需用 `<el-dropdown(?=[\s/>])`。

## 设计 token：禁硬编码颜色

- 组件样式只用 **`--mc-*`** 语义 token（见 `apps/admin/src/styles/tokens.css` 与 `packages/ui/src/base/base.css`），
  EP 变量用经 override 的 `--el-*`。**不要在 `.vue <style>` / css 里直写 `#rrggbb` / `rgb(...)` 颜色**。
- 暗黑模式靠 `html.dark` 重映射 token 自动跟随；**硬编码色面（菜单选中底、浅色面板）会在暗黑下穿帮**——
  选中底走 `applyThemeColor`、浅色面用 `--mc-fill-hover`，别写死。
- ⚠️ 机检：`check-frontend-hardcoded-color`（**告警级**，存量较多，逐步还）。

## 改造 Element Plus 组件

- 想把 EP 组件改成卡片/自定义风格，**用复合选择器** `.el-xxx.your-class` 压过 EP 默认——
  只用单个自定义类，HMR 重排样式后会被 EP 默认值打回。
- CSS 布局类 bug 优先用**独立复现页**定位，别在大页面里盲调。

## Element Plus 单实例（vite dedupe）

- 每个 app 的 `vite.config.ts` 必须 `resolve.dedupe` 含 `element-plus`（连带 `@element-plus/icons-vue`/`vue`/`vue-router`/`pinia`/`vue-i18n`）。
- 否则 pnpm 把 `@matecloud/ui` 链成独立包 → element-plus 被加载成**两个实例** → 链接包内的 EP 组件吃不到全局
  `ElConfigProvider` 的 locale/config（如 `<el-pagination>` 退回英文）。
- ⚠️ 机检：`check-vite-dedupe`（**阻断级**）。

## 其他易踩点（记忆库沉淀）

- **菜单分组节点无 `path`**：`el-sub-menu :index` 必须 fallback 到 `id`，否则同级分组会一起展开。
- **缺省布尔 prop 会被转成 `false` 而非 `undefined`**：受控/非受控两用组件别用 `!== undefined` 判受控，靠内部 `ref` + `watch`。
- **模板里别写字面 `{{ }}`**（会被 Vue 当插值解析）；需要展示双花括号要转义。

## 验证（流程铁律）

- 改 `mate-ui` 后跑**真实 vite build**：`pnpm --filter @matecloud/admin build`（`vue-tsc --noEmit` 不够，构建期才暴露的问题它发现不了）；
  动了 desktop 也跑 `pnpm --filter @matecloud/desktop typecheck`。
- 注释**只写技术、不写来源产品**（Dify/FastGPT/qKnow 等）——见 [规则 04](04-open-source.md)，机检 `check-competitor-names` 覆盖 `.vue/.ts`。
