package com.classroomiq.backend.common.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de CORS (prefijo {@code app.cors} en application.yml). Solo los orígenes listados
 * pueden llamar a la API desde el navegador — en desarrollo, el dev server del frontend
 * ({@code http://localhost:5173}); en producción, el dominio donde se sirva la SPA.
 *
 * @param allowedOrigins orígenes permitidos (acepta lista YAML o cadena separada por comas)
 */
@ConfigurationProperties(prefix = "app.cors")
public record AppCorsProperties(List<String> allowedOrigins) {
}
