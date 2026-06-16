package com.classroomiq.backend.evaluacion.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.classroomiq.backend.evaluacion.domain.EstadoEvaluacion;

/**
 * Borrador de evaluación completo de una entrega para la pantalla de revisión: el estado, los
 * totales (sugerido y final), el comentario general y todos los criterios con sus citas.
 */
public record BorradorResponse(
        UUID id,
        UUID entregaId,
        UUID rubricaId,
        EstadoEvaluacion estado,
        BigDecimal puntajeTotalSugerido,
        BigDecimal puntajeTotalFinal,
        String comentarioGeneral,
        Instant aprobadaAt,
        List<CriterioEvaluadoResponse> criterios) {
}
