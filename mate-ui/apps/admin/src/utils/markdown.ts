/**
 * Markdown 渲染 —— 已迁移到 @matecloud/ui 的共享内核 (markdown-it + KaTeX 公式 + 代码高亮)。
 * 此处仅再导出, 保持既有 `@/utils/markdown` 引用不动 (公开聊天页 / 对话测试 / 引用徽标处理)。
 */
export { renderMarkdown, escapeHtml, preprocessLatex } from '@matecloud/ui'
