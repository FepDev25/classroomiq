import type { Borrador } from './api'

/**
 * Total proyectado a partir de los puntajes actuales de los criterios, espejando
 * la lógica del backend: `SUMA` suma los puntajes, `PROMEDIO` los promedia. Usa
 * `puntajeFinal` si el docente lo ajustó, si no el sugerido por el asistente.
 */
export function proyectarTotal(borrador: Borrador, modoTotal: string | undefined): number {
  const puntajes = (borrador.criterios ?? []).map((c) => c.puntajeFinal ?? c.puntajeSugerido ?? 0)
  if (puntajes.length === 0) return 0
  const suma = puntajes.reduce((a, b) => a + b, 0)
  return modoTotal === 'PROMEDIO' ? suma / puntajes.length : suma
}
