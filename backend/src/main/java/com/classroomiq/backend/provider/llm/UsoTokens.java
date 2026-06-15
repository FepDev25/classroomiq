package com.classroomiq.backend.provider.llm;

/**
 * Tokens consumidos por una llamada al LLM. Es la base para las métricas de uso y costo del
 * portal admin (Fase 6): se acumula por docente/institución para estimar el gasto del mes.
 *
 * @param inputTokens  tokens de entrada (prompt) procesados
 * @param outputTokens tokens de salida generados
 */
public record UsoTokens(long inputTokens, long outputTokens) {
}
