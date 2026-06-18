import { expect, test } from '@playwright/test'

/**
 * E2E del portal admin. Requiere el stack vivo (ver playwright.config.ts) y la
 * cuenta admin sembrada por el DataSeeder. Cubre login → cuentas → asignación
 * de coordinador → dashboard de métricas de uso/costo.
 */
const EMAIL = process.env.E2E_ADMIN_EMAIL ?? 'admin@demo.local'
const PASSWORD = process.env.E2E_ADMIN_PASSWORD ?? 'admin12345'

async function login(page: import('@playwright/test').Page) {
  await page.goto('/login')
  await page.getByLabel(/email/i).fill(EMAIL)
  await page.getByLabel(/contraseña/i).fill(PASSWORD)
  await page.getByRole('button', { name: /entrar|iniciar/i }).click()
  // El dispatcher por rol lleva al admin a /admin/cuentas.
  await expect(page).toHaveURL(/\/admin\/cuentas/)
}

test('el admin ve la gestión de cuentas', async ({ page }) => {
  await login(page)
  await expect(page.getByRole('heading', { name: /cuentas/i })).toBeVisible()
  await expect(page.getByRole('button', { name: /nueva cuenta/i })).toBeVisible()
})

test('el admin abre el dashboard de uso y costo', async ({ page }) => {
  await login(page)
  await page.getByRole('link', { name: /uso y costo/i }).click()
  await expect(page).toHaveURL(/\/admin\/metricas/)
  await expect(page.getByRole('heading', { name: /uso y costo/i })).toBeVisible()
  // Selector de mes presente; las tarjetas resumen dependen de datos del mes.
  await expect(page.getByLabel(/^mes$/i)).toBeVisible()
})

test('el admin navega a la asignación de coordinadores', async ({ page }) => {
  await login(page)
  await page.getByRole('link', { name: /coordinadores/i }).click()
  await expect(page).toHaveURL(/\/admin\/coordinadores/)
  await expect(page.getByRole('heading', { name: /coordinadores/i })).toBeVisible()
})
