import { expect, test } from '@playwright/test'

import { escanearA11y, loginComo } from './a11y'

/**
 * Pase de accesibilidad (axe-core) sobre las vistas NUEVAS de la v2 multi-rol:
 * home del docente, portal admin (cuentas, uso/costo, coordinadores) y home del
 * coordinador. Requiere el stack vivo (ver playwright.config.ts) y las cuentas
 * sembradas por el DataSeeder. DoD H6: sin violaciones a11y críticas/serias.
 */
const DOCENTE = {
  email: process.env.E2E_EMAIL ?? 'docente@demo.local',
  password: process.env.E2E_PASSWORD ?? 'docente12345',
}
const ADMIN = {
  email: process.env.E2E_ADMIN_EMAIL ?? 'admin@demo.local',
  password: process.env.E2E_ADMIN_PASSWORD ?? 'admin12345',
}
const COORD = {
  email: process.env.E2E_COORD_EMAIL ?? 'coordinador@demo.local',
  password: process.env.E2E_COORD_PASSWORD ?? 'coord12345',
}

test('a11y — home del docente', async ({ page }) => {
  await loginComo(page, DOCENTE.email, DOCENTE.password)
  await expect(page.getByRole('heading', { name: /hola de nuevo/i })).toBeVisible()
  await escanearA11y(page, 'home docente')
})

test('a11y — admin: cuentas', async ({ page }) => {
  await loginComo(page, ADMIN.email, ADMIN.password)
  await expect(page).toHaveURL(/\/admin\/cuentas/)
  await expect(page.getByRole('heading', { name: /cuentas/i })).toBeVisible()
  await escanearA11y(page, 'admin cuentas')
})

test('a11y — admin: uso y costo', async ({ page }) => {
  await loginComo(page, ADMIN.email, ADMIN.password)
  await page.getByRole('link', { name: /uso y costo/i }).click()
  await expect(page.getByRole('heading', { name: /uso y costo/i })).toBeVisible()
  await escanearA11y(page, 'admin uso y costo')
})

test('a11y — admin: coordinadores', async ({ page }) => {
  await loginComo(page, ADMIN.email, ADMIN.password)
  await page.getByRole('link', { name: /coordinadores/i }).click()
  await expect(page.getByRole('heading', { name: /coordinadores/i })).toBeVisible()
  await escanearA11y(page, 'admin coordinadores')
})

test('a11y — home del coordinador', async ({ page }) => {
  await loginComo(page, COORD.email, COORD.password)
  await expect(page).toHaveURL(/\/coordinador/)
  await expect(page.getByRole('heading', { level: 1, name: /materias asignadas/i })).toBeVisible()
  await escanearA11y(page, 'home coordinador')
})
