import { FileCode, FileText } from 'lucide-react'

import { TipoEntregaBadge } from '@/features/lotes/badges'
import type { Entrega } from '@/features/lotes/api'
import type { ArchivoContenido, ContenidoEntrega, CriterioEvaluado, SeccionContenido } from './api'
import { citasResaltables, resaltar, type CitaResaltable } from './resaltado'

/**
 * Panel izquierdo de la revisión: el documento COMPLETO de la entrega, con los
 * fragmentos citados por el motor resaltados en su contexto. Cada resaltado lleva
 * el id de DOM destino de su chip de cita (panel derecho), así el scroll + destello
 * funciona sobre el texto en contexto, no sobre una lista suelta.
 */
export function PanelDocumento({
  entrega,
  contenido,
  criterios,
}: {
  entrega: Entrega
  contenido: ContenidoEntrega
  criterios: CriterioEvaluado[]
}) {
  const citas = citasResaltables(criterios)
  const archivos = contenido.archivos ?? []

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center gap-2">
        <h2 className="text-lg font-medium">{entrega.identificadorEstudiante}</h2>
        {entrega.tipo ? <TipoEntregaBadge tipo={entrega.tipo} /> : null}
      </div>

      {archivos.length === 0 ? (
        <p className="text-muted-foreground text-sm">
          No pudimos reconstruir el contenido de esta entrega.
        </p>
      ) : (
        <div className="space-y-6">
          {archivos.map((archivo) => (
            <ArchivoBloque key={archivo.archivoId} archivo={archivo} citas={citas} />
          ))}
        </div>
      )}
    </div>
  )
}

function ArchivoBloque({
  archivo,
  citas,
}: {
  archivo: ArchivoContenido
  citas: CitaResaltable[]
}) {
  const esCodigo = archivo.rol === 'CODIGO'
  const secciones = archivo.secciones ?? []

  return (
    <section className="space-y-2">
      <h3 className="text-foreground flex items-center gap-2 text-sm font-medium">
        {esCodigo ? (
          <FileCode className="text-muted-foreground size-4 shrink-0" aria-hidden />
        ) : (
          <FileText className="text-muted-foreground size-4 shrink-0" aria-hidden />
        )}
        <span className="truncate">{archivo.nombreOriginal}</span>
      </h3>
      <div className="border-border bg-card divide-border divide-y rounded-lg border">
        {secciones.map((seccion, i) => (
          <SeccionBloque key={i} seccion={seccion} esCodigo={esCodigo} citas={citas} />
        ))}
      </div>
    </section>
  )
}

function SeccionBloque({
  seccion,
  esCodigo,
  citas,
}: {
  seccion: SeccionContenido
  esCodigo: boolean
  citas: CitaResaltable[]
}) {
  const tramos = resaltar(seccion.texto ?? '', citas)

  return (
    <div className="px-3 py-2.5">
      {seccion.titulo ? (
        <p className="text-muted-foreground mb-1.5 text-xs font-medium">{seccion.titulo}</p>
      ) : null}
      {esCodigo ? (
        <pre className="overflow-x-auto text-xs leading-relaxed">
          <code>
            {tramos.map((tramo, i) =>
              tramo.domId ? (
                <mark key={i} id={tramo.domId} className="bg-estado-evaluando/30 rounded-sm">
                  {tramo.texto}
                </mark>
              ) : (
                <span key={i}>{tramo.texto}</span>
              ),
            )}
          </code>
        </pre>
      ) : (
        <p className="prose-entrega text-sm leading-relaxed whitespace-pre-wrap">
          {tramos.map((tramo, i) =>
            tramo.domId ? (
              <mark key={i} id={tramo.domId} className="bg-estado-evaluando/30 rounded-sm px-0.5">
                {tramo.texto}
              </mark>
            ) : (
              <span key={i}>{tramo.texto}</span>
            ),
          )}
        </p>
      )}
    </div>
  )
}
