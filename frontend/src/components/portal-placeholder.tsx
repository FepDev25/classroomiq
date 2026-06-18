import type { ReactNode } from 'react'

import { MarginMark } from '@/components/brand'

/**
 * Landing provisional de un portal aún sin pantallas (admin/coordinador en H0).
 * Mantiene la identidad visual; las vistas reales llegan en hitos posteriores.
 */
export function PortalPlaceholder({
  eyebrow,
  title,
  children,
  nota,
}: {
  eyebrow: string
  title: string
  children: ReactNode
  nota?: string
}) {
  return (
    <section className="mx-auto max-w-2xl py-10">
      <p className="text-muted-foreground font-mono text-xs tracking-[0.2em] uppercase">{eyebrow}</p>
      <h1 className="mt-3 text-3xl font-semibold tracking-tight">{title}</h1>
      <div className="prose-entrega text-muted-foreground mt-4 max-w-xl text-lg">{children}</div>
      {nota ? (
        <p className="text-muted-foreground mt-10 flex items-center gap-2 font-mono text-xs">
          <MarginMark className="text-primary" />
          {nota}
        </p>
      ) : null}
    </section>
  )
}
