import { useMemo, useState, type ReactNode } from 'react'
import { createFileRoute, Link, redirect } from '@tanstack/react-router'
import { BookOpen, Boxes, ClipboardCheck, Loader2, Plus } from 'lucide-react'

import { useMaterias } from '@/features/materias/hooks'
import { useLotes } from '@/features/lotes/hooks'
import { MateriaFormDialog } from '@/features/materias/materia-form-dialog'
import { LoteFormDialog } from '@/features/lotes/lote-form-dialog'
import { EstadoLoteBadge } from '@/features/lotes/badges'
import type { Lote } from '@/features/lotes/api'
import { EmptyState, ErrorState, LoadingRows } from '@/components/states'
import { MarginMark } from '@/components/brand'
import { Button } from '@/components/ui/button'

/**
 * Dispatcher de la raíz autenticada: enruta por rol. ADMIN y COORDINADOR van a
 * la home de su portal; DOCENTE se queda aquí y ve su panel. Así el destino
 * post-login (que siempre cae en `/`) depende del rol del token.
 */
export const Route = createFileRoute('/_authed/')({
  beforeLoad: ({ context }) => {
    const rol = context.auth.rol
    if (rol === 'ADMIN') throw redirect({ to: '/admin' })
    if (rol === 'COORDINADOR') throw redirect({ to: '/coordinador' })
  },
  component: Home,
})

function Home() {
  const materias = useMaterias()
  const lotes = useLotes()

  const [materiaOpen, setMateriaOpen] = useState(false)
  const [loteOpen, setLoteOpen] = useState(false)

  const nombreMateria = useMemo(() => {
    const map = new Map<string, string>()
    for (const m of materias.data ?? []) if (m.id) map.set(m.id, m.nombre ?? '')
    return map
  }, [materias.data])

  const materiasActivas = (materias.data ?? []).filter((m) => !m.archivada)
  const todosLotes = lotes.data ?? []
  const listos = todosLotes.filter((l) => l.estado === 'LISTO')
  const enCurso = todosLotes.filter((l) => l.estado !== 'LISTO')

  const cargando = materias.isPending || lotes.isPending
  const conError = materias.isError || lotes.isError

  return (
    <section className="space-y-8">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-muted-foreground font-mono text-xs tracking-[0.2em] uppercase">
            Panel del docente
          </p>
          <h1 className="mt-2 text-3xl font-semibold tracking-tight">Hola de nuevo</h1>
          <p className="text-muted-foreground mt-1 text-sm">
            Tus materias, lotes de entregas y borradores de evaluación, de un vistazo.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" onClick={() => setMateriaOpen(true)}>
            <Plus />
            Nueva materia
          </Button>
          <Button onClick={() => setLoteOpen(true)}>
            <Plus />
            Nuevo lote
          </Button>
        </div>
      </header>

      {cargando ? (
        <LoadingRows rows={5} />
      ) : conError ? (
        <ErrorState
          title="No pudimos cargar tu panel."
          message="Revisa tu conexión e inténtalo de nuevo."
          onRetry={() => {
            materias.refetch()
            lotes.refetch()
          }}
        />
      ) : materiasActivas.length === 0 && todosLotes.length === 0 ? (
        <EmptyState
          icon={<BookOpen className="size-8" />}
          title="Empieza por crear una materia"
          message="Una materia agrupa tus rúbricas y lotes de entregas. Luego crea un lote, sube las entregas y deja que el modelo prepare los borradores."
          action={
            <Button onClick={() => setMateriaOpen(true)}>
              <Plus />
              Nueva materia
            </Button>
          }
        />
      ) : (
        <>
          <div className="grid gap-3 sm:grid-cols-3">
            <StatCard
              to="/materias"
              icon={<BookOpen className="size-5" />}
              valor={materiasActivas.length}
              label={materiasActivas.length === 1 ? 'Materia activa' : 'Materias activas'}
            />
            <StatCard
              to="/lotes"
              icon={<ClipboardCheck className="size-5" />}
              valor={listos.length}
              label={listos.length === 1 ? 'Lote listo para revisar' : 'Lotes listos para revisar'}
              acento={listos.length > 0}
            />
            <StatCard
              to="/lotes"
              icon={<Loader2 className="size-5" />}
              valor={enCurso.length}
              label={enCurso.length === 1 ? 'Lote en curso' : 'Lotes en curso'}
            />
          </div>

          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-medium">Listos para revisar</h2>
              <Link
                to="/lotes"
                className="text-muted-foreground hover:text-foreground text-sm font-medium"
              >
                Ver todos
              </Link>
            </div>
            {listos.length === 0 ? (
              <EmptyState
                icon={<ClipboardCheck className="size-7" />}
                title="Nada pendiente de revisión"
                message="Cuando un lote termine de procesarse y evaluarse, aparecerá aquí para que revises los borradores."
              />
            ) : (
              <ul className="grid gap-3 sm:grid-cols-2">
                {listos.map((lote) => (
                  <LoteCard
                    key={lote.id}
                    lote={lote}
                    materia={lote.materiaId ? nombreMateria.get(lote.materiaId) : undefined}
                    cta="Revisar"
                  />
                ))}
              </ul>
            )}
          </div>

          {enCurso.length > 0 ? (
            <div className="space-y-3">
              <h2 className="text-lg font-medium">En curso</h2>
              <ul className="grid gap-3 sm:grid-cols-2">
                {enCurso.map((lote) => (
                  <LoteCard
                    key={lote.id}
                    lote={lote}
                    materia={lote.materiaId ? nombreMateria.get(lote.materiaId) : undefined}
                    cta="Abrir"
                  />
                ))}
              </ul>
            </div>
          ) : null}
        </>
      )}

      <p className="text-muted-foreground flex items-center gap-2 font-mono text-xs">
        <MarginMark className="text-primary" />
        El modelo propone; tú decides. La nota final es tuya.
      </p>

      <MateriaFormDialog open={materiaOpen} onOpenChange={setMateriaOpen} />
      <LoteFormDialog open={loteOpen} onOpenChange={setLoteOpen} />
    </section>
  )
}

