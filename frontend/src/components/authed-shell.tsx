import type { ReactNode } from 'react'
import { Link, useNavigate } from '@tanstack/react-router'
import { LogOut } from 'lucide-react'

import { useAuth } from '@/features/auth/auth-context'
import type { Rol } from '@/features/auth/session'
import { Brand } from '@/components/brand'
import { ThemeToggle } from '@/components/theme-toggle'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

const ROL_LABEL: Record<Rol, string> = {
  ADMIN: 'Administrador',
  DOCENTE: 'Docente',
  COORDINADOR: 'Coordinador',
}

/**
 * Cascarón de las vistas autenticadas: cabecera con marca + navegación + menú
 * de usuario, contenido, y pie con el principio inamovible. La navegación por
 * materias/lotes/revisión se amplía a partir de H2.
 */
export function AuthedShell({ children }: { children: ReactNode }) {
  return (
    <div className="bg-background text-foreground flex min-h-svh flex-col">
      <header className="border-border bg-background/80 sticky top-0 z-10 border-b backdrop-blur-sm">
        <div className="mx-auto flex h-14 w-full max-w-6xl items-center justify-between gap-4 px-4 sm:px-6">
          <div className="flex items-center gap-6">
            <Link to="/" className="focus-visible:ring-ring/50 rounded-sm focus-visible:ring-2">
              <Brand />
            </Link>
          </div>
          <div className="flex items-center gap-1">
            <ThemeToggle />
            <UserMenu />
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6">{children}</main>

      <footer className="border-border border-t">
        <div className="mx-auto w-full max-w-6xl px-4 py-4 sm:px-6">
          <p className="text-muted-foreground text-xs">
            classroomiq genera borradores de evaluación como asistencia al docente. La nota final es
            responsabilidad exclusiva del profesor.
          </p>
        </div>
      </footer>
    </div>
  )
}

function UserMenu() {
  const { session, signOut } = useAuth()
  const navigate = useNavigate()

  if (!session) return null

  const initials = session.email.slice(0, 2).toUpperCase()

  function onSignOut() {
    signOut()
    navigate({ to: '/login' })
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon" className="rounded-full" aria-label="Menú de usuario">
          <Avatar className="size-8">
            <AvatarFallback className="bg-secondary text-secondary-foreground text-xs font-medium">
              {initials}
            </AvatarFallback>
          </Avatar>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-56">
        <DropdownMenuLabel className="flex flex-col gap-0.5">
          <span className="truncate text-sm font-medium">{session.email}</span>
          <span className="text-muted-foreground text-xs font-normal">
            {ROL_LABEL[session.rol]}
          </span>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem onSelect={onSignOut}>
          <LogOut />
          Cerrar sesión
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
