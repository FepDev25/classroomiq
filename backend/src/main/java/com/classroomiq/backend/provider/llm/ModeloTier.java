package com.classroomiq.backend.provider.llm;

/**
 * Nivel de modelo a usar para una solicitud (estrategia "potente + económico" del CLAUDE.md).
 *
 * <p>POTENTE: modelo fuerte para el análisis de evaluación por criterio (calidad del borrador).
 * ECONOMICO: modelo barato para tareas simples (clasificación/extracción) que aparecen en fases
 * posteriores. El mapeo a un modelo concreto lo resuelve {@link LlmProvider#modelo(ModeloTier)}
 * desde la configuración ({@code app.llm.modelo-potente} / {@code app.llm.modelo-economico}).
 */
public enum ModeloTier {
    POTENTE,
    ECONOMICO
}
