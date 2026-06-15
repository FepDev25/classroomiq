package com.classroomiq.backend.usuario.dto;

import com.classroomiq.backend.usuario.domain.Rol;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta de cuenta por el admin. El rol debe ser DOCENTE o COORDINADOR (no ADMIN);
 * eso se valida en el servicio.
 */
public record CreateUsuarioRequest(
        @NotBlank @Email String email,
        @NotBlank String nombre,
        @NotBlank @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres") String password,
        @NotNull Rol rol) {
}
