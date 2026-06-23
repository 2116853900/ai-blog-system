import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('/vue') || id.includes('@vue')) return 'vendor-vue'
            if (id.includes('markdown-it')) return 'vendor-markdown'
            if (id.includes('axios')) return 'vendor-http'
            return 'vendor'
          }
          if (id.includes('/src/views/admin/')) return 'admin'
        }
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
