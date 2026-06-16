package com.classroomiq.backend.evaluacion.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Nivel de desempeño disponible para un criterio, con su rango de puntos ya resuelto (las tres
 * formas de puntaje se traducen a [min, max] absolutos). Alimenta el selector de nivel y la
 * validación del puntaje en la pantalla de revisión.
 */
public record NivelOpcionResponse(
        UUID id,
        String nombre,
        String descripcion,
        BigDecimal puntajeMin,
        BigDecimal puntajeMax,
        int orden) {
}
