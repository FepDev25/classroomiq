package com.classroomiq.backend.entrega.dto;

import java.util.UUID;

import com.classroomiq.backend.entrega.domain.EstadoLote;

public record LoteResponse(
        UUID id,
        UUID materiaId,
        UUID rubricaId,
        String nombre,
        EstadoLote estado) {
}
