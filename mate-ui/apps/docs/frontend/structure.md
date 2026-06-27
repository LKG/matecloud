# 前端项目结构

## apps/admin

管理后台主应用：

```
apps/admin/
├── src/
│   ├── views/            # 页面组件（按功能模块分目录）
│   │   ├── system/       # 系统管理（用户、角色、菜单、部门、字典）
│   │   ├── microservice/ # 微服务运维（灰度、限流、超时）
│   │   ├── monitor/      # 监控（日志、在线用户）
│   │   └── tenant/       # 租户管理
│   ├── router/           # 路由配置
│   ├── i18n/             # 国际化（zh-CN / en-US）
│   ├── layouts/          # 布局组件
│   └── App.vue           # 根组件
├── public/               # 静态资源
└── vite.config.ts        # Vite 配置
```

## packages/core

共享核心逻辑：

```
packages/core/
├── src/
│   ├── api/              # API 客户端（按服务分文件）
│   ├── types/            # TypeScript 类型定义
│   ├── stores/           # Pinia Store
│   └── constants/        # 常量
```

## packages/ui

共享 UI 组件：

```
packages/ui/
├── src/
│   ├── MateTable/        # 通用表格组件
│   ├── MatePageCard/     # 页面卡片容器
│   ├── MateStatTile/     # 统计卡片
│   ├── MateForm/         # 动态表单
│   └── index.ts          # 统一导出
```

## packages/hooks

组合式函数：

```
packages/hooks/
├── src/
│   ├── useTable.ts       # 表格数据加载、分页、排序
│   ├── useForm.ts        # 表单校验、提交
│   ├── useDict.ts        # 字典数据加载
│   └── index.ts
```

## packages/utils

通用工具函数（格式化、校验、存储等）。
