package com.classroomiq.backend.provider.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Configuración del LLM (prefijo {@code app.llm}).
 *
 * <p>{@code @Validated} hace fail-fast al arrancar si falta una propiedad obligatoria.
 *
 * @param modeloPotente   modelo para el análisis de evaluación (ej. {@code claude-sonnet-4-6})
 * @param modeloEconomico modelo para tareas simples (ej. {@code claude-haiku-4-5})
 * @param maxTokens       tope de tokens de salida por respuesta
 * @param effort          nivel de esfuerzo para el tier potente ({@code low|medium|high|max});
 *                        no se aplica al tier económico (Haiku no soporta el parámetro)
 * @param verifyOnStartup si es {@code true}, al arrancar se hace una llamada sonda al proveedor
 *                        (ver {@link LlmStartupVerifier}); off por defecto para no atar el
 *                        arranque ni los tests a la disponibilidad/costo del proveedor
 * @param anthropic       configuración específica del proveedor Anthropic
 */
@ConfigurationProperties(prefix = "app.llm")
@Validated
public record LlmProperties(
        @NotBlank String provider,
        @NotBlank String modeloPotente,
        @NotBlank String modeloEconomico,
        @Positive int maxTokens,
        @NotBlank String effort,
        boolean verifyOnStartup,
        @NotNull @Valid Anthropic anthropic) {

    /**
     * @param apiKey clave de API de Anthropic (desde {@code ANTHROPIC_API_KEY} en el {@code .env}).
     *               Opcional en dev/test para no atar el arranque del contexto a tener clave; en
     *               prod es obligatoria vía {@code application-prod.yml} ({@code ${ANTHROPIC_API_KEY}}
     *               sin default → falla al arrancar si falta). Una llamada real sin clave da 401.
     */
    public record Anthropic(String apiKey) {
    }
}
