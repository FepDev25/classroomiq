package com.classroomiq.backend.rubrica.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.classroomiq.backend.rubrica.domain.TipoPuntaje;

public record NivelResponse(
        UUID id,
        String nombre,
        String descripcion,
        TipoPuntaje tipoPuntaje,
        BigDecimal puntajeMin,
        BigDecimal puntajeMax,
        BigDecimal puntajeValor,
        BigDecimal pctMin,
        BigDecimal pctMax,
        int orden) {
}
