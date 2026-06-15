package com.classroomiq.backend.common.error;

/** Violación de una regla de negocio (ej. los puntajes de la rúbrica no cierran). HTTP 422. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String message) {
        super(message);
    }
}
