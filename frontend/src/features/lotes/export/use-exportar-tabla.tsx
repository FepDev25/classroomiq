import { useState } from 'react'
import { toast } from 'sonner'

import { descargar, slug } from './descargar'
import { recolectarAprobadas } from './recolectar'
import { construirTabla, tablaACsv } from './tabla'

export type FormatoTabla = 'xlsx' | 'csv'

const XLSX_MIME = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'

/**
 * Exporta las evaluaciones APROBADAS de un lote como hoja de cálculo (una fila
 * por estudiante, columnas por criterio). Todo en cliente, igual que el PDF.
 * SheetJS (`xlsx`) se carga de forma diferida y solo para `.xlsx`; el CSV se
 * arma sin dependencias.
 */
export function useExportarTabla(loteId: string) {
  const [exportando, setExportando] = useState<FormatoTabla | null>(null)

  async function exportar(loteNombre: string, formato: FormatoTabla) {
    setExportando(formato)
    try {
      const { aprobadas, totalMax } = await recolectarAprobadas(loteId)
      if (aprobadas.length === 0) {
        toast.error('No hay evaluaciones aprobadas para exportar.')
        return
      }

      const tabla = construirTabla(aprobadas, totalMax)
      const base = `evaluaciones-${slug(loteNombre)}`

      if (formato === 'csv') {
        descargar(new Blob([tablaACsv(tabla)], { type: 'text/csv;charset=utf-8' }), `${base}.csv`)
        toast.success('CSV exportado')
        return
      }

      const XLSX = await import('xlsx')
      const hoja = XLSX.utils.aoa_to_sheet([tabla.headers, ...tabla.filas])
      const libro = XLSX.utils.book_new()
      XLSX.utils.book_append_sheet(libro, hoja, 'Evaluaciones')
      const buffer = XLSX.write(libro, { bookType: 'xlsx', type: 'array' }) as ArrayBuffer
      descargar(new Blob([buffer], { type: XLSX_MIME }), `${base}.xlsx`)
      toast.success('Excel exportado')
    } catch {
      toast.error('No pudimos generar el archivo.')
    } finally {
      setExportando(null)
    }
  }

  return { exportar, exportando }
}
