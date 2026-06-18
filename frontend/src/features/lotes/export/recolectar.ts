import { getBorrador, type Borrador } from '@/features/revision/api'
import { getRubrica } from '@/features/rubricas/api'
import { listEntregas, type Entrega } from '../api'

export interface EvaluacionAprobada {
  entrega: Entrega
  borrador: Borrador
}

export interface AprobadasResult {
  /** Evaluaciones APROBADAS del lote, ordenadas por identificador del estudiante. */
  aprobadas: EvaluacionAprobada[]
  /** Puntaje total de la rúbrica (para encabezados), o `null` si no se pudo resolver. */
  totalMax: number | null
}

/**
 * Recolecta las evaluaciones APROBADAS de un lote: entregas LISTO cuyo borrador
 * existe y está aprobado. Compartido por los exportadores (PDF y Excel/CSV) para
 * no duplicar la lógica de recolección. El backend no expone un endpoint de
 * export, así que se compone en cliente.
 */
export async function recolectarAprobadas(loteId: string): Promise<AprobadasResult> {
  const entregas = await listEntregas(loteId)
  const listas = entregas.filter((e) => e.estado === 'LISTO' && e.id)

  const resultados = await Promise.all(
    listas.map(async (entrega) => {
      try {
        return { entrega, borrador: await getBorrador(entrega.id!) }
      } catch {
        // Entrega indexada pero sin evaluación (404): se ignora.
        return null
      }
    }),
  )

  const aprobadas = resultados
    .filter((r): r is EvaluacionAprobada => r != null && r.borrador.estado === 'APROBADA')
    .sort((a, b) =>
      (a.entrega.identificadorEstudiante ?? '').localeCompare(
        b.entrega.identificadorEstudiante ?? '',
      ),
    )

  let totalMax: number | null = null
  const rubricaId = aprobadas[0]?.borrador.rubricaId
  if (rubricaId) {
    try {
      totalMax = (await getRubrica(rubricaId)).puntajeTotal ?? null
    } catch {
      totalMax = null
    }
  }

  return { aprobadas, totalMax }
}
