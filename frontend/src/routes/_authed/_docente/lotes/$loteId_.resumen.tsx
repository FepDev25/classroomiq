import { createFileRoute, Link } from '@tanstack/react-router'
import { toast } from 'sonner'
import { ArrowLeft, Sparkles, TriangleAlert } from 'lucide-react'

import { ApiError } from '@/api/errors'
import { useLote } from '@/features/lotes/hooks'
import { useGenerarNarrativa, useResumen } from '@/features/reportes/hooks'
import { ResumenGrupoView } from '@/features/reportes/resumen-grupo-view'
import { EmptyState, ErrorState, LoadingRows } from '@/components/states'
import { Button } from '@/components/ui/button'

export const Route = createFileRoute('/_authed/_docente/lotes/$loteId_/resumen')({
  component: ResumenPage,
})

function ResumenPage() {
  const { loteId } = Route.useParams()
  const lote = useLote(loteId)
  const resumen = useResumen(loteId)
  const narrativa = useGenerarNarrativa(loteId)

  const incompleto =
    resumen.isError && resumen.error instanceof ApiError && resumen.error.status === 422

  function onGenerarNarrativa() {
    narrativa.mutate(undefined, {
      onSuccess: () => toast.success('Narrativa generada'),
      onError: () => toast.error('No pudimos generar la narrativa.'),
    })
  }

  return (
    <section className="space-y-6">
      <div>
        <Link
          to="/lotes/$loteId"
          params={{ loteId }}
          className="text-muted-foreground hover:text-foreground inline-flex items-center gap-1 text-sm"
        >
          <ArrowLeft className="size-4" />
          Volver al lote
        </Link>
        <h1 className="mt-2 text-2xl font-semibold tracking-tight">
          Resumen del grupo {lote.data?.nombre ? `· ${lote.data.nombre}` : ''}
        </h1>
      </div>

      {resumen.isPending ? (
        <LoadingRows rows={5} />
      ) : incompleto ? (
        <EmptyState
          icon={<TriangleAlert className="size-8" />}
          title="El resumen aún no está disponible"
          message="Se genera cuando todas las entregas del lote están revisadas y aprobadas."
        />
      ) : resumen.isError ? (
        <ErrorState message="No pudimos cargar el resumen." onRetry={() => resumen.refetch()} />
      ) : (
        <ResumenGrupoView
          resumen={resumen.data}
          mensajeSinNarrativa="Genera un resumen en lenguaje natural del desempeño del grupo (útil para informes o reuniones de coordinación)."
          narrativaAccion={
            <Button
              variant="outline"
              size="sm"
              onClick={onGenerarNarrativa}
              disabled={narrativa.isPending}
            >
              <Sparkles />
              {narrativa.isPending
                ? 'Generando…'
                : resumen.data.narrativa
                  ? 'Regenerar'
                  : 'Generar narrativa'}
            </Button>
          }
        />
      )}
    </section>
  )
}
