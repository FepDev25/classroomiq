import { api } from '@/api/client'
import { toApiError } from '@/api/errors'
import type { components } from '@/api/schema'

export type Usuario = components['schemas']['UsuarioResponse']
export type CreateUsuario = components['schemas']['CreateUsuarioRequest']
export type AdminMateria = components['schemas']['AdminMateriaResponse']
export type Materia = components['schemas']['MateriaResponse']
export type Rol = components['schemas']['Rol']

// ---- Cuentas (usuarios)

export async function listUsuarios(): Promise<Usuario[]> {
  const { data, error } = await api.GET('/api/admin/usuarios')
  if (error) throw toApiError(error)
  return data
}

export async function crearUsuario(body: CreateUsuario): Promise<Usuario> {
  const { data, error } = await api.POST('/api/admin/usuarios', { body })
  if (error) throw toApiError(error)
  return data
}

export async function cambiarActivoUsuario(id: string, activo: boolean): Promise<Usuario> {
  const ruta = activo ? '/api/admin/usuarios/{id}/activar' : '/api/admin/usuarios/{id}/desactivar'
  const { data, error } = await api.POST(ruta, { params: { path: { id } } })
  if (error) throw toApiError(error)
  return data
}

// ---- Catálogo de materias y asignación a coordinadores

export async function listMateriasAdmin(): Promise<AdminMateria[]> {
  const { data, error } = await api.GET('/api/admin/materias')
  if (error) throw toApiError(error)
  return data
}

export async function listMateriasAsignadas(coordinadorId: string): Promise<Materia[]> {
  const { data, error } = await api.GET('/api/admin/coordinadores/{coordinadorId}/materias', {
    params: { path: { coordinadorId } },
  })
  if (error) throw toApiError(error)
  return data
}

export async function asignarMateria(coordinadorId: string, materiaId: string): Promise<Materia[]> {
  const { data, error } = await api.POST(
    '/api/admin/coordinadores/{coordinadorId}/materias/{materiaId}',
    { params: { path: { coordinadorId, materiaId } } },
  )
  if (error) throw toApiError(error)
  return data
}

export async function desasignarMateria(
  coordinadorId: string,
  materiaId: string,
): Promise<Materia[]> {
  const { data, error } = await api.DELETE(
    '/api/admin/coordinadores/{coordinadorId}/materias/{materiaId}',
    { params: { path: { coordinadorId, materiaId } } },
  )
  if (error) throw toApiError(error)
  return data
}
