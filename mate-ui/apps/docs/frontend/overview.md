# 前端总览

MateCloud 前端（`mate-ui`）是一个基于 **pnpm workspace** 的 monorepo，采用 Turbo 构建编排。

## 技术栈

| 技术 | 用途 |
|------|------|
| Vue 3 | UI 框架（Composition API + `<script setup>`） |
| TypeScript | 类型安全 |
| Vite | 构建工具 + 开发服务器 |
| Element Plus | UI 组件库 |
| pnpm | 包管理（workspace monorepo） |
| Turbo | 构建编排 |

## 项目结构

```
mate-ui/
├── apps/
│   ├── admin/            # 管理后台 SPA（主应用）
│   ├── desktop/          # 桌面端（Electron）
│   └── mobile/           # 移动端
├── packages/
│   ├── core/             # API 客户端、类型定义、Store
│   ├── hooks/            # 组合式函数（useTable, useForm, ...）
│   ├── ui/               # 共享 UI 组件（MateTable, MatePageCard, ...）
│   └── utils/            # 通用工具函数
└── tooling/              # 构建工具配置
```

## 快速开始

```bash
cd mate-ui
pnpm install
pnpm dev              # → http://localhost:3000
```

默认开发账号：`admin` / `admin123`

## API 代理

Vite 开发服务器将 `/api` 代理到 `http://127.0.0.1:9010`（网关），确保后端服务已启动。

## 构建

```bash
pnpm build            # 输出到 apps/admin/dist/
```
