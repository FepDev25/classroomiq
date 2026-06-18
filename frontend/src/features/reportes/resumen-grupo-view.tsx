import type { ReactNode } from 'react'
import { BarChart3, Sparkles } from 'lucide-react'

import type { ResumenGrupo } from './api'
import { Histograma } from './histograma'
import { MapaDominio } from './mapa-dominio'
import { Badge } from '@/components/ui/badge'

/**
 * Presentación del resumen por grupo (estadísticas, histograma, criterios
 * difíciles, mapa de dominio, narrativa). Solo agregados — nunca evaluaciones
 * individuales ni trabajos. Lo comparten la vista del docente (que pasa el botón
 * de generar narrativa) y la del coordinador (solo lectura, sin acción).
 */
export function ResumenGrupoView({
  resumen,
  narrativaAccion,
  mensajeSinNarrativa = 'Aún no se ha generado una narrativa para este grupo.',
}: {
  resumen: ResumenGrupo
  narrativaAccion?: ReactNode
  mensajeSinNarrativa?: string
}) {
  const stats = resumen.estadisticas
  const dificiles = resumen.criteriosDificiles ?? []

  return (
    <div className="space-y-8">
      <p className="text-muted-foreground text-sm">
        {resumen.totalEvaluaciones} {resumen.totalEvaluaciones === 1 ? 'entrega' : 'entregas'}{' '}
        evaluadas · puntaje máximo {resumen.puntajeTotalRubrica}
      </p>

      {stats ? (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <Estadistica etiqueta="Promedio" valor={stats.promedio} />
          <Estadistica etiqueta="Mediana" valor={stats.mediana} />
          <Estadistica etiqueta="Mínima" valor={stats.minima} />
          <Estadistica etiqueta="Máxima" valor={stats.maxima} />
        </div>
      ) : null}

      {stats?.histograma && stats.histograma.length > 0 ? (
        <div>
          <h2 className="mb-3 flex items-center gap-2 text-lg font-medium">
            <BarChart3 className="text-muted-foreground size-5" aria-hidden />
            Distribución de notas
          </h2>
          <Histograma rangos={stats.histograma} />
        </div>
      ) : null}

      {dificiles.length > 0 ? (
        <div>
          <h2 className="mb-2 text-lg font-medium">Criterios con mayor dificultad</h2>
          <div className="flex flex-wrap gap-2">
            {dificiles.map((nombre) => (
              <Badge key={nombre} className="bg-estado-evaluando text-white">
                {nombre}
              </Badge>
            ))}
          </div>
        </div>
      ) : null}

      {resumen.criterios && resumen.criterios.length > 0 ? (
        <div>
          <h2 className="mb-3 text-lg font-medium">Mapa de dominio por criterio</h2>
          <MapaDominio criterios={resumen.criterios} />
        </div>
      ) : null}

      <div>
        <div className="mb-3 flex items-center justify-between gap-2">
          <h2 className="flex items-center gap-2 text-lg font-medium">
            <Sparkles className="text-muted-foreground size-5" aria-hidden />
            Narrativa
          </h2>
          {narrativaAccion}
        </div>
        {resumen.narrativa ? (
          <p className="prose-entrega border-border bg-card rounded-lg border p-4 text-sm leading-relaxed">
            {resumen.narrativa}
          </p>
        ) : (
          <p className="text-muted-foreground text-sm">{mensajeSinNarrativa}</p>
        )}
      </div>
    </div>
  )
}

function Estadistica({ etiqueta, valor }: { etiqueta: string; valor: number | undefined }) {
  return (
    <div className="border-border bg-card rounded-lg border p-3">
      <p className="text-muted-foreground text-xs">{etiqueta}</p>
      <p className="mt-0.5 text-xl font-semibold tabular-nums">{valor?.toFixed(2) ?? '—'}</p>
    </div>
  )
}
