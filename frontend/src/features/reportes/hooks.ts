import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { queryKeys } from '@/api/queryKeys'
import { ApiError } from '@/api/errors'
import { generarNarrativa, getResumen } from './api'

export function useResumen(loteId: string) {
  return useQuery({
    queryKey: queryKeys.resumen(loteId),
    queryFn: () => getResumen(loteId),
    // 422 = lote incompleto: no reintentar, lo muestra la vista como aviso.
    retry: (count, error) => !(error instanceof ApiError && error.status === 422) && count < 2,
    staleTime: Infinity,
  })
}

export function useGenerarNarrativa(loteId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => generarNarrativa(loteId),
    onSuccess: (resumen) => qc.setQueryData(queryKeys.resumen(loteId), resumen),
  })
}
