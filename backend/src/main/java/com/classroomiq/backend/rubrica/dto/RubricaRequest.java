package com.classroomiq.backend.rubrica.dto;

import java.math.BigDecimal;
import java.util.List;

import com.classroomiq.backend.rubrica.domain.ModoTotal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RubricaRequest(
        @NotBlank String nombre,
        String descripcion,
        @NotNull @Positive BigDecimal puntajeTotal,
        @NotNull ModoTotal modoTotal,
        @NotEmpty @Valid List<CriterioRequest> criterios) {
}
