package com.classroomiq.backend.entrega.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Habilita {@link StorageProperties} para el módulo de almacenamiento de entregas. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StorageProperties.class)
class StorageConfig {
}
