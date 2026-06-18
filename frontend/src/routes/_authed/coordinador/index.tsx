import { createFileRoute, Link } from '@tanstack/react-router'
import { BookOpen, ChevronRight } from 'lucide-react'

import { useMaterias } from '@/features/coordinador/hooks'
import { EmptyState, ErrorState, LoadingRows } from '@/components/states'
import { Badge } from '@/components/ui/badge'

export const Route = createFileRoute('/_authed/coordinador/')({
  component: CoordinadorHome,
})

function CoordinadorHome() {
  const materias = useMaterias()

  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Materias asignadas</h1>
        <p className="text-muted-foreground mt-1 text-sm">
          Acceso de solo lectura a los reportes agregados por grupo. Nunca verás trabajos ni
          evaluaciones individuales.
        </p>
      </div>

      {materias.isPending ? (
        <LoadingRows rows={4} />
      ) : materias.isError ? (
        <ErrorState message="No pudimos cargar tus materias." onRetry={() => materias.refetch()} />
      ) : materias.data.length === 0 ? (
        <EmptyState
          icon={<BookOpen className="size-8" />}
          title="Sin materias asignadas"
          message="El administrador aún no te ha asignado materias. Cuando lo haga, sus reportes aparecerán aquí."
        />
      ) : (
        <ul className="space-y-2">
          {materias.data.map((m) => (
            <li key={m.id}>
              <Link
                to="/coordinador/materias/$materiaId"
                params={{ materiaId: m.id ?? '' }}
                className="border-border bg-card hover:bg-accent focus-visible:ring-ring/50 flex items-center justify-between gap-3 rounded-lg border p-4 transition-colors focus-visible:ring-2 focus-visible:outline-none"
              >
                <div className="min-w-0">
                  <p className="truncate font-medium">{m.nombre}</p>
                  {m.periodoAcademico ? (
                    <p className="text-muted-foreground truncate text-sm">{m.periodoAcademico}</p>
                  ) : null}
                </div>
                <div className="flex items-center gap-2">
                  {m.archivada ? <Badge variant="secondary">Archivada</Badge> : null}
                  <ChevronRight className="text-muted-foreground size-5 shrink-0" aria-hidden />
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
