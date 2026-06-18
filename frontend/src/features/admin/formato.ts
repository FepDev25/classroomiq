import type { components } from '@/api/schema'

type OperacionLlm = components['schemas']['OperacionLlm']

/** Etiqueta legible por operación del motor LLM. */
export const OPERACION_LABEL: Record<OperacionLlm, string> = {
  EVALUACION: 'Evaluación',
  NARRATIVA: 'Narrativa',
}

/** Formatea un costo según la moneda del backend; cae a número plano si falla. */
export function formatoMoneda(valor: number | undefined, moneda: string | undefined): string {
  const n = valor ?? 0
  if (moneda) {
    try {
      return new Intl.NumberFormat('es', {
        style: 'currency',
        currency: moneda,
        maximumFractionDigits: 4,
      }).format(n)
    } catch {
      // Código de moneda no estándar: cae al formato plano.
    }
  }
  return `${new Intl.NumberFormat('es', { maximumFractionDigits: 4 }).format(n)} ${moneda ?? ''}`.trim()
}

/** Formatea un conteo de tokens con separadores de miles. */
export function formatoTokens(valor: number | undefined): string {
  return new Intl.NumberFormat('es').format(valor ?? 0)
}

/** Mes actual en formato `YYYY-MM` (UTC, igual que el default del backend). */
export function mesActual(): string {
  const ahora = new Date()
  const y = ahora.getUTCFullYear()
  const m = String(ahora.getUTCMonth() + 1).padStart(2, '0')
  return `${y}-${m}`
}

/** Los últimos `n` meses en formato `YYYY-MM`, del más reciente al más antiguo. */
export function ultimosMeses(n: number): string[] {
  const meses: string[] = []
  const base = new Date()
  base.setUTCDate(1)
  for (let i = 0; i < n; i++) {
    const y = base.getUTCFullYear()
    const m = String(base.getUTCMonth() + 1).padStart(2, '0')
    meses.push(`${y}-${m}`)
    base.setUTCMonth(base.getUTCMonth() - 1)
  }
  return meses
}

const NOMBRE_MES = [
  'enero',
  'febrero',
  'marzo',
  'abril',
  'mayo',
  'junio',
  'julio',
  'agosto',
  'septiembre',
  'octubre',
  'noviembre',
  'diciembre',
]

/** Etiqueta legible de un mes `YYYY-MM` → "junio 2026". */
export function etiquetaMes(mes: string): string {
  const [y, m] = mes.split('-')
  const idx = Number(m) - 1
  if (!y || Number.isNaN(idx) || idx < 0 || idx > 11) return mes
  return `${NOMBRE_MES[idx]} ${y}`
}
