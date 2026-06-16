package com.classroomiq.backend.evaluacion.motor;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Forma del JSON que el LLM devuelve al evaluar un criterio. El motor instruye este contrato en el
 * prompt (ver {@link PromptEvaluacion}) y lo parsea con Jackson de forma tolerante
 * ({@link RespuestaEvaluacionParser}).
 *
 * <p>Se mantiene la salida como JSON-en-prompt (no como structured output específico de un
 * proveedor) para no acoplar el motor al SDK de Anthropic: cualquier {@code LlmProvider} —incluido
 * el local self-hosted a futuro— produce texto y este contrato lo interpreta.
 *
 * @param nivel        nombre del nivel de desempeño sugerido (debe coincidir con uno de la rúbrica)
 * @param puntaje      puntaje sugerido dentro del rango del nivel
 * @param justificacion justificación textual objetiva con evidencia del trabajo
 * @param citas        fragmentos textuales del trabajo que sustentan la evaluación
 * @param advertencia  aviso si el contenido es insuficiente para evaluar con confianza; null si no aplica
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EvaluacionLlmRespuesta(
        String nivel,
        BigDecimal puntaje,
        String justificacion,
        List<String> citas,
        String advertencia) {
}
