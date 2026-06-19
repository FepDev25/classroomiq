import { api } from '@/api/client'
import { toApiError } from '@/api/errors'
import type { components } from '@/api/schema'

export type Borrador = components['schemas']['BorradorResponse']
export type CriterioEvaluado = components['schemas']['CriterioEvaluadoResponse']
export type NivelOpcion = components['schemas']['NivelOpcionResponse']
export type Cita = components['schemas']['CitaResponse']
export type CriterioRevisionRequest = components['schemas']['CriterioRevisionRequest']
export type EstadoEvaluacion = components['schemas']['EstadoEvaluacion']
export type ContenidoEntrega = components['schemas']['ContenidoEntregaResponse']
export type ArchivoContenido = components['schemas']['ArchivoContenidoResponse']
export type SeccionContenido = components['schemas']['SeccionContenidoResponse']

export async function getBorrador(entregaId: string): Promise<Borrador> {
  const { data, error } = await api.GET('/api/entregas/{entregaId}/evaluacion', {
    params: { path: { entregaId } },
  })
  if (error) throw toApiError(error)
  return data
}

export async function getContenido(entregaId: string): Promise<ContenidoEntrega> {
  const { data, error } = await api.GET('/api/entregas/{id}/contenido', {
    params: { path: { id: entregaId } },
  })
  if (error) throw toApiError(error)
  return data
}

export async function patchCriterio(
  evaluacionId: string,
  criterioId: string,
  body: CriterioRevisionRequest,
): Promise<Borrador> {
  const { data, error } = await api.PATCH(
    '/api/evaluaciones/{evaluacionId}/criterios/{criterioId}',
    {
      params: { path: { evaluacionId, criterioId } },
      body,
    },
  )
  if (error) throw toApiError(error)
  return data
}

export async function patchComentario(
  evaluacionId: string,
  comentarioGeneral: string | null,
): Promise<Borrador> {
  const { data, error } = await api.PATCH('/api/evaluaciones/{evaluacionId}/comentario', {
    params: { path: { evaluacionId } },
    body: { comentarioGeneral },
  })
  if (error) throw toApiError(error)
  return data
}

export async function aprobarEvaluacion(evaluacionId: string): Promise<Borrador> {
  const { data, error } = await api.POST('/api/evaluaciones/{evaluacionId}/aprobar', {
    params: { path: { evaluacionId } },
  })
  if (error) throw toApiError(error)
  return data
}
