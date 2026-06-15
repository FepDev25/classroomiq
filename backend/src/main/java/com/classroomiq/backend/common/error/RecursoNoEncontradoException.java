package com.classroomiq.backend.common.error;

/** Recurso inexistente o fuera del alcance del tenant/docente. Se traduce a HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}
