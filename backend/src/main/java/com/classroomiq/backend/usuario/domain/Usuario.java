package com.classroomiq.backend.usuario.domain;

import java.time.Instant;

import com.classroomiq.backend.common.domain.AbstractTenantEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Cuenta de usuario (admin, docente o coordinador) perteneciente a una institución.
 * No hay self-signup: el admin crea las cuentas.
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
public class Usuario extends AbstractTenantEntity {

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "ultimo_acceso")
    private Instant ultimoAcceso;
}
