import type { ReactNode } from 'react'
import { act, renderHook } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fetchEventSource } from '@microsoft/fetch-event-source'

import { queryKeys } from '@/api/queryKeys'
import type { Entrega } from '@/features/lotes/api'
import { useEventosLote } from './useEventosLote'

// La librería SSE se mockea: capturamos las opciones para disparar onmessage.
vi.mock('@microsoft/fetch-event-source', () => ({
  fetchEventSource: vi.fn(() => new Promise<void>(() => {})),
}))

const LOTE = 'lote-1'

function setup(entregas: Entrega[]) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  qc.setQueryData(queryKeys.entregas(LOTE), entregas)
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  )
  renderHook(() => useEventosLote(LOTE), { wrapper })
  // El mock acumula llamadas entre tests; usamos la última (la de este setup).
  const llamadas = vi.mocked(fetchEventSource).mock.calls
  const opciones = llamadas[llamadas.length - 1][1]
  const emitir = (entregaId: string, estado: Entrega['estado']) =>
    act(() => opciones.onmessage?.({ data: JSON.stringify({ entregaId, estado }) } as never))
  const leer = () => qc.getQueryData<Entrega[]>(queryKeys.entregas(LOTE))
  return { emitir, leer }
}

describe('useEventosLote', () => {
  it('abre el stream con la URL de eventos del lote', () => {
    setup([])
    const llamadas = vi.mocked(fetchEventSource).mock.calls
    const [url] = llamadas[llamadas.length - 1]
    expect(url).toContain(`/api/lotes/${LOTE}/eventos`)
  })

  it('actualiza el estado de la entrega en el cache por cada evento', () => {
    const { emitir, leer } = setup([
      { id: 'e1', estado: 'PENDIENTE' },
      { id: 'e2', estado: 'PENDIENTE' },
    ] as Entrega[])

    emitir('e1', 'PROCESANDO')
    expect(leer()?.find((e) => e.id === 'e1')?.estado).toBe('PROCESANDO')
    expect(leer()?.find((e) => e.id === 'e2')?.estado).toBe('PENDIENTE')

    emitir('e1', 'LISTO')
    expect(leer()?.find((e) => e.id === 'e1')?.estado).toBe('LISTO')
  })

  it('ignora eventos sin entregaId o estado', () => {
    const { leer } = setup([{ id: 'e1', estado: 'PENDIENTE' }] as Entrega[])
    const opciones = vi.mocked(fetchEventSource).mock.calls[0][1]
    act(() => opciones.onmessage?.({ data: JSON.stringify({ foo: 'bar' }) } as never))
    expect(leer()?.[0].estado).toBe('PENDIENTE')
  })
})
