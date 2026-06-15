package com.classroomiq.backend.entrega.procesamiento;

import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Bus en memoria que reemite los {@link EntregaEstadoEvent} del worker hacia los streams SSE
 * (Hito 6). Recibe los eventos vía {@link EventListener} (en el hilo del worker que los publica)
 * y los multiplexa con un {@link Sinks.Many} a todos los suscriptores activos.
 *
 * <p>El suscriptor filtra por {@code tenantId} y {@code loteId} con valores capturados en el hilo
 * del request — nunca con el {@code TenantContext} ThreadLocal, que ya no está disponible en el
 * hilo reactivo. Esto garantiza que un tenant solo reciba eventos de sus propios lotes.
 *
 * <p>El sink es {@code directBestEffort}: no replica eventos a suscriptores tardíos (el progreso es
 * en tiempo real) ni se autocancela cuando no hay suscriptores. {@link #onEstado} es
 * {@code synchronized} para serializar las emisiones concurrentes de varios hilos del executor, y
 * usa {@code tryEmitNext} (no lanza) para que un fallo de emisión nunca rompa al worker.
 */
@Component
public class ProcesamientoEventBus {

    private final Sinks.Many<EntregaEstadoEvent> sink = Sinks.many().multicast().directBestEffort();

    @EventListener
    public synchronized void onEstado(EntregaEstadoEvent event) {
        sink.tryEmitNext(event);
    }

    /** Stream de eventos del lote indicado, acotado al tenant del suscriptor. */
    public Flux<EntregaEstadoEvent> suscribir(UUID loteId, UUID tenantId) {
        return sink.asFlux()
                .filter(ev -> ev.tenantId().equals(tenantId) && ev.loteId().equals(loteId));
    }
}
