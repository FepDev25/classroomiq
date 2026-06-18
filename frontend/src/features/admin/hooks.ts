import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { queryKeys } from '@/api/queryKeys'
import {
  asignarMateria,
  cambiarActivoUsuario,
  crearUsuario,
  desasignarMateria,
  listMateriasAdmin,
  listMateriasAsignadas,
  listUsuarios,
  type CreateUsuario,
} from './api'

export function useUsuarios() {
  return useQuery({ queryKey: queryKeys.usuarios, queryFn: listUsuarios })
}

export function useCrearUsuario() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateUsuario) => crearUsuario(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.usuarios }),
  })
}

export function useCambiarActivoUsuario() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, activo }: { id: string; activo: boolean }) =>
      cambiarActivoUsuario(id, activo),
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.usuarios }),
  })
}

export function useMateriasAdmin() {
  return useQuery({ queryKey: queryKeys.materiasAdmin, queryFn: listMateriasAdmin })
}

export function useMateriasAsignadas(coordinadorId: string) {
  return useQuery({
    queryKey: queryKeys.coordinadorMaterias(coordinadorId),
    queryFn: () => listMateriasAsignadas(coordinadorId),
    enabled: Boolean(coordinadorId),
  })
}

export function useAsignarMateria(coordinadorId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (materiaId: string) => asignarMateria(coordinadorId, materiaId),
    // La respuesta es la lista actualizada de asignadas: la fijamos directo.
    onSuccess: (asignadas) =>
      qc.setQueryData(queryKeys.coordinadorMaterias(coordinadorId), asignadas),
  })
}

export function useDesasignarMateria(coordinadorId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (materiaId: string) => desasignarMateria(coordinadorId, materiaId),
    onSuccess: (asignadas) =>
      qc.setQueryData(queryKeys.coordinadorMaterias(coordinadorId), asignadas),
  })
}
