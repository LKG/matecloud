# RFC-032: Frontend Skeleton — Monorepo + Toolchain

- **Status**: Draft
- **Created**: 2026-04-12
- **Wave**: FE-1 (first, sequential)
- **Dependencies**: None

## Scope

创建 `mate-ui/` 前端 Monorepo 骨架。完成后 `pnpm install && pnpm dev --filter admin` 能看到空白页。

## 交付物

```
mate-ui/
├── package.json
├── pnpm-workspace.yaml
├── turbo.json
├── .gitignore
├── .npmrc
├── apps/
│   └── admin/
│       ├── package.json
│       ├── index.html
│       ├── vite.config.ts
│       ├── tsconfig.json
│       ├── src/
│       │   ├── main.ts
│       │   ├── App.vue
│       │   └── env.d.ts
│       └── uno.config.ts
├── packages/
│   ├── core/
│   │   ├── package.json
│   │   ├── tsconfig.json
│   │   └── src/
│   │       └── index.ts
│   ├── ui/
│   │   ├── package.json
│   │   ├── tsconfig.json
│   │   └── src/
│   │       └── index.ts
│   ├── hooks/
│   │   ├── package.json
│   │   └── src/
│   │       └── index.ts
│   └── utils/
│       ├── package.json
│       └── src/
│           └── index.ts
└── tooling/
    ├── eslint-config/
    │   ├── package.json
    │   └── index.js
    └── tsconfig/
        ├── package.json
        ├── base.json
        ├── vue.json
        └── node.json
```

## 源码

### Root package.json

```json
{
  "name": "mate-ui",
  "private": true,
  "packageManager": "pnpm@9.15.0",
  "scripts": {
    "dev": "turbo dev",
    "build": "turbo build",
    "lint": "turbo lint",
    "typecheck": "turbo typecheck",
    "clean": "turbo clean && rm -rf node_modules"
  },
  "devDependencies": {
    "turbo": "^2.4.0",
    "typescript": "^5.7.0"
  }
}
```

### pnpm-workspace.yaml

```yaml
packages:
  - 'apps/*'
  - 'packages/*'
  - 'tooling/*'
```

### turbo.json

```json
{
  "$schema": "https://turbo.build/schema.json",
  "tasks": {
    "build": {
      "dependsOn": ["^build"],
      "outputs": ["dist/**"]
    },
    "dev": {
      "cache": false,
      "persistent": true
    },
    "lint": {
      "dependsOn": ["^build"]
    },
    "typecheck": {
      "dependsOn": ["^build"]
    },
    "clean": {
      "cache": false
    }
  }
}
```

### .npmrc

```ini
shamefully-hoist=true
strict-peer-dependencies=false
auto-install-peers=true
```

### tooling/tsconfig/base.json

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "lib": ["ES2022", "DOM", "DOM.Iterable"],
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "declaration": true,
    "declarationMap": true,
    "sourceMap": true
  }
}
```

### tooling/tsconfig/vue.json

```json
{
  "extends": "./base.json",
  "compilerOptions": {
    "jsx": "preserve",
    "jsxImportSource": "vue",
    "paths": {
      "@/*": ["./src/*"]
    }
  }
}
```

### tooling/eslint-config/index.js

```js
module.exports = {
  root: true,
  env: { browser: true, node: true, es2022: true },
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-recommended',
    'plugin:@typescript-eslint/recommended',
    'prettier',
  ],
  parser: 'vue-eslint-parser',
  parserOptions: {
    parser: '@typescript-eslint/parser',
    ecmaVersion: 2022,
    sourceType: 'module',
  },
  rules: {
    'vue/multi-word-component-names': 'off',
    '@typescript-eslint/no-explicit-any': 'warn',
  },
}
```

### apps/admin/package.json

```json
{
  "name": "@matecloud/admin",
  "private": true,
  "version": "1.0.0",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc --noEmit && vite build",
    "preview": "vite preview",
    "lint": "eslint src --ext .ts,.vue --fix",
    "typecheck": "vue-tsc --noEmit"
  },
  "dependencies": {
    "vue": "^3.5.0",
    "vue-router": "^4.5.0",
    "pinia": "^3.0.0",
    "pinia-plugin-persistedstate": "^4.0.0",
    "element-plus": "^2.9.0",
    "@element-plus/icons-vue": "^2.3.0",
    "axios": "^1.7.0",
    "@vueuse/core": "^12.0.0",
    "@matecloud/core": "workspace:*",
    "@matecloud/ui": "workspace:*",
    "@matecloud/hooks": "workspace:*",
    "@matecloud/utils": "workspace:*"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.2.0",
    "vite": "^6.0.0",
    "vue-tsc": "^2.2.0",
    "unocss": "^0.65.0",
    "@unocss/preset-wind": "^0.65.0",
    "unplugin-auto-import": "^0.19.0",
    "unplugin-vue-components": "^0.28.0",
    "typescript": "^5.7.0",
    "@matecloud/eslint-config": "workspace:*",
    "@matecloud/tsconfig": "workspace:*"
  }
}
```

### apps/admin/vite.config.ts

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import UnoCSS from 'unocss/vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { resolve } from 'path'

export default defineConfig({
  plugins: [
    vue(),
    UnoCSS(),
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia', '@vueuse/core'],
      resolvers: [ElementPlusResolver()],
      dts: 'src/auto-imports.d.ts',
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts',
    }),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:9010',
        changeOrigin: true,
      },
    },
  },
})
```

### apps/admin/uno.config.ts

```typescript
import { defineConfig, presetWind4, presetIcons } from 'unocss'

export default defineConfig({
  presets: [
    presetWind4(),
    presetIcons({ scale: 1.2 }),
  ],
  theme: {
    colors: {
      primary: '#409eff',
    },
  },
})
```

### apps/admin/src/main.ts

```typescript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import App from './App.vue'
import 'virtual:uno.css'

const app = createApp(App)
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)
app.use(pinia)
app.mount('#app')
```

### apps/admin/src/App.vue

```vue
<template>
  <div class="h-screen flex items-center justify-center">
    <h1 class="text-2xl font-bold text-primary">MateCloud Admin</h1>
  </div>
</template>
```

### packages/core/package.json

```json
{
  "name": "@matecloud/core",
  "version": "1.0.0",
  "main": "./src/index.ts",
  "types": "./src/index.ts",
  "dependencies": {
    "axios": "^1.7.0",
    "pinia": "^3.0.0",
    "vue": "^3.5.0"
  }
}
```

### packages/ui/package.json

```json
{
  "name": "@matecloud/ui",
  "version": "1.0.0",
  "main": "./src/index.ts",
  "types": "./src/index.ts",
  "dependencies": {
    "vue": "^3.5.0",
    "element-plus": "^2.9.0"
  }
}
```

## 验证

```bash
cd mate-ui
pnpm install
pnpm dev --filter @matecloud/admin
# 浏览器 http://localhost:3000 看到 "MateCloud Admin"
```
