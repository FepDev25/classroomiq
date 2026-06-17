package com.classroomiq.backend.coordinador.web;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.classroomiq.backend.coordinador.AsignacionCoordinadorService;
import com.classroomiq.backend.materia.dto.MateriaResponse;

/**
 * Gestión de asignaciones materia↔coordinador por el admin institucional (Fase 5, sección 7).
 * Asignar materias a un coordinador le da acceso de solo lectura a sus reportes agregados.
 */
@RestController
@RequestMapping("/api/admin/coordinadores")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCoordinadorController {

    private final AsignacionCoordinadorService asignaciones;

    public AdminCoordinadorController(AsignacionCoordinadorService asignaciones) {
        this.asignaciones = asignaciones;
    }

    @GetMapping("/{coordinadorId}/materias")
    public List<MateriaResponse> listar(@PathVariable UUID coordinadorId) {
        return asignaciones.listar(coordinadorId);
    }

    @PostMapping("/{coordinadorId}/materias/{materiaId}")
    public List<MateriaResponse> asignar(@PathVariable UUID coordinadorId, @PathVariable UUID materiaId) {
        return asignaciones.asignar(coordinadorId, materiaId);
    }

    @DeleteMapping("/{coordinadorId}/materias/{materiaId}")
    public List<MateriaResponse> desasignar(@PathVariable UUID coordinadorId, @PathVariable UUID materiaId) {
        return asignaciones.desasignar(coordinadorId, materiaId);
    }
}
