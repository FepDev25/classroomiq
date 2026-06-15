package com.classroomiq.backend.provider.llm;

/**
 * Fallo al invocar el proveedor de LLM (red, autenticación, respuesta inválida o rechazo de
 * seguridad). Es no chequeada para no contaminar la firma de {@link LlmProvider}.
 */
public class LlmException extends RuntimeException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
