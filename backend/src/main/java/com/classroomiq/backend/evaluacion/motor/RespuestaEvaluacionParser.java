package com.classroomiq.backend.evaluacion.motor;

import org.springframework.stereotype.Component;

import com.classroomiq.backend.provider.llm.LlmException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Extrae y parsea el JSON que el LLM produce al evaluar un criterio.
 *
 * <p>Es tolerante con el envoltorio habitual de los modelos: recorta texto antes/después del objeto
 * JSON y los bloques de código markdown (```json ... ```), quedándose con el primer objeto
 * balanceado {@code { ... }}. Así no depende de que el modelo responda "solo JSON" al pie de la letra.
 */
@Component
public class RespuestaEvaluacionParser {

    private final ObjectMapper objectMapper;

    public RespuestaEvaluacionParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EvaluacionLlmRespuesta parsear(String textoLlm) {
        if (textoLlm == null || textoLlm.isBlank()) {
            throw new LlmException("El LLM devolvió una respuesta vacía al evaluar el criterio");
        }
        String json = extraerObjetoJson(textoLlm);
        try {
            return objectMapper.readValue(json, EvaluacionLlmRespuesta.class);
        } catch (Exception e) {
            throw new LlmException("No se pudo parsear el JSON de evaluación del LLM: " + recorte(textoLlm), e);
        }
    }

    /** Devuelve el primer objeto JSON balanceado del texto (ignora prosa y cercos de código). */
    private String extraerObjetoJson(String texto) {
        int inicio = texto.indexOf('{');
        if (inicio < 0) {
            throw new LlmException("La respuesta del LLM no contiene un objeto JSON: " + recorte(texto));
        }
        int profundidad = 0;
        boolean enCadena = false;
        boolean escape = false;
        for (int i = inicio; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (enCadena) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    enCadena = false;
                }
                continue;
            }
            switch (c) {
                case '"' -> enCadena = true;
                case '{' -> profundidad++;
                case '}' -> {
                    profundidad--;
                    if (profundidad == 0) {
                        return texto.substring(inicio, i + 1);
                    }
                }
                default -> {
                    // sin efecto
                }
            }
        }
        throw new LlmException("La respuesta del LLM tiene un objeto JSON sin cerrar: " + recorte(texto));
    }

    private String recorte(String texto) {
        String limpio = texto.strip();
        return limpio.length() <= 300 ? limpio : limpio.substring(0, 300) + "…";
    }
}
