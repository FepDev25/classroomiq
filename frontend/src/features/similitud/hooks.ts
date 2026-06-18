import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { queryKeys } from '@/api/queryKeys'
import { ApiError } from '@/api/errors'
import { generarSimilitud, getReporteSimilitud } from './api'

export function useReporteSimilitud(loteId: string) {
  return useQuery({
    queryKey: queryKeys.similitud(loteId),
    queryFn: () => getReporteSimilitud(loteId),
    // El reporte aún no existe (404) no es un error que valga la pena reintentar.
    retry: (count, error) => !(error instanceof ApiError && error.status === 404) && count < 2,
    staleTime: Infinity,
  })
}

export function useGenerarSimilitud(loteId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (umbral?: number) => generarSimilitud(loteId, umbral),
    onSuccess: (reporte) => qc.setQueryData(queryKeys.similitud(loteId), reporte),
  })
}
