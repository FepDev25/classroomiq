import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'

import { ApiError } from '@/api/errors'
import { useActualizarMateria, useCrearMateria } from './hooks'
import type { Materia } from './api'
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
import { Textarea } from '@/components/ui/textarea'

const materiaSchema = z.object({
  nombre: z.string().trim().min(1, 'La materia necesita un nombre'),
  periodoAcademico: z.string().optional(),
  descripcion: z.string().optional(),
})

type MateriaValues = z.infer<typeof materiaSchema>

export function MateriaFormDialog({
  materia,
  open,
  onOpenChange,
}: {
  materia?: Materia
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const esEdicion = Boolean(materia?.id)

  const form = useForm<MateriaValues>({
    resolver: zodResolver(materiaSchema),
    defaultValues: { nombre: '', periodoAcademico: '', descripcion: '' },
  })

  // Rellena (o limpia) el formulario cada vez que se abre.
  useEffect(() => {
    if (open) {
      form.reset({
        nombre: materia?.nombre ?? '',
        periodoAcademico: materia?.periodoAcademico ?? '',
        descripcion: materia?.descripcion ?? '',
      })
    }
  }, [open, materia, form])

  const crear = useCrearMateria()
  const actualizar = useActualizarMateria(materia?.id ?? '')
  const mutation = esEdicion ? actualizar : crear

  function onSubmit(values: MateriaValues) {
    const body = {
      nombre: values.nombre.trim(),
      periodoAcademico: values.periodoAcademico?.trim() || undefined,
      descripcion: values.descripcion?.trim() || undefined,
    }
    mutation.mutate(body, {
      onSuccess: () => {
        toast.success(esEdicion ? 'Materia actualizada' : 'Materia creada')
        onOpenChange(false)
      },
      onError: (error: unknown) => {
        const message = error instanceof ApiError ? error.message : 'No pudimos guardar la materia.'
        toast.error(message)
      },
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{esEdicion ? 'Editar materia' : 'Nueva materia'}</DialogTitle>
          <DialogDescription>
            Una materia agrupa tus rúbricas y lotes de entregas.
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
                    <Input placeholder="Algoritmos y Estructuras de Datos" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="periodoAcademico"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>
                    Período académico <span className="text-muted-foreground">(opcional)</span>
                  </FormLabel>
                  <FormControl>
                    <Input placeholder="2026-1" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="descripcion"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>
                    Descripción <span className="text-muted-foreground">(opcional)</span>
                  </FormLabel>
                  <FormControl>
                    <Textarea rows={3} placeholder="Carrera, comisión, notas…" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <DialogFooter>
              <Button
                type="button"
                variant="ghost"
                onClick={() => onOpenChange(false)}
                disabled={mutation.isPending}
              >
                Cancelar
              </Button>
              <Button type="submit" disabled={mutation.isPending}>
                {mutation.isPending
                  ? 'Guardando…'
                  : esEdicion
                    ? 'Guardar cambios'
                    : 'Crear materia'}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
