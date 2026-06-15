package com.classroomiq.backend.rubrica.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CriterioResponse(
        UUID id,
        String nombre,
        String descripcion,
        BigDecimal puntajeMaximo,
        boolean evaluablePorContenido,
        int orden,
        List<NivelResponse> niveles) {
}
