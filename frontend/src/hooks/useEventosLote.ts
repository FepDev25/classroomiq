import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { fetchEventSource } from '@microsoft/fetch-event-source'

import { baseUrl } from '@/api/client'
import { queryKeys } from '@/api/queryKeys'
import { getToken } from '@/features/auth/session'
import { ESTADOS_TERMINALES, type Entrega } from '@/features/lotes/api'
import type { components } from '@/api/schema'

type EntregaEvento = components['schemas']['EntregaEventoResponse']

/**
 * Abre el stream SSE de estado de un lote y mantiene el cache de entregas en
 * vivo. La `EventSource` nativa no permite el header `Authorization`, así que
 * usamos `@microsoft/fetch-event-source` (SSE sobre fetch, con headers, abort
 * y reconexión). Cada evento `EntregaEventoResponse` actualiza el estado de la
 * entrega en el cache; al alcanzar un estado terminal se invalida la query para
 * traer datos que el evento no incluye (`mensajeError`, archivos finales).
 *
 * El stream se mantiene mientras el componente esté montado y `enabled` sea
 * true; se aborta al desmontar. Se evita `EventSource` y cualquier query en el
 * hilo del stream — solo escrituras de cache.
 */
export function useEventosLote(loteId: string, enabled = true) {
  const qc = useQueryClient()

  useEffect(() => {
    if (!enabled || !loteId) return

    const controller = new AbortController()
    const token = getToken()

    fetchEventSource(`${baseUrl}/api/lotes/${loteId}/eventos`, {
      signal: controller.signal,
      // El navegador suele suspender fetch en pestañas ocultas; lo mantenemos
      // abierto para no perder transiciones mientras el docente cambia de pestaña.
      openWhenHidden: true,
      headers: token ? { Authorization: `Bearer ${token}` } : undefined,
      onmessage(ev) {
        if (!ev.data) return
        let evento: EntregaEvento
        try {
          evento = JSON.parse(ev.data) as EntregaEvento
        } catch {
          return
        }
        if (!evento.entregaId || !evento.estado) return

        const estado = evento.estado
        qc.setQueryData<Entrega[]>(queryKeys.entregas(loteId), (entregas) =>
          entregas?.map((e) => (e.id === evento.entregaId ? { ...e, estado } : e)),
        )

        // El evento no trae mensajeError ni los archivos finales: al cerrar una
        // transición, revalidamos la entrega completa desde el backend.
        if (ESTADOS_TERMINALES.has(estado)) {
          qc.invalidateQueries({ queryKey: queryKeys.entregas(loteId) })
          qc.invalidateQueries({ queryKey: queryKeys.lote(loteId) })
        }
      },
      onerror(err) {
        // Devolver (no lanzar) deja que la librería reintente con backoff.
        // Lanzar abortaría el stream definitivamente.
        console.warn('SSE del lote interrumpido, reintentando…', err)
      },
    }).catch(() => {
      // Abort al desmontar entra aquí; no es un error accionable.
    })

    return () => controller.abort()
  }, [loteId, enabled, qc])
}
