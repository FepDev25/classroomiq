package com.classroomiq.backend.metricas.costo;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Habilita {@link CostoProperties} (tarifas y umbral del costo estimado, Fase 6). */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CostoProperties.class)
class CostoConfig {
}
