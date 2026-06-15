package com.classroomiq.backend.entrega.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoteRequest(
        @NotNull UUID materiaId,
        @NotNull UUID rubricaId,
        @NotBlank @Size(max = 255) String nombre) {
}
