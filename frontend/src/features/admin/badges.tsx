import { Badge } from '@/components/ui/badge'
import type { Rol } from './api'

const ROL: Record<Rol, string> = {
  ADMIN: 'Administrador',
  DOCENTE: 'Docente',
  COORDINADOR: 'Coordinador',
}

export function RolBadge({ rol }: { rol: Rol }) {
  return <Badge variant="outline">{ROL[rol]}</Badge>
}

export function EstadoCuentaBadge({ activo }: { activo: boolean }) {
  return activo ? (
    <Badge className="bg-estado-listo text-white">Activa</Badge>
  ) : (
    <Badge variant="secondary">Inactiva</Badge>
  )
}
