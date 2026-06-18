import { api } from '@/api/client'
import { toApiError } from '@/api/errors'
import type { components } from '@/api/schema'

export type Materia = components['schemas']['MateriaResponse']
export type Lote = components['schemas']['LoteResponse']
export type ResumenGrupo = components['schemas']['ResumenGrupoResponse']

/**
 * Cliente de la vista de coordinación: solo lectura de reportes agregados de las
 * materias asignadas por el admin. El backend nunca sirve a este rol evaluaciones
 * individuales, trabajos ni similitud.
 */

export async function listMaterias(): Promise<Materia[]> {
  const { data, error } = await api.GET('/api/coordinador/materias')
  if (error) throw toApiError(error)
  return data
}

export async function listLotes(materiaId: string): Promise<Lote[]> {
  const { data, error } = await api.GET('/api/coordinador/materias/{materiaId}/lotes', {
    params: { path: { materiaId } },
  })
  if (error) throw toApiError(error)
  return data
}

export async function getResumen(loteId: string): Promise<ResumenGrupo> {
  const { data, error } = await api.GET('/api/coordinador/lotes/{loteId}/resumen', {
    params: { path: { loteId } },
  })
  if (error) throw toApiError(error)
  return data
}
