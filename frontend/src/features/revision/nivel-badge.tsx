import { cn } from '@/lib/utils'
import { Badge } from '@/components/ui/badge'

const CLASES: Record<string, string> = {
  excelente: 'border-nivel-excelente/40 text-nivel-excelente bg-nivel-excelente/10',
  bueno: 'border-nivel-bueno/40 text-nivel-bueno bg-nivel-bueno/10',
  basico: 'border-nivel-basico/40 text-nivel-basico bg-nivel-basico/10',
  insuficiente: 'border-nivel-insuficiente/40 text-nivel-insuficiente bg-nivel-insuficiente/10',
}

/** Badge de nivel coloreado por su familia (derivada de la posición del nivel). */
export function NivelBadge({
  nombre,
  familia,
  className,
}: {
  nombre: string
  familia: string
  className?: string
}) {
  return (
    <Badge variant="outline" className={cn(CLASES[familia] ?? CLASES.bueno, className)}>
      {nombre}
    </Badge>
  )
}
