package com.classroomiq.backend.metricas.repository;

import com.classroomiq.backend.metricas.domain.OperacionLlm;

/**
 * Proyección del desglose del uso del LLM de un docente por modelo y operación (Fase 6), para el
 * detalle del portal admin. De una sola consulta se derivan las vistas por-modelo (para el costo)
 * y por-operación (evaluación vs narrativa).
 */
public interface UsoModeloOperacion {

    String getModelo();

    OperacionLlm getOperacion();

    long getInputTokens();

    long getOutputTokens();
}
