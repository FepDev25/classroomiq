package com.classroomiq.backend.similitud;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cableado del módulo de similitud (Fase 5): habilita su configuración. Los servicios de cálculo
 * (semántico, textual) y la orquestación/persistencia del reporte se registran por {@code @Service}.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SimilitudProperties.class)
class SimilitudConfig {
}
