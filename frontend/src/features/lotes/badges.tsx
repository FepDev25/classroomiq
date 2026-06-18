import { Loader2 } from 'lucide-react'

import { cn } from '@/lib/utils'
import { Badge } from '@/components/ui/badge'
import type { EstadoEntrega, EstadoLote, TipoEntrega } from './api'

const ESTADO_ENTREGA: Record<EstadoEntrega, { label: string; dot: string; activo: boolean }> = {
  PENDIENTE: { label: 'Pendiente', dot: 'bg-estado-pendiente', activo: false },
  PROCESANDO: { label: 'Procesando', dot: 'bg-estado-procesando', activo: true },
  EVALUANDO: { label: 'Evaluando', dot: 'bg-estado-evaluando', activo: true },
  LISTO: { label: 'Listo', dot: 'bg-estado-listo', activo: false },
  ERROR: { label: 'Error', dot: 'bg-estado-error', activo: false },
}

/** Badge semántico del estado de una entrega, con punto de color y spinner en estados activos. */
export function EstadoEntregaBadge({ estado }: { estado: EstadoEntrega }) {
  const info = ESTADO_ENTREGA[estado]
  return (
    <Badge variant="outline" className="gap-1.5" aria-label={`Estado: ${info.label}`}>
      {info.activo ? (
        <Loader2 className="size-3 animate-spin" aria-hidden />
      ) : (
        <span className={cn('size-2 rounded-full', info.dot)} aria-hidden />
      )}
      {info.label}
    </Badge>
  )
}

const ESTADO_LOTE: Record<EstadoLote, string> = {
  ABIERTO: 'Abierto',
  PROCESANDO: 'Procesando',
  LISTO: 'Listo',
}

export function EstadoLoteBadge({ estado }: { estado: EstadoLote }) {
  return <Badge variant="secondary">{ESTADO_LOTE[estado]}</Badge>
}

const TIPO_ENTREGA: Record<TipoEntrega, string> = {
  DOCUMENTO: 'Documento',
  CODIGO: 'Código',
  MIXTA: 'Mixta',
}

export function TipoEntregaBadge({ tipo }: { tipo: TipoEntrega }) {
  return (
    <Badge variant="outline" className="font-normal">
      {TIPO_ENTREGA[tipo]}
    </Badge>
  )
}
