import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { queryKeys } from '@/api/queryKeys'
import {
  aprobarEvaluacion,
  getBorrador,
  patchComentario,
  patchCriterio,
  type Borrador,
  type CriterioRevisionRequest,
} from './api'

export function useBorrador(entregaId: string) {
  return useQuery({
    queryKey: queryKeys.evaluacion(entregaId),
    queryFn: () => getBorrador(entregaId),
    // El borrador no cambia solo; lo invalidamos nosotros tras cada edición.
    staleTime: Infinity,
  })
}

interface PatchCriterioVars {
  criterioId: string
  body: CriterioRevisionRequest
}

/**
 * Edita un criterio con actualización optimista: aplica el cambio al cache de
 * inmediato y hace rollback si el backend lo rechaza (rango inválido → 422,
 * evaluación aprobada → 409). La respuesta del PATCH (borrador completo con el
 * total recalculado) es la verdad final.
 */
export function usePatchCriterio(entregaId: string, evaluacionId: string) {
  const qc = useQueryClient()
  const key = queryKeys.evaluacion(entregaId)
  return useMutation({
    mutationFn: ({ criterioId, body }: PatchCriterioVars) =>
      patchCriterio(evaluacionId, criterioId, body),
    async onMutate({ criterioId, body }) {
      await qc.cancelQueries({ queryKey: key })
      const previo = qc.getQueryData<Borrador>(key)
      qc.setQueryData<Borrador>(key, (b) =>
        b
          ? {
              ...b,
              criterios: b.criterios?.map((c) => (c.id === criterioId ? { ...c, ...body } : c)),
            }
          : b,
      )
      return { previo }
    },
    onError(_err, _vars, context) {
      if (context?.previo) qc.setQueryData(key, context.previo)
    },
    onSuccess(borrador) {
      qc.setQueryData(key, borrador)
    },
  })
}

export function usePatchComentario(entregaId: string, evaluacionId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (comentario: string | null) => patchComentario(evaluacionId, comentario),
    onSuccess: (borrador) => qc.setQueryData(queryKeys.evaluacion(entregaId), borrador),
  })
}

export function useAprobar(entregaId: string, evaluacionId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => aprobarEvaluacion(evaluacionId),
    onSuccess: (borrador) => {
      qc.setQueryData(queryKeys.evaluacion(entregaId), borrador)
    },
  })
}
