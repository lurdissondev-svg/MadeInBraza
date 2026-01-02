import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  base: '/web/',
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  build: {
    // Use env var for Docker, default for local dev
    outDir: process.env.BUILD_OUTPUT || '../backend/web/dist',
    emptyOutDir: true
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'https://braza.lurdisson.com.br',
        changeOrigin: true
      }
    }
  }
})
