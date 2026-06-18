import { useState } from 'react'
import { AlertTriangle, Quote } from 'lucide-react'

import { cn } from '@/lib/utils'
import { Label } from '@/components/ui/label'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Switch } from '@/components/ui/switch'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { NivelBadge } from './nivel-badge'
import { acotar, familiaNivel, nivelesOrdenados } from './nivel'
import { irAEvidencia } from './evidencia'
import type { CriterioEvaluado, CriterioRevisionRequest } from './api'

/** Estado editable que el docente decide para un criterio. */
interface Edicion {
  nivelFinalId: string | null
  puntajeFinal: number | null
  justificacionEditada: string
  revisadoManual: boolean
}

function edicionInicial(criterio: CriterioEvaluado): Edicion {
  return {
    nivelFinalId: criterio.nivelFinalId ?? criterio.nivelSugeridoId ?? null,
    puntajeFinal: criterio.puntajeFinal ?? criterio.puntajeSugerido ?? null,
    justificacionEditada: criterio.justificacionEditada ?? criterio.justificacion ?? '',
    revisadoManual: criterio.revisadoManual ?? false,
  }
}

export function TarjetaCriterio({
  criterio,
  readOnly,
  onGuardar,
}: {
  criterio: CriterioEvaluado
  readOnly: boolean
  onGuardar: (criterioId: string, body: CriterioRevisionRequest) => void
}) {
  const [edicion, setEdicion] = useState<Edicion>(() => edicionInicial(criterio))

  const niveles = nivelesOrdenados(criterio.niveles ?? [])
  const indiceNivel = niveles.findIndex((n) => n.id === edicion.nivelFinalId)
  const nivelSel = indiceNivel >= 0 ? niveles[indiceNivel] : undefined
  const puntajeMax = criterio.puntajeMaximo ?? 0
  const min = nivelSel?.puntajeMin ?? 0
  const max = nivelSel?.puntajeMax ?? puntajeMax

  function commit(parcial: Partial<Edicion>) {
    const siguiente = { ...edicion, ...parcial }
    setEdicion(siguiente)
    if (criterio.id) {
      onGuardar(criterio.id, {
        nivelFinalId: siguiente.nivelFinalId,
        puntajeFinal: siguiente.puntajeFinal,
        justificacionEditada: siguiente.justificacionEditada || null,
        revisadoManual: siguiente.revisadoManual,
      })
    }
  }

  function onCambioNivel(nivelId: string) {
    const nuevo = niveles.find((n) => n.id === nivelId)
    // Al cambiar de nivel, acotamos el puntaje al nuevo rango (espeja el backend).
    const puntaje =
      nuevo && edicion.puntajeFinal != null
        ? acotar(edicion.puntajeFinal, nuevo.puntajeMin ?? 0, nuevo.puntajeMax ?? puntajeMax)
        : (nuevo?.puntajeMax ?? edicion.puntajeFinal)
    commit({ nivelFinalId: nivelId, puntajeFinal: puntaje ?? null })
  }

  function onBlurPuntaje() {
    if (edicion.puntajeFinal == null) return
    const acotado = acotar(edicion.puntajeFinal, min, max)
    if (acotado !== edicion.puntajeFinal) commit({ puntajeFinal: acotado })
    else commit({})
  }

  const sugeridoTexto =
    criterio.nivelSugeridoNombre != null || criterio.puntajeSugerido != null
      ? [
          criterio.nivelSugeridoNombre,
          criterio.puntajeSugerido != null ? `${criterio.puntajeSugerido} pts` : null,
        ]
          .filter(Boolean)
          .join(' · ')
      : null

  return (
    <article className="border-border bg-card rounded-lg border p-4">
      <header className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <h3 className="font-medium">{criterio.nombreCriterio}</h3>
          {criterio.descripcionCriterio ? (
            <p className="text-muted-foreground mt-0.5 text-sm">{criterio.descripcionCriterio}</p>
          ) : null}
        </div>
        {nivelSel ? (
          <NivelBadge
            nombre={nivelSel.nombre ?? ''}
            familia={familiaNivel(indiceNivel, niveles.length)}
          />
        ) : null}
      </header>

      {!criterio.evaluable ? (
        <p className="text-estado-evaluando border-estado-evaluando/30 bg-estado-evaluando/5 mt-3 flex items-start gap-2 rounded-md border px-3 py-2 text-sm">
          <AlertTriangle className="mt-0.5 size-4 shrink-0" aria-hidden />
          Este criterio requiere el juicio del docente; el asistente no lo evaluó.
        </p>
      ) : null}

      {criterio.advertencia ? (
        <p className="text-muted-foreground border-border mt-3 flex items-start gap-2 rounded-md border border-dashed px-3 py-2 text-sm">
          <AlertTriangle className="text-estado-evaluando mt-0.5 size-4 shrink-0" aria-hidden />
          {criterio.advertencia}
        </p>
      ) : null}

      <div className="mt-4 grid gap-4 sm:grid-cols-[1fr_8rem]">
        <div className="space-y-1.5">
          <Label htmlFor={`nivel-${criterio.id}`}>Nivel</Label>
          <Select
            value={edicion.nivelFinalId ?? undefined}
            onValueChange={onCambioNivel}
            disabled={readOnly || niveles.length === 0}
          >
            <SelectTrigger id={`nivel-${criterio.id}`} className="w-full">
              <SelectValue placeholder="Elige un nivel" />
            </SelectTrigger>
            <SelectContent>
              {niveles.map((nivel) => (
                <SelectItem key={nivel.id} value={nivel.id ?? ''}>
                  {nivel.nombre}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-1.5">
          <Label htmlFor={`puntaje-${criterio.id}`}>
            Puntaje <span className="text-muted-foreground font-normal">/ {puntajeMax}</span>
          </Label>
          <Input
            id={`puntaje-${criterio.id}`}
            type="number"
            inputMode="decimal"
            step="any"
            min={min}
            max={max}
            value={edicion.puntajeFinal ?? ''}
            disabled={readOnly}
            onChange={(e) =>
              setEdicion((prev) => ({
                ...prev,
                puntajeFinal: e.target.value === '' ? null : Number(e.target.value),
              }))
            }
            onBlur={onBlurPuntaje}
          />
          {nivelSel ? (
            <p className="text-muted-foreground text-xs">
              Rango {min}–{max}
            </p>
          ) : null}
        </div>
      </div>

      <div className="mt-4 space-y-1.5">
        <Label htmlFor={`justif-${criterio.id}`}>Justificación</Label>
        <Textarea
          id={`justif-${criterio.id}`}
          rows={4}
          className="prose-entrega"
          placeholder={
            criterio.evaluable
              ? 'Ajusta la justificación del asistente o reescríbela…'
              : 'Escribe tu justificación para este criterio…'
          }
          value={edicion.justificacionEditada}
          disabled={readOnly}
          onChange={(e) =>
            setEdicion((prev) => ({ ...prev, justificacionEditada: e.target.value }))
          }
          onBlur={() => {
            const original = criterio.justificacionEditada ?? criterio.justificacion ?? ''
            if (edicion.justificacionEditada !== original) commit({})
          }}
        />
      </div>

      {criterio.citas && criterio.citas.length > 0 ? (
        <div className="mt-3 flex flex-wrap items-center gap-1.5">
          <span className="text-muted-foreground inline-flex items-center gap-1 text-xs">
            <Quote className="size-3" aria-hidden />
            Citas:
          </span>
          {criterio.citas.map((cita, i) => (
            <button
              key={cita.id ?? i}
              type="button"
              onClick={() => irAEvidencia(criterio.id ?? '', i)}
              className="border-border hover:bg-accent focus-visible:ring-ring/50 rounded-md border px-2 py-0.5 text-xs font-medium transition-colors focus-visible:ring-2 focus-visible:outline-none"
            >
              [{i + 1}]
            </button>
          ))}
        </div>
      ) : null}

      <footer
        className={cn(
          'mt-4 flex items-center justify-between gap-2 border-t pt-3',
          'border-border',
        )}
      >
        {sugeridoTexto ? (
          <span className="text-muted-foreground text-xs">Sugerido: {sugeridoTexto}</span>
        ) : (
          <span />
        )}
        <div className="flex items-center gap-2 text-sm">
          <Switch
            id={`revisado-${criterio.id}`}
            checked={edicion.revisadoManual}
            disabled={readOnly}
            onCheckedChange={(v) => commit({ revisadoManual: v })}
          />
          <Label htmlFor={`revisado-${criterio.id}`} className="font-normal">
            Revisado
          </Label>
        </div>
      </footer>
    </article>
  )
}
