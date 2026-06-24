package com.classroomiq.backend.evaluacion.web;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.classroomiq.backend.evaluacion.RevisionService;
import com.classroomiq.backend.evaluacion.dto.BorradorResponse;
import com.classroomiq.backend.evaluacion.dto.ComentarioRequest;
import com.classroomiq.backend.evaluacion.dto.CriterioRevisionRequest;

import jakarta.validation.Valid;

/**
 * API de revisión del borrador por el docente (Fase 4, Hito 5): leer el borrador de una entrega,
 * editar cada criterio, fijar el comentario general y aprobar la evaluación. Rol DOCENTE; el scoping
 * por docente y el congelado tras la aprobación los aplica {@link RevisionService}.
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('DOCENTE')")
public class RevisionController {

    private final RevisionService revision;

    public RevisionController(RevisionService revision) {
        this.revision = revision;
    }

    /** Borrador completo de una entrega (criterios + citas) para el panel de revisión. */
    @GetMapping("/entregas/{entregaId}/evaluacion")
    public BorradorResponse obtener(@PathVariable UUID entregaId) {
        return revision.obtenerPorEntrega(entregaId);
    }

    /** Edita un criterio del borrador (puntaje/nivel/justificación/toggle revisado). */
    @PatchMapping("/evaluaciones/{evaluacionId}/criterios/{criterioId}")
    public BorradorResponse editarCriterio(@PathVariable UUID evaluacionId,
            @PathVariable UUID criterioId, @Valid @RequestBody CriterioRevisionRequest request) {
        return revision.editarCriterio(evaluacionId, criterioId, request);
    }

    /** Fija el comentario general para el estudiante. */
    @PatchMapping("/evaluaciones/{evaluacionId}/comentario")
    public BorradorResponse editarComentario(@PathVariable UUID evaluacionId,
            @Valid @RequestBody ComentarioRequest request) {
        return revision.editarComentario(evaluacionId, request);
    }

    /** Aprueba la evaluación: recalcula el total final y la congela. */
    @PostMapping("/evaluaciones/{evaluacionId}/aprobar")
    public BorradorResponse aprobar(@PathVariable UUID evaluacionId) {
        return revision.aprobar(evaluacionId);
    }
}
