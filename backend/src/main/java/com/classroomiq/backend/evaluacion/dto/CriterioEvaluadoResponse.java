package com.classroomiq.backend.evaluacion.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Un criterio dentro del borrador de evaluación, enriquecido con los datos de la rúbrica (nombre,
 * descripción, niveles disponibles) para renderizar el panel de revisión sin consultas extra.
 *
 * <p>Distingue lo que sugirió el motor ({@code nivelSugerido*}, {@code puntajeSugerido},
 * {@code justificacion}, {@code advertencia}) de lo que ajusta el docente ({@code nivelFinalId},
 * {@code puntajeFinal}, {@code justificacionEditada}, {@code revisadoManual}).
 *
 * @param id                  id del criterio evaluado (para los PATCH de edición)
 * @param criterioId          id del criterio de la rúbrica
 * @param nombreCriterio      nombre del criterio
 * @param descripcionCriterio descripción del criterio
 * @param puntajeMaximo       puntaje máximo del criterio
 * @param evaluable           si el motor lo puntúa (false = requiere juicio del docente)
 * @param nivelSugeridoId     nivel sugerido por el motor
 * @param nivelSugeridoNombre nombre del nivel sugerido
 * @param puntajeSugerido     puntaje sugerido por el motor
 * @param justificacion       justificación del motor
 * @param advertencia         aviso del motor (contenido insuficiente, ajustes, etc.)
 * @param nivelFinalId        nivel elegido por el docente
 * @param puntajeFinal        puntaje fijado por el docente
 * @param justificacionEditada justificación reescrita por el docente
 * @param revisadoManual      si el docente lo marcó como revisado
 * @param orden               orden del criterio en la rúbrica
 * @param niveles             niveles disponibles con su rango de puntos
 * @param citas               fragmentos citados como evidencia
 */
public record CriterioEvaluadoResponse(
        UUID id,
        UUID criterioId,
        String nombreCriterio,
        String descripcionCriterio,
        BigDecimal puntajeMaximo,
        boolean evaluable,
        UUID nivelSugeridoId,
        String nivelSugeridoNombre,
        BigDecimal puntajeSugerido,
        String justificacion,
        String advertencia,
        UUID nivelFinalId,
        BigDecimal puntajeFinal,
        String justificacionEditada,
        boolean revisadoManual,
        int orden,
        List<NivelOpcionResponse> niveles,
        List<CitaResponse> citas) {
}
