package com.classroomiq.backend.common.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.classroomiq.backend.usuario.domain.Rol;

/**
 * Acceso al usuario autenticado del request actual (id, tenant y rol), leído del JWT.
 * Lo usan los servicios para aplicar el aislamiento por docente (ej. materia.docenteId).
 */
@Component
public class AuthContext {

    public UUID requireUserId() {
        return UUID.fromString(jwt().getSubject());
    }

    public UUID requireTenantId() {
        return UUID.fromString(jwt().getClaimAsString("tenant_id"));
    }

    public Rol requireRol() {
        return Rol.valueOf(jwt().getClaimAsString("rol"));
    }

    private Jwt jwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        }
        throw new IllegalStateException("No hay un usuario autenticado en el contexto");
    }
}
