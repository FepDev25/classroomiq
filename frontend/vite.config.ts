/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { tanstackRouter } from '@tanstack/router-plugin/vite'
import path from 'node:path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    // El plugin de router debe ir antes que el de React.
    tanstackRouter({ target: 'react', autoCodeSplitting: true }),
    react(),
    tailwindcss(),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    css: true,
    // baseUrl absoluto para que MSW intercepte por origen en los tests.
    env: { VITE_API_URL: 'http://localhost:8080' },
    // Playwright vive en e2e/ y se corre con `pnpm e2e`, no con Vitest.
    exclude: ['e2e/**', 'node_modules/**', 'dist/**'],
  },
})
