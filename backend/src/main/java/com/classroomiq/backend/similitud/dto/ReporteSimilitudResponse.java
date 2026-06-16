package com.classroomiq.backend.similitud.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Reporte de similitud de un lote: pares ordenados de mayor a menor similitud semántica (lista +
 * insumo de la matriz de calor) y, para los pares marcados, los fragmentos de mayor similitud.
 *
 * <p>{@code aviso} es el mensaje fijo no-acusatorio: el sistema señala similitud para revisión
 * manual, no determina deshonestidad académica. {@code entregas} da los rótulos (identificador del
 * estudiante) para la visualización.
 */
public record ReporteSimilitudResponse(
        UUID loteId,
        double umbral,
        Instant generadoAt,
        String aviso,
        List<EntregaResumen> entregas,
        List<ParSimilitudResponse> pares) {

    /** Rótulo de una entrega en el reporte. */
    public record EntregaResumen(UUID entregaId, String identificador) {
    }
}
