package com.classroomiq.backend.evaluacion.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.classroomiq.backend.evaluacion.EvaluacionService;
import com.classroomiq.backend.evaluacion.dto.EvaluacionLoteResponse;

/**
 * Dispara la generación de borradores de evaluación de un lote (Fase 4). Paso explícito posterior al
 * indexado: el docente procesa el lote (Fase 3) y luego lo evalúa. El progreso por entrega llega por
 * el stream SSE existente del lote ({@code GET /api/lotes/{id}/eventos}), que ya emite las
 * transiciones EVALUANDO/LISTO publicadas por el worker.
 */
@RestController
@RequestMapping("/api/lotes")
@PreAuthorize("hasRole('DOCENTE')")
public class EvaluacionController {

    private final EvaluacionService evaluacion;

    public EvaluacionController(EvaluacionService evaluacion) {
        this.evaluacion = evaluacion;
    }

    @PostMapping("/{id}/evaluar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EvaluacionLoteResponse evaluar(@PathVariable UUID id) {
        return new EvaluacionLoteResponse(evaluacion.evaluarLote(id));
    }
}
