import type { ReactNode } from 'react'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { http, HttpResponse } from 'msw'

import { server } from '@/test/msw/server'
import { contenidoEntregaFixture } from '@/test/msw/handlers'
import { useContenidoEntrega } from './hooks'

const BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

function crearWrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  )
}

describe('useContenidoEntrega (MSW contra el contrato)', () => {
  it('devuelve el contenido reconstruido de la entrega', async () => {
    const { result } = renderHook(() => useContenidoEntrega('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'), {
      wrapper: crearWrapper(),
    })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data?.archivos?.[0].nombreOriginal).toBe(
      contenidoEntregaFixture.archivos?.[0].nombreOriginal,
    )
    expect(result.current.data?.archivos?.[0].secciones?.[0].texto).toContain('analiza el problema')
  })

  it('no consulta sin entregaId (query deshabilitada)', () => {
    const { result } = renderHook(() => useContenidoEntrega(''), { wrapper: crearWrapper() })
    expect(result.current.fetchStatus).toBe('idle')
  })

  it('propaga un error de la API (fallback al panel de fragmentos)', async () => {
    server.use(
      http.get(`${BASE}/api/entregas/:id/contenido`, () =>
        HttpResponse.json({ status: 500, title: 'Error' }, { status: 500 }),
      ),
    )
    const { result } = renderHook(() => useContenidoEntrega('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'), {
      wrapper: crearWrapper(),
    })
    await waitFor(() => expect(result.current.isError).toBe(true))
  })
})
