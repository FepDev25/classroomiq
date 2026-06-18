import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'

import { ApiError } from '@/api/errors'
import { useCrearUsuario } from './hooks'
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

const usuarioSchema = z.object({
  nombre: z.string().trim().min(1, 'Ingresa un nombre'),
  email: z.string().trim().min(1, 'Ingresa un email').email('Ingresa un email válido'),
  password: z.string().min(8, 'Mínimo 8 caracteres'),
  rol: z.enum(['DOCENTE', 'COORDINADOR']),
})

type UsuarioValues = z.infer<typeof usuarioSchema>

/** Crea una cuenta de docente o coordinador. El admin no se crea por esta vía (lo dice el backend). */
export function UsuarioFormDialog({
  open,
  onOpenChange,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const crear = useCrearUsuario()

  const form = useForm<UsuarioValues>({
    resolver: zodResolver(usuarioSchema),
    defaultValues: { nombre: '', email: '', password: '', rol: 'DOCENTE' },
  })

  // form.reset también limpia los errores (incluido el `root`) al reabrir.
  useEffect(() => {
    if (open) form.reset({ nombre: '', email: '', password: '', rol: 'DOCENTE' })
  }, [open, form])

  const formError = form.formState.errors.root?.message

  function onSubmit(values: UsuarioValues) {
    form.clearErrors('root')
    crear.mutate(
      {
        nombre: values.nombre.trim(),
        email: values.email.trim(),
        password: values.password,
        rol: values.rol,
      },
      {
        onSuccess: () => {
          toast.success('Cuenta creada')
          onOpenChange(false)
        },
        onError: (error: unknown) => {
          if (error instanceof ApiError && error.status === 409) {
            form.setError('email', { message: 'Ya existe una cuenta con ese email' })
          } else if (error instanceof ApiError) {
            form.setError('root', { message: error.message })
          } else {
            form.setError('root', { message: 'No pudimos crear la cuenta.' })
          }
        },
      },
    )
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Nueva cuenta</DialogTitle>
          <DialogDescription>
            Crea una cuenta de docente o coordinador. La persona inicia sesión con estas credenciales.
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
                    <Input placeholder="Ada Lovelace" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="email"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Email</FormLabel>
                  <FormControl>
                    <Input type="email" autoComplete="off" placeholder="docente@institucion.edu" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="password"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Contraseña inicial</FormLabel>
                  <FormControl>
                    <Input type="text" autoComplete="off" placeholder="Mínimo 8 caracteres" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="rol"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Rol</FormLabel>
                  <Select value={field.value} onValueChange={field.onChange}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value="DOCENTE">Docente</SelectItem>
                      <SelectItem value="COORDINADOR">Coordinador</SelectItem>
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            {formError ? (
              <p role="alert" className="text-destructive text-sm">
                {formError}
              </p>
            ) : null}

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
                {crear.isPending ? 'Creando…' : 'Crear cuenta'}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
