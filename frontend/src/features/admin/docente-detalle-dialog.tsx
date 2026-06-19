import { ErrorState, LoadingRows } from '@/components/states'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'

import type { DocenteUso } from './api'
import { useDocenteUsoDetalle } from './hooks'
import { OPERACION_LABEL, etiquetaMes, formatoMoneda, formatoTokens } from './formato'

/**
 * Detalle de uso/costo de un docente en el mes: desglose por modelo y por
 * operación (evaluación vs narrativa). Se abre desde la tabla del resumen.
 */
export function DocenteDetalleDialog({
  docente,
  mes,
  onClose,
}: {
  docente: DocenteUso | null
  mes: string
  onClose: () => void
}) {
  return (
    <Dialog open={Boolean(docente)} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-2xl">
        {docente ? (
          <DetalleContenido docenteId={docente.docenteId!} docente={docente} mes={mes} />
        ) : null}
      </DialogContent>
    </Dialog>
  )
}

function DetalleContenido({
  docenteId,
  docente,
  mes,
}: {
  docenteId: string
  docente: DocenteUso
  mes: string
}) {
  const detalle = useDocenteUsoDetalle(docenteId, mes)

  return (
    <>
      <DialogHeader>
        <DialogTitle>{docente.nombre ?? docente.email ?? 'Docente'}</DialogTitle>
        <DialogDescription>
          Uso y costo estimado · {etiquetaMes(mes)}
          {docente.email ? ` · ${docente.email}` : ''}
        </DialogDescription>
      </DialogHeader>

      {detalle.isPending ? (
        <LoadingRows rows={4} />
      ) : detalle.isError ? (
        <ErrorState
          title="No pudimos cargar el detalle."
          message="Revisa tu conexión e inténtalo de nuevo."
          onRetry={() => detalle.refetch()}
        />
      ) : (
        <div className="space-y-6">
          <div className="grid grid-cols-3 gap-3 text-sm">
            <Resumen
              etiqueta="Costo total"
              valor={formatoMoneda(detalle.data.costoTotal, detalle.data.moneda)}
            />
            <Resumen
              etiqueta="Tokens entrada"
              valor={formatoTokens(detalle.data.totalInputTokens)}
            />
            <Resumen
              etiqueta="Tokens salida"
              valor={formatoTokens(detalle.data.totalOutputTokens)}
            />
          </div>

          <Seccion titulo="Por modelo">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Modelo</TableHead>
                  <TableHead className="text-right">Entrada</TableHead>
                  <TableHead className="text-right">Salida</TableHead>
                  <TableHead className="text-right">Costo</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(detalle.data.porModelo ?? []).map((m) => (
                  <TableRow key={m.modelo}>
                    <TableCell className="font-medium">{m.modelo}</TableCell>
                    <TableCell className="text-right tabular-nums">
                      {formatoTokens(m.inputTokens)}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {formatoTokens(m.outputTokens)}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {formatoMoneda(m.costoEstimado, detalle.data.moneda)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Seccion>

          <Seccion titulo="Por operación">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Operación</TableHead>
                  <TableHead className="text-right">Entrada</TableHead>
                  <TableHead className="text-right">Salida</TableHead>
                  <TableHead className="text-right">Costo</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(detalle.data.porOperacion ?? []).map((o) => (
                  <TableRow key={o.operacion}>
                    <TableCell className="font-medium">
                      {o.operacion ? OPERACION_LABEL[o.operacion] : '—'}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {formatoTokens(o.inputTokens)}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {formatoTokens(o.outputTokens)}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">
                      {formatoMoneda(o.costoEstimado, detalle.data.moneda)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Seccion>
        </div>
      )}
    </>
  )
}

function Resumen({ etiqueta, valor }: { etiqueta: string; valor: string }) {
  return (
    <div className="border-border bg-card rounded-lg border p-3">
      <p className="text-muted-foreground text-xs">{etiqueta}</p>
      <p className="mt-1 font-semibold tabular-nums">{valor}</p>
    </div>
  )
}

function Seccion({ titulo, children }: { titulo: string; children: React.ReactNode }) {
  return (
    <div className="space-y-2">
      <h3 className="text-sm font-medium">{titulo}</h3>
      <div className="border-border overflow-hidden rounded-lg border">{children}</div>
    </div>
  )
}
