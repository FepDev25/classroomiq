import { api } from '@/api/client'
import { toApiError } from '@/api/errors'
import type { components } from '@/api/schema'

export type Lote = components['schemas']['LoteResponse']
export type LoteRequest = components['schemas']['LoteRequest']
export type Entrega = components['schemas']['EntregaResponse']
export type TipoEntrega = components['schemas']['TipoEntrega']
export type EstadoEntrega = components['schemas']['EstadoEntrega']
export type EstadoLote = components['schemas']['EstadoLote']

/** Estados de entrega en los que el procesamiento/evaluación ya no avanza solo. */
export const ESTADOS_TERMINALES: ReadonlySet<EstadoEntrega> = new Set<EstadoEntrega>([
  'LISTO',
  'ERROR',
])

export async function listLotes(): Promise<Lote[]> {
  const { data, error } = await api.GET('/api/lotes')
  if (error) throw toApiError(error)
  return data
}

export async function getLote(id: string): Promise<Lote> {
  const { data, error } = await api.GET('/api/lotes/{id}', { params: { path: { id } } })
  if (error) throw toApiError(error)
  return data
}

export async function crearLote(body: LoteRequest): Promise<Lote> {
  const { data, error } = await api.POST('/api/lotes', { body })
  if (error) throw toApiError(error)
  return data
}

export async function eliminarLote(id: string): Promise<void> {
  const { error } = await api.DELETE('/api/lotes/{id}', { params: { path: { id } } })
  if (error) throw toApiError(error)
}

export async function getEntrega(id: string): Promise<Entrega> {
  const { data, error } = await api.GET('/api/entregas/{id}', { params: { path: { id } } })
  if (error) throw toApiError(error)
  return data
}

export async function listEntregas(loteId: string): Promise<Entrega[]> {
  const { data, error } = await api.GET('/api/lotes/{loteId}/entregas', {
    params: { path: { loteId } },
  })
  if (error) throw toApiError(error)
  return data
}

export interface SubirEntregaInput {
  identificadorEstudiante: string
  tipo: TipoEntrega
  archivos: File[]
}

export async function subirEntrega(loteId: string, input: SubirEntregaInput): Promise<Entrega> {
  const { data, error } = await api.POST('/api/lotes/{loteId}/entregas', {
    params: { path: { loteId } },
    body: {
      identificadorEstudiante: input.identificadorEstudiante,
      tipo: input.tipo,
      // openapi-typescript tipa `archivos` como string[] (binary); en runtime van File[].
      // El bodySerializer arma el FormData real, así que el cast queda acotado aquí.
      archivos: input.archivos as unknown as string[],
    },
    bodySerializer(body) {
      const fd = new FormData()
      fd.append('identificadorEstudiante', body.identificadorEstudiante)
      fd.append('tipo', body.tipo)
      for (const archivo of input.archivos) fd.append('archivos', archivo)
      return fd
    },
  })
  if (error) throw toApiError(error)
  return data
}

export async function eliminarEntrega(id: string): Promise<void> {
  const { error } = await api.DELETE('/api/entregas/{id}', { params: { path: { id } } })
  if (error) throw toApiError(error)
}

export async function procesarLote(id: string): Promise<number> {
  const { data, error } = await api.POST('/api/lotes/{id}/procesar', { params: { path: { id } } })
  if (error) throw toApiError(error)
  return data.entregasEncoladas ?? 0
}

export async function evaluarLote(id: string): Promise<number> {
  const { data, error } = await api.POST('/api/lotes/{id}/evaluar', { params: { path: { id } } })
  if (error) throw toApiError(error)
  return data.entregasEncoladas ?? 0
}
