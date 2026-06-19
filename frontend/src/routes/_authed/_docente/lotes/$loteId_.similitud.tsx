import { useMemo, useState } from 'react'
import { createFileRoute, Link } from '@tanstack/react-router'
import { toast } from 'sonner'
import { ArrowLeft, Info, RefreshCw, ScanSearch } from 'lucide-react'

import { ApiError } from '@/api/errors'
import { useLote } from '@/features/lotes/hooks'
import { useGenerarSimilitud, useReporteSimilitud } from '@/features/similitud/hooks'
import { HeatmapSimilitud } from '@/features/similitud/heatmap'
import { ParCard } from '@/features/similitud/par-card'
import { EmptyState, ErrorState, LoadingRows } from '@/components/states'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

export const Route = createFileRoute('/_authed/_docente/lotes/$loteId_/similitud')({
  component: SimilitudPage,
})

const UMBRAL_DEFAULT = 0.75

function SimilitudPage() {
  const { loteId } = Route.useParams()
  const lote = useLote(loteId)
  const reporte = useReporteSimilitud(loteId)
  const generar = useGenerarSimilitud(loteId)

  const [umbral, setUmbral] = useState(UMBRAL_DEFAULT)

  const noGenerado =
    reporte.isError && reporte.error instanceof ApiError && reporte.error.status === 404

  const nombre = useMemo(() => {
    const map = new Map<string, string>()
    for (const e of reporte.data?.entregas ?? []) {
      if (e.entregaId) map.set(e.entregaId, e.identificador ?? '—')
    }
    return map
  }, [reporte.data])

  function onGenerar() {
    generar.mutate(umbral, {
      onSuccess: () => toast.success('Reporte de similitud generado'),
      onError: (error: unknown) => {
        if (error instanceof ApiError && error.status === 422) {
          toast.error(error.message || 'Se necesitan al menos 2 entregas procesadas.')
        } else {
          toast.error('No pudimos generar el reporte.')
        }
      },
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
          Similitud {lote.data?.nombre ? `· ${lote.data.nombre}` : ''}
        </h1>
      </div>

      {reporte.isPending ? (
        <LoadingRows rows={5} />
      ) : noGenerado ? (
        <div className="space-y-4">
          <EmptyState
            icon={<ScanSearch className="size-8" />}
            title="Aún no hay reporte de similitud"
            message="Genera el reporte para comparar las entregas procesadas del lote (se necesitan al menos 2)."
          />
          <div className="border-border bg-card mx-auto flex max-w-sm flex-col gap-3 rounded-lg border p-4">
            <div className="space-y-1.5">
              <Label htmlFor="umbral">Umbral de similitud alta (0–1)</Label>
              <Input
                id="umbral"
                type="number"
                min={0}
                max={1}
                step={0.05}
                value={umbral}
                onChange={(e) => setUmbral(Number(e.target.value))}
              />
              <p className="text-muted-foreground text-xs">
                Los pares por encima del umbral se marcan para revisión manual.
              </p>
            </div>
            <Button onClick={onGenerar} disabled={generar.isPending}>
              <ScanSearch />
              {generar.isPending ? 'Generando…' : 'Generar reporte'}
            </Button>
          </div>
        </div>
      ) : reporte.isError ? (
        <ErrorState
          title="No pudimos cargar el reporte."
          message="Revisa tu conexión e inténtalo de nuevo."
          onRetry={() => reporte.refetch()}
        />
      ) : (
        <Reporte
          reporte={reporte.data}
          nombre={nombre}
          umbral={umbral}
          setUmbral={setUmbral}
          regenerando={generar.isPending}
          onRegenerar={onGenerar}
        />
      )}
    </section>
  )
}

function Reporte({
  reporte,
  nombre,
  umbral,
  setUmbral,
  regenerando,
  onRegenerar,
}: {
  reporte: NonNullable<ReturnType<typeof useReporteSimilitud>['data']>
  nombre: Map<string, string>
  umbral: number
  setUmbral: (v: number) => void
  regenerando: boolean
  onRegenerar: () => void
}) {
  const pares = reporte.pares ?? []
  return (
    <div className="space-y-6">
      {/* Aviso no-acusatorio fijo del backend: se muestra tal cual. */}
      {reporte.aviso ? (
        <p className="border-border text-muted-foreground flex items-start gap-2 rounded-md border border-dashed px-3 py-2 text-sm">
          <Info className="mt-0.5 size-4 shrink-0" aria-hidden />
          {reporte.aviso}
        </p>
      ) : null}

      <div className="flex flex-wrap items-end justify-between gap-3">
        <p className="text-muted-foreground text-sm">
          Umbral actual: <span className="text-foreground font-medium">{reporte.umbral}</span>
        </p>
        <div className="flex items-end gap-2">
          <div className="space-y-1">
            <Label htmlFor="umbral-regen" className="text-xs">
              Umbral
            </Label>
            <Input
              id="umbral-regen"
              type="number"
              min={0}
              max={1}
              step={0.05}
              value={umbral}
              onChange={(e) => setUmbral(Number(e.target.value))}
              className="h-9 w-24"
            />
          </div>
          <Button variant="outline" onClick={onRegenerar} disabled={regenerando}>
            <RefreshCw className={regenerando ? 'animate-spin' : undefined} />
            {regenerando ? 'Regenerando…' : 'Regenerar'}
          </Button>
        </div>
      </div>

      {(reporte.entregas?.length ?? 0) >= 2 ? (
        <div>
          <h2 className="mb-3 text-lg font-medium">Mapa de calor</h2>
          <HeatmapSimilitud entregas={reporte.entregas ?? []} pares={pares} />
        </div>
      ) : null}

      <div>
        <h2 className="mb-3 text-lg font-medium">Pares por similitud</h2>
        {pares.length === 0 ? (
          <p className="text-muted-foreground text-sm">No hay pares para comparar.</p>
        ) : (
          <ul className="space-y-2">
            {pares.map((par, i) => (
              <li key={i}>
                <ParCard
                  par={par}
                  nombreA={nombre.get(par.entregaAId ?? '') ?? '—'}
                  nombreB={nombre.get(par.entregaBId ?? '') ?? '—'}
                />
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
