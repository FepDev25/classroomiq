package com.classroomiq.backend.rubrica.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.classroomiq.backend.rubrica.domain.ModoTotal;

public record RubricaResponse(
        UUID id,
        UUID materiaId,
        String nombre,
        String descripcion,
        BigDecimal puntajeTotal,
        ModoTotal modoTotal,
        List<CriterioResponse> criterios) {
}
