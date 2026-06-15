package com.classroomiq.backend.rubrica.domain;

/**
 * Cómo se calcula el puntaje total de una rúbrica a partir de sus criterios.
 * SUMA: total = suma de los puntajes de los criterios.
 * PROMEDIO: total = promedio de los criterios (reescalado al puntaje total).
 */
public enum ModoTotal {
    SUMA,
    PROMEDIO
}
