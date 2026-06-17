package com.classroomiq.backend.metricas.domain;

/**
 * Operación de negocio que originó una llamada al LLM (Fase 6). Sirve para desglosar el uso/costo
 * por tipo de tarea en el portal admin y para trazar a qué se debió cada consumo.
 *
 * <p>EVALUACION: motor de evaluación por criterio (tier potente, una llamada por criterio).
 * NARRATIVA: texto narrativo del resumen por grupo (tier económico). Nuevas operaciones con LLM de
 * fases futuras se agregan aquí.
 */
public enum OperacionLlm {
    EVALUACION,
    NARRATIVA
}
