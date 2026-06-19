import { useMemo, useState } from 'react'
import { createFileRoute, Link, useNavigate } from '@tanstack/react-router'
import { toast } from 'sonner'
import { Boxes, ChevronRight, MoreHorizontal, Plus, Trash2 } from 'lucide-react'

import { ApiError } from '@/api/errors'
import { useLotes, useEliminarLote } from '@/features/lotes/hooks'
import { useMaterias } from '@/features/materias/hooks'
import { EstadoLoteBadge } from '@/features/lotes/badges'
import { LoteFormDialog } from '@/features/lotes/lote-form-dialog'
import type { Lote } from '@/features/lotes/api'
import { EmptyState, ErrorState, LoadingRows } from '@/components/states'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'

export const Route = createFileRoute('/_authed/_docente/lotes/')({
  component: LotesPage,
})

function LotesPage() {
  const navigate = useNavigate()
  const { data, isPending, isError, refetch } = useLotes()
  const materias = useMaterias()
  const eliminar = useEliminarLote()

  const [formOpen, setFormOpen] = useState(false)
  const [eliminarTarget, setEliminarTarget] = useState<Lote | undefined>(undefined)

  const nombreMateria = useMemo(() => {
    const map = new Map<string, string>()
    for (const m of materias.data ?? []) if (m.id) map.set(m.id, m.nombre ?? '')
    return map
  }, [materias.data])

  function confirmarEliminar() {
    const lote = eliminarTarget
    if (!lote?.id) return
    eliminar.mutate(lote.id, {
      onSuccess: () => {
        toast.success('Lote eliminado')
        setEliminarTarget(undefined)
      },
      onError: (error: unknown) => {
        toast.error(error instanceof ApiError ? error.message : 'No pudimos eliminar el lote.')
        setEliminarTarget(undefined)
      },
    })
  }

  return (
    <section className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Lotes de entregas</h1>
          <p className="text-muted-foreground mt-1 text-sm">
            Cada lote agrupa las entregas de un trabajo y se evalúa con una rúbrica.
          </p>
        </div>
        <Button onClick={() => setFormOpen(true)}>
          <Plus />
          Nuevo lote
        </Button>
      </div>

      {isPending ? (
        <LoadingRows rows={5} />
      ) : isError ? (
        <ErrorState
          title="No pudimos cargar los lotes."
          message="Revisa tu conexión e inténtalo de nuevo."
          onRetry={() => refetch()}
        />
      ) : data.length === 0 ? (
        <EmptyState
          icon={<Boxes className="size-8" />}
          title="Aún no tienes lotes"
          message="Crea un lote para subir las entregas de un trabajo y procesarlas."
          action={
            <Button onClick={() => setFormOpen(true)}>
              <Plus />
              Nuevo lote
            </Button>
          }
        />
      ) : (
        <div className="border-border overflow-hidden rounded-lg border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Lote</TableHead>
                <TableHead>Materia</TableHead>
                <TableHead>Estado</TableHead>
                <TableHead>
                  <span className="sr-only">Acciones</span>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.map((lote) => (
                <TableRow
                  key={lote.id}
                  className="hover:bg-muted/50 cursor-pointer"
                  onClick={() => {
                    if (!lote.id) return
                    navigate({ to: '/lotes/$loteId', params: { loteId: lote.id } })
                  }}
                >
                  <TableCell>
                    <Link
                      to="/lotes/$loteId"
                      params={{ loteId: lote.id ?? '' }}
                      className="text-foreground hover:text-primary font-medium hover:underline"
                    >
                      {lote.nombre}
                    </Link>
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {(lote.materiaId && nombreMateria.get(lote.materiaId)) || '—'}
                  </TableCell>
                  <TableCell>
                    {lote.estado ? <EstadoLoteBadge estado={lote.estado} /> : null}
                  </TableCell>
                  <TableCell>
                    <div
                      className="flex items-center justify-end gap-1"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button
                            variant="ghost"
                            size="icon"
                            aria-label={`Acciones de ${lote.nombre}`}
                            onClick={(e) => e.stopPropagation()}
                          >
                            <MoreHorizontal />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem
                            variant="destructive"
                            onSelect={() => setEliminarTarget(lote)}
                          >
                            <Trash2 />
                            Eliminar
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                      <ChevronRight className="text-muted-foreground size-4 shrink-0" aria-hidden />
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <LoteFormDialog open={formOpen} onOpenChange={setFormOpen} />

      <AlertDialog
        open={Boolean(eliminarTarget)}
        onOpenChange={(open) => !open && setEliminarTarget(undefined)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>¿Eliminar “{eliminarTarget?.nombre}”?</AlertDialogTitle>
            <AlertDialogDescription>
              Se eliminará el lote, sus entregas y los archivos en disco. Esta acción no se puede
              deshacer.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={eliminar.isPending}>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              onClick={confirmarEliminar}
              disabled={eliminar.isPending}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {eliminar.isPending ? 'Eliminando…' : 'Eliminar'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </section>
  )
}
