package com.classroomiq.backend.evaluacion.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Edición de un criterio por el docente en la revisión. Lleva el estado completo de los campos
 * editables: cada llamada los reemplaza (un valor nulo limpia el campo correspondiente).
 *
 * @param nivelFinalId         nivel elegido por el docente (debe ser uno del criterio; null para limpiar)
 * @param puntajeFinal         puntaje fijado por el docente (validado contra el rango; null para limpiar)
 * @param justificacionEditada justificación reescrita (null para limpiar)
 * @param revisadoManual       si el docente marca el criterio como revisado
 */
public record CriterioRevisionRequest(
        UUID nivelFinalId,
        BigDecimal puntajeFinal,
        String justificacionEditada,
        boolean revisadoManual) {
}
