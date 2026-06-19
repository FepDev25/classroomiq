import { useState } from 'react'
import { createFileRoute, Link } from '@tanstack/react-router'
import { toast } from 'sonner'
import { ArrowLeft, ListChecks, Pencil, Plus, Trash2 } from 'lucide-react'

import { ApiError } from '@/api/errors'
import { useMateria } from '@/features/materias/hooks'
import { MateriaFormDialog } from '@/features/materias/materia-form-dialog'
import { useEliminarRubrica, useRubricas } from '@/features/rubricas/api'
import type { RubricaResponse } from '@/features/rubricas/form'
import { EmptyState, ErrorState, LoadingRows } from '@/components/states'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
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

export const Route = createFileRoute('/_authed/_docente/materias/$materiaId')({
  component: MateriaDetallePage,
})

function MateriaDetallePage() {
  const { materiaId } = Route.useParams()
  const materia = useMateria(materiaId)
  const rubricas = useRubricas(materiaId)
  const eliminar = useEliminarRubrica(materiaId)

  const [editOpen, setEditOpen] = useState(false)
  const [eliminarTarget, setEliminarTarget] = useState<RubricaResponse | undefined>(undefined)

  function confirmarEliminar() {
    const rubrica = eliminarTarget
    if (!rubrica?.id) return
    eliminar.mutate(rubrica.id, {
      onSuccess: () => {
        toast.success('Rúbrica eliminada')
        setEliminarTarget(undefined)
      },
      onError: (error: unknown) => {
        toast.error(error instanceof ApiError ? error.message : 'No pudimos eliminar la rúbrica.')
        setEliminarTarget(undefined)
      },
    })
  }

  return (
    <section className="space-y-8">
      <div>
        <Link
          to="/materias"
          className="text-muted-foreground hover:text-foreground inline-flex items-center gap-1 text-sm"
        >
          <ArrowLeft className="size-4" />
          Materias
        </Link>

        {materia.isPending ? (
          <div className="mt-4">
            <LoadingRows rows={1} />
          </div>
        ) : materia.isError ? (
          <div className="mt-4">
            <ErrorState
              title="No pudimos cargar la materia."
              message="Revisa tu conexión e inténtalo de nuevo."
              onRetry={() => materia.refetch()}
            />
          </div>
        ) : (
          <div className="mt-3 flex items-start justify-between gap-4">
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-2xl font-semibold tracking-tight">{materia.data.nombre}</h1>
                {materia.data.archivada ? <Badge variant="secondary">Archivada</Badge> : null}
              </div>
              {materia.data.periodoAcademico ? (
                <p className="text-muted-foreground mt-1 text-sm">
                  {materia.data.periodoAcademico}
                </p>
              ) : null}
              {materia.data.descripcion ? (
                <p className="prose-entrega text-muted-foreground mt-2 max-w-2xl">
                  {materia.data.descripcion}
                </p>
              ) : null}
            </div>
            {!materia.data.archivada ? (
              <Button variant="outline" size="sm" onClick={() => setEditOpen(true)}>
                <Pencil />
                Editar
              </Button>
            ) : null}
          </div>
        )}
      </div>

      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-medium">Rúbricas</h2>
          <Button asChild size="sm">
            <Link to="/rubricas/nueva" search={{ materiaId }}>
              <Plus />
              Nueva rúbrica
            </Link>
          </Button>
        </div>

        {rubricas.isPending ? (
          <LoadingRows rows={3} />
        ) : rubricas.isError ? (
          <ErrorState
            message="No pudimos cargar las rúbricas."
            onRetry={() => rubricas.refetch()}
          />
        ) : rubricas.data.length === 0 ? (
          <EmptyState
            icon={<ListChecks className="size-8" />}
            title="Aún no hay rúbricas"
            message="Define una rúbrica con criterios y niveles para evaluar las entregas de esta materia."
            action={
              <Button asChild>
                <Link to="/rubricas/nueva" search={{ materiaId }}>
                  <Plus />
                  Nueva rúbrica
                </Link>
              </Button>
            }
          />
        ) : (
          <ul className="grid gap-3 sm:grid-cols-2">
            {rubricas.data.map((rubrica) => (
              <RubricaCard
                key={rubrica.id}
                rubrica={rubrica}
                onEliminar={() => setEliminarTarget(rubrica)}
              />
            ))}
          </ul>
        )}
      </div>

      {materia.data ? (
        <MateriaFormDialog materia={materia.data} open={editOpen} onOpenChange={setEditOpen} />
      ) : null}

      <AlertDialog
        open={Boolean(eliminarTarget)}
        onOpenChange={(open) => !open && setEliminarTarget(undefined)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>¿Eliminar “{eliminarTarget?.nombre}”?</AlertDialogTitle>
            <AlertDialogDescription>
              Se eliminará la rúbrica y sus criterios. Esta acción no se puede deshacer.
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

function RubricaCard({
  rubrica,
  onEliminar,
}: {
  rubrica: RubricaResponse
  onEliminar: () => void
}) {
  const criterios = rubrica.criterios?.length ?? 0
  return (
    <li className="border-border bg-card flex flex-col rounded-lg border p-4">
      <div className="flex items-start justify-between gap-2">
        <h3 className="font-medium">{rubrica.nombre}</h3>
        <span className="text-muted-foreground font-mono text-xs">
          {rubrica.puntajeTotal} pts · {rubrica.modoTotal === 'PROMEDIO' ? 'promedio' : 'suma'}
        </span>
      </div>
      {rubrica.descripcion ? (
        <p className="text-muted-foreground mt-1 line-clamp-2 text-sm">{rubrica.descripcion}</p>
      ) : null}
      <p className="text-muted-foreground mt-2 text-sm">
        {criterios} {criterios === 1 ? 'criterio' : 'criterios'}
      </p>
      <div className="mt-4 flex items-center gap-2">
        <Button asChild variant="outline" size="sm">
          <Link to="/rubricas/$rubricaId" params={{ rubricaId: rubrica.id ?? '' }}>
            <Pencil />
            Editar
          </Link>
        </Button>
        <Button
          variant="ghost"
          size="sm"
          onClick={onEliminar}
          className="text-destructive hover:text-destructive"
        >
          <Trash2 />
          Eliminar
        </Button>
      </div>
    </li>
  )
}
