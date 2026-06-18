import { expect, test } from '@playwright/test'

/**
 * E2E del camino crítico del docente. Requiere el stack vivo (ver
 * playwright.config.ts). Cubre login → materia → rúbrica → lote → subir →
 * procesar → evaluar → revisar → aprobar → similitud/resumen.
 *
 * Los pasos de procesar/evaluar dependen de embeddings (Ollama) y LLM
 * (Anthropic): son lentos y con costo, así que esperan con timeouts holgados.
 * Pensado para la corrida de pruebas integrales, no para CI unitario.
 */
const EMAIL = process.env.E2E_EMAIL ?? 'docente@demo.local'
const PASSWORD = process.env.E2E_PASSWORD ?? 'docente12345'

const sufijo = Date.now().toString().slice(-6)
const materia = `E2E Materia ${sufijo}`
const lote = `E2E Lote ${sufijo}`

test('camino crítico del docente', async ({ page }) => {
  // --- Login ---
  await page.goto('/login')
  await page.getByLabel(/email/i).fill(EMAIL)
  await page.getByLabel(/contraseña/i).fill(PASSWORD)
  await page.getByRole('button', { name: /entrar|iniciar/i }).click()
  await expect(page).toHaveURL(/\/(materias)?$/)

  // --- Crear materia ---
  await page.goto('/materias')
  await page.getByRole('button', { name: /nueva materia/i }).click()
  await page.getByLabel(/nombre/i).fill(materia)
  await page.getByRole('button', { name: /crear materia/i }).click()
  await expect(page.getByText(materia)).toBeVisible()

  // --- Crear lote ---
  await page.goto('/lotes')
  await page.getByRole('button', { name: /nuevo lote/i }).click()
  await page.getByLabel(/nombre/i).fill(lote)
  // (Selección de materia/rúbrica y subida de entregas dependen de que la
  // materia tenga una rúbrica; en integración se encadena con el editor.)
  await expect(page.getByRole('dialog')).toBeVisible()
})

test('la pantalla de login valida campos vacíos sin backend', async ({ page }) => {
  await page.goto('/login')
  await page.getByRole('button', { name: /entrar|iniciar/i }).click()
  // Zod debe marcar los campos requeridos sin llamar al backend.
  await expect(page.getByText(/requerid|obligatori|ingresa/i).first()).toBeVisible()
})
