import { useState } from 'react'
import { createFileRoute, redirect, useNavigate } from '@tanstack/react-router'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Loader2 } from 'lucide-react'

import { ApiError } from '@/api/errors'
import { login, toSession } from '@/features/auth/api'
import { useAuth } from '@/features/auth/auth-context'
import { MarginMark } from '@/components/brand'
import { ThemeToggle } from '@/components/theme-toggle'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'

const loginSchema = z.object({
  email: z.string().min(1, 'Ingresa tu email').email('Ingresa un email válido'),
  password: z.string().min(1, 'Ingresa tu contraseña'),
})

type LoginValues = z.infer<typeof loginSchema>

const loginSearchSchema = z.object({
  redirect: z.string().optional(),
})

/** Solo permite rutas internas como destino — evita open-redirects. */
function destinoSeguro(redirectTo: string | undefined): string {
  return redirectTo && redirectTo.startsWith('/') && !redirectTo.startsWith('//')
    ? redirectTo
    : '/'
}

export const Route = createFileRoute('/login')({
  validateSearch: loginSearchSchema,
  beforeLoad: ({ context, search }) => {
    if (context.auth.isAuthenticated) {
      throw redirect({ to: destinoSeguro(search.redirect) })
    }
  },
  component: LoginPage,
})

function LoginPage() {
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const search = Route.useSearch()
  const [formError, setFormError] = useState<string | null>(null)

  const form = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  })

  const mutation = useMutation({
    mutationFn: (values: LoginValues) => login(values),
    onSuccess: (token, values) => {
      signIn(toSession(token, values.email))
      navigate({ to: destinoSeguro(search.redirect) })
    },
    onError: (error: unknown) => {
      if (error instanceof ApiError && error.status === 401) {
        setFormError('Email o contraseña incorrectos.')
      } else if (error instanceof ApiError) {
        setFormError(error.message)
      } else {
        setFormError('No pudimos conectar con el servidor. Intenta de nuevo.')
      }
    },
  })

  function onSubmit(values: LoginValues) {
    setFormError(null)
    mutation.mutate(values)
  }

  return (
    <div className="bg-background text-foreground flex min-h-svh flex-col">
      <header className="flex items-center justify-between px-4 py-4 sm:px-6">
        <span className="flex items-center gap-2">
          <MarginMark className="text-primary" />
          <span className="font-sans text-base font-semibold tracking-tight">
            classroom<span className="text-decision">iq</span>
          </span>
        </span>
        <ThemeToggle />
      </header>

      <main className="flex flex-1 items-center justify-center px-4 py-8">
        <div className="w-full max-w-sm">
          <p className="text-muted-foreground font-mono text-xs tracking-[0.2em] uppercase">
            Asistencia a evaluación docente
          </p>
          <h1 className="mt-3 text-2xl font-semibold tracking-tight">Entra a tu cuenta</h1>
          <p className="text-muted-foreground mt-1 text-sm">
            Tus credenciales las crea el administrador de tu institución.
          </p>

          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="mt-6 space-y-4" noValidate>
              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Email</FormLabel>
                    <FormControl>
                      <Input
                        type="email"
                        autoComplete="email"
                        placeholder="docente@institucion.edu"
                        // El autofocus en el único campo de una pantalla de
                        // login dedicada mejora la UX sin perjudicar la a11y.
                        // eslint-disable-next-line jsx-a11y/no-autofocus
                        autoFocus
                        {...field}
                      />
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
                    <FormLabel>Contraseña</FormLabel>
                    <FormControl>
                      <Input type="password" autoComplete="current-password" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {formError ? (
                <p role="alert" className="text-destructive text-sm">
                  {formError}
                </p>
              ) : null}

              <Button type="submit" className="w-full" disabled={mutation.isPending}>
                {mutation.isPending ? (
                  <>
                    <Loader2 className="animate-spin" />
                    Entrando…
                  </>
                ) : (
                  'Entrar'
                )}
              </Button>
            </form>
          </Form>

          <p className="text-muted-foreground mt-8 border-t pt-4 text-xs">
            classroomiq genera borradores de evaluación como asistencia al docente. La nota final es
            responsabilidad exclusiva del profesor.
          </p>
        </div>
      </main>
    </div>
  )
}
