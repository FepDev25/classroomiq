package com.classroomiq.backend.entrega.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Configuración del almacenamiento local de archivos de entregas (prefijo {@code app.storage}).
 *
 * @param basePath directorio raíz donde se guardan los archivos, organizados por
 *                 tenant/materia/lote/entrega. Los archivos son sensibles y no salen del servidor.
 */
@ConfigurationProperties(prefix = "app.storage")
@Validated
public record StorageProperties(@NotBlank String basePath) {
}
