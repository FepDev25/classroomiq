import { useState } from 'react'
import { createFileRoute } from '@tanstack/react-router'
import { AlertTriangle, BarChart3 } from 'lucide-react'

import { useMetricasUso } from '@/features/admin/hooks'
import type { DocenteUso } from '@/features/admin/api'
import { CostoChart } from '@/features/admin/costo-chart'
import { DocenteDetalleDialog } from '@/features/admin/docente-detalle-dialog'
import {
  etiquetaMes,
  formatoMoneda,
  formatoTokens,
  mesActual,
  ultimosMeses,
} from '@/features/admin/formato'
import { EmptyState, ErrorState, LoadingRows } from '@/components/states'
import { Card, CardContent, CardDescription, CardTitle } from '@/components/ui/card'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'

export const Route = createFileRoute('/_authed/admin/metricas')({
  component: MetricasPage,
})

const MESES = ultimosMeses(12)

function MetricasPage() {
  const [mes, setMes] = useState(mesActual())
  const metricas = useMetricasUso(mes)
  const [detalle, setDetalle] = useState<DocenteUso | null>(null)

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Uso y costo</h1>
          <p className="text-muted-foreground mt-1 text-sm">
            Consumo estimado del modelo por docente. El costo es una estimación según los tokens
            procesados, no una factura.
          </p>
        </div>
        <div className="w-44 space-y-2">
          <label className="text-sm font-medium" htmlFor="mes">
            Mes
          </label>
          <Select value={mes} onValueChange={setMes}>
            <SelectTrigger id="mes">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {MESES.map((m) => (
                <SelectItem key={m} value={m}>
                  {etiquetaMes(m)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {metricas.isPending ? (
        <LoadingRows rows={5} />
      ) : metricas.isError ? (
        <ErrorState
          message="No pudimos cargar las métricas del mes."
          onRetry={() => metricas.refetch()}
        />
      ) : (
        <>
          {metricas.data.umbralSuperado ? (
            <div className="border-estado-error/40 bg-estado-error/10 text-foreground flex items-start gap-3 rounded-lg border p-4">
              <AlertTriangle className="text-estado-error mt-0.5 size-5 shrink-0" />
              <div className="text-sm">
                <p className="font-medium">Umbral mensual superado</p>
                <p className="text-muted-foreground mt-0.5">
                  El costo estimado de {etiquetaMes(mes)} (
                  {formatoMoneda(metricas.data.costoTotal, metricas.data.moneda)}) supera el umbral
                  configurado de {formatoMoneda(metricas.data.umbralMensual, metricas.data.moneda)}.
                </p>
              </div>
            </div>
          ) : null}

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Metrica
              titulo="Costo estimado"
              valor={formatoMoneda(metricas.data.costoTotal, metricas.data.moneda)}
              detalle={`Umbral: ${formatoMoneda(metricas.data.umbralMensual, metricas.data.moneda)}`}
            />
            <Metrica titulo="Tokens entrada" valor={formatoTokens(metricas.data.totalInputTokens)} />
            <Metrica titulo="Tokens salida" valor={formatoTokens(metricas.data.totalOutputTokens)} />
            <Metrica titulo="Docentes con uso" valor={String(metricas.data.docentes?.length ?? 0)} />
          </div>

          {(metricas.data.docentes?.length ?? 0) === 0 ? (
            <EmptyState
              icon={<BarChart3 className="size-8" />}
              title="Sin uso registrado"
              message={`No hay consumo del modelo en ${etiquetaMes(mes)}.`}
            />
          ) : (
            <>
              <Card>
                <CardContent className="pt-6">
                  <CostoChart docentes={metricas.data.docentes!} moneda={metricas.data.moneda} />
                </CardContent>
              </Card>

              <div className="border-border overflow-hidden rounded-lg border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Docente</TableHead>
                      <TableHead className="text-right">Entrada</TableHead>
                      <TableHead className="text-right">Salida</TableHead>
                      <TableHead className="text-right">Total</TableHead>
                      <TableHead className="text-right">Costo estimado</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {metricas.data.docentes!.map((d) => (
                      <TableRow
                        key={d.docenteId}
                        className="hover:bg-muted/50 cursor-pointer"
                        onClick={() => setDetalle(d)}
                      >
                        <TableCell className="font-medium">
                          {d.nombre ?? '—'}
                          {d.email ? (
                            <span className="text-muted-foreground block text-xs font-normal">
                              {d.email}
                            </span>
                          ) : null}
                        </TableCell>
                        <TableCell className="text-right tabular-nums">
                          {formatoTokens(d.inputTokens)}
                        </TableCell>
                        <TableCell className="text-right tabular-nums">
                          {formatoTokens(d.outputTokens)}
                        </TableCell>
                        <TableCell className="text-right tabular-nums">
                          {formatoTokens(d.totalTokens)}
                        </TableCell>
                        <TableCell className="text-right font-medium tabular-nums">
                          {formatoMoneda(d.costoEstimado, metricas.data.moneda)}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
              <p className="text-muted-foreground text-xs">
                Haz clic en un docente para ver el desglose por modelo y operación.
              </p>
            </>
          )}
        </>
      )}

      <DocenteDetalleDialog docente={detalle} mes={mes} onClose={() => setDetalle(null)} />
    </section>
  )
}

function Metrica({
  titulo,
  valor,
  detalle,
}: {
  titulo: string
  valor: string
  detalle?: string
}) {
  return (
    <Card>
      <CardContent className="pt-6">
        <CardDescription>{titulo}</CardDescription>
        <CardTitle className="mt-1 text-2xl tabular-nums">{valor}</CardTitle>
        {detalle ? <p className="text-muted-foreground mt-1 text-xs">{detalle}</p> : null}
      </CardContent>
    </Card>
  )
}
