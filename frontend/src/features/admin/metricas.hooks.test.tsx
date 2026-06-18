import type { ReactNode } from 'react'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'

import { server } from '@/test/msw/server'
import { metricasUsoFixture } from '@/test/msw/handlers'
import { useDocenteUsoDetalle, useMetricasUso } from './hooks'

const BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

function crearWrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  )
}

describe('useMetricasUso (MSW contra el contrato)', () => {
  it('devuelve el resumen de uso del mes con docentes ordenados por costo', async () => {
    const { result } = renderHook(() => useMetricasUso('2026-06'), { wrapper: crearWrapper() })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data?.costoTotal).toBe(metricasUsoFixture.costoTotal)
    expect(result.current.data?.docentes?.[0].nombre).toBe('Ada Lovelace')
  })

  it('propaga la bandera de umbral superado', async () => {
    server.use(
      http.get(`${BASE}/api/admin/metricas/uso`, () =>
        HttpResponse.json({ ...metricasUsoFixture, umbralSuperado: true }),
      ),
    )
    const { result } = renderHook(() => useMetricasUso('2026-06'), { wrapper: crearWrapper() })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data?.umbralSuperado).toBe(true)
  })

  it('propaga un error de la API', async () => {
    server.use(
      http.get(`${BASE}/api/admin/metricas/uso`, () =>
        HttpResponse.json({ status: 500, title: 'Error' }, { status: 500 }),
      ),
    )
    const { result } = renderHook(() => useMetricasUso('2026-06'), { wrapper: crearWrapper() })
    await waitFor(() => expect(result.current.isError).toBe(true))
  })
})

describe('useDocenteUsoDetalle (MSW contra el contrato)', () => {
  it('devuelve el desglose por modelo y por operación', async () => {
    const { result } = renderHook(
      () => useDocenteUsoDetalle('d1111111-1111-1111-1111-111111111111', '2026-06'),
      { wrapper: crearWrapper() },
    )
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data?.porModelo).toHaveLength(2)
    expect(result.current.data?.porOperacion?.map((o) => o.operacion)).toEqual([
      'EVALUACION',
      'NARRATIVA',
    ])
  })

  it('no consulta sin docenteId (query deshabilitada)', async () => {
    const { result } = renderHook(() => useDocenteUsoDetalle('', '2026-06'), {
      wrapper: crearWrapper(),
    })
    expect(result.current.fetchStatus).toBe('idle')
    expect(result.current.isPending).toBe(true)
  })
})
