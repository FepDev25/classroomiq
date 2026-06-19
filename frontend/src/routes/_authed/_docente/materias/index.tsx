import { useMemo, useState } from 'react'
import { createFileRoute, Link, useNavigate } from '@tanstack/react-router'
import {
  createColumnHelper,
  flexRender,
  getCoreRowModel,
  getSortedRowModel,
  useReactTable,
  type SortingState,
} from '@tanstack/react-table'
import { toast } from 'sonner'
import { BookOpen, ChevronRight, MoreHorizontal, Pencil, Plus, Archive } from 'lucide-react'

import { ApiError } from '@/api/errors'
import { useArchivarMateria, useMaterias } from '@/features/materias/hooks'
import type { Materia } from '@/features/materias/api'
import { MateriaFormDialog } from '@/features/materias/materia-form-dialog'
import { EmptyState, ErrorState, LoadingRows } from '@/components/states'
import { Badge } from '@/components/ui/badge'
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
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'

export const Route = createFileRoute('/_authed/_docente/materias/')({
  component: MateriasPage,
})

const columnHelper = createColumnHelper<Materia>()

function MateriasPage() {
  const navigate = useNavigate()
  const { data, isPending, isError, refetch } = useMaterias()
  const archivar = useArchivarMateria()

  const [tab, setTab] = useState<'activas' | 'archivadas'>('activas')
  const [sorting, setSorting] = useState<SortingState>([])
  const [formOpen, setFormOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<Materia | undefined>(undefined)
  const [archivarTarget, setArchivarTarget] = useState<Materia | undefined>(undefined)

  const materias = useMemo(
    () => (data ?? []).filter((m) => (tab === 'activas' ? !m.archivada : m.archivada)),
    [data, tab],
  )

  const columns = useMemo(
    () => [
      columnHelper.accessor('nombre', {
        header: 'Materia',
        cell: (info) => (
          <Link
            to="/materias/$materiaId"
            params={{ materiaId: info.row.original.id ?? '' }}
            className="text-foreground hover:text-primary font-medium hover:underline"
          >
            {info.getValue()}
          </Link>
        ),
      }),
      columnHelper.accessor('periodoAcademico', {
        header: 'Período',
        cell: (info) => <span className="text-muted-foreground">{info.getValue() || '—'}</span>,
      }),
      columnHelper.accessor('archivada', {
        header: 'Estado',
        cell: (info) =>
          info.getValue() ? (
            <Badge variant="secondary">Archivada</Badge>
          ) : (
            <Badge className="bg-estado-listo text-white">Activa</Badge>
          ),
      }),
      columnHelper.display({
        id: 'acciones',
        header: () => <span className="sr-only">Acciones</span>,
        cell: (info) => {
          const materia = info.row.original
          return (
            // Detiene la propagación para que ni el menú ni el "ghost click"
            // que Radix emite al cerrarse disparen la navegación de la fila.
            <div
              className="flex items-center justify-end gap-1"
              onClick={(e) => e.stopPropagation()}
            >
              {!materia.archivada ? (
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      aria-label={`Acciones de ${materia.nombre}`}
                      onClick={(e) => e.stopPropagation()}
                    >
                      <MoreHorizontal />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem
                      onSelect={() => {
                        setEditTarget(materia)
                        setFormOpen(true)
                      }}
                    >
                      <Pencil />
                      Editar
                    </DropdownMenuItem>
                    <DropdownMenuItem
                      variant="destructive"
                      onSelect={() => setArchivarTarget(materia)}
                    >
                      <Archive />
                      Archivar
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              ) : null}
              <ChevronRight className="text-muted-foreground size-4 shrink-0" aria-hidden />
            </div>
          )
        },
      }),
    ],
    [],
  )

  // El React Compiler avisa que useReactTable devuelve funciones no memoizables;
  // aquí no las pasamos a componentes memoizados, así que es seguro.
  // eslint-disable-next-line react-hooks/incompatible-library
  const table = useReactTable({
    data: materias,
    columns,
    state: { sorting },
    onSortingChange: setSorting,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
  })

  function confirmarArchivar() {
    const materia = archivarTarget
    if (!materia?.id) return
    archivar.mutate(materia.id, {
      onSuccess: () => {
        toast.success('Materia archivada')
        setArchivarTarget(undefined)
      },
      onError: (error: unknown) => {
        toast.error(error instanceof ApiError ? error.message : 'No pudimos archivar la materia.')
        setArchivarTarget(undefined)
      },
    })
  }

  return (
    <section className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Materias</h1>
          <p className="text-muted-foreground mt-1 text-sm">
            Tus materias agrupan rúbricas y lotes de entregas.
          </p>
        </div>
        <Button
          onClick={() => {
            setEditTarget(undefined)
            setFormOpen(true)
          }}
        >
          <Plus />
          Nueva materia
        </Button>
      </div>

      <Tabs value={tab} onValueChange={(v) => setTab(v as typeof tab)}>
        <TabsList>
          <TabsTrigger value="activas">Activas</TabsTrigger>
          <TabsTrigger value="archivadas">Archivadas</TabsTrigger>
        </TabsList>
      </Tabs>

      {isPending ? (
        <LoadingRows rows={5} />
      ) : isError ? (
        <ErrorState
          title="No pudimos cargar las materias."
          message="Revisa tu conexión e inténtalo de nuevo."
          onRetry={() => refetch()}
        />
      ) : materias.length === 0 ? (
        <EmptyState
          icon={<BookOpen className="size-8" />}
          title={tab === 'activas' ? 'Aún no tienes materias' : 'No hay materias archivadas'}
          message={
            tab === 'activas'
              ? 'Crea tu primera materia para empezar a definir rúbricas.'
              : 'Las materias que archives aparecerán aquí.'
          }
          action={
            tab === 'activas' ? (
              <Button
                onClick={() => {
                  setEditTarget(undefined)
                  setFormOpen(true)
                }}
              >
                <Plus />
                Nueva materia
              </Button>
            ) : undefined
          }
        />
      ) : (
        <div className="border-border overflow-hidden rounded-lg border">
          <Table>
            <TableHeader>
              {table.getHeaderGroups().map((hg) => (
                <TableRow key={hg.id}>
                  {hg.headers.map((header) => (
                    <TableHead key={header.id}>
                      {header.isPlaceholder
                        ? null
                        : flexRender(header.column.columnDef.header, header.getContext())}
                    </TableHead>
                  ))}
                </TableRow>
              ))}
            </TableHeader>
            <TableBody>
              {table.getRowModel().rows.map((row) => (
                <TableRow
                  key={row.id}
                  className="hover:bg-muted/50 cursor-pointer"
                  onClick={() => {
                    if (!row.original.id) return
                    navigate({
                      to: '/materias/$materiaId',
                      params: { materiaId: row.original.id },
                    })
                  }}
                >
                  {row.getVisibleCells().map((cell) => (
                    <TableCell key={cell.id}>
                      {flexRender(cell.column.columnDef.cell, cell.getContext())}
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <MateriaFormDialog materia={editTarget} open={formOpen} onOpenChange={setFormOpen} />

      <AlertDialog
        open={Boolean(archivarTarget)}
        onOpenChange={(open) => !open && setArchivarTarget(undefined)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>¿Archivar “{archivarTarget?.nombre}”?</AlertDialogTitle>
            <AlertDialogDescription>
              La materia y sus datos se conservan para referencia histórica, pero dejará de aparecer
              en la vista activa. Esta acción no se puede deshacer desde la app.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={archivar.isPending}>Cancelar</AlertDialogCancel>
            <AlertDialogAction onClick={confirmarArchivar} disabled={archivar.isPending}>
              {archivar.isPending ? 'Archivando…' : 'Archivar'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </section>
  )
}
