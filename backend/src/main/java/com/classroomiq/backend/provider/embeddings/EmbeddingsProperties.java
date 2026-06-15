package com.classroomiq.backend.provider.embeddings;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Configuración de embeddings (prefijo {@code app.embeddings}).
 *
 * <p>{@code @Validated} hace fail-fast al arrancar si falta una propiedad obligatoria.
 *
 * @param provider        proveedor activo (por ahora {@code ollama})
 * @param dimension       dimensión de los vectores; debe coincidir con la columna {@code vector(N)}
 * @param verifyOnStartup si es {@code true}, al arrancar se contacta al proveedor y se valida la
 *                        dimensión real (ver {@link EmbeddingsStartupVerifier}); off por defecto
 *                        para no atar el arranque/los tests a la disponibilidad del proveedor
 * @param ollama          configuración específica de Ollama
 */
@ConfigurationProperties(prefix = "app.embeddings")
@Validated
public record EmbeddingsProperties(
        @NotBlank String provider,
        @Positive int dimension,
        boolean verifyOnStartup,
        @NotNull @Valid Ollama ollama) {

    /**
     * @param baseUrl   URL base del servidor Ollama (ej. {@code http://localhost:11434})
     * @param model     modelo de embeddings (ej. {@code bge-m3})
     * @param timeout   timeout de conexión y lectura por llamada
     * @param batchSize cantidad máxima de textos por petición a {@code /api/embed}
     */
    public record Ollama(
            @NotBlank String baseUrl,
            @NotBlank String model,
            @NotNull Duration timeout,
            @Positive int batchSize) {
    }
}
