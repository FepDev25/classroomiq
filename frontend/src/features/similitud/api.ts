import { api } from '@/api/client'
import { toApiError } from '@/api/errors'
import type { components } from '@/api/schema'

export type ReporteSimilitud = components['schemas']['ReporteSimilitudResponse']
export type ParSimilitud = components['schemas']['ParSimilitudResponse']
export type FragmentoSimilar = components['schemas']['FragmentoSimilarResponse']
export type EntregaResumenSimilitud = components['schemas']['EntregaResumenSimilitud']

export async function getReporteSimilitud(loteId: string): Promise<ReporteSimilitud> {
  const { data, error } = await api.GET('/api/lotes/{id}/similitud', {
    params: { path: { id: loteId } },
  })
  if (error) throw toApiError(error)
  return data
}

export async function generarSimilitud(loteId: string, umbral?: number): Promise<ReporteSimilitud> {
  const { data, error } = await api.POST('/api/lotes/{id}/similitud', {
    params: { path: { id: loteId } },
    body: umbral != null ? { umbral } : {},
  })
  if (error) throw toApiError(error)
  return data
}
