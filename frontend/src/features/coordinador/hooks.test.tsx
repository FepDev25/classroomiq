import type { ReactNode } from 'react'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'

import { server } from '@/test/msw/server'
import { lotesFixture, materiasFixture, resumenGrupoFixture } from '@/test/msw/handlers'
import { useLotes, useMaterias, useResumen } from './hooks'

const BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

function crearWrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  )
}

describe('coordinador hooks (MSW contra el contrato)', () => {
  it('useMaterias devuelve las materias asignadas', async () => {
    const { result } = renderHook(() => useMaterias(), { wrapper: crearWrapper() })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toHaveLength(materiasFixture.length)
  })

  it('useLotes devuelve los lotes de la materia', async () => {
    const { result } = renderHook(() => useLotes('11111111-1111-1111-1111-111111111111'), {
      wrapper: crearWrapper(),
    })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toHaveLength(lotesFixture.length)
  })

  it('useResumen devuelve el resumen agregado del lote', async () => {
    const { result } = renderHook(() => useResumen('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'), {
      wrapper: crearWrapper(),
    })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data?.narrativa).toBe(resumenGrupoFixture.narrativa)
  })

  it('useResumen no reintenta ante 422 (lote incompleto)', async () => {
    let llamadas = 0
    server.use(
      http.get(`${BASE}/api/coordinador/lotes/:loteId/resumen`, () => {
        llamadas += 1
        return HttpResponse.json({ status: 422, title: 'Lote incompleto' }, { status: 422 })
      }),
    )
    const { result } = renderHook(() => useResumen('zzzz'), { wrapper: crearWrapper() })
    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(llamadas).toBe(1)
  })
})
