package com.classroomiq.backend.metricas.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Uso y costo estimado de un docente en el mes (Fase 6). {@code nombre}/{@code email} identifican al
 * docente para el admin; los tokens y el costo son del rango consultado.
 */
public record DocenteUsoResponse(
        UUID docenteId,
        String nombre,
        String email,
        long inputTokens,
        long outputTokens,
        long totalTokens,
        BigDecimal costoEstimado) {
}
