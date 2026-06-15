package com.classroomiq.backend.entrega.dto;

import java.util.UUID;

import com.classroomiq.backend.entrega.domain.RolArchivo;

public record ArchivoResponse(
        UUID id,
        String nombreOriginal,
        String mimeType,
        long tamanoBytes,
        RolArchivo rol,
        int orden) {
}
