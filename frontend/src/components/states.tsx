import type { ReactNode } from 'react'
import { AlertTriangle } from 'lucide-react'

import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'

/** Bloque de carga: barras neutras (no spinner) que sugieren la forma del contenido. */
export function LoadingRows({ rows = 4, className }: { rows?: number; className?: string }) {
  return (
    <div className={cn('space-y-2', className)} aria-busy="true" aria-live="polite">
      <span className="sr-only">Cargando…</span>
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="bg-muted h-12 animate-pulse rounded-md" />
      ))}
    </div>
  )
}

export function ErrorState({
  title = 'No pudimos cargar esto',
  message,
  onRetry,
}: {
  title?: string
  message?: string
  onRetry?: () => void
}) {
  return (
    <div className="border-border flex flex-col items-center rounded-lg border border-dashed px-6 py-12 text-center">
      <span className="text-destructive flex size-10 items-center justify-center">
        <AlertTriangle className="size-6" />
      </span>
      <h3 className="mt-2 text-base font-medium">{title}</h3>
      {message ? <p className="text-muted-foreground mt-1 max-w-sm text-sm">{message}</p> : null}
      {onRetry ? (
        <Button variant="outline" size="sm" className="mt-4" onClick={onRetry}>
          Reintentar
        </Button>
      ) : null}
    </div>
  )
}

export function EmptyState({
  icon,
  title,
  message,
  action,
}: {
  icon?: ReactNode
  title: string
  message?: string
  action?: ReactNode
}) {
  return (
    <div className="border-border flex flex-col items-center rounded-lg border border-dashed px-6 py-12 text-center">
      {icon ? <span className="text-muted-foreground mb-3">{icon}</span> : null}
      <h3 className="text-base font-medium">{title}</h3>
      {message ? <p className="text-muted-foreground mt-1 max-w-sm text-sm">{message}</p> : null}
      {action ? <div className="mt-4">{action}</div> : null}
    </div>
  )
}
