package com.classroomiq.backend.similitud.dto;

import java.util.UUID;

import com.classroomiq.backend.similitud.domain.TipoSimilitud;

/**
 * Par de fragmentos similares para la visualización lado a lado: el texto de la entrega A junto al
 * de la entrega B, con su grado de similitud y el tipo de análisis que lo detectó (semántico/textual).
 */
public record FragmentoSimilarResponse(
        TipoSimilitud tipo,
        UUID fragmentoAId,
        String textoA,
        UUID fragmentoBId,
        String textoB,
        double similitud) {
}
