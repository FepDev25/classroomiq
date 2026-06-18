import { useEffect } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'

import { ApiError } from '@/api/errors'
import { useMaterias } from '@/features/materias/hooks'
import { useRubricas } from '@/features/rubricas/api'
import { useCrearLote } from './hooks'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

const loteSchema = z.object({
  nombre: z.string().trim().min(1, 'El lote necesita un nombre'),
  materiaId: z.string().uuid('Elige una materia'),
  rubricaId: z.string().uuid('Elige una rúbrica'),
})

type LoteValues = z.infer<typeof loteSchema>

export function LoteFormDialog({
  open,
  onOpenChange,
  materiaIdInicial,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Si se abre desde una materia, la preselecciona. */
  materiaIdInicial?: string
}) {
  const materias = useMaterias()
  const crear = useCrearLote()

  const form = useForm<LoteValues>({
    resolver: zodResolver(loteSchema),
    defaultValues: { nombre: '', materiaId: materiaIdInicial ?? '', rubricaId: '' },
  })

  const materiaId = useWatch({ control: form.control, name: 'materiaId' })
  const rubricas = useRubricas(materiaId)

  useEffect(() => {
    if (open) {
      form.reset({ nombre: '', materiaId: materiaIdInicial ?? '', rubricaId: '' })
    }
  }, [open, materiaIdInicial, form])

  const materiasActivas = (materias.data ?? []).filter((m) => !m.archivada)

  function onSubmit(values: LoteValues) {
    crear.mutate(
      { nombre: values.nombre.trim(), materiaId: values.materiaId, rubricaId: values.rubricaId },
      {
        onSuccess: () => {
          toast.success('Lote creado')
          onOpenChange(false)
        },
        onError: (error: unknown) => {
          toast.error(error instanceof ApiError ? error.message : 'No pudimos crear el lote.')
        },
      },
    )
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Nuevo lote de entregas</DialogTitle>
          <DialogDescription>
            Un lote agrupa las entregas de un trabajo y se evalúa con una rúbrica de la materia.
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="nombre"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Nombre</FormLabel>
                  <FormControl>
                    <Input placeholder="Proyecto Final — Grupo A" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="materiaId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Materia</FormLabel>
                  <Select
                    value={field.value || undefined}
                    onValueChange={(value) => {
                      field.onChange(value)
                      // Cambiar de materia invalida la rúbrica elegida.
                      form.setValue('rubricaId', '')
                    }}
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Elige una materia" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {materiasActivas.map((m) => (
                        <SelectItem key={m.id} value={m.id ?? ''}>
                          {m.nombre}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="rubricaId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Rúbrica</FormLabel>
                  <Select
                    value={field.value || undefined}
                    onValueChange={field.onChange}
                    disabled={!materiaId || rubricas.isPending}
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue
                          placeholder={
                            !materiaId
                              ? 'Elige primero una materia'
                              : rubricas.isPending
                                ? 'Cargando rúbricas…'
                                : (rubricas.data?.length ?? 0) === 0
                                  ? 'Esta materia no tiene rúbricas'
                                  : 'Elige una rúbrica'
                          }
                        />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {(rubricas.data ?? []).map((r) => (
                        <SelectItem key={r.id} value={r.id ?? ''}>
                          {r.nombre}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            <DialogFooter>
              <Button
                type="button"
                variant="ghost"
                onClick={() => onOpenChange(false)}
                disabled={crear.isPending}
              >
                Cancelar
              </Button>
              <Button type="submit" disabled={crear.isPending}>
                {crear.isPending ? 'Creando…' : 'Crear lote'}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
