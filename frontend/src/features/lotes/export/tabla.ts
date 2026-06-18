import type { EvaluacionAprobada } from './recolectar'

/** Celda de la tabla de export: texto o número (los vacíos se representan como ''). */
export type Celda = string | number

export interface TablaExport {
  headers: string[]
  filas: Celda[][]
}

/**
 * Construye la tabla de export (una fila por evaluación aprobada). Las columnas
 * de criterio se derivan de la rúbrica —vía los borradores, por `criterioId` y
 * ordenadas por `orden`— para que todas las filas queden alineadas aunque algún
 * borrador no tenga exactamente los mismos criterios. Cada criterio aporta dos
 * columnas: nivel final y puntaje final.
 */
export function construirTabla(aprobadas: EvaluacionAprobada[], totalMax: number | null): TablaExport {
  const criterios = new Map<string, { nombre: string; max: number; orden: number }>()
  for (const { borrador } of aprobadas) {
    for (const c of borrador.criterios ?? []) {
      if (c.criterioId && !criterios.has(c.criterioId)) {
        criterios.set(c.criterioId, {
          nombre: c.nombreCriterio ?? '',
          max: c.puntajeMaximo ?? 0,
          orden: c.orden ?? 0,
        })
      }
    }
  }
  const cols = [...criterios.entries()].sort((a, b) => a[1].orden - b[1].orden)

  const headers = ['Estudiante']
  for (const [, c] of cols) {
    headers.push(`${c.nombre} — Nivel`)
    headers.push(`${c.nombre} — Puntaje (/${c.max})`)
  }
  headers.push(totalMax != null ? `Total (/${totalMax})` : 'Total')
  headers.push('Comentario general')

  const filas: Celda[][] = aprobadas.map(({ entrega, borrador }) => {
    const porId = new Map((borrador.criterios ?? []).map((c) => [c.criterioId, c]))
    const fila: Celda[] = [entrega.identificadorEstudiante ?? '—']
    for (const [id] of cols) {
      const c = porId.get(id)
      const nivel = c?.niveles?.find((n) => n.id === c.nivelFinalId)?.nombre ?? ''
      fila.push(nivel)
      fila.push(c?.puntajeFinal ?? '')
    }
    fila.push(borrador.puntajeTotalFinal ?? '')
    fila.push(borrador.comentarioGeneral ?? '')
    return fila
  })

  return { headers, filas }
}

/** Serializa la tabla a CSV (RFC 4180) con BOM para que Excel detecte UTF-8. */
export function tablaACsv(tabla: TablaExport): string {
  const esc = (v: Celda) => {
    const s = String(v ?? '')
    return /[",\n\r]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s
  }
  const lineas = [tabla.headers, ...tabla.filas].map((fila) => fila.map(esc).join(','))
  return '\ufeff' + lineas.join('\r\n')
}
