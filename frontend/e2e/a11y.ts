import AxeBuilder from '@axe-core/playwright'
import { expect, type Page } from '@playwright/test'

/**
 * Helpers de accesibilidad para los e2e. Ejecutan axe-core sobre la página ya
 * renderizada y fallan ante violaciones de impacto `critical` o `serious`
 * (el DoD de H6 exige "sin violaciones a11y críticas en las vistas nuevas").
 * Las de impacto `moderate`/`minor` se imprimen como aviso, no bloquean.
 */
const IMPACTO_BLOQUEANTE = new Set(['critical', 'serious'])

export async function escanearA11y(page: Page, etiqueta: string) {
  const resultados = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze()

  const bloqueantes = resultados.violations.filter((v) => IMPACTO_BLOQUEANTE.has(v.impact ?? ''))
  const leves = resultados.violations.filter((v) => !IMPACTO_BLOQUEANTE.has(v.impact ?? ''))

  if (leves.length > 0) {
    const resumen = leves.map((v) => `  · [${v.impact}] ${v.id}: ${v.help}`).join('\n')
    console.warn(`[a11y] ${etiqueta} — violaciones no bloqueantes:\n${resumen}`)
  }

  const detalle = bloqueantes
    .map((v) => {
      const nodos = v.nodes.map((n) => `      ${n.target.join(' ')}`).join('\n')
      return `  ✕ [${v.impact}] ${v.id}: ${v.help}\n    ${v.helpUrl}\n${nodos}`
    })
    .join('\n')

  expect(bloqueantes, `Violaciones a11y críticas/serias en "${etiqueta}":\n${detalle}`).toEqual([])
}

export async function loginComo(page: Page, email: string, password: string) {
  await page.goto('/login')
  await page.getByLabel(/email/i).fill(email)
  await page.getByLabel(/contraseña/i).fill(password)
  await page.getByRole('button', { name: /entrar|iniciar/i }).click()
}
