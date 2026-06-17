import { lazy, Suspense, type ReactNode } from 'react'
import {
  createRootRouteWithContext,
  Link,
  Outlet,
  useRouter,
  type ErrorComponentProps,
} from '@tanstack/react-router'

import type { AuthContextValue } from '@/features/auth/auth-context'
import { Button } from '@/components/ui/button'

export interface RouterContext {
  auth: AuthContextValue
}

// Lazy + tree-shaken en producción: los devtools no se empaquetan en el build.
const RouterDevtools = import.meta.env.PROD
  ? () => null
  : lazy(() =>
      import('@tanstack/react-router-devtools').then((m) => ({
        default: m.TanStackRouterDevtools,
      })),
    )

export const Route = createRootRouteWithContext<RouterContext>()({
  component: RootLayout,
  errorComponent: RootError,
  notFoundComponent: NotFound,
})

function RootLayout() {
  return (
    <>
      <Outlet />
      <Suspense>
        <RouterDevtools position="bottom-right" />
      </Suspense>
    </>
  )
}

function Centered({ children }: { children: ReactNode }) {
  return (
    <div className="bg-background text-foreground flex min-h-svh items-center justify-center px-4">
      <div className="max-w-md text-center">{children}</div>
    </div>
  )
}

function RootError({ error }: ErrorComponentProps) {
  const router = useRouter()
  return (
    <Centered>
      <p className="text-muted-foreground font-mono text-xs tracking-[0.2em] uppercase">Error</p>
      <h1 className="mt-3 text-2xl font-semibold tracking-tight">Algo salió mal</h1>
      <p className="text-muted-foreground mt-2 text-sm">{error.message}</p>
      <Button className="mt-6" onClick={() => router.invalidate()}>
        Reintentar
      </Button>
    </Centered>
  )
}

function NotFound() {
  return (
    <Centered>
      <p className="text-muted-foreground font-mono text-xs tracking-[0.2em] uppercase">404</p>
      <h1 className="mt-3 text-2xl font-semibold tracking-tight">Página no encontrada</h1>
      <p className="text-muted-foreground mt-2 text-sm">
        La página que buscas no existe o no tienes acceso a ella.
      </p>
      <Button className="mt-6" asChild>
        <Link to="/">Volver al inicio</Link>
      </Button>
    </Centered>
  )
}
