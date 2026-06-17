import {
  createContext,
  use,
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'

import { clearSession, getSession, setSession, type Rol, type Session } from './session'

export interface AuthContextValue {
  session: Session | null
  isAuthenticated: boolean
  rol: Rol | null
  signIn: (session: Session) => void
  signOut: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

const STORAGE_KEY = 'classroomiq.session'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSessionState] = useState<Session | null>(() => getSession())

  const signIn = useCallback((next: Session) => {
    setSession(next)
    setSessionState(next)
  }, [])

  const signOut = useCallback(() => {
    clearSession()
    setSessionState(null)
  }, [])

  // Sincroniza el cierre de sesión entre pestañas (storage event de otra tab).
  useEffect(() => {
    function onStorage(event: StorageEvent) {
      if (event.key === STORAGE_KEY) {
        setSessionState(getSession())
      }
    }
    window.addEventListener('storage', onStorage)
    return () => window.removeEventListener('storage', onStorage)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      isAuthenticated: session !== null,
      rol: session?.rol ?? null,
      signIn,
      signOut,
    }),
    [session, signIn, signOut],
  )

  return <AuthContext value={value}>{children}</AuthContext>
}

export function useAuth(): AuthContextValue {
  const ctx = use(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return ctx
}
