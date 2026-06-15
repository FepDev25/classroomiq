package com.classroomiq.backend.auth.dto;

import java.util.UUID;

import com.classroomiq.backend.usuario.domain.Rol;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UUID usuarioId,
        Rol rol) {
}
