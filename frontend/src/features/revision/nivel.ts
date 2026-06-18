import type { NivelOpcion } from './api'

/**
 * Color semántico de un nivel. Los nombres de nivel son arbitrarios por rúbrica
 * (p. ej. "Logrado"/"No logrado" o "Excelente".."Insuficiente"), así que el
 * color se deriva de la POSICIÓN del nivel dentro del criterio (peor → mejor),
 * no del nombre. Devuelve una de las 4 familias de `--nivel-*`.
 */
export function familiaNivel(indice: number, total: number): string {
  if (total <= 1) return 'bueno'
  // Fracción 0 (peor) .. 1 (mejor) repartida en 4 bandas.
  const fraccion = indice / (total - 1)
  if (fraccion >= 0.75) return 'excelente'
  if (fraccion >= 0.5) return 'bueno'
  if (fraccion >= 0.25) return 'basico'
  return 'insuficiente'
}

/** Niveles ordenados de peor a mejor por su `orden`. */
export function nivelesOrdenados(niveles: NivelOpcion[]): NivelOpcion[] {
  return [...niveles].sort((a, b) => (a.orden ?? 0) - (b.orden ?? 0))
}

/** Acota un puntaje al rango [min, max]. Espeja el clamp del backend. */
export function acotar(valor: number, min: number, max: number): number {
  return Math.min(Math.max(valor, min), max)
}
