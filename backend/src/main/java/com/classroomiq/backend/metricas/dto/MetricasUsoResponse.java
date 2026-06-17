package com.classroomiq.backend.metricas.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Métricas de uso/costo del LLM de la institución para un mes (Fase 6, portal admin). Desglose por
 * docente + total institucional + bandera de alerta si el costo supera el umbral configurado.
 *
 * @param mes             mes consultado en formato {@code YYYY-MM}
 * @param moneda          moneda de las tarifas (ej. {@code USD})
 * @param umbralMensual   umbral configurado que dispara la alerta
 * @param umbralSuperado  true si {@code costoTotal} supera el umbral
 * @param costoTotal      costo estimado total de la institución en el mes
 * @param totalInputTokens  tokens de entrada de toda la institución en el mes
 * @param totalOutputTokens tokens de salida de toda la institución en el mes
 * @param docentes        uso/costo desglosado por docente (orden descendente por costo)
 */
public record MetricasUsoResponse(
        String mes,
        String moneda,
        BigDecimal umbralMensual,
        boolean umbralSuperado,
        BigDecimal costoTotal,
        long totalInputTokens,
        long totalOutputTokens,
        List<DocenteUsoResponse> docentes) {
}
