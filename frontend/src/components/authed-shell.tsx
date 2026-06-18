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

/** Enlaces de navegación por rol. Crece a medida que se construyen las pantallas. */
const NAV_POR_ROL: Record<Rol, { to: string; label: string }[]> = {
  DOCENTE: [
    { to: '/materias', label: 'Materias' },
    { to: '/lotes', label: 'Lotes' },
  ],
  ADMIN: [
    { to: '/admin/cuentas', label: 'Cuentas' },
    { to: '/admin/coordinadores', label: 'Coordinadores' },
    { to: '/admin/metricas', label: 'Uso y costo' },
  ],
  COORDINADOR: [{ to: '/coordinador', label: 'Reportes' }],
}

/**
 * Cascarón de las vistas autenticadas: cabecera con marca + navegación + menú
 * de usuario, contenido, y pie con el principio inamovible. La navegación por
 * materias/lotes/revisión se amplía a partir de H2.
 */
export function AuthedShell({ children }: { children: ReactNode }) {
  const { rol } = useAuth()
  const navItems = rol ? NAV_POR_ROL[rol] : []

  return (
    <div className="bg-background text-foreground flex min-h-svh flex-col">
      <header className="border-border bg-background/80 sticky top-0 z-10 border-b backdrop-blur-sm">
        <div className="mx-auto flex h-14 w-full max-w-6xl items-center justify-between gap-4 px-4 sm:px-6">
          <div className="flex items-center gap-6">
            <Link to="/" className="focus-visible:ring-ring/50 rounded-sm focus-visible:ring-2">
              <Brand />
            </Link>
            {navItems.length > 0 ? (
              <nav className="flex items-center gap-1 text-sm">
                {navItems.map((item) => (
                  <NavLink key={item.to} to={item.to}>
                    {item.label}
                  </NavLink>
                ))}
              </nav>
            ) : null}
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

function NavLink({ to, children }: { to: string; children: ReactNode }) {
  return (
    <Link
      to={to}
      className="text-muted-foreground hover:text-foreground hover:bg-accent rounded-md px-3 py-1.5 font-medium transition-colors"
      activeProps={{ className: 'text-foreground bg-accent' }}
      activeOptions={{ exact: false }}
    >
      {children}
    </Link>
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
