package com.classroomiq.backend.materia.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.classroomiq.backend.materia.AdminMateriaService;
import com.classroomiq.backend.materia.dto.AdminMateriaResponse;

/**
 * Catálogo de materias de la institución para el admin, usado para asignar materias a
 * coordinadores (ver {@code AdminCoordinadorController}). Solo lectura, rol ADMIN.
 */
@RestController
@RequestMapping("/api/admin/materias")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMateriaController {

    private final AdminMateriaService materias;

    public AdminMateriaController(AdminMateriaService materias) {
        this.materias = materias;
    }

    @GetMapping
    public List<AdminMateriaResponse> listar() {
        return materias.listarTodas();
    }
}
