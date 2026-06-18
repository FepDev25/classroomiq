import { createFileRoute, redirect } from '@tanstack/react-router'

/** El portal admin entra por Cuentas. */
export const Route = createFileRoute('/_authed/admin/')({
  beforeLoad: () => {
    throw redirect({ to: '/admin/cuentas' })
  },
})
