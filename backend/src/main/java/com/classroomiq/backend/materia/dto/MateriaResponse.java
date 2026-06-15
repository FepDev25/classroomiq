package com.classroomiq.backend.materia.dto;

import java.util.UUID;

public record MateriaResponse(
        UUID id,
        String nombre,
        String periodoAcademico,
        String descripcion,
        boolean archivada) {
}
