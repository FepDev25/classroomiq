/**
 * Resaltado de citas dentro del texto de una sección. El motor cita fragmentos
 * verbatim (`textoCitado`), pero el documento se reconstruye por re-extracción,
 * así que los saltos de línea y espacios pueden diferir. Por eso el emparejado es
 * por subcadena con espacios normalizados (y sin distinción de mayúsculas), y se
 * mapea la posición normalizada de vuelta al texto original para envolver el
 * tramo exacto en un `<mark>`.
 */

import { evidenciaDomId } from './evidencia'

/** Una cita a buscar dentro de una sección. */
export interface CitaResaltable {
  /** id de DOM destino (de `evidenciaDomId`), para el scroll desde los chips. */
  domId: string
  textoCitado: string
}

/** Un tramo del texto: plano si `domId` es undefined, resaltado si está presente. */
export interface TramoTexto {
  texto: string
  domId?: string
}

interface Rango {
  inicio: number
  fin: number
  domId: string
}

/** Normaliza colapsando espacios y bajando a minúsculas, guardando el mapa de índices. */
function normalizarConMapa(texto: string): { norm: string; mapa: number[] } {
  let norm = ''
  const mapa: number[] = []
  let prevEspacio = false
  for (let i = 0; i < texto.length; i++) {
    const c = texto[i]
    if (/\s/.test(c)) {
      if (!prevEspacio && norm.length > 0) {
        norm += ' '
        mapa.push(i)
        prevEspacio = true
      }
    } else {
      norm += c.toLowerCase()
      mapa.push(i)
      prevEspacio = false
    }
  }
  return { norm, mapa }
}

function normalizarConsulta(texto: string): string {
  return texto.replace(/\s+/g, ' ').trim().toLowerCase()
}

/**
 * Parte el texto de una sección en tramos planos y resaltados, según las citas
 * que aparezcan en él. Cada cita se busca una vez (primera ocurrencia); los
 * rangos solapados se descartan dejando el primero. El orden de las citas define
 * la prioridad.
 */
export function resaltar(texto: string, citas: CitaResaltable[]): TramoTexto[] {
  if (!texto) return []
  const { norm, mapa } = normalizarConMapa(texto)

  const rangos: Rango[] = []
  for (const cita of citas) {
    const aguja = normalizarConsulta(cita.textoCitado ?? '')
    if (aguja.length < 3) continue // ruido: citas vacías o triviales
    const posNorm = norm.indexOf(aguja)
    if (posNorm < 0) continue
    const inicio = mapa[posNorm]
    const fin = mapa[posNorm + aguja.length - 1] + 1
    rangos.push({ inicio, fin, domId: cita.domId })
  }

  // Orden por inicio; descartar solapes (el primero gana).
  rangos.sort((a, b) => a.inicio - b.inicio)
  const limpios: Rango[] = []
  let ultimoFin = -1
  for (const r of rangos) {
    if (r.inicio >= ultimoFin) {
      limpios.push(r)
      ultimoFin = r.fin
    }
  }

  if (limpios.length === 0) return [{ texto }]

  const tramos: TramoTexto[] = []
  let cursor = 0
  for (const r of limpios) {
    if (r.inicio > cursor) tramos.push({ texto: texto.slice(cursor, r.inicio) })
    tramos.push({ texto: texto.slice(r.inicio, r.fin), domId: r.domId })
    cursor = r.fin
  }
  if (cursor < texto.length) tramos.push({ texto: texto.slice(cursor) })
  return tramos
}

/** Aplana las citas de los criterios a `CitaResaltable[]` con su id de DOM. */
export function citasResaltables(
  criterios: { id?: string; citas?: { textoCitado?: string }[] }[],
): CitaResaltable[] {
  const out: CitaResaltable[] = []
  for (const criterio of criterios) {
    if (!criterio.id) continue
    ;(criterio.citas ?? []).forEach((cita, i) => {
      if (cita.textoCitado) {
        out.push({ domId: evidenciaDomId(criterio.id!, i), textoCitado: cita.textoCitado })
      }
    })
  }
  return out
}
