import { createFileRoute, Link } from '@tanstack/react-router'
import { ArrowLeft, ChevronRight, Layers, Lock } from 'lucide-react'

import { useLotes, useMaterias } from '@/features/coordinador/hooks'
import { EstadoLoteBadge } from '@/features/lotes/badges'
import { EmptyState, ErrorState, LoadingRows } from '@/components/states'

export const Route = createFileRoute('/_authed/coordinador/materias/$materiaId')({
  component: LotesMateriaPage,
})

function LotesMateriaPage() {
  const { materiaId } = Route.useParams()
  const materias = useMaterias()
  const lotes = useLotes(materiaId)

  const materia = materias.data?.find((m) => m.id === materiaId)

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
        <h1 className="mt-2 text-2xl font-semibold tracking-tight">
          {materia?.nombre ?? 'Lotes de la materia'}
        </h1>
        <p className="text-muted-foreground mt-1 text-sm">
          Elige un lote para ver su resumen agregado por grupo.
        </p>
      </div>

      {lotes.isPending ? (
        <LoadingRows rows={4} />
      ) : lotes.isError ? (
        <ErrorState message="No pudimos cargar los lotes." onRetry={() => lotes.refetch()} />
      ) : lotes.data.length === 0 ? (
        <EmptyState
          icon={<Layers className="size-8" />}
          title="Sin lotes"
          message="Esta materia aún no tiene lotes de entregas."
        />
      ) : (
        <ul className="space-y-2">
          {lotes.data.map((lote) => {
            const listo = lote.estado === 'LISTO'
            const contenido = (
              <>
                <div className="flex min-w-0 items-center gap-3">
                  <span className="truncate font-medium">{lote.nombre}</span>
                  {lote.estado ? <EstadoLoteBadge estado={lote.estado} /> : null}
                </div>
                {listo ? (
                  <ChevronRight className="text-muted-foreground size-5 shrink-0" aria-hidden />
                ) : (
                  <span className="text-muted-foreground flex items-center gap-1 text-xs">
                    <Lock className="size-3.5" aria-hidden />
                    Resumen al completar
                  </span>
                )}
              </>
            )
            return (
              <li key={lote.id}>
                {listo ? (
                  <Link
                    to="/coordinador/lotes/$loteId/resumen"
                    params={{ loteId: lote.id ?? '' }}
                    className="border-border bg-card hover:bg-accent focus-visible:ring-ring/50 flex items-center justify-between gap-3 rounded-lg border p-4 transition-colors focus-visible:ring-2 focus-visible:outline-none"
                  >
                    {contenido}
                  </Link>
                ) : (
                  <div className="border-border bg-card flex items-center justify-between gap-3 rounded-lg border p-4 opacity-70">
                    {contenido}
                  </div>
                )}
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
