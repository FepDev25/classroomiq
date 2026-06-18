import type { EntregaResumenSimilitud, ParSimilitud } from './api'

function clavePar(a: string, b: string): string {
  return [a, b].sort().join('|')
}

/**
 * Mapa de calor de similitud semántica entre todos los pares del lote. La
 * intensidad del color codifica la similitud (0..1); la diagonal es la propia
 * entrega. Es una matriz simple de celdas, no un chart (más legible y barato).
 */
export function HeatmapSimilitud({
  entregas,
  pares,
}: {
  entregas: EntregaResumenSimilitud[]
  pares: ParSimilitud[]
}) {
  const valor = new Map<string, number>()
  for (const par of pares) {
    if (par.entregaAId && par.entregaBId) {
      valor.set(clavePar(par.entregaAId, par.entregaBId), par.similitudSemantica ?? 0)
    }
  }

  if (entregas.length < 2) return null

  return (
    <div className="overflow-x-auto">
      <table className="border-separate border-spacing-0.5 text-xs">
        <thead>
          <tr>
            <th className="p-1" />
            {entregas.map((e, i) => (
              <th
                key={e.entregaId}
                className="text-muted-foreground p-1 text-center font-medium"
                title={e.identificador}
              >
                {i + 1}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {entregas.map((fila, i) => (
            <tr key={fila.entregaId}>
              <th
                className="text-muted-foreground max-w-32 truncate p-1 text-right font-medium"
                title={fila.identificador}
              >
                {i + 1}. {fila.identificador}
              </th>
              {entregas.map((col, j) => {
                if (i === j) {
                  return (
                    <td
                      key={col.entregaId}
                      className="bg-muted size-8 rounded-sm text-center align-middle"
                    >
                      <span className="text-muted-foreground">·</span>
                    </td>
                  )
                }
                const v =
                  fila.entregaId && col.entregaId
                    ? (valor.get(clavePar(fila.entregaId, col.entregaId)) ?? 0)
                    : 0
                const pct = Math.round(v * 100)
                return (
                  <td
                    key={col.entregaId}
                    className="size-8 rounded-sm text-center align-middle tabular-nums"
                    style={{
                      backgroundColor: `color-mix(in oklch, var(--primary) ${pct}%, transparent)`,
                    }}
                    title={`${fila.identificador} ↔ ${col.identificador}: ${pct}%`}
                  >
                    {pct}
                  </td>
                )
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
