package com.classroomiq.backend.entrega.extraccion;

/**
 * Fallo al extraer texto de un archivo de entrega (corrupto, no soportado, o protegido).
 * El pipeline de procesamiento (Hito 5) la captura y marca la entrega en estado ERROR.
 */
public class ExtraccionException extends RuntimeException {

    public ExtraccionException(String message) {
        super(message);
    }

    public ExtraccionException(String message, Throwable cause) {
        super(message, cause);
    }
}
