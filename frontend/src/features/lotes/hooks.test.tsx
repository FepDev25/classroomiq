import type { ReactNode } from 'react'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'

import { server } from '@/test/msw/server'
import { lotesFixture } from '@/test/msw/handlers'
import { useLotes } from './hooks'

// Cliente estable por test: crearlo dentro del wrapper lo recrearía en cada
// render y la query nunca se estabilizaría.
function crearWrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  )
}

describe('useLotes (contra el contrato mockeado con MSW)', () => {
  it('devuelve los lotes del docente', async () => {
    const { result } = renderHook(() => useLotes(), { wrapper: crearWrapper() })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toHaveLength(lotesFixture.length)
    expect(result.current.data?.[0].nombre).toBe('Proyecto Final — Grupo A')
  })

  it('propaga un error de la API', async () => {
    server.use(
      http.get(`${import.meta.env.VITE_API_URL}/api/lotes`, () =>
        HttpResponse.json({ status: 500, title: 'Error' }, { status: 500 }),
      ),
    )
    const { result } = renderHook(() => useLotes(), { wrapper: crearWrapper() })
    await waitFor(() => expect(result.current.isError).toBe(true))
  })
})
