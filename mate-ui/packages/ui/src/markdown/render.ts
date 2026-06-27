import MarkdownIt from 'markdown-it'
import katexPlugin from '@vscode/markdown-it-katex'
import hljs from 'highlight.js/lib/common'

/**
 * 共享 Markdown 渲染内核 —— 公开聊天页、管理端对话测试、发布测试共用。
 * 能力: GFM 表格 / 代码高亮 / 行内 · 块级公式 / 链接新窗 / 标题 · 引用 · 分隔线。
 * 输出沿用 .md-* class, 样式随 @matecloud/ui 全局注入。
 *
 * 性能: 流式中用 fast 实例 (跳过 highlight.js 自动识别, 它是逐 token 重渲的瓶颈),
 *       回答完成后再用全量实例做语法高亮。
 * 安全: markdown-it 默认 html:false (不透传原始 HTML 标签), 天然防注入; 公式由 KaTeX 自行转义。
 */

const COPY_ICON = '<svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>'

/** 代码块外壳: 顶部工具条 (语言标签 + 复制按钮) + 代码体。复制由消息组件事件委托处理。 */
function codeShell(inner: string, lang: string, codeCls: string): string {
  const label = lang ? escapeHtml(lang) : 'text'
  return `<div class="md-code"><div class="md-code__bar"><span class="md-code__lang">${label}</span>`
    + `<button class="md-code__copy" type="button" data-copy aria-label="复制代码">${COPY_ICON}<span>复制</span></button></div>`
    + `<pre class="md-code__pre"><code class="${codeCls}">${inner}</code></pre></div>`
}

function applyRules(inst: MarkdownIt) {
  inst.use(katexPlugin, { throwOnError: false, errorColor: 'var(--el-color-danger)' })

  inst.renderer.rules.code_inline = (tokens, idx) =>
    `<code class="md-inline">${escapeHtml(tokens[idx].content)}</code>`

  const addClass = (name: string, cls: string) => {
    inst.renderer.rules[name] = (tokens, idx, options, _env, self) => {
      tokens[idx].attrJoin('class', cls)
      return self.renderToken(tokens, idx, options)
    }
  }
  addClass('heading_open', 'md-h')
  addClass('blockquote_open', 'md-quote')
  addClass('hr', 'md-hr')

  inst.renderer.rules.table_open = () => '<div class="md-table-wrap"><table class="md-table">'
  inst.renderer.rules.table_close = () => '</table></div>'

  const defaultLinkOpen = inst.renderer.rules.link_open
    || ((tokens, idx, options, _env, self) => self.renderToken(tokens, idx, options))
  inst.renderer.rules.link_open = (tokens, idx, options, env, self) => {
    tokens[idx].attrSet('target', '_blank')
    tokens[idx].attrSet('rel', 'noopener noreferrer')
    return defaultLinkOpen(tokens, idx, options, env, self)
  }
  return inst
}

/** 全量实例: 代码块走 highlight.js (回答完成时用)。 */
const md = applyRules(new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  highlight(code: string, lang: string): string {
    const escaped = escapeHtml(code)
    let body = escaped
    let cls = 'hljs'
    try {
      if (lang && hljs.getLanguage(lang)) {
        body = hljs.highlight(code, { language: lang, ignoreIllegals: true }).value
        cls = `hljs language-${lang}`
      } else {
        const auto = hljs.highlightAuto(code)
        body = auto.value
        cls = auto.language ? `hljs language-${auto.language}` : 'hljs'
      }
    } catch {
      body = escaped
    }
    return codeShell(body, lang, cls)
  },
}))

/** 流式实例: 代码块仅转义不高亮 (避免逐 token 跑 highlightAuto 拖慢)。 */
const mdFast = applyRules(new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  highlight(code: string, lang: string): string {
    return codeShell(escapeHtml(code), lang, 'hljs')
  },
}))

export function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

/**
 * 公式分隔符归一: 大模型输出的 \[..\] / \(..\) 统一成 $$..$$ / $..$,
 * 否则 KaTeX 插件不识别。归一前先把代码块/行内代码抽离占位 (私有区字符做哨兵,
 * 不与正文数字冲突), 避免误伤代码里的反斜杠。
 */
export function preprocessLatex(src: string): string {
  if (!src.includes('\\[') && !src.includes('\\(')) return src
  const blocks: string[] = []
  const open = String.fromCharCode(0xE000)
  const close = String.fromCharCode(0xE001)
  const stash = (s: string) =>
    s.replace(/```[\s\S]*?```|`[^`\n]+`/g, (m) => {
      blocks.push(m)
      return `${open}${blocks.length - 1}${close}`
    })
  const restore = (s: string) =>
    s.replace(new RegExp(`${open}(\\d+)${close}`, 'g'), (_m, i) => blocks[Number(i)])

  let out = stash(src)
  out = out.replace(/\\\[([\s\S]+?)\\\]/g, (_m, body) => `$$${body}$$`)
  out = out.replace(/\\\(([\s\S]+?)\\\)/g, (_m, body) => `$${body}$`)
  return restore(out)
}

/** fast=true: 流式中渲染 (跳过语法高亮); 省略/false: 全量渲染 (含 highlight.js)。 */
export function renderMarkdown(s: string, opts?: { fast?: boolean }): string {
  if (!s) return ''
  return (opts?.fast ? mdFast : md).render(preprocessLatex(s))
}
