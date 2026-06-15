package com.classroomiq.backend.entrega.procesamiento;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Habilita {@link ChunkingProperties} para el módulo de procesamiento de entregas. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ChunkingProperties.class)
class ProcesamientoConfig {
}
