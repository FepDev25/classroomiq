import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { queryKeys } from '@/api/queryKeys'
import {
  crearLote,
  eliminarEntrega,
  eliminarLote,
  evaluarLote,
  getEntrega,
  getLote,
  listEntregas,
  listLotes,
  procesarLote,
  subirEntrega,
  type LoteRequest,
  type SubirEntregaInput,
} from './api'

export function useLotes() {
  return useQuery({ queryKey: queryKeys.lotes, queryFn: listLotes })
}

export function useLote(id: string) {
  return useQuery({ queryKey: queryKeys.lote(id), queryFn: () => getLote(id) })
}

export function useEntregas(loteId: string) {
  return useQuery({ queryKey: queryKeys.entregas(loteId), queryFn: () => listEntregas(loteId) })
}

export function useEntrega(id: string) {
  return useQuery({ queryKey: queryKeys.entrega(id), queryFn: () => getEntrega(id) })
}

export function useCrearLote() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: LoteRequest) => crearLote(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.lotes }),
  })
}

export function useEliminarLote() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => eliminarLote(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.lotes }),
  })
}

export function useSubirEntrega(loteId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (input: SubirEntregaInput) => subirEntrega(loteId, input),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.entregas(loteId) }),
  })
}

export function useEliminarEntrega(loteId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => eliminarEntrega(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.entregas(loteId) }),
  })
}

export function useProcesarLote(loteId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => procesarLote(loteId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.entregas(loteId) })
      qc.invalidateQueries({ queryKey: queryKeys.lote(loteId) })
      qc.invalidateQueries({ queryKey: queryKeys.lotes })
    },
  })
}

export function useEvaluarLote(loteId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => evaluarLote(loteId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.entregas(loteId) })
      qc.invalidateQueries({ queryKey: queryKeys.lote(loteId) })
    },
  })
}
