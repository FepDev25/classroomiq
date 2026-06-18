import { useMemo, useState } from 'react'
import { createFileRoute, Link } from '@tanstack/react-router'
import { toast } from 'sonner'
import { BookOpen, Trash2, UserCog } from 'lucide-react'

import { ApiError } from '@/api/errors'
import {
  useAsignarMateria,
  useDesasignarMateria,
  useMateriasAdmin,
  useMateriasAsignadas,
  useUsuarios,
} from '@/features/admin/hooks'
import type { AdminMateria } from '@/features/admin/api'
import { EmptyState, ErrorState, LoadingRows } from '@/components/states'
import { Button } from '@/components/ui/button'
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

export const Route = createFileRoute('/_authed/admin/coordinadores')({
  component: CoordinadoresPage,
})

function CoordinadoresPage() {
  const usuarios = useUsuarios()
  const coordinadores = useMemo(
    () => (usuarios.data ?? []).filter((u) => u.rol === 'COORDINADOR'),
    [usuarios.data],
  )

  // El primero queda seleccionado por defecto sin tocar estado (evita setState en efecto).
  const [coordId, setCoordId] = useState('')
  const coordSeleccionado = coordId || coordinadores[0]?.id || ''

  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Coordinadores</h1>
        <p className="text-muted-foreground mt-1 text-sm">
          Asigna materias a un coordinador para darle acceso de solo lectura a sus reportes
          agregados. Nunca verá trabajos ni evaluaciones individuales.
        </p>
      </div>

      {usuarios.isPending ? (
        <LoadingRows rows={4} />
      ) : usuarios.isError ? (
        <ErrorState message="No pudimos cargar las cuentas." onRetry={() => usuarios.refetch()} />
      ) : coordinadores.length === 0 ? (
        <EmptyState
          icon={<UserCog className="size-8" />}
          title="No hay coordinadores"
          message="Crea una cuenta con rol Coordinador para poder asignarle materias."
          action={
            <Button asChild>
              <Link to="/admin/cuentas">Ir a Cuentas</Link>
            </Button>
          }
        />
      ) : (
        <>
          <div className="max-w-sm space-y-2">
            <label className="text-sm font-medium" htmlFor="coordinador">
              Coordinador
            </label>
            <Select value={coordSeleccionado} onValueChange={setCoordId}>
              <SelectTrigger id="coordinador">
                <SelectValue placeholder="Elige un coordinador" />
              </SelectTrigger>
              <SelectContent>
                {coordinadores.map((c) => (
                  <SelectItem key={c.id} value={c.id ?? ''}>
                    {c.nombre} · {c.email}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {coordSeleccionado ? <AsignacionPanel coordinadorId={coordSeleccionado} /> : null}
        </>
      )}
    </section>
  )
}

function AsignacionPanel({ coordinadorId }: { coordinadorId: string }) {
  const asignadas = useMateriasAsignadas(coordinadorId)
  const catalogo = useMateriasAdmin()
  const asignar = useAsignarMateria(coordinadorId)
  const desasignar = useDesasignarMateria(coordinadorId)

  const [pick, setPick] = useState('')

  const asignadasIds = useMemo(
    () => new Set((asignadas.data ?? []).map((m) => m.id)),
    [asignadas.data],
  )

  // Catálogo disponible (no asignado aún), agrupado por docente dueño.
  const porDocente = useMemo(() => {
    const grupos = new Map<string, AdminMateria[]>()
    for (const m of catalogo.data ?? []) {
      if (asignadasIds.has(m.id)) continue
      const docente = m.docenteNombre ?? 'Sin docente'
      const lista = grupos.get(docente) ?? []
      lista.push(m)
      grupos.set(docente, lista)
    }
    return [...grupos.entries()].sort((a, b) => a[0].localeCompare(b[0]))
  }, [catalogo.data, asignadasIds])

  function onAsignar() {
    if (!pick) return
    asignar.mutate(pick, {
      onSuccess: () => {
        toast.success('Materia asignada')
        setPick('')
      },
      onError: (error: unknown) =>
        toast.error(error instanceof ApiError ? error.message : 'No pudimos asignar la materia.'),
    })
  }

  function onDesasignar(materiaId: string) {
    desasignar.mutate(materiaId, {
      onSuccess: () => toast.success('Materia desasignada'),
      onError: (error: unknown) =>
        toast.error(error instanceof ApiError ? error.message : 'No pudimos desasignar la materia.'),
    })
  }

  const hayDisponibles = porDocente.length > 0

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <div className="space-y-3">
        <h2 className="text-lg font-medium">Materias asignadas</h2>
        {asignadas.isPending ? (
          <LoadingRows rows={2} />
        ) : asignadas.isError ? (
          <ErrorState message="No pudimos cargar las asignaciones." onRetry={() => asignadas.refetch()} />
        ) : (asignadas.data?.length ?? 0) === 0 ? (
          <EmptyState
            icon={<BookOpen className="size-7" />}
            title="Sin materias asignadas"
            message="Este coordinador aún no tiene acceso a ningún reporte."
          />
        ) : (
          <ul className="space-y-2">
            {asignadas.data!.map((m) => (
              <li
                key={m.id}
                className="border-border bg-card flex items-center justify-between gap-2 rounded-lg border p-3"
              >
                <div className="min-w-0">
                  <p className="truncate font-medium">{m.nombre}</p>
                  {m.periodoAcademico ? (
                    <p className="text-muted-foreground truncate text-sm">{m.periodoAcademico}</p>
                  ) : null}
                </div>
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label={`Desasignar ${m.nombre}`}
                  onClick={() => m.id && onDesasignar(m.id)}
                  disabled={desasignar.isPending}
                  className="text-muted-foreground hover:text-destructive"
                >
                  <Trash2 className="size-4" />
                </Button>
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="space-y-3">
        <h2 className="text-lg font-medium">Asignar una materia</h2>
        {catalogo.isPending ? (
          <LoadingRows rows={2} />
        ) : catalogo.isError ? (
          <ErrorState message="No pudimos cargar el catálogo." onRetry={() => catalogo.refetch()} />
        ) : !hayDisponibles ? (
          <p className="text-muted-foreground text-sm">
            No hay materias disponibles para asignar (todas ya están asignadas a este coordinador, o
            aún no existen materias).
          </p>
        ) : (
          <div className="flex flex-wrap items-end gap-2">
            <div className="min-w-64 flex-1 space-y-2">
              <label className="text-sm font-medium" htmlFor="materia">
                Materia
              </label>
              <Select value={pick} onValueChange={setPick}>
                <SelectTrigger id="materia">
                  <SelectValue placeholder="Elige una materia" />
                </SelectTrigger>
                <SelectContent>
                  {porDocente.map(([docente, materias]) => (
                    <SelectGroup key={docente}>
                      <SelectLabel>{docente}</SelectLabel>
                      {materias.map((m) => (
                        <SelectItem key={m.id} value={m.id ?? ''}>
                          {m.nombre}
                          {m.archivada ? ' (archivada)' : ''}
                        </SelectItem>
                      ))}
                    </SelectGroup>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <Button onClick={onAsignar} disabled={!pick || asignar.isPending}>
              {asignar.isPending ? 'Asignando…' : 'Asignar'}
            </Button>
          </div>
        )}
      </div>
    </div>
  )
}
