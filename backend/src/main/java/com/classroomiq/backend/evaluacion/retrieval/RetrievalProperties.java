package com.classroomiq.backend.evaluacion.retrieval;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Positive;

/**
 * Configuración del retrieval semántico por criterio (prefijo {@code app.evaluacion.retrieval}).
 *
 * @param topK cantidad de fragmentos más relevantes que se recuperan por criterio para construir
 *             el prompt del LLM. Un valor mayor da más contexto pero encarece la llamada.
 */
@ConfigurationProperties(prefix = "app.evaluacion.retrieval")
@Validated
public record RetrievalProperties(@Positive int topK) {
}
