import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'MateCloud',
  description: 'DDD 微服务脚手架 — Spring Boot 4 + Spring Cloud 2025 + Dubbo + Spring AI',
  lang: 'zh-CN',

  head: [
    ['link', { rel: 'icon', type: 'image/png', href: '/logo.png' }],
  ],

  themeConfig: {
    logo: '/logo.png',

    nav: [
      { text: '指南', link: '/guide/introduction', activeMatch: '/guide/' },
      { text: '架构', link: '/architecture/overview', activeMatch: '/architecture/' },
      { text: 'Starters', link: '/starters/overview', activeMatch: '/starters/' },
      { text: 'CLI', link: '/cli/overview', activeMatch: '/cli/' },
      { text: '前端', link: '/frontend/overview', activeMatch: '/frontend/' },
      { text: 'AI', link: '/ai/overview', activeMatch: '/ai/' },
      { text: '部署', link: '/deploy/docker', activeMatch: '/deploy/' },
    ],

    sidebar: {
      '/guide/': [
        {
          text: '开始',
          items: [
            { text: '简介', link: '/guide/introduction' },
            { text: '快速开始', link: '/guide/quick-start' },
            { text: '项目结构', link: '/guide/project-structure' },
            { text: '配置体系', link: '/guide/configuration' },
          ],
        },
        {
          text: '基础',
          items: [
            { text: '编码规范', link: '/guide/conventions' },
            { text: '错误码', link: '/guide/error-codes' },
            { text: '新建业务模块', link: '/guide/new-module' },
            { text: '菜单配置', link: '/guide/menu-config' },
          ],
        },
      ],
      '/architecture/': [
        {
          text: '架构设计',
          items: [
            { text: '总体架构', link: '/architecture/overview' },
            { text: 'DDD 四层结构', link: '/architecture/ddd' },
            { text: 'CQRS 模式', link: '/architecture/cqrs' },
            { text: '多租户', link: '/architecture/multi-tenant' },
          ],
        },
      ],
      '/starters/': [
        {
          text: 'Starter 指南',
          items: [
            { text: '总览', link: '/starters/overview' },
            { text: 'mate-ds-starter', link: '/starters/ds' },
            { text: 'mate-web-starter', link: '/starters/web' },
            { text: 'mate-cache-starter', link: '/starters/cache' },
            { text: 'mate-sa-token-starter', link: '/starters/sa-token' },
            { text: 'mate-mq-starter', link: '/starters/mq' },
            { text: 'mate-security-starter', link: '/starters/security' },
            { text: 'mate-file-starter', link: '/starters/file' },
            { text: 'mate-tenant-starter', link: '/starters/tenant' },
            { text: 'mate-ai-starter', link: '/starters/ai' },
          ],
        },
      ],
      '/cli/': [
        {
          text: 'CLI 工具',
          items: [
            { text: '总览', link: '/cli/overview' },
            { text: '脚手架命令', link: '/cli/scaffold' },
            { text: '服务管理', link: '/cli/service' },
            { text: 'Nacos 配置', link: '/cli/config' },
            { text: 'AI 命令', link: '/cli/ai' },
            { text: 'MCP Server', link: '/cli/mcp' },
          ],
        },
      ],
      '/frontend/': [
        {
          text: '前端开发',
          items: [
            { text: '总览', link: '/frontend/overview' },
            { text: '项目结构', link: '/frontend/structure' },
            { text: '开发指南', link: '/frontend/dev-guide' },
            { text: '组件库', link: '/frontend/components' },
          ],
        },
      ],
      '/ai/': [
        {
          text: 'AI 集成',
          items: [
            { text: '总览', link: '/ai/overview' },
            { text: '@Tool 注解', link: '/ai/tool-annotation' },
            { text: 'LLM 提供商', link: '/ai/providers' },
            { text: 'MCP 协议', link: '/ai/mcp' },
          ],
        },
      ],
      '/deploy/': [
        {
          text: '部署运维',
          items: [
            { text: 'Docker Compose', link: '/deploy/docker' },
            { text: '生产部署', link: '/deploy/production' },
            { text: '数据库迁移', link: '/deploy/database' },
          ],
        },
      ],
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com/matevip/matecloud' },
    ],

    footer: {
      message: 'Released under the Apache 2.0 License.',
      copyright: 'Copyright © 2024-2026 MateCloud Contributors',
    },

    search: {
      provider: 'local',
    },

    editLink: {
      pattern: 'https://github.com/matevip/matecloud/edit/main/mate-ui/apps/docs/:path',
      text: '在 GitHub 上编辑此页',
    },

    lastUpdated: {
      text: '最后更新',
    },

    outline: {
      label: '页面导航',
    },

    docFooter: {
      prev: '上一篇',
      next: '下一篇',
    },
  },
})
