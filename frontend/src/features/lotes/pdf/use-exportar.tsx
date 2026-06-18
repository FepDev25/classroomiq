import { useState } from 'react'
import { toast } from 'sonner'

import { descargar, slug } from '../export/descargar'
import { recolectarAprobadas } from '../export/recolectar'
import type { DatosPdf, EvaluacionPdf } from './evaluaciones-pdf'

/**
 * Exporta a PDF las evaluaciones APROBADAS de un lote, generándolo en cliente
 * (no hay endpoint de backend — ver roadmap). Recolecta las entregas LISTO,
 * pide su borrador, filtra las aprobadas y arma el documento.
 */
export function useExportarPdf(loteId: string) {
  const [exportando, setExportando] = useState(false)

  async function exportar(loteNombre: string) {
    setExportando(true)
    try {
      const { aprobadas, totalMax } = await recolectarAprobadas(loteId)

      if (aprobadas.length === 0) {
        toast.error('No hay evaluaciones aprobadas para exportar.')
        return
      }

      const evaluaciones: EvaluacionPdf[] = aprobadas.map(({ entrega, borrador }) => ({
        identificador: entrega.identificadorEstudiante ?? '—',
        total: borrador.puntajeTotalFinal ?? null,
        comentario: borrador.comentarioGeneral ?? null,
        criterios: (borrador.criterios ?? []).map((c) => ({
          nombre: c.nombreCriterio ?? '',
          nivel: (c.niveles ?? []).find((n) => n.id === c.nivelFinalId)?.nombre ?? null,
          puntaje: c.puntajeFinal ?? null,
          max: c.puntajeMaximo ?? 0,
          justificacion: c.justificacionEditada ?? c.justificacion ?? null,
        })),
      }))

      const datos: DatosPdf = {
        loteNombre,
        totalMax,
        generadoAt: new Date().toLocaleString('es'),
        evaluaciones,
      }

      // Carga diferida: @react-pdf (~1 MB) solo se descarga al exportar.
      const [{ pdf }, { EvaluacionesPdf: Documento }] = await Promise.all([
        import('@react-pdf/renderer'),
        import('./evaluaciones-pdf'),
      ])
      const blob = await pdf(<Documento datos={datos} />).toBlob()
      descargar(blob, `evaluaciones-${slug(loteNombre)}.pdf`)
      toast.success('PDF exportado')
    } catch {
      toast.error('No pudimos generar el PDF.')
    } finally {
      setExportando(false)
    }
  }

  return { exportar, exportando }
}
