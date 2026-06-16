package com.classroomiq.backend.similitud.web;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.classroomiq.backend.similitud.SimilitudService;
import com.classroomiq.backend.similitud.dto.GenerarSimilitudRequest;
import com.classroomiq.backend.similitud.dto.ReporteSimilitudResponse;

/**
 * Reporte de similitud de un lote (Fase 5). Solo el docente dueño del lote: el servicio valida la
 * propiedad (404 si es ajeno). Generar es un paso explícito posterior a procesar las entregas; el
 * cálculo es síncrono (aritmética sobre embeddings/texto ya persistidos, sin llamadas externas).
 */
@RestController
@RequestMapping("/api/lotes")
@PreAuthorize("hasRole('DOCENTE')")
public class SimilitudController {

    private final SimilitudService similitud;

    public SimilitudController(SimilitudService similitud) {
        this.similitud = similitud;
    }

    @PostMapping("/{id}/similitud")
    public ReporteSimilitudResponse generar(@PathVariable UUID id,
            @Valid @RequestBody(required = false) GenerarSimilitudRequest request) {
        return similitud.generar(id, request != null ? request.umbral() : null);
    }

    @GetMapping("/{id}/similitud")
    public ReporteSimilitudResponse obtener(@PathVariable UUID id) {
        return similitud.obtener(id);
    }
}
