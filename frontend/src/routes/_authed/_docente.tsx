import { createFileRoute, Outlet, redirect } from '@tanstack/react-router'

/**
 * Layout sin segmento de URL que agrupa todas las rutas del docente
 * (materias, lotes, rúbricas, revisión) y exige el rol DOCENTE. Un admin o
 * coordinador que intente entrar por URL directa rebota al dispatcher de `/`,
 * que lo lleva a la home de su rol.
 */
export const Route = createFileRoute('/_authed/_docente')({
  beforeLoad: ({ context }) => {
    if (context.auth.rol !== 'DOCENTE') {
      throw redirect({ to: '/' })
    }
  },
  component: Outlet,
})
