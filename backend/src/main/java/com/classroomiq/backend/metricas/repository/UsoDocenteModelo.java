package com.classroomiq.backend.metricas.repository;

import java.util.UUID;

/**
 * Proyección de la agregación del uso del LLM por docente y modelo en un rango de fechas (Fase 6).
 * El costo se calcula on-read sobre estos totales; agrupar por modelo permite aplicar la tarifa
 * correcta a cada uno antes de sumar el costo por docente.
 */
public interface UsoDocenteModelo {

    UUID getDocenteId();

    String getModelo();

    long getInputTokens();

    long getOutputTokens();
}
