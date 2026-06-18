import { api } from '@/api/client'
import { toApiError } from '@/api/errors'
import type { components } from '@/api/schema'

export type ResumenGrupo = components['schemas']['ResumenGrupoResponse']
export type EstadisticasNotas = components['schemas']['EstadisticasNotas']
export type RangoNota = components['schemas']['RangoNota']
export type CriterioResumen = components['schemas']['CriterioResumen']
export type NivelConteo = components['schemas']['NivelConteo']

export async function getResumen(loteId: string): Promise<ResumenGrupo> {
  const { data, error } = await api.GET('/api/lotes/{id}/resumen', {
    params: { path: { id: loteId } },
  })
  if (error) throw toApiError(error)
  return data
}

export async function generarNarrativa(loteId: string): Promise<ResumenGrupo> {
  const { data, error } = await api.POST('/api/lotes/{id}/resumen/narrativa', {
    params: { path: { id: loteId } },
  })
  if (error) throw toApiError(error)
  return data
}
