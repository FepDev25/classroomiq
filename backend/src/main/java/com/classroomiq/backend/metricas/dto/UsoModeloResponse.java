package com.classroomiq.backend.metricas.dto;

import java.math.BigDecimal;

/** Uso/costo de un modelo concreto en el detalle de un docente (Fase 6). */
public record UsoModeloResponse(
        String modelo,
        long inputTokens,
        long outputTokens,
        long totalTokens,
        BigDecimal costoEstimado) {
}