function StatCard({
  to,
  icon,
  valor,
  label,
  acento,
}: {
  to: string
  icon: ReactNode
  valor: number
  label: string
  acento?: boolean
}) {
  return (
    <Link
      to={to}
      className="border-border bg-card hover:border-primary/40 focus-visible:ring-ring/50 flex items-center gap-4 rounded-lg border p-4 transition-colors focus-visible:ring-2 focus-visible:outline-none"
    >
      <span
        className={
          acento
            ? 'bg-primary/10 text-primary flex size-10 items-center justify-center rounded-md'
            : 'bg-secondary text-secondary-foreground flex size-10 items-center justify-center rounded-md'
        }
        aria-hidden
      >
        {icon}
      </span>
      <span>
        <span className="block font-mono text-2xl font-semibold tabular-nums">{valor}</span>
        <span className="text-muted-foreground text-sm">{label}</span>
      </span>
    </Link>
  )
}

function LoteCard({ lote, materia, cta }: { lote: Lote; materia?: string; cta: string }) {
  return (
    <li className="border-border bg-card flex flex-col gap-3 rounded-lg border p-4">
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <Link
            to="/lotes/$loteId"
            params={{ loteId: lote.id ?? '' }}
            className="text-foreground hover:text-primary block truncate font-medium hover:underline"
          >
            {lote.nombre}
          </Link>
          <p className="text-muted-foreground mt-0.5 truncate text-sm">{materia || '—'}</p>
        </div>
        {lote.estado ? <EstadoLoteBadge estado={lote.estado} /> : null}
      </div>
      <Button asChild variant="outline" size="sm" className="self-start">
        <Link to="/lotes/$loteId" params={{ loteId: lote.id ?? '' }}>
          {cta}
          <Boxes />
        </Link>
      </Button>
    </li>
  )
}
