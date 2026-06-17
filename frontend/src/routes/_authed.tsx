import { createFileRoute, Outlet, redirect } from '@tanstack/react-router'

import { AuthedShell } from '@/components/authed-shell'

/**
 * Layout sin segmento de ruta que protege todo lo que cuelga de él: si no hay
 * sesión, redirige a /login conservando el destino para volver tras entrar.
 */
export const Route = createFileRoute('/_authed')({
  beforeLoad: ({ context, location }) => {
    if (!context.auth.isAuthenticated) {
      throw redirect({ to: '/login', search: { redirect: location.href } })
    }
  },
  component: AuthedLayout,
})

function AuthedLayout() {
  return (
    <AuthedShell>
      <Outlet />
    </AuthedShell>
  )
}
