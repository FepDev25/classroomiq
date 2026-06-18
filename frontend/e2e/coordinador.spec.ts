import { expect, test } from '@playwright/test'

/**
 * E2E de la vista de coordinación (solo lectura). Requiere el stack vivo y la
 * cuenta coordinador sembrada. Verifica el acceso a las materias asignadas y que
 * NO existe navegación a trabajos, evaluaciones individuales ni similitud.
 */
const EMAIL = process.env.E2E_COORD_EMAIL ?? 'coordinador@demo.local'
const PASSWORD = process.env.E2E_COORD_PASSWORD ?? 'coord12345'

test('el coordinador ve sus materias asignadas en modo lectura', async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel(/email/i).fill(EMAIL)
  await page.getByLabel(/contraseña/i).fill(PASSWORD)
  await page.getByRole('button', { name: /entrar|iniciar/i }).click()

  // El dispatcher por rol lleva al coordinador a /coordinador.
  await expect(page).toHaveURL(/\/coordinador/)
  await expect(page.getByRole('heading', { name: /materias asignadas/i })).toBeVisible()

  // La vista de coordinación no ofrece navegación de docente.
  await expect(page.getByRole('link', { name: /^materias$/i })).toHaveCount(0)
  await expect(page.getByRole('link', { name: /^lotes$/i })).toHaveCount(0)
})

test('el coordinador no puede entrar a rutas de docente (guard por rol)', async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel(/email/i).fill(EMAIL)
  await page.getByLabel(/contraseña/i).fill(PASSWORD)
  await page.getByRole('button', { name: /entrar|iniciar/i }).click()
  await expect(page).toHaveURL(/\/coordinador/)

  // Forzar una ruta de docente: el guard debe rebotar fuera de /materias.
  await page.goto('/materias')
  await expect(page).not.toHaveURL(/\/materias$/)
})
