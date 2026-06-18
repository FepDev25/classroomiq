import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'

import type { RangoNota } from './api'

/** Histograma de distribución de notas finales del lote (Recharts). */
export function Histograma({ rangos }: { rangos: RangoNota[] }) {
  const data = rangos.map((r) => ({ etiqueta: r.etiqueta ?? '', cantidad: r.cantidad ?? 0 }))

  return (
    <div className="h-64 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} margin={{ top: 8, right: 8, bottom: 8, left: -16 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
          <XAxis
            dataKey="etiqueta"
            tick={{ fontSize: 12, fill: 'var(--muted-foreground)' }}
            stroke="var(--border)"
          />
          <YAxis
            allowDecimals={false}
            tick={{ fontSize: 12, fill: 'var(--muted-foreground)' }}
            stroke="var(--border)"
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
            formatter={(value) => [String(value), 'Entregas']}
          />
          <Bar dataKey="cantidad" fill="var(--primary)" radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
