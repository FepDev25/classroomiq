package com.classroomiq.backend.similitud.calculo;

import java.util.UUID;

import com.classroomiq.backend.similitud.domain.TipoSimilitud;

/**
 * Coincidencia entre un fragmento de la entrega A y uno de la entrega B, con su grado de similitud.
 * Es el material para la visualización lado a lado del par. {@code tipo} indica de qué análisis
 * (semántico o textual) provino la coincidencia.
 */
public record MatchFragmento(
        TipoSimilitud tipo,
        UUID fragmentoAId,
        String textoA,
        UUID fragmentoBId,
        String textoB,
        double similitud) {
}
