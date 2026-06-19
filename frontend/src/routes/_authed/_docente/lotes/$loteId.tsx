import { useMemo, useState } from 'react'
import { createFileRoute, Link } from '@tanstack/react-router'
import { toast } from 'sonner'
import {
  ArrowLeft,
  BarChart3,
  ClipboardCheck,
  FileDown,
  FileSpreadsheet,
  FileText,
  FileUp,
  Cpu,
  ScanSearch,
  Sparkles,
  Trash2,
  Users,
} from 'lucide-react'

import { ApiError } from '@/api/errors'
import { useEventosLote } from '@/hooks/useEventosLote'
import {
  useEntregas,
  useEvaluarLote,
  useEliminarEntrega,
  useLote,
  useProcesarLote,
} from '@/features/lotes/hooks'
import { EstadoEntregaBadge, EstadoLoteBadge, TipoEntregaBadge } from '@/features/lotes/badges'
import { SubirEntregaDialog } from '@/features/lotes/subir-entrega-dialog'
import { useExportarPdf } from '@/features/lotes/pdf/use-exportar'
import { useExportarTabla } from '@/features/lotes/export/use-exportar-tabla'
import type { Entrega } from '@/features/lotes/api'
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

export const Route = createFileRoute('/_authed/_docente/lotes/$loteId')({
  component: LoteDetallePage,
})

