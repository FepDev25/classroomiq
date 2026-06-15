package com.classroomiq.backend.evaluacion.domain;

/**
 * Estado del borrador de evaluación de una entrega.
 * BORRADOR: el motor generó la propuesta y el docente la está revisando/editando.
 * APROBADA: el docente aprobó la evaluación final (queda congelada como definitiva).
 */
public enum EstadoEvaluacion {
    BORRADOR,
    APROBADA
}
