package com.classroomiq.backend.materia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MateriaRequest(
        @NotBlank String nombre,
        @Size(max = 100) String periodoAcademico,
        @Size(max = 4000) String descripcion) {
}
