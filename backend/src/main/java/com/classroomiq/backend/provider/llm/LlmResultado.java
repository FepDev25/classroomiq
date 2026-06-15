package com.classroomiq.backend.provider.llm;

/**
 * Resultado de una llamada al LLM.
 *
 * @param texto      texto generado (concatenación de los bloques de texto de la respuesta)
 * @param modelo     identificador del modelo que produjo la respuesta (para trazas y costo)
 * @param stopReason motivo de finalización del modelo (ej. {@code end_turn}, {@code max_tokens},
 *                   {@code refusal}); nulo si el proveedor no lo informa
 * @param uso        tokens consumidos (para métricas de costo)
 */
public record LlmResultado(String texto, String modelo, String stopReason, UsoTokens uso) {
}
