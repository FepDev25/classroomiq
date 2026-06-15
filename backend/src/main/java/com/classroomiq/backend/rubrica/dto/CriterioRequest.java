package com.classroomiq.backend.rubrica.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Criterio de la rúbrica. {@code evaluablePorContenido} es opcional (por defecto true).
 * El orden lo determina la posición en la lista.
 */
public record CriterioRequest(
        @NotBlank String nombre,
        String descripcion,
        @NotNull @Positive BigDecimal puntajeMaximo,
        Boolean evaluablePorContenido,
        @NotEmpty @Valid List<NivelRequest> niveles) {
}
