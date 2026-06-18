import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'

import type { DocenteUso } from './api'
import { formatoMoneda } from './formato'

/** Costo estimado por docente en el mes (Recharts). */
export function CostoChart({
  docentes,
  moneda,
}: {
  docentes: DocenteUso[]
  moneda: string | undefined
}) {
  const data = docentes.map((d) => ({
    nombre: d.nombre ?? d.email ?? '—',
    costo: d.costoEstimado ?? 0,
  }))

  return (
    <div className="h-64 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} margin={{ top: 8, right: 8, bottom: 8, left: -8 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
          <XAxis
            dataKey="nombre"
            tick={{ fontSize: 12, fill: 'var(--muted-foreground)' }}
            stroke="var(--border)"
            interval={0}
          />
          <YAxis
            tick={{ fontSize: 12, fill: 'var(--muted-foreground)' }}
            stroke="var(--border)"
            width={72}
            tickFormatter={(v) => formatoMoneda(Number(v), moneda)}
          />
          <Tooltip
            cursor={{ fill: 'var(--muted)' }}
            contentStyle={{
              backgroundColor: 'var(--popover)',
              border: '1px solid var(--border)',
              borderRadius: '0.5rem',
              fontSize: 12,
              color: 'var(--popover-foreground)',
            }}
            labelStyle={{ color: 'var(--foreground)' }}
            formatter={(value) => [formatoMoneda(Number(value), moneda), 'Costo estimado']}
          />
          <Bar dataKey="costo" fill="var(--primary)" radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
