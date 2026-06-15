package com.classroomiq.backend.entrega.dto;

import java.util.UUID;

import com.classroomiq.backend.entrega.domain.EstadoEntrega;

/**
 * Carga de un evento SSE de progreso de procesamiento. El {@code loteId} lo conoce el cliente por
 * la URL del stream y el {@code tenantId} no se expone: solo viajan la entrega y su nuevo estado.
 */
public record EntregaEventoResponse(UUID entregaId, EstadoEntrega estado) {
}
