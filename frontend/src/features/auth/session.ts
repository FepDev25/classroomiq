import type { components } from '@/api/schema'

export type Rol = components['schemas']['Rol']

/**
 * Sesión del docente. El JWT (y los datos que porta) vive en memoria para
 * acceso rápido desde el cliente de API, con respaldo en localStorage para
 * sobrevivir recargas. No hay refresh token: al expirar (o ante un 401) se
 * limpia. Tradeoff de XSS asumido y mitigado por CSP + no inyectar HTML sin
 * sanitizar (ver §2 del roadmap).
 */
export interface Session {
  token: string
  rol: Rol
  usuarioId: string
  /** Email que el usuario tecleó al entrar — solo para mostrar en la UI. */
  email: string
  /** Epoch en ms en que el token deja de ser válido. */
  expiresAt: number
}

const STORAGE_KEY = 'classroomiq.session'

let cache: Session | null | undefined

function read(): Session | null {
  if (cache !== undefined) return cache
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    cache = raw ? (JSON.parse(raw) as Session) : null
  } catch {
    cache = null
  }
  return cache
}

/** Sesión vigente, o `null` si no hay o ya expiró (en cuyo caso la limpia). */
export function getSession(): Session | null {
  const session = read()
  if (!session) return null
  if (Date.now() >= session.expiresAt) {
    clearSession()
    return null
  }
  return session
}

export function setSession(session: Session): void {
  cache = session
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function clearSession(): void {
  cache = null
  localStorage.removeItem(STORAGE_KEY)
}

/** Token vigente para el header Authorization, o `null`. */
export function getToken(): string | null {
  return getSession()?.token ?? null
}
