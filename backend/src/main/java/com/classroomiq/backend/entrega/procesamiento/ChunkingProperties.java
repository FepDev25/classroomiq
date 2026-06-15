package com.classroomiq.backend.entrega.procesamiento;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Configuración del chunking (prefijo {@code app.chunking}). El presupuesto se mide en caracteres
 * (aproximación a tokens: ~4 chars/token); {@code maxChars} de 2000 ≈ 500 tokens, dentro del
 * contexto de bge-m3, y {@code overlapChars} de 300 ≈ 15% de solape.
 *
 * @param maxChars     tamaño objetivo máximo de un chunk en caracteres
 * @param overlapChars solape entre chunks consecutivos, en caracteres
 */
@ConfigurationProperties(prefix = "app.chunking")
@Validated
public record ChunkingProperties(@Positive int maxChars, @PositiveOrZero int overlapChars) {
}
