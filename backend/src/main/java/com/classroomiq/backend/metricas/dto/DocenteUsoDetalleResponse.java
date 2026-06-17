package com.classroomiq.backend.metricas.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Detalle de uso/costo de un docente en el mes (Fase 6, portal admin): totales + desglose por modelo
 * (para el costo) y por operación (evaluación vs narrativa).
 */
public record DocenteUsoDetalleResponse(
        UUID docenteId,
        String nombre,
        String email,
        String mes,
        String moneda,
        long totalInputTokens,
        long totalOutputTokens,
        BigDecimal costoTotal,
        List<UsoModeloResponse> porModelo,
        List<UsoOperacionResponse> porOperacion) {
}
