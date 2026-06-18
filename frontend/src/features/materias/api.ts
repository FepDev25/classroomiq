import { api } from '@/api/client'
import { toApiError } from '@/api/errors'
import type { components } from '@/api/schema'

export type Materia = components['schemas']['MateriaResponse']
export type MateriaRequest = components['schemas']['MateriaRequest']

export async function listMaterias(): Promise<Materia[]> {
  const { data, error } = await api.GET('/api/materias')
  if (error) throw toApiError(error)
  return data
}

export async function getMateria(id: string): Promise<Materia> {
  const { data, error } = await api.GET('/api/materias/{id}', { params: { path: { id } } })
  if (error) throw toApiError(error)
  return data
}

export async function crearMateria(body: MateriaRequest): Promise<Materia> {
  const { data, error } = await api.POST('/api/materias', { body })
  if (error) throw toApiError(error)
  return data
}

export async function actualizarMateria(id: string, body: MateriaRequest): Promise<Materia> {
  const { data, error } = await api.PUT('/api/materias/{id}', { params: { path: { id } }, body })
  if (error) throw toApiError(error)
  return data
}

export async function archivarMateria(id: string): Promise<Materia> {
  const { data, error } = await api.POST('/api/materias/{id}/archivar', {
    params: { path: { id } },
  })
  if (error) throw toApiError(error)
  return data
}
