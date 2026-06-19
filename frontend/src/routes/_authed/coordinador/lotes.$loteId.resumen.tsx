import { createFileRoute, Link } from '@tanstack/react-router'
import { ArrowLeft, TriangleAlert } from 'lucide-react'

import { ApiError } from '@/api/errors'
import { useResumen } from '@/features/coordinador/hooks'
import { ResumenGrupoView } from '@/features/reportes/resumen-grupo-view'
import { EmptyState, ErrorState, LoadingRows } from '@/components/states'

export const Route = createFileRoute('/_authed/coordinador/lotes/$loteId/resumen')({
  component: ResumenCoordinadorPage,
})

function ResumenCoordinadorPage() {
  const { loteId } = Route.useParams()
  const resumen = useResumen(loteId)

  const incompleto =
    resumen.isError && resumen.error instanceof ApiError && resumen.error.status === 422

  return (
    <section className="space-y-6">
      <div>
        <Link
          to="/coordinador"
          className="text-muted-foreground hover:text-foreground inline-flex items-center gap-1 text-sm"
        >
          <ArrowLeft className="size-4" />
          Volver a materias
        </Link>
        <h1 className="mt-2 text-2xl font-semibold tracking-tight">Resumen del grupo</h1>
        <p className="text-muted-foreground mt-1 text-sm">
          Reporte agregado de solo lectura. No incluye trabajos ni evaluaciones individuales.
        </p>
      </div>

      {resumen.isPending ? (
        <LoadingRows rows={5} />
      ) : incompleto ? (
        <EmptyState
          icon={<TriangleAlert className="size-8" />}
          title="El resumen aún no está disponible"
          message="Se genera cuando el docente ha revisado y aprobado todas las entregas del lote."
        />
      ) : resumen.isError ? (
        <ErrorState
          title="No pudimos cargar el resumen."
          message="Revisa tu conexión e inténtalo de nuevo."
          onRetry={() => resumen.refetch()}
        />
      ) : (
        <ResumenGrupoView
          resumen={resumen.data}
          mensajeSinNarrativa="El docente aún no ha generado una narrativa para este grupo."
        />
      )}
    </section>
  )
}
