import { familiaNivel } from '@/features/revision/nivel'
import type { CriterioResumen } from './api'

const BG_FAMILIA: Record<string, string> = {
  excelente: 'bg-nivel-excelente',
  bueno: 'bg-nivel-bueno',
  basico: 'bg-nivel-basico',
  insuficiente: 'bg-nivel-insuficiente',
}

const pct = (v: number | null | undefined) => (v == null ? '—' : `${Math.round(v)}%`)

/**
 * Mapa de dominio grupal: por criterio, una barra apilada que muestra qué
 * porcentaje del grupo alcanzó cada nivel. El color codifica la posición del
 * nivel (peor→mejor), igual que en la revisión.
 */
export function MapaDominio({ criterios }: { criterios: CriterioResumen[] }) {
  return (
    <ul className="space-y-4">
      {criterios.map((criterio) => {
        const dist = criterio.distribucion ?? []
        return (
          <li key={criterio.criterioId} className="space-y-1.5">
            <div className="flex items-baseline justify-between gap-2">
              <span className="text-sm font-medium">{criterio.nombre}</span>
              <span className="text-muted-foreground text-xs tabular-nums">
                Promedio {pct(criterio.promedioPct)}
              </span>
            </div>
            <div className="bg-muted flex h-5 w-full overflow-hidden rounded-full">
              {dist.map((nivel, i) => {
                if (!nivel.porcentaje) return null
                const familia = familiaNivel(i, dist.length)
                return (
                  <div
                    key={nivel.nivelId ?? i}
                    className={BG_FAMILIA[familia] ?? BG_FAMILIA.bueno}
                    style={{ width: `${nivel.porcentaje}%` }}
                    title={`${nivel.nombre}: ${nivel.cantidad} (${Math.round(nivel.porcentaje)}%)`}
                  />
                )
              })}
            </div>
            <div className="text-muted-foreground flex flex-wrap gap-x-3 gap-y-0.5 text-xs">
              {dist.map((nivel, i) => {
                const familia = familiaNivel(i, dist.length)
                return (
                  <span key={nivel.nivelId ?? i} className="inline-flex items-center gap-1">
                    <span
                      className={`size-2 rounded-full ${BG_FAMILIA[familia] ?? BG_FAMILIA.bueno}`}
                    />
                    {nivel.nombre} ({nivel.cantidad})
                  </span>
                )
              })}
              {criterio.sinNivel ? <span>Sin nivel ({criterio.sinNivel})</span> : null}
            </div>
          </li>
        )
      })}
    </ul>
  )
}
