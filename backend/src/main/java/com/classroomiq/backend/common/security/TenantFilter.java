package com.classroomiq.backend.common.security;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.classroomiq.backend.common.tenant.TenantContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Puebla el {@link TenantContext} a partir del claim {@code tenant_id} del JWT autenticado,
 * y lo limpia al finalizar el request. Se inserta en la cadena de seguridad después de la
 * autenticación del bearer token (ver {@code SecurityConfig}); no es un @Component para
 * evitar que Spring Boot lo registre además como filtro global del servlet.
 */
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                String tenantId = jwtAuth.getToken().getClaimAsString("tenant_id");
                if (tenantId != null) {
                    TenantContext.set(UUID.fromString(tenantId));
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
