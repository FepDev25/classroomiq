package com.classroomiq.backend.entrega.procesamiento;

import java.util.UUID;

import com.classroomiq.backend.entrega.domain.EstadoEntrega;

/**
 * Evento de transición de estado de una entrega, publicado por el worker en cada cambio.
 * El stream SSE (Hito 6) lo consume para notificar el progreso en tiempo real.
 */
public record EntregaEstadoEvent(UUID tenantId, UUID loteId, UUID entregaId, EstadoEntrega estado) {
}
