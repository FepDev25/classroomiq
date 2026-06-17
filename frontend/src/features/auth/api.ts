import { api } from '@/api/client'
import { ApiError, toApiError } from '@/api/errors'
import type { components } from '@/api/schema'
import type { Session } from './session'

export type LoginRequest = components['schemas']['LoginRequest']
export type TokenResponse = components['schemas']['TokenResponse']

/**
 * Inicia sesión contra el endpoint público `/api/auth/login`. Tipos derivados
 * del contrato (`openapi.yaml`): si el backend cambia la forma, esto deja de
 * compilar.
 */
export async function login(body: LoginRequest): Promise<TokenResponse> {
  const { data, error } = await api.POST('/api/auth/login', { body })
  if (error) throw toApiError(error)
  return data
}

/**
 * Construye la `Session` a partir de la respuesta del token y el email tecleado.
 * Los campos del `TokenResponse` son opcionales en el contrato; si falta lo
 * esencial, tratamos la respuesta como inválida.
 */
export function toSession(token: TokenResponse, email: string): Session {
  if (!token.accessToken || !token.rol || !token.usuarioId) {
    throw new ApiError({
      type: 'about:blank',
      title: 'Respuesta de login inválida',
      status: 0,
      detail: 'El servidor no devolvió un token utilizable.',
    })
  }
  const expiresInMs = (token.expiresIn ?? 3600) * 1000
  return {
    token: token.accessToken,
    rol: token.rol,
    usuarioId: token.usuarioId,
    email,
    expiresAt: Date.now() + expiresInMs,
  }
}
