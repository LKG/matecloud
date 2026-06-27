import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const host = process.env.TAURI_DEV_HOST

export default defineConfig({
  plugins: [vue()],
  // pnpm links @matecloud/ui as a separate package; without dedupe Vite loads
  // element-plus/vue as TWO instances (app vs. linked package), breaking EP's
  // provide/inject Symbols (locale/config). Force a single instance — same
  // rationale as apps/admin.
  resolve: {
    dedupe: ['element-plus', '@element-plus/icons-vue', 'vue', 'vue-router', 'pinia', 'vue-i18n'],
  },
  clearScreen: false,
  server: {
    port: 5175,
    strictPort: true,
    host: host || false,
    hmr: host
      ? { protocol: 'ws', host, port: 5176 }
      : undefined,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:9010',
        changeOrigin: true,
      },
    },
  },
  envPrefix: ['VITE_', 'TAURI_'],
})
