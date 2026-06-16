package com.classroomiq.backend.similitud.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/**
 * Petición para generar (o regenerar) el reporte de similitud de un lote.
 *
 * @param umbral umbral de "similitud alta" en {@code [0, 1]}; si es {@code null} se usa el default
 *               configurado ({@code app.similitud.umbral-default}).
 */
public record GenerarSimilitudRequest(
        @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal umbral) {
}
