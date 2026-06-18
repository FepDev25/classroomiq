import { useState } from 'react'
import { ChevronDown } from 'lucide-react'

import { cn } from '@/lib/utils'
import { Badge } from '@/components/ui/badge'
import type { ParSimilitud } from './api'

const pct = (v: number | null | undefined) => (v == null ? '—' : `${Math.round(v * 100)}%`)

export function ParCard({
  par,
  nombreA,
  nombreB,
}: {
  par: ParSimilitud
  nombreA: string
  nombreB: string
}) {
  const [abierto, setAbierto] = useState(false)
  const fragmentos = par.fragmentos ?? []

  return (
    <div
      className={cn(
        'border-border bg-card rounded-lg border',
        par.superaUmbral && 'border-estado-evaluando/50 bg-estado-evaluando/5',
      )}
    >
      <button
        type="button"
        onClick={() => setAbierto((v) => !v)}
        className="flex w-full items-center gap-3 px-4 py-3 text-left"
        aria-expanded={abierto}
      >
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-medium">
            {nombreA} <span className="text-muted-foreground">↔</span> {nombreB}
          </p>
          <p className="text-muted-foreground mt-0.5 text-xs">
            Semántica {pct(par.similitudSemantica)} · Textual {pct(par.similitudTextual)}
          </p>
        </div>
        {par.superaUmbral ? (
          <Badge className="bg-estado-evaluando shrink-0 text-white">Sobre el umbral</Badge>
        ) : null}
        <ChevronDown
          className={cn(
            'text-muted-foreground size-4 shrink-0 transition-transform',
            abierto && 'rotate-180',
          )}
          aria-hidden
        />
      </button>

      {abierto ? (
        <div className="border-border border-t px-4 py-3">
          {fragmentos.length === 0 ? (
            <p className="text-muted-foreground text-sm">
              No se identificaron fragmentos puntuales para este par.
            </p>
          ) : (
            <ul className="space-y-3">
              {fragmentos.map((frag, i) => (
                <li key={i}>
                  <div className="mb-1 flex items-center gap-2">
                    <Badge variant="outline" className="font-normal">
                      {frag.tipo === 'TEXTUAL' ? 'Textual' : 'Semántica'}
                    </Badge>
                    <span className="text-muted-foreground text-xs">{pct(frag.similitud)}</span>
                  </div>
                  <div className="grid gap-2 sm:grid-cols-2">
                    <blockquote className="border-border bg-muted/40 prose-entrega rounded-md border-l-2 px-3 py-2 text-sm">
                      <span className="text-muted-foreground mb-1 block text-xs not-italic">
                        {nombreA}
                      </span>
                      {frag.textoA}
                    </blockquote>
                    <blockquote className="border-border bg-muted/40 prose-entrega rounded-md border-l-2 px-3 py-2 text-sm">
                      <span className="text-muted-foreground mb-1 block text-xs not-italic">
                        {nombreB}
                      </span>
                      {frag.textoB}
                    </blockquote>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      ) : null}
    </div>
  )
}
