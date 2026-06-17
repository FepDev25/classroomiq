package com.classroomiq.backend.metricas.dto;

import java.math.BigDecimal;

import com.classroomiq.backend.metricas.domain.OperacionLlm;

/** Uso/costo por tipo de operación (evaluación vs narrativa) en el detalle de un docente (Fase 6). */
public record UsoOperacionResponse(
        OperacionLlm operacion,
        long inputTokens,
        long outputTokens,
        long totalTokens,
        BigDecimal costoEstimado) {
}
