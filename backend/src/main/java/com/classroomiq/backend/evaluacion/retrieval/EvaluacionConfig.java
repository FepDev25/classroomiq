package com.classroomiq.backend.evaluacion.retrieval;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cableado del módulo de evaluación: por ahora habilita la configuración del retrieval por criterio.
 * El resto del motor (provider LLM, motor de evaluación) se añade en los hitos siguientes.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RetrievalProperties.class)
class EvaluacionConfig {
}
