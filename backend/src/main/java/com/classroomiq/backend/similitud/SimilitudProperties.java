package com.classroomiq.backend.similitud;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;

/**
 * Configuración de la detección de similitud (prefijo {@code app.similitud}).
 *
 * @param umbralDefault umbral de "similitud alta" por defecto cuando el docente no especifica uno
 *                      al generar el reporte; en {@code [0, 1]} (CLAUDE.md: default 0.75). Supera el
 *                      umbral ⇒ el par se marca para revisión manual.
 * @param topFragmentos cantidad de pares de fragmentos de mayor similitud que se guardan por par de
 *                      entregas para la visualización lado a lado.
 */
@ConfigurationProperties(prefix = "app.similitud")
@Validated
public record SimilitudProperties(
        @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal umbralDefault,
        @Positive int topFragmentos) {
}