function LoteDetallePage() {
  const { loteId } = Route.useParams()
  const lote = useLote(loteId)
  const entregas = useEntregas(loteId)
  const procesar = useProcesarLote(loteId)
  const evaluar = useEvaluarLote(loteId)
  const eliminar = useEliminarEntrega(loteId)
  const { exportar: exportarPdf, exportando: exportandoPdf } = useExportarPdf(loteId)
  const { exportar: exportarTabla, exportando: exportandoTabla } = useExportarTabla(loteId)
  const exportando = exportandoPdf || exportandoTabla !== null

  // Stream SSE en vivo: mantiene los estados de las entregas actualizados.
  useEventosLote(loteId)

  const [subirOpen, setSubirOpen] = useState(false)
  const [eliminarTarget, setEliminarTarget] = useState<Entrega | undefined>(undefined)

  const lista = useMemo(() => entregas.data ?? [], [entregas.data])
  const resumen = useMemo(() => {
    let pendientesOError = 0
    let listas = 0
    let activas = 0
    for (const e of lista) {
      if (e.estado === 'PENDIENTE' || e.estado === 'ERROR') pendientesOError++
      if (e.estado === 'LISTO') listas++
      if (e.estado === 'PROCESANDO' || e.estado === 'EVALUANDO') activas++
    }
    return { pendientesOError, listas, activas, total: lista.length }
  }, [lista])

  const puedeProcesar = resumen.pendientesOError > 0 && resumen.activas === 0 && !procesar.isPending
  const puedeEvaluar = resumen.listas > 0 && resumen.activas === 0 && !evaluar.isPending

  function onProcesar() {
    procesar.mutate(undefined, {
      onSuccess: (encoladas) =>
        toast.success(
          encoladas > 0
            ? `Procesando ${encoladas} ${encoladas === 1 ? 'entrega' : 'entregas'}…`
            : 'No hay entregas pendientes de procesar.',
        ),
      onError: (error: unknown) =>
        toast.error(
          error instanceof ApiError ? error.message : 'No pudimos iniciar el procesamiento.',
        ),
    })
  }

  function onEvaluar() {
    evaluar.mutate(undefined, {
      onSuccess: (encoladas) =>
        toast.success(
          encoladas > 0
            ? `Evaluando ${encoladas} ${encoladas === 1 ? 'entrega' : 'entregas'}…`
            : 'No hay entregas listas para evaluar.',
        ),
      onError: (error: unknown) =>
        toast.error(
          error instanceof ApiError ? error.message : 'No pudimos iniciar la evaluación.',
        ),
    })
  }

  function confirmarEliminar() {
    const entrega = eliminarTarget
    if (!entrega?.id) return
    eliminar.mutate(entrega.id, {
      onSuccess: () => {
        toast.success('Entrega eliminada')
        setEliminarTarget(undefined)
      },
      onError: (error: unknown) => {
        toast.error(error instanceof ApiError ? error.message : 'No pudimos eliminar la entrega.')
        setEliminarTarget(undefined)
      },
    })
  }

  return (
    <section className="space-y-8">
      <div>
        <Link
          to="/lotes"
          className="text-muted-foreground hover:text-foreground inline-flex items-center gap-1 text-sm"
        >
          <ArrowLeft className="size-4" />
          Lotes
        </Link>

        {lote.isPending ? (
          <div className="mt-4">
            <LoadingRows rows={1} />
          </div>
        ) : lote.isError ? (
          <div className="mt-4">
            <ErrorState
              title="No pudimos cargar el lote."
              message="Revisa tu conexión e inténtalo de nuevo."
              onRetry={() => lote.refetch()}
            />
          </div>
        ) : (
          <div className="mt-3 flex flex-wrap items-start justify-between gap-4">
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-2xl font-semibold tracking-tight">{lote.data.nombre}</h1>
                {lote.data.estado ? <EstadoLoteBadge estado={lote.data.estado} /> : null}
              </div>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <Button variant="outline" onClick={() => setSubirOpen(true)}>
                <FileUp />
                Subir entrega
              </Button>
              <Button variant="outline" onClick={onProcesar} disabled={!puedeProcesar}>
                <Cpu />
                {procesar.isPending ? 'Encolando…' : 'Procesar'}
              </Button>
              <Button onClick={onEvaluar} disabled={!puedeEvaluar}>
                <Sparkles />
                {evaluar.isPending ? 'Encolando…' : 'Evaluar'}
              </Button>
              <Button asChild variant="ghost">
                <Link to="/lotes/$loteId/similitud" params={{ loteId }}>
                  <ScanSearch />
                  Similitud
                </Link>
              </Button>
              <Button asChild variant="ghost">
                <Link to="/lotes/$loteId/resumen" params={{ loteId }}>
                  <BarChart3 />
                  Resumen
                </Link>
              </Button>
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" disabled={exportando}>
                    <FileDown />
                    {exportando ? 'Exportando…' : 'Exportar'}
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuItem
                    onSelect={() => lote.data && exportarPdf(lote.data.nombre ?? 'lote')}
                  >
                    <FileDown />
                    PDF
                  </DropdownMenuItem>
                  <DropdownMenuItem
                    onSelect={() => lote.data && exportarTabla(lote.data.nombre ?? 'lote', 'xlsx')}
                  >
                    <FileSpreadsheet />
                    Excel (.xlsx)
                  </DropdownMenuItem>
                  <DropdownMenuItem
                    onSelect={() => lote.data && exportarTabla(lote.data.nombre ?? 'lote', 'csv')}
                  >
                    <FileText />
                    CSV
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
          </div>
        )}
      </div>

      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-medium">Entregas</h2>
          {resumen.activas > 0 ? (
            <span className="text-muted-foreground inline-flex items-center gap-1.5 text-sm">
              <span className="bg-estado-procesando size-2 animate-pulse rounded-full" />
              {resumen.activas} en proceso
            </span>
          ) : null}
        </div>

        {entregas.isPending ? (
          <LoadingRows rows={3} />
        ) : entregas.isError ? (
          <ErrorState
            message="No pudimos cargar las entregas."
            onRetry={() => entregas.refetch()}
          />
        ) : lista.length === 0 ? (
          <EmptyState
            icon={<Users className="size-8" />}
            title="Aún no hay entregas"
            message="Sube las entregas de los estudiantes para procesarlas y evaluarlas."
            action={
              <Button onClick={() => setSubirOpen(true)}>
                <FileUp />
                Subir entrega
              </Button>
            }
          />
        ) : (
          <ul className="space-y-2">
            {lista.map((entrega) => (
              <EntregaRow
                key={entrega.id}
                entrega={entrega}
                onEliminar={() => setEliminarTarget(entrega)}
              />
            ))}
          </ul>
        )}
      </div>

      <SubirEntregaDialog loteId={loteId} open={subirOpen} onOpenChange={setSubirOpen} />

      <AlertDialog
        open={Boolean(eliminarTarget)}
        onOpenChange={(open) => !open && setEliminarTarget(undefined)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              ¿Eliminar la entrega de “{eliminarTarget?.identificadorEstudiante}”?
            </AlertDialogTitle>
            <AlertDialogDescription>
              Se eliminará la entrega y sus archivos. Esta acción no se puede deshacer.
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

function EntregaRow({ entrega, onEliminar }: { entrega: Entrega; onEliminar: () => void }) {
  const archivos = entrega.archivos?.length ?? 0
  return (
    <li className="border-border bg-card rounded-lg border p-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <span className="font-medium">{entrega.identificadorEstudiante}</span>
          {entrega.tipo ? <TipoEntregaBadge tipo={entrega.tipo} /> : null}
        </div>
        <div className="flex items-center gap-2">
          {entrega.estado ? <EstadoEntregaBadge estado={entrega.estado} /> : null}
          {entrega.estado === 'LISTO' && entrega.id ? (
            <Button asChild variant="outline" size="sm">
              <Link to="/entregas/$entregaId/revision" params={{ entregaId: entrega.id }}>
                <ClipboardCheck />
                Revisar
              </Link>
            </Button>
          ) : null}
          <Button
            variant="ghost"
            size="icon"
            aria-label={`Eliminar entrega de ${entrega.identificadorEstudiante}`}
            onClick={onEliminar}
            className="text-muted-foreground hover:text-destructive"
          >
            <Trash2 className="size-4" />
          </Button>
        </div>
      </div>
      <p className="text-muted-foreground mt-1 text-xs">
        {archivos} {archivos === 1 ? 'archivo' : 'archivos'}
      </p>
      {entrega.estado === 'ERROR' && entrega.mensajeError ? (
        <p className="text-destructive border-estado-error/30 bg-estado-error/5 mt-2 rounded-md border px-3 py-2 text-sm">
          {entrega.mensajeError}
        </p>
      ) : null}
    </li>
  )
}
