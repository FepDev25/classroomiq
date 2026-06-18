import createClient, { type Middleware } from 'openapi-fetch'

import type { paths } from './schema'
import { clearSession, getToken } from '@/features/auth/session'

export const baseUrl = import.meta.env.VITE_API_URL

if (!baseUrl) {
  // Falla ruidosa en dev: sin baseUrl el cliente no puede llamar al backend.
  console.warn('VITE_API_URL no está definida; revisa tu archivo .env')
}

/** Inyecta el Bearer en cada request si hay sesión. */
const authMiddleware: Middleware = {
  onRequest({ request }) {
    const token = getToken()
    if (token) {
      request.headers.set('Authorization', `Bearer ${token}`)
    }
    return request
  },
}

/**
 * Maneja el 401 de forma global: limpia la sesión y redirige al login.
 * El mapeo del resto de `ProblemDetail` (400/403/404/409/422) lo hace cada
 * feature al consumir el `{ data, error }` con `toApiError`.
 */
const unauthorizedMiddleware: Middleware = {
  onResponse({ response }) {
    if (response.status === 401) {
      clearSession()
      const onLogin = window.location.pathname.startsWith('/login')
      if (!onLogin) {
        window.location.assign('/login')
      }
    }
    return response
  },
}

export const api = createClient<paths>({
  baseUrl,
  // Difiere al fetch global en cada llamada (no captura la referencia al crear
  // el cliente). Necesario para que MSW pueda interceptar en los tests.
  fetch: (input: Request) => globalThis.fetch(input),
})
api.use(authMiddleware, unauthorizedMiddleware)
