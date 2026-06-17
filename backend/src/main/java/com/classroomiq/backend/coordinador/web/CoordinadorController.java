package com.classroomiq.backend.coordinador.web;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.classroomiq.backend.coordinador.CoordinadorReporteService;
import com.classroomiq.backend.entrega.dto.LoteResponse;
import com.classroomiq.backend.materia.dto.MateriaResponse;
import com.classroomiq.backend.reportes.dto.ResumenGrupoResponse;

/**
 * Acceso de solo lectura del coordinador a los reportes agregados de sus materias asignadas
 * (Fase 5, sección 7). Solo rol COORDINADOR. No expone evaluaciones, trabajos ni similitud.
 */
@RestController
@RequestMapping("/api/coordinador")
@PreAuthorize("hasRole('COORDINADOR')")
public class CoordinadorController {

    private final CoordinadorReporteService reportes;

    public CoordinadorController(CoordinadorReporteService reportes) {
        this.reportes = reportes;
    }

    @GetMapping("/materias")
    public List<MateriaResponse> materias() {
        return reportes.materiasAsignadas();
    }

    @GetMapping("/materias/{materiaId}/lotes")
    public List<LoteResponse> lotes(@PathVariable UUID materiaId) {
        return reportes.lotesDeMateria(materiaId);
    }

    @GetMapping("/lotes/{loteId}/resumen")
    public ResumenGrupoResponse resumen(@PathVariable UUID loteId) {
        return reportes.resumenDeLote(loteId);
    }
}
