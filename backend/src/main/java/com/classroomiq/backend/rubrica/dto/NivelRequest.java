package com.classroomiq.backend.rubrica.dto;

import java.math.BigDecimal;

import com.classroomiq.backend.rubrica.domain.TipoPuntaje;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Nivel de desempeño. Según {@code tipoPuntaje} se usan distintos campos de puntaje;
 * la coherencia (campos presentes y dentro de rango) la verifica {@code RubricaValidator}.
 */
public record NivelRequest(
        @NotBlank String nombre,
        String descripcion,
        @NotNull TipoPuntaje tipoPuntaje,
        BigDecimal puntajeMin,
        BigDecimal puntajeMax,
        BigDecimal puntajeValor,
        BigDecimal pctMin,
        BigDecimal pctMax) {
}
