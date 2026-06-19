import { useState } from 'react'
import { createFileRoute } from '@tanstack/react-router'
import { toast } from 'sonner'
import { Plus, Users } from 'lucide-react'

import { ApiError } from '@/api/errors'
import { useCambiarActivoUsuario, useUsuarios } from '@/features/admin/hooks'
import type { Usuario } from '@/features/admin/api'
import { EstadoCuentaBadge, RolBadge } from '@/features/admin/badges'
import { UsuarioFormDialog } from '@/features/admin/usuario-form-dialog'
import { EmptyState, ErrorState, LoadingRows } from '@/components/states'
import { Button } from '@/components/ui/button'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'

export const Route = createFileRoute('/_authed/admin/cuentas')({
  component: CuentasPage,
})

function formatoAcceso(valor: string | null | undefined): string {
  if (!valor) return 'Nunca'
  const d = new Date(valor)
  return Number.isNaN(d.getTime()) ? '—' : d.toLocaleString('es')
}

function CuentasPage() {
  const usuarios = useUsuarios()
  const cambiarActivo = useCambiarActivoUsuario()
  const [formOpen, setFormOpen] = useState(false)

  function onToggle(usuario: Usuario) {
    if (!usuario.id || usuario.activo == null) return
    cambiarActivo.mutate(
      { id: usuario.id, activo: !usuario.activo },
      {
        onSuccess: (u) => toast.success(u.activo ? 'Cuenta activada' : 'Cuenta desactivada'),
        onError: (error: unknown) =>
          toast.error(
            error instanceof ApiError
              ? error.message
              : 'No pudimos cambiar el estado de la cuenta.',
          ),
      },
    )
  }

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Cuentas</h1>
          <p className="text-muted-foreground mt-1 text-sm">
            Gestiona las cuentas de docentes y coordinadores de tu institución.
          </p>
        </div>
        <Button onClick={() => setFormOpen(true)}>
          <Plus />
          Nueva cuenta
        </Button>
      </div>

      {usuarios.isPending ? (
        <LoadingRows rows={5} />
      ) : usuarios.isError ? (
        <ErrorState
          title="No pudimos cargar las cuentas."
          message="Revisa tu conexión e inténtalo de nuevo."
          onRetry={() => usuarios.refetch()}
        />
      ) : usuarios.data.length === 0 ? (
        <EmptyState
          icon={<Users className="size-8" />}
          title="Aún no hay cuentas"
          message="Crea la primera cuenta de docente o coordinador."
          action={
            <Button onClick={() => setFormOpen(true)}>
              <Plus />
              Nueva cuenta
            </Button>
          }
        />
      ) : (
        <div className="border-border overflow-x-auto rounded-lg border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Nombre</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>Rol</TableHead>
                <TableHead>Estado</TableHead>
                <TableHead>Último acceso</TableHead>
                <TableHead>
                  <span className="sr-only">Acciones</span>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {usuarios.data.map((u) => (
                <TableRow key={u.id}>
                  <TableCell className="font-medium">{u.nombre}</TableCell>
                  <TableCell className="text-muted-foreground">{u.email}</TableCell>
                  <TableCell>{u.rol ? <RolBadge rol={u.rol} /> : null}</TableCell>
                  <TableCell>
                    <EstadoCuentaBadge activo={Boolean(u.activo)} />
                  </TableCell>
                  <TableCell className="text-muted-foreground text-sm">
                    {formatoAcceso(u.ultimoAcceso)}
                  </TableCell>
                  <TableCell>
                    <div className="flex justify-end">
                      {u.rol === 'ADMIN' ? (
                        <span className="text-muted-foreground text-xs">—</span>
                      ) : (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => onToggle(u)}
                          disabled={cambiarActivo.isPending}
                        >
                          {u.activo ? 'Desactivar' : 'Activar'}
                        </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <UsuarioFormDialog open={formOpen} onOpenChange={setFormOpen} />
    </section>
  )
}
