package com.classroomiq.backend.entrega.dto;

import java.util.List;
import java.util.UUID;

import com.classroomiq.backend.entrega.domain.EstadoEntrega;
import com.classroomiq.backend.entrega.domain.TipoEntrega;

public record EntregaResponse(
        UUID id,
        UUID loteId,
        String identificadorEstudiante,
        TipoEntrega tipo,
        EstadoEntrega estado,
        String mensajeError,
        List<ArchivoResponse> archivos) {
}
