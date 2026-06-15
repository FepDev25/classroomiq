package com.classroomiq.backend.provider.llm;

/**
 * Solicitud a un {@link LlmProvider}: el tier de modelo, el prompt de sistema (instrucciones de
 * rol/estilo) y el prompt de usuario (el contenido a procesar).
 *
 * <p>El esquema de salida estructurada (JSON por criterio) se añadirá en el hito del motor de
 * evaluación (Fase 4 — H3), donde la forma de la respuesta queda concreta. Por ahora la respuesta
 * es texto libre que el llamador interpreta.
 *
 * @param tier   nivel de modelo (potente para evaluación, económico para tareas simples)
 * @param system prompt de sistema; puede ser nulo o vacío
 * @param prompt prompt de usuario con el contenido a evaluar (no nulo)
 */
public record LlmSolicitud(ModeloTier tier, String system, String prompt) {
}
