package com.classroomiq.backend.reportes.web;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.classroomiq.backend.reportes.NarrativaGrupoService;
import com.classroomiq.backend.reportes.ResumenGrupoService;
import com.classroomiq.backend.reportes.dto.ResumenGrupoResponse;

/**
 * Resumen por grupo de un lote (Fase 5, sección 6). Solo el docente dueño del lote (el servicio
 * valida la propiedad; 404 si es ajeno). Se computa sobre las evaluaciones aprobadas del lote.
 */
@RestController
@RequestMapping("/api/lotes")
@PreAuthorize("hasRole('DOCENTE')")
public class ResumenController {

    private final ResumenGrupoService resumen;
    private final NarrativaGrupoService narrativa;

    public ResumenController(ResumenGrupoService resumen, NarrativaGrupoService narrativa) {
        this.resumen = resumen;
        this.narrativa = narrativa;
    }

    @GetMapping("/{id}/resumen")
    public ResumenGrupoResponse obtener(@PathVariable UUID id) {
        return resumen.obtener(id);
    }

    /** Genera (o regenera) el texto narrativo del resumen con el LLM y lo persiste. */
    @PostMapping("/{id}/resumen/narrativa")
    public ResumenGrupoResponse generarNarrativa(@PathVariable UUID id) {
        return narrativa.generar(id);
    }
}
