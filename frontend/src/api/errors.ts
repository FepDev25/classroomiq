import type { components } from './schema'

/** RFC 7807 — forma de error uniforme del backend (ApiExceptionHandler). */
export type ProblemDetail = components['schemas']['ProblemDetail']

/**
 * Error tipado que envuelve un ProblemDetail. Las funciones de la capa de API
 * lanzan esto para que TanStack Query lo trate como error y la UI mapee el
 * `status`/`detail` a la experiencia adecuada (ver §2 del roadmap).
 */
export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail

  constructor(problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? 'Error de la API')
    this.name = 'ApiError'
    this.status = problem.status ?? 0
    this.problem = problem
  }
}

function looksLikeProblem(value: unknown): value is ProblemDetail {
  return (
    typeof value === 'object' &&
    value !== null &&
    ('detail' in value || 'title' in value || 'status' in value || 'type' in value)
  )
}

/** Normaliza el `error` que devuelve openapi-fetch a un ApiError. */
export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error
  if (looksLikeProblem(error)) return new ApiError(error)
  return new ApiError({ type: 'about:blank', title: 'Error inesperado', status: 0 })
}
