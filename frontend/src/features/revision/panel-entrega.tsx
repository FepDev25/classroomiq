import { FileCode, FileText, Quote } from 'lucide-react'

import { TipoEntregaBadge } from '@/features/lotes/badges'
import type { Entrega } from '@/features/lotes/api'
import { evidenciaDomId } from './evidencia'
import type { CriterioEvaluado } from './api'

function esArchivoCodigo(nombre: string | undefined, mime: string | undefined): boolean {
  if (mime?.includes('zip')) return true
  return Boolean(nombre && nombre.toLowerCase().endsWith('.zip'))
}

/**
 * Panel izquierdo de la revisión: identidad de la entrega, sus archivos y la
 * evidencia (fragmentos citados por el LLM, agrupados por criterio). Cada
 * fragmento es el destino del scroll desde los chips de cita del panel derecho.
 */
export function PanelEntrega({
  entrega,
  criterios,
}: {
  entrega: Entrega
  criterios: CriterioEvaluado[]
}) {
  const conCitas = criterios.filter((c) => (c.citas?.length ?? 0) > 0)

  return (
    <div className="space-y-6">
      <div>
        <div className="flex flex-wrap items-center gap-2">
          <h2 className="text-lg font-medium">{entrega.identificadorEstudiante}</h2>
          {entrega.tipo ? <TipoEntregaBadge tipo={entrega.tipo} /> : null}
        </div>
        {entrega.archivos && entrega.archivos.length > 0 ? (
          <ul className="mt-3 space-y-1.5">
            {entrega.archivos.map((archivo) => {
              const esCodigo = esArchivoCodigo(archivo.nombreOriginal, archivo.mimeType)
              return (
                <li
                  key={archivo.id}
                  className="border-border bg-card flex items-center gap-2 rounded-md border px-3 py-2 text-sm"
                >
                  {esCodigo ? (
                    <FileCode className="text-muted-foreground size-4 shrink-0" aria-hidden />
                  ) : (
                    <FileText className="text-muted-foreground size-4 shrink-0" aria-hidden />
                  )}
                  <span className="truncate">{archivo.nombreOriginal}</span>
                  {typeof archivo.tamanoBytes === 'number' ? (
                    <span className="text-muted-foreground ml-auto shrink-0 text-xs">
                      {(archivo.tamanoBytes / 1024).toFixed(0)} KB
                    </span>
                  ) : null}
                </li>
              )
            })}
          </ul>
        ) : null}
      </div>

      <div>
        <div className="text-muted-foreground flex items-center gap-2 text-sm font-medium">
          <Quote className="size-4" aria-hidden />
          Fragmentos citados
        </div>
        {conCitas.length === 0 ? (
          <p className="text-muted-foreground mt-2 text-sm">
            El borrador no citó fragmentos específicos de esta entrega.
          </p>
        ) : (
          <div className="mt-3 space-y-5">
            {conCitas.map((criterio) => (
              <div key={criterio.id}>
                <h3 className="text-foreground text-sm font-medium">{criterio.nombreCriterio}</h3>
                <ul className="mt-2 space-y-2">
                  {(criterio.citas ?? []).map((cita, i) => (
                    <li
                      key={cita.id ?? i}
                      id={evidenciaDomId(criterio.id ?? '', i)}
                      className="border-border bg-muted/40 rounded-md border-l-2 px-3 py-2"
                    >
                      <span className="text-muted-foreground mr-2 text-xs font-medium">
                        [{i + 1}]
                      </span>
                      <span className="prose-entrega text-sm">{cita.textoCitado}</span>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
