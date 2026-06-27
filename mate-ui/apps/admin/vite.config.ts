import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import UnoCSS from 'unocss/vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { resolve } from 'path'

export default defineConfig(({ mode }) => {
  // /api 代理目标: 默认本地网关 (CLAUDE.md 约定 9010); 远端/联调地址放 .env.local 的
  // VITE_API_PROXY_TARGET, 避免把外网 IP 硬编码进仓库 (数据串环境 + 调试不可复现)。
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_API_PROXY_TARGET || 'http://127.0.0.1:9010'
  return {
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
    // pnpm links @matecloud/ui as a separate package, so without dedupe Vite
    // loads element-plus/vue as TWO module instances (app vs. linked package).
    // That mismatches Element Plus's provide/inject Symbols (e.g.
    // localeContextKey) — ElConfigProvider in the app would provide zh-cn but
    // <el-pagination> inside MatePagination (from @matecloud/ui) reads a
    // different Symbol and falls back to English. Force a single instance.
    dedupe: ['element-plus', '@element-plus/icons-vue', 'vue', 'vue-router', 'pinia', 'vue-i18n'],
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: apiTarget,
        changeOrigin: true,
      },
    },
  },
  build: {
    // Vite 8's default rolldown bundler / minifier OOMs on Windows when
    // echarts (~870KB pre-min) is in the graph. esbuild minifier sidesteps
    // that and produces output within ~5% of rolldown's size — worth it.
    minify: 'esbuild',
  },
  }
})
