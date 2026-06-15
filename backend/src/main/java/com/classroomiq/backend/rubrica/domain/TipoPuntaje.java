package com.classroomiq.backend.rubrica.domain;

/**
 * Forma en que un nivel de desempeño expresa su puntaje (ver SCHEMA.md):
 * RANGO: rango de puntos absolutos [puntajeMin, puntajeMax].
 * FIJO: un único valor de puntos (puntajeValor).
 * BANDA_PCT: banda porcentual [pctMin, pctMax] del puntaje máximo del criterio.
 */
public enum TipoPuntaje {
    RANGO,
    FIJO,
    BANDA_PCT
}
