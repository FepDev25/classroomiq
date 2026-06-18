import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { api } from '@/api/client'
import { toApiError } from '@/api/errors'
import { queryKeys } from '@/api/queryKeys'
import type { RubricaRequest, RubricaResponse } from './form'

export async function listRubricas(materiaId: string): Promise<RubricaResponse[]> {
  const { data, error } = await api.GET('/api/materias/{materiaId}/rubricas', {
    params: { path: { materiaId } },
  })
  if (error) throw toApiError(error)
  return data
}

export async function getRubrica(id: string): Promise<RubricaResponse> {
  const { data, error } = await api.GET('/api/rubricas/{id}', { params: { path: { id } } })
  if (error) throw toApiError(error)
  return data
}

export async function crearRubrica(
  materiaId: string,
  body: RubricaRequest,
): Promise<RubricaResponse> {
  const { data, error } = await api.POST('/api/materias/{materiaId}/rubricas', {
    params: { path: { materiaId } },
    body,
  })
  if (error) throw toApiError(error)
  return data
}

export async function actualizarRubrica(
  id: string,
  body: RubricaRequest,
): Promise<RubricaResponse> {
  const { data, error } = await api.PUT('/api/rubricas/{id}', { params: { path: { id } }, body })
  if (error) throw toApiError(error)
  return data
}

export async function eliminarRubrica(id: string): Promise<void> {
  const { error } = await api.DELETE('/api/rubricas/{id}', { params: { path: { id } } })
  if (error) throw toApiError(error)
}

// --- Hooks ---

export function useRubricas(materiaId: string) {
  return useQuery({
    queryKey: queryKeys.rubricas(materiaId),
    queryFn: () => listRubricas(materiaId),
  })
}

export function useRubrica(id: string) {
  return useQuery({ queryKey: queryKeys.rubrica(id), queryFn: () => getRubrica(id) })
}

export function useCrearRubrica(materiaId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: RubricaRequest) => crearRubrica(materiaId, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.rubricas(materiaId) }),
  })
}

export function useActualizarRubrica(id: string, materiaId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: RubricaRequest) => actualizarRubrica(id, body),
    onSuccess: (rubrica) => {
      qc.invalidateQueries({ queryKey: queryKeys.rubricas(materiaId) })
      qc.setQueryData(queryKeys.rubrica(id), rubrica)
    },
  })
}

export function useEliminarRubrica(materiaId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => eliminarRubrica(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.rubricas(materiaId) }),
  })
}
