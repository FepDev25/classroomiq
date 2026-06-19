import { useMemo, useState } from 'react'
import { createFileRoute, Link } from '@tanstack/react-router'
import { toast } from 'sonner'
import { ArrowLeft, CheckCircle2, Lock, Sparkles } from 'lucide-react'

import { ApiError } from '@/api/errors'
import { useEntrega } from '@/features/lotes/hooks'
import { useRubrica } from '@/features/rubricas/api'
import {
  useAprobar,
  useBorrador,
  useContenidoEntrega,
  usePatchComentario,
  usePatchCriterio,
} from '@/features/revision/hooks'
import { proyectarTotal } from '@/features/revision/total'
import { PanelEntrega } from '@/features/revision/panel-entrega'
import { PanelDocumento } from '@/features/revision/panel-documento'
import { TarjetaCriterio } from '@/features/revision/tarjeta-criterio'
import { EmptyState, ErrorState, LoadingRows } from '@/components/states'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
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

export const Route = createFileRoute('/_authed/_docente/entregas/$entregaId/revision')({
  component: RevisionPage,
})

function RevisionPage() {
  const { entregaId } = Route.useParams()
  const entrega = useEntrega(entregaId)
  const borrador = useBorrador(entregaId)
  const contenido = useContenidoEntrega(entregaId)
  const rubrica = useRubrica(borrador.data?.rubricaId ?? '')

  const evaluacionId = borrador.data?.id ?? ''
  const patchCriterio = usePatchCriterio(entregaId, evaluacionId)
  const patchComentario = usePatchComentario(entregaId, evaluacionId)
  const aprobar = useAprobar(entregaId, evaluacionId)

  const [aprobarOpen, setAprobarOpen] = useState(false)

  const aprobada = borrador.data?.estado === 'APROBADA'
  const total = useMemo(
    () => (borrador.data ? proyectarTotal(borrador.data, rubrica.data?.modoTotal) : 0),
    [borrador.data, rubrica.data?.modoTotal],
  )

  function guardarCriterio(
    criterioId: string,
    body: Parameters<typeof patchCriterio.mutate>[0]['body'],
  ) {
    patchCriterio.mutate(
      { criterioId, body },
      {
        onError: (error: unknown) => {
          if (error instanceof ApiError && error.status === 409) {
            toast.error('La evaluación ya fue aprobada; recargamos su estado.')
            borrador.refetch()
          } else if (error instanceof ApiError && error.status === 422) {
            toast.error(error.message)
            borrador.refetch()
          } else {
            toast.error('No pudimos guardar el cambio.')
          }
        },
      },
    )
  }

  function onAprobar() {
    aprobar.mutate(undefined, {
      onSuccess: () => {
        toast.success('Evaluación aprobada')
        setAprobarOpen(false)
      },
      onError: (error: unknown) => {
        if (error instanceof ApiError && error.status === 409) {
          toast.error('La evaluación ya estaba aprobada.')
          borrador.refetch()
        } else {
          toast.error(
            error instanceof ApiError ? error.message : 'No pudimos aprobar la evaluación.',
          )
        }
        setAprobarOpen(false)
      },
    })
  }

  // Borrador inexistente (entrega indexada pero no evaluada): el GET da 404.
  if (borrador.isError && borrador.error instanceof ApiError && borrador.error.status === 404) {
    return (
      <section className="space-y-6">
        <VolverAlLote loteId={entrega.data?.loteId} />
        <EmptyState
          icon={<Sparkles className="size-8" />}
          title="Esta entrega aún no tiene borrador"
          message="Evalúa el lote para generar el borrador de evaluación y luego revísalo aquí."
        />
      </section>
    )
  }

  if (borrador.isPending || entrega.isPending) {
    return (
      <section className="space-y-6">
        <VolverAlLote loteId={entrega.data?.loteId} />
        <LoadingRows rows={6} />
      </section>
    )
  }

  if (borrador.isError) {
    return (
      <section className="space-y-6">
        <VolverAlLote loteId={entrega.data?.loteId} />
        <ErrorState message="No pudimos cargar el borrador." onRetry={() => borrador.refetch()} />
      </section>
    )
  }

  const datos = borrador.data
  const criterios = datos.criterios ?? []

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <VolverAlLote loteId={entrega.data?.loteId} />
          <div className="mt-2 flex items-center gap-2">
            <h1 className="text-2xl font-semibold tracking-tight">
              {entrega.data?.identificadorEstudiante ?? 'Revisión'}
            </h1>
            {aprobada ? (
              <Badge className="bg-estado-listo gap-1 text-white">
                <Lock className="size-3" aria-hidden />
                Aprobada
              </Badge>
            ) : (
              <Badge variant="secondary">Borrador</Badge>
            )}
          </div>
        </div>
        <div className="text-right">
          <p className="text-muted-foreground text-xs">
            {aprobada ? 'Total final' : 'Total proyectado'}
          </p>
          <p className="text-2xl font-semibold tabular-nums">
            {(aprobada ? (datos.puntajeTotalFinal ?? total) : total).toFixed(2)}
            {rubrica.data?.puntajeTotal != null ? (
              <span className="text-muted-foreground text-base font-normal">
                {' '}
                / {rubrica.data.puntajeTotal}
              </span>
            ) : null}
          </p>
        </div>
      </div>

      {/* Principio inamovible: visible donde el docente decide la nota. */}
      <p className="border-border text-muted-foreground rounded-md border border-dashed px-3 py-2 text-sm">
        Borrador generado como asistencia. La nota final es responsabilidad del docente.
      </p>

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="lg:sticky lg:top-20 lg:self-start lg:max-h-[calc(100svh-6rem)] lg:overflow-y-auto">
          {entrega.data ? (
            contenido.data && (contenido.data.archivos?.length ?? 0) > 0 ? (
              // Documento completo con citas resaltadas en contexto.
              <PanelDocumento
                entrega={entrega.data}
                contenido={contenido.data}
                criterios={criterios}
              />
            ) : (
              // Fallback: si no hay contenido (error/cargando/vacío), la lista de citas.
              <PanelEntrega entrega={entrega.data} criterios={criterios} />
            )
          ) : null}
        </div>

        <div className="space-y-4">
          {criterios.map((criterio) => (
            <TarjetaCriterio
              // Remontar al aprobar refleja los valores finales en modo lectura.
              key={`${criterio.id}-${datos.estado}`}
              criterio={criterio}
              readOnly={aprobada}
              onGuardar={guardarCriterio}
            />
          ))}

          <div className="border-border bg-card space-y-3 rounded-lg border p-4">
            <ComentarioGeneral
              key={datos.estado}
              inicial={datos.comentarioGeneral ?? ''}
              readOnly={aprobada}
              onGuardar={(texto) =>
                patchComentario.mutate(texto || null, {
                  onError: () => toast.error('No pudimos guardar el comentario.'),
                })
              }
            />

            {aprobada ? (
              <p className="text-muted-foreground flex items-center gap-2 text-sm">
                <CheckCircle2 className="text-estado-listo size-4" aria-hidden />
                Evaluación congelada. Para cambios, contacta al administrador.
              </p>
            ) : (
              <Button onClick={() => setAprobarOpen(true)} disabled={aprobar.isPending}>
                <CheckCircle2 />
                Aprobar evaluación
              </Button>
            )}
          </div>
        </div>
      </div>

      <AlertDialog open={aprobarOpen} onOpenChange={setAprobarOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>¿Aprobar la evaluación?</AlertDialogTitle>
            <AlertDialogDescription>
              Se recalcula el puntaje total final y la evaluación queda congelada. No podrás
              editarla después desde la app.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={aprobar.isPending}>Cancelar</AlertDialogCancel>
            <AlertDialogAction onClick={onAprobar} disabled={aprobar.isPending}>
              {aprobar.isPending ? 'Aprobando…' : 'Aprobar'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </section>
  )
}

function VolverAlLote({ loteId }: { loteId: string | undefined }) {
  if (!loteId) {
    return (
      <Link
        to="/lotes"
        className="text-muted-foreground hover:text-foreground inline-flex items-center gap-1 text-sm"
      >
        <ArrowLeft className="size-4" />
        Lotes
      </Link>
    )
  }
  return (
    <Link
      to="/lotes/$loteId"
      params={{ loteId }}
      className="text-muted-foreground hover:text-foreground inline-flex items-center gap-1 text-sm"
    >
      <ArrowLeft className="size-4" />
      Volver al lote
    </Link>
  )
}

function ComentarioGeneral({
  inicial,
  readOnly,
  onGuardar,
}: {
  inicial: string
  readOnly: boolean
  onGuardar: (texto: string) => void
}) {
  const [texto, setTexto] = useState(inicial)
  return (
    <div className="space-y-1.5">
      <label htmlFor="comentario-general" className="text-sm font-medium">
        Comentario general <span className="text-muted-foreground font-normal">(opcional)</span>
      </label>
      <Textarea
        id="comentario-general"
        rows={3}
        className="prose-entrega"
        placeholder="Un comentario para el estudiante sobre el trabajo en conjunto…"
        value={texto}
        disabled={readOnly}
        onChange={(e) => setTexto(e.target.value)}
        onBlur={() => {
          if (texto !== inicial) onGuardar(texto)
        }}
      />
    </div>
  )
}
