package com.classroomiq.backend.evaluacion.dto;

import jakarta.validation.constraints.Size;

/** Comentario general del docente para el estudiante (opcional). */
public record ComentarioRequest(
        @Size(max = 4000) String comentarioGeneral) {
}
