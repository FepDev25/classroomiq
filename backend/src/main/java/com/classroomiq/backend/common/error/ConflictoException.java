package com.classroomiq.backend.common.error;

/** Conflicto de negocio (ej. email ya en uso). Se traduce a HTTP 409. */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String message) {
        super(message);
    }
}
