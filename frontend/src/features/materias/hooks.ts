import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { queryKeys } from '@/api/queryKeys'
import {
  actualizarMateria,
  archivarMateria,
  crearMateria,
  getMateria,
  listMaterias,
  type MateriaRequest,
} from './api'

export function useMaterias() {
  return useQuery({ queryKey: queryKeys.materias, queryFn: listMaterias })
}

export function useMateria(id: string) {
  return useQuery({
    queryKey: queryKeys.materia(id),
    queryFn: () => getMateria(id),
    enabled: Boolean(id),
  })
}

export function useCrearMateria() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: MateriaRequest) => crearMateria(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.materias }),
  })
}

export function useActualizarMateria(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: MateriaRequest) => actualizarMateria(id, body),
    onSuccess: (materia) => {
      qc.invalidateQueries({ queryKey: queryKeys.materias })
      qc.setQueryData(queryKeys.materia(id), materia)
    },
  })
}

export function useArchivarMateria() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => archivarMateria(id),
    onSuccess: (materia) => {
      qc.invalidateQueries({ queryKey: queryKeys.materias })
      if (materia.id) qc.setQueryData(queryKeys.materia(materia.id), materia)
    },
  })
}
