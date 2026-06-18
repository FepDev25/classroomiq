import { createFileRoute, Outlet, redirect } from '@tanstack/react-router'

/**
 * Layout del portal de administración. Exige rol ADMIN; cualquier otro rol
 * rebota al dispatcher de `/`. Las pantallas (cuentas, coordinadores,
 * métricas) se construyen en los hitos H3–H4 de la v2.
 */
export const Route = createFileRoute('/_authed/admin')({
  beforeLoad: ({ context }) => {
    if (context.auth.rol !== 'ADMIN') {
      throw redirect({ to: '/' })
    }
  },
  component: Outlet,
})
