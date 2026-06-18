import { useQuery } from '@tanstack/react-query'

import { queryKeys } from '@/api/queryKeys'
import { ApiError } from '@/api/errors'
import { getResumen, listLotes, listMaterias } from './api'

export function useMaterias() {
  return useQuery({ queryKey: queryKeys.coordinadorMateriasList, queryFn: listMaterias })
}

export function useLotes(materiaId: string) {
  return useQuery({
    queryKey: queryKeys.coordinadorLotes(materiaId),
    queryFn: () => listLotes(materiaId),
    enabled: Boolean(materiaId),
  })
}

export function useResumen(loteId: string) {
  return useQuery({
    queryKey: queryKeys.coordinadorResumen(loteId),
    queryFn: () => getResumen(loteId),
    // 422 = lote incompleto: no reintentar, lo muestra la vista como aviso.
    retry: (count, error) => !(error instanceof ApiError && error.status === 422) && count < 2,
    staleTime: Infinity,
  })
}
