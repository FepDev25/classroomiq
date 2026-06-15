package com.classroomiq.backend.usuario.dto;

import java.time.Instant;
import java.util.UUID;

import com.classroomiq.backend.usuario.domain.Rol;

/** Vista de un usuario hacia el admin. No expone el hash de la contraseña. */
public record UsuarioResponse(
        UUID id,
        String email,
        String nombre,
        Rol rol,
        boolean activo,
        Instant ultimoAcceso) {
}
