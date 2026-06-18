import { createFileRoute, Link } from '@tanstack/react-router'
import { toast } from 'sonner'
import { ArrowLeft, BarChart3, Sparkles, TriangleAlert } from 'lucide-react'

import { ApiError } from '@/api/errors'
import { useLote } from '@/features/lotes/hooks'
import { useGenerarNarrativa, useResumen } from '@/features/reportes/hooks'
import { Histograma } from '@/features/reportes/histograma'
import { MapaDominio } from '@/features/reportes/mapa-dominio'
import { EmptyState, ErrorState, LoadingRows } from '@/components/states'
import { Badge } from '@/components/ui/badge'
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
        <Contenido
          resumen={resumen.data}
          generando={narrativa.isPending}
          onGenerarNarrativa={onGenerarNarrativa}
        />
      )}
    </section>
  )
}

function Contenido({
  resumen,
  generando,
  onGenerarNarrativa,
}: {
  resumen: NonNullable<ReturnType<typeof useResumen>['data']>
  generando: boolean
  onGenerarNarrativa: () => void
}) {
  const stats = resumen.estadisticas
  const dificiles = resumen.criteriosDificiles ?? []

  return (
    <div className="space-y-8">
      <p className="text-muted-foreground text-sm">
        {resumen.totalEvaluaciones} {resumen.totalEvaluaciones === 1 ? 'entrega' : 'entregas'}{' '}
        evaluadas · puntaje máximo {resumen.puntajeTotalRubrica}
      </p>

      {stats ? (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <Estadistica etiqueta="Promedio" valor={stats.promedio} />
          <Estadistica etiqueta="Mediana" valor={stats.mediana} />
          <Estadistica etiqueta="Mínima" valor={stats.minima} />
          <Estadistica etiqueta="Máxima" valor={stats.maxima} />
        </div>
      ) : null}

      {stats?.histograma && stats.histograma.length > 0 ? (
        <div>
          <h2 className="mb-3 flex items-center gap-2 text-lg font-medium">
            <BarChart3 className="text-muted-foreground size-5" aria-hidden />
            Distribución de notas
          </h2>
          <Histograma rangos={stats.histograma} />
        </div>
      ) : null}

      {dificiles.length > 0 ? (
        <div>
          <h2 className="mb-2 text-lg font-medium">Criterios con mayor dificultad</h2>
          <div className="flex flex-wrap gap-2">
            {dificiles.map((nombre) => (
              <Badge key={nombre} className="bg-estado-evaluando text-white">
                {nombre}
              </Badge>
            ))}
          </div>
        </div>
      ) : null}

      {resumen.criterios && resumen.criterios.length > 0 ? (
        <div>
          <h2 className="mb-3 text-lg font-medium">Mapa de dominio por criterio</h2>
          <MapaDominio criterios={resumen.criterios} />
        </div>
      ) : null}

      <div>
        <div className="mb-3 flex items-center justify-between gap-2">
          <h2 className="flex items-center gap-2 text-lg font-medium">
            <Sparkles className="text-muted-foreground size-5" aria-hidden />
            Narrativa
          </h2>
          <Button variant="outline" size="sm" onClick={onGenerarNarrativa} disabled={generando}>
            <Sparkles />
            {generando ? 'Generando…' : resumen.narrativa ? 'Regenerar' : 'Generar narrativa'}
          </Button>
        </div>
        {resumen.narrativa ? (
          <p className="prose-entrega border-border bg-card rounded-lg border p-4 text-sm leading-relaxed">
            {resumen.narrativa}
          </p>
        ) : (
          <p className="text-muted-foreground text-sm">
            Genera un resumen en lenguaje natural del desempeño del grupo (útil para informes o
            reuniones de coordinación).
          </p>
        )}
      </div>
    </div>
  )
}

function Estadistica({ etiqueta, valor }: { etiqueta: string; valor: number | undefined }) {
  return (
    <div className="border-border bg-card rounded-lg border p-3">
      <p className="text-muted-foreground text-xs">{etiqueta}</p>
      <p className="mt-0.5 text-xl font-semibold tabular-nums">{valor?.toFixed(2) ?? '—'}</p>
    </div>
  )
}
