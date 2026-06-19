import { expect, test, type Page } from '@playwright/test'

import { loginComo } from './a11y'

/**
 * Pase responsive (tablet, 768×1024) sobre las vistas NUEVAS de la v2 multi-rol.
 * Verifica que no haya desbordamiento horizontal (scroll lateral) y que la
 * navegación del rol siga visible. Requiere el stack vivo y las cuentas del seed.
 */
const TABLET = { width: 768, height: 1024 }

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

/** Falla si el documento desborda horizontalmente el viewport (tolerancia 1px). */
async function sinScrollHorizontal(page: Page, etiqueta: string) {
  const overflow = await page.evaluate(() => {
    const el = document.documentElement
    return { scrollWidth: el.scrollWidth, clientWidth: el.clientWidth }
  })
  expect(
    overflow.scrollWidth,
    `"${etiqueta}" desborda horizontalmente: scrollWidth=${overflow.scrollWidth} > clientWidth=${overflow.clientWidth}`,
  ).toBeLessThanOrEqual(overflow.clientWidth + 1)
}

test.use({ viewport: TABLET })

test('responsive — home del docente', async ({ page }) => {
  await loginComo(page, DOCENTE.email, DOCENTE.password)
  await expect(page.getByRole('heading', { name: /hola de nuevo/i })).toBeVisible()
  await sinScrollHorizontal(page, 'home docente')
})

test('responsive — admin: cuentas', async ({ page }) => {
  await loginComo(page, ADMIN.email, ADMIN.password)
  await expect(page).toHaveURL(/\/admin\/cuentas/)
  await expect(page.getByRole('heading', { level: 1, name: /cuentas/i })).toBeVisible()
  await expect(page.getByRole('navigation')).toBeVisible()
  await sinScrollHorizontal(page, 'admin cuentas')
})

test('responsive — admin: uso y costo', async ({ page }) => {
  await loginComo(page, ADMIN.email, ADMIN.password)
  await page.getByRole('link', { name: /uso y costo/i }).click()
  await expect(page.getByRole('heading', { level: 1, name: /uso y costo/i })).toBeVisible()
  await sinScrollHorizontal(page, 'admin uso y costo')
})

test('responsive — admin: coordinadores', async ({ page }) => {
  await loginComo(page, ADMIN.email, ADMIN.password)
  await page.getByRole('link', { name: /coordinadores/i }).click()
  await expect(page.getByRole('heading', { level: 1, name: /coordinadores/i })).toBeVisible()
  await sinScrollHorizontal(page, 'admin coordinadores')
})

test('responsive — home del coordinador', async ({ page }) => {
  await loginComo(page, COORD.email, COORD.password)
  await expect(page).toHaveURL(/\/coordinador/)
  await expect(page.getByRole('heading', { level: 1, name: /materias asignadas/i })).toBeVisible()
  await sinScrollHorizontal(page, 'home coordinador')
})
