import { createFileRoute, Outlet, redirect } from '@tanstack/react-router'

/**
 * Layout de la vista de coordinación (solo lectura de reportes agregados de
 * materias asignadas). Exige rol COORDINADOR; cualquier otro rol rebota al
 * dispatcher de `/`. Las pantallas se construyen en el hito H5 de la v2.
 */
export const Route = createFileRoute('/_authed/coordinador')({
  beforeLoad: ({ context }) => {
    if (context.auth.rol !== 'COORDINADOR') {
      throw redirect({ to: '/' })
    }
  },
  component: Outlet,
})
