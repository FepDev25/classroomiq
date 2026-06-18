package com.classroomiq.backend.materia.dto;

import java.util.UUID;

/**
 * Materia vista por el admin institucional: incluye el docente dueño para que el admin pueda
 * elegirla al asignar materias a coordinadores. El admin no ve el contenido (rúbricas/entregas).
 */
public record AdminMateriaResponse(
        UUID id,
        String nombre,
        String periodoAcademico,
        UUID docenteId,
        String docenteNombre,
        boolean archivada) {
}
