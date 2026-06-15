package com.classroomiq.backend.provider.embeddings;

/**
 * Fallo al generar embeddings (proveedor no disponible, respuesta inválida o dimensión
 * inesperada). Es no chequeada para no contaminar la firma de {@link EmbeddingProvider}.
 */
public class EmbeddingException extends RuntimeException {

    public EmbeddingException(String message) {
        super(message);
    }

    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }
}
