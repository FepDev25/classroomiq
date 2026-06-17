package com.classroomiq.backend.metricas.costo;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Tarifas y umbral para estimar el costo del uso del LLM (Fase 6, prefijo {@code app.costos}).
 *
 * <p>El costo NO se persiste: se calcula on-read desde estas tarifas porque los tokens son el hecho
 * inmutable y el precio es una estimación que puede cambiar. {@code tarifas} mapea el id del modelo
 * (ej. {@code claude-sonnet-4-6}) a su precio por millón de tokens de entrada y de salida.
 *
 * @param moneda        moneda de las tarifas (informativa, ej. {@code USD})
 * @param umbralMensual costo total institucional del mes que dispara la alerta del portal admin
 * @param tarifas       precio por modelo (input/output por millón de tokens)
 */
@ConfigurationProperties(prefix = "app.costos")
@Validated
public record CostoProperties(
        @NotBlank String moneda,
        @NotNull @PositiveOrZero BigDecimal umbralMensual,
        @NotNull Map<String, Tarifa> tarifas) {

    /**
     * @param inputPorMillon  precio por 1.000.000 de tokens de entrada
     * @param outputPorMillon precio por 1.000.000 de tokens de salida
     */
    public record Tarifa(
            @NotNull @PositiveOrZero BigDecimal inputPorMillon,
            @NotNull @PositiveOrZero BigDecimal outputPorMillon) {
    }
}
