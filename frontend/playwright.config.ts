import { defineConfig, devices } from '@playwright/test'

/**
 * Configuración de Playwright para el e2e del camino crítico del docente.
 *
 * Requiere el stack vivo (backend Spring + Postgres + Ollama + frontend) — se
 * corre en la fase de pruebas integrales, no en el `vitest` de CI unitario.
 * Levanta el frontend con `pnpm dev` y apunta al backend vía `VITE_API_URL`.
 * Variables: `E2E_BASE_URL` (default http://localhost:5173),
 * `E2E_EMAIL`/`E2E_PASSWORD` (default credenciales del seed demo).
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: 'list',
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
    trace: 'on-first-retry',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'pnpm dev',
    url: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
})
