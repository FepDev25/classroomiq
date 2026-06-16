package com.classroomiq.backend.similitud.dto;

import java.util.List;
import java.util.UUID;

/**
 * Similitud entre un par de entregas del lote, para la lista ordenada y la matriz de calor.
 *
 * @param similitudTextual {@code null} si alguna entrega no tenía prosa comparable.
 * @param superaUmbral     true si la similitud semántica iguala o supera el umbral del reporte:
 *                         el par se marca para revisión manual (nunca como acusación).
 * @param fragmentos       fragmentos de mayor similitud para la vista lado a lado.
 */
public record ParSimilitudResponse(
        UUID entregaAId,
        UUID entregaBId,
        double similitudSemantica,
        Double similitudTextual,
        boolean superaUmbral,
        List<FragmentoSimilarResponse> fragmentos) {
}
